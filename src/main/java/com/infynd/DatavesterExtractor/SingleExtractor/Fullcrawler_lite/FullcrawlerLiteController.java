package com.infynd.DatavesterExtractor.SingleExtractor.Fullcrawler_lite;

import com.infynd.DatavesterExtractor.dto.CrawlResultDto;
import com.infynd.DatavesterExtractor.dto.TaskMessage;
import com.infynd.DatavesterExtractor.entities.FullcrawlingEntity;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.URL;
import java.net.UnknownHostException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

@Service
@Component
@Slf4j
public class FullcrawlerLiteController {

    @Value("${crawler.worker.id:0}")
    private int workerId;

    @Value("${crawler.worker.total:1}")
    private int totalWorkers;

    @Value("${crawler.batch.size:300}")
    private int batchSize;

    @Value("${crawler.concurrency:80}")
    private int concurrency;

    @Value("${crawler.rabbitmq.exchange:crawler.direct.exchange}")
    private String exchangeName;

    @Value("${crawler.rabbitmq.routing.results:crawler.results.key}")
    private String resultRoutingKey;

    @Autowired(required = false)
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private Extractor extractor;

    @Autowired
    private PageDetails pageDetails;

    @PostConstruct
    public void init() {
        log.info("====================================================================");
        log.info("CRAWLER WORKER INITIALIZED | Worker ID: {}/{} | Batch Size: {} | Concurrency: {}",
                workerId, totalWorkers, batchSize, concurrency);
        log.info("====================================================================");
    }

    /**
     * Distributed Worker Consumer:
     * Listens to crawler.tasks.queue, runs the crawl pipeline, drops rawHtml immediately,
     * and publishes extracted metadata (CrawlResultDto) to crawler.results.queue.
     */
    @RabbitListener(queues = "${crawler.rabbitmq.queue.tasks:crawler.tasks.queue}",
                    concurrency = "${crawler.consumer.concurrency:20-50}")
    public void processQueueTask(TaskMessage task) {
        if (task == null || task.getDomain() == null || task.getDomain().trim().isEmpty()) {
            return;
        }

        try {
            String domain = task.getDomain().trim();
            FullcrawlingEntity fullcrawlingEntity = new FullcrawlingEntity();
            fullcrawlingEntity.setInput(domain);
            fullcrawlingEntity.setDomain(domain);

            FullcrawlingEntity crawledOutput = bingScrape(fullcrawlingEntity, task.getId());
            // Drop raw HTML immediately to keep RAM usage minimal
            crawledOutput.setRawHtml(null);

            if (rabbitTemplate != null) {
                CrawlResultDto resultDto = CrawlResultDto.fromEntity(crawledOutput);
                resultDto.setId(task.getId());
                rabbitTemplate.convertAndSend(exchangeName, resultRoutingKey, resultDto);
                log.info("Crawled domain: {} (ID: {}) -> Sent to Result Queue", domain, task.getId());
            }
        } catch (Exception e) {
            log.error("Error processing queue task for domain: " + task.getDomain(), e);
        }
    }

    private FullcrawlingEntity bingScrape(FullcrawlingEntity fullcrawlingEntity, Long entityId) {
        try {
            String domain = fullcrawlingEntity.getDomain();
            if (resolveDomain(domain, fullcrawlingEntity)) {
                fullcrawlingEntity.setResolver("valid");
                fullcrawlingEntity = extractor.extract(fullcrawlingEntity);
                fullcrawlingEntity = pageDetails.extractCompanyInfo(fullcrawlingEntity);
            } else {
                fullcrawlingEntity.setUrl(domain);
                fullcrawlingEntity.setResolver("invalid");
                fullcrawlingEntity.setHostIp(null);
                Instant instant = Instant.now();
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                String formattedInstant = formatter.format(instant.atZone(ZoneId.of("UTC")));
                fullcrawlingEntity.setTimeStamp(formattedInstant);
                log.warn("Skipping domain: {} as it is not resolvable.", domain);
            }
        } catch (Exception e) {
            log.error("Error while extracting homepage for entity " + entityId, e);
        }

        return fullcrawlingEntity;
    }

