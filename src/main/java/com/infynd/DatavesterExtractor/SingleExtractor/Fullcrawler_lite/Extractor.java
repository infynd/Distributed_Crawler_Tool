package com.infynd.DatavesterExtractor.SingleExtractor.Fullcrawler_lite;

import com.infynd.DatavesterExtractor.entities.FullcrawlingEntity;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class Extractor {

    private static final int MAX_HTML_BODY_BYTES = 3 * 1024 * 1024; // 3 MB protection against OOM

    private static final List<String> USER_AGENTS = List.of(
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36",
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36",
            "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36"
    );

    // High-performance shared connection pool across all crawler worker threads
    private static final PoolingHttpClientConnectionManager connectionManager;
    private static final CloseableHttpClient sharedHttpClient;

    static {
        connectionManager = new PoolingHttpClientConnectionManager();
        connectionManager.setMaxTotal(200);
        connectionManager.setDefaultMaxPerRoute(50);

        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectTimeout(15_000)
                .setConnectionRequestTimeout(20_000)
                .setSocketTimeout(15_000)
                .build();

        sharedHttpClient = HttpClients.custom()
                .setConnectionManager(connectionManager)
                .setDefaultRequestConfig(requestConfig)
                .evictIdleConnections(30, TimeUnit.SECONDS)
                .evictExpiredConnections()
                .build();
    }

    @PreDestroy
    public void cleanup() {
        try {
            if (sharedHttpClient != null) {
                sharedHttpClient.close();
            }
            if (connectionManager != null) {
                connectionManager.close();
            }
        } catch (Exception e) {
            log.warn("Error closing HTTP client connection manager: {}", e.getMessage());
        }
    }

    private String getRandomUA() {
        return USER_AGENTS.get(new Random().nextInt(USER_AGENTS.size()));
    }

    public FullcrawlingEntity extract(FullcrawlingEntity fullcrawlingEntity) {
        log.info("Processing to extract html: {}", fullcrawlingEntity.getDomain());

        String url = normalizeUrl(fullcrawlingEntity.getDomain());

        Instant instant = Instant.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        String formattedInstant = formatter.format(instant.atZone(ZoneId.of("UTC")));
        fullcrawlingEntity.setTimeStamp(formattedInstant);

        String rawDomain = fullcrawlingEntity.getDomain() != null ? fullcrawlingEntity.getDomain().trim() : "";
        String cleanDomain = rawDomain.replaceAll("(?i)^https?://", "").replaceAll("(?i)^www\\.", "").replaceAll("/.*$", "");
        String[] urlAttempts = {
                "https://" + cleanDomain,
                "https://www.cleanDomain".replace("cleanDomain", cleanDomain)
        };

        boolean successful = false;

        for (String attemptUrl : urlAttempts) {
            try {
                long startTime = System.currentTimeMillis();
                Connection.Response response = Jsoup.connect(attemptUrl)
                        .userAgent(getRandomUA())
                        .followRedirects(true)
                        .timeout(15_000)
                        .maxBodySize(MAX_HTML_BODY_BYTES)
                        .execute();
                long endTime = System.currentTimeMillis();
                long loadTimeMs = endTime - startTime;

                int responseCode = response.statusCode();
                log.info("Jsoup response code for {}: {}", attemptUrl, responseCode);

                if (responseCode != 200) {
                    log.warn("Non-200 response ({}). Skipping Jsoup for {}", responseCode, attemptUrl);
                    continue;
                }

                Document document = response.parse();
                String title = document.title();
                fullcrawlingEntity.setLoadTimeMs(loadTimeMs);
                fullcrawlingEntity.setTitle(!title.isEmpty() ? title : "empty");
                fullcrawlingEntity.setResponseCode(String.valueOf(responseCode));
                fullcrawlingEntity.setRawHtml(document.outerHtml());

                String redirectedUrl = response.url().toString();
                fullcrawlingEntity.setUrl(redirectedUrl);
                fullcrawlingEntity.setWebServer("jsoup");

                String normalized = getCleanDomain(redirectedUrl);
                fullcrawlingEntity.setNormalizedDomain(normalized != null ? normalized.replace(":443", "") : "");

                successful = true;
                break;

            } catch (IOException e) {
                log.warn("Failed to connect jsoup to: {}. Error: {}", attemptUrl, e.getMessage());
            }
        }

        // Fallback to Playwright if Jsoup never succeeded with 200
        if (!successful) {
            fullcrawlingEntity = playwrightFallback(url, fullcrawlingEntity, urlAttempts);
        }

        return fullcrawlingEntity;
    }

    private FullcrawlingEntity playwrightFallback(String url,
                                                  FullcrawlingEntity fullcrawlingEntity,
                                                  String[] urlAttempts) {
        log.info("Entering Playwright fallback for: {}", url);
        boolean success = false;

        for (String attemptUrl : urlAttempts) {
            try {
                long start = System.currentTimeMillis();

                Map<String, Object> pwResult = fetchWithPlaywright(attemptUrl);
                String html = (String) pwResult.get("html");
                int statusCode = (int) pwResult.get("status");

                long loadTime = System.currentTimeMillis() - start;

                if (StringUtils.isBlank(html)) {
                    log.warn("Playwright returned empty HTML for {}", attemptUrl);
                    continue;
                }

                Document doc = Jsoup.parse(html);

                fullcrawlingEntity.setRawHtml(html);
                fullcrawlingEntity.setTitle(StringUtils.defaultIfBlank(doc.title(), "empty"));
                fullcrawlingEntity.setLoadTimeMs(loadTime);
                fullcrawlingEntity.setUrl(attemptUrl);
                fullcrawlingEntity.setWebServer("playwright");
                fullcrawlingEntity.setResponseCode(String.valueOf(statusCode));

                String normalized = getCleanDomain(attemptUrl);
                fullcrawlingEntity.setNormalizedDomain(
                        normalized != null ? normalized.replace(":443", "") : ""
                );

                success = true;
                break;

            } catch (Exception e) {
                log.warn("Playwright failed for {}: {}", attemptUrl, e.getMessage());
            }
        }

        if (!success) {
            log.warn("Jsoup + Playwright failed. Marking as unreachable.");

            fullcrawlingEntity.setUrl(url);
            fullcrawlingEntity.setNormalizedDomain("");
            fullcrawlingEntity.setTitle("empty");
            fullcrawlingEntity.setResponseCode("0");
            fullcrawlingEntity.setWebServer("unreachable");
            fullcrawlingEntity.setRawHtml("");
        }

        return fullcrawlingEntity;
    }

    private Map<String, Object> fetchWithPlaywright(String url) {
        Map<String, Object> result = new HashMap<>();
        result.put("html", "");
        result.put("status", 0);

        try {
            HttpPost post = new HttpPost("http://173.212.240.94:5110/render");
            post.setHeader("Content-Type", "application/json");

            JSONObject jsonObj = new JSONObject();
            jsonObj.put("url", url);
            post.setEntity(new StringEntity(jsonObj.toString(), StandardCharsets.UTF_8));
            log.info("Calling Playwright render service for {}", url);

            try (CloseableHttpResponse response = sharedHttpClient.execute(post)) {
                String body = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
                JSONObject obj = new JSONObject(body);

                String html = obj.optString("html", "");
                int status = obj.optInt("status", 0);

                result.put("html", html);
                result.put("status", status);
            }
        } catch (Exception e) {
            log.error("Playwright render call failed for {}: {}", url, e.getMessage());
        }

        return result;
    }

    private String normalizeUrl(String url) {
        if (url == null) return "";
        if (url.startsWith("https://") || url.startsWith("http://")) {
            return url;
        } else if (url.startsWith("www")) {
            return "http://" + url;
        } else {
            return "http://www." + url;
        }
    }

    public String getCleanDomain(String domain) {
        if (StringUtils.isNotBlank(domain)) {
            domain = domain.replaceAll("http://", "");
            domain = domain.replaceAll("https://", "");
            domain = domain.replaceAll("https:", "");
            domain = domain.replaceAll("http:", "");

            if (domain.startsWith("/")) {
                domain = domain.substring(1);
            }

            while (domain.startsWith("www.")) {
                int index = domain.indexOf("www.");
                domain = domain.substring(index + 4);
            }
            if (domain.contains("?")) {
                int index = domain.indexOf("?");
                domain = domain.substring(0, index);
            }
            if (domain.contains("/")) {
                int index = domain.indexOf("/");
                domain = domain.substring(0, index);
            }
            return domain;
        }
        return null;
    }
}