    private boolean resolveDomain(String domain, FullcrawlingEntity crawlerDomainEntity) {
        String formattedDomain = formatUrl(domain);
        boolean resolved = tryResolveDomain(formattedDomain, crawlerDomainEntity);
        if (!resolved && formattedDomain.startsWith("www.")) {
            String domainWithoutWww = formattedDomain.substring(4);
            resolved = tryResolveDomain(domainWithoutWww, crawlerDomainEntity);
        }
        return resolved;
    }

    private boolean tryResolveDomain(String domain, FullcrawlingEntity crawlerDomainEntity) {
        try {
            InetAddress address = InetAddress.getByName(domain);
            String ipAddress = address.getHostAddress();
            log.info("{} resolved to IP: {}", domain, ipAddress);
            crawlerDomainEntity.setHostIp(ipAddress);
            resolveCountryInfo(ipAddress, crawlerDomainEntity);
            return true;
        } catch (UnknownHostException e) {
            log.warn("{} does not exist or is not resolvable.", domain);
            return false;
        } catch (SecurityException se) {
            log.warn("Security error while resolving {}: {}", domain, se.getMessage());
            return false;
        } catch (Exception ex) {
            log.error("Unexpected error resolving {}: {}", domain, ex.getMessage());
            return false;
        }
    }

    private void resolveCountryInfo(String ipAddress, FullcrawlingEntity crawlerDomainEntity) {
        if (isPrivateIP(ipAddress)) {
            log.info("Skipping country lookup for private IP: {}", ipAddress);
            return;
        }

        String apiUrl = "http://173.212.240.94:3009/geoip?ip=" + ipAddress;
        HttpURLConnection connection = null;

        try {
            connection = (HttpURLConnection) new URL(apiUrl).openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(1500);
            connection.setReadTimeout(1500);
            connection.setRequestProperty("User-Agent", "Mozilla/5.0");

            int responseCode = connection.getResponseCode();
            if (responseCode != 200) {
                log.warn("IP API returned status {} for IP {}", responseCode, ipAddress);
                return;
            }

            try (InputStream inputStream = connection.getInputStream();
                 BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {

                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }

                if (response.length() == 0) {
                    log.warn("Empty response from IP API for IP {}", ipAddress);
                    return;
                }

                JSONObject jsonResponse = new JSONObject(response.toString());
                if (jsonResponse.has("error")) {
                    log.warn("IP API error for IP {}: {}", ipAddress, jsonResponse.optString("reason", "Unknown error"));
                    return;
                }

                if (jsonResponse.has("countryName")) {
                    String countryName = jsonResponse.getString("countryName");
                    crawlerDomainEntity.setIpCountry(countryName);
                    log.info("Resolved country {} for IP {}", countryName, ipAddress);
                } else {
                    log.warn("No countryName information in response for IP {}", ipAddress);
                }
            }
        } catch (Exception e) {
            log.error("Error occurred while resolving country for IP {}: {}", ipAddress, e.getMessage());
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private boolean isPrivateIP(String ipAddress) {
        try {
            InetAddress addr = InetAddress.getByName(ipAddress);
            return addr.isSiteLocalAddress() || addr.isLoopbackAddress() || addr.isAnyLocalAddress();
        } catch (UnknownHostException e) {
            return false;
        }
    }

    private String formatUrl(String url) {
        url = url.replaceAll("\"", "").trim();
        if (url.startsWith("https://www.") || url.startsWith("http://www.")) {
            return url.replace("https://", "").replace("http://", "").trim();
        } else if (url.startsWith("www.")) {
            return url.trim();
        } else if (url.startsWith("http://") || url.startsWith("https://")) {
            return url.replace("https://", "www.").replace("http://", "www.").trim();
        } else {
            return "www." + url.trim();
        }
    }
}
