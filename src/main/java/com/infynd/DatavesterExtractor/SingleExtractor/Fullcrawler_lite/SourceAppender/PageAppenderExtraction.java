package com.infynd.DatavesterExtractor.SingleExtractor.Fullcrawler_lite.SourceAppender;

import com.infynd.DatavesterExtractor.SingleExtractor.Fullcrawler_lite.addressextraction.AddressProcessFunctionInterfaceProcess;
import com.infynd.DatavesterExtractor.SingleExtractor.Fullcrawler_lite.utils.WebsiteCrawlerConstants;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.Serializable;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class PageAppenderExtraction {

    @Autowired
    AddressProcessFunctionInterfaceProcess addressProcessFunctionInterface;

    private static final int MAX_PAGES_TOTAL = Integer.MAX_VALUE; // No limit, scrape all priority pages found!
    private static final int MAX_DEEP_MATCHED_PAGES = 3;
    private static final int TIMEOUT_SECONDS = 20;
    private static final int THREADS = 5;
    private static final List<String> KEYWORDS = Arrays.asList(
            "about", "contact", "company", "team", "solution", "product", "platform",
            "services", "get-in-touch", "impressum", "kontakt", "uber-uns", "equipe",
            "qui", "sommes", "nous", "contactez", "contacter",
            "location", "locations", "find-us", "store-locator", "branches"
    );

    static class ScrapedPage {
        String url;
        String rawHtml;
        String cleanedText;
        Set<String> outboundLinks;

        public ScrapedPage(String url, String rawHtml, String cleanedText, Set<String> outboundLinks) {
            this.url = url;
            this.rawHtml = rawHtml;
            this.cleanedText = cleanedText;
            this.outboundLinks = outboundLinks;
        }
    }

    private boolean isPriority(String link) {
        String lower = link.toLowerCase();
        return lower.contains("contact") || lower.contains("about") || lower.contains("location") || lower.contains("find-us") || lower.contains("store-locator") || lower.contains("privacy") || lower.contains("terms") || lower.contains("product");
    }

    @SuppressWarnings("unchecked")
    public Map<String, Serializable> extract(Map<String, Serializable> extractedData) {
        log.info("extraction of all pagesource- {}", extractedData.get(WebsiteCrawlerConstants.NORMALIZED_DOMAIN));

        String home = (String) extractedData.get(WebsiteCrawlerConstants.RAW_HTML);
        if (home != null) {
            extractedData.put(WebsiteCrawlerConstants.Home_page, home);
        }

        String domain = (String) extractedData.get(WebsiteCrawlerConstants.NORMALIZED_DOMAIN);
        Set<String> allLinks = (Set<String>) extractedData.get(WebsiteCrawlerConstants.ALL_LINKS);

        if (allLinks == null) {
            allLinks = new HashSet<>();
        }

        // We will collect ALL discovered links here so legacy populator can see them all.
        Set<String> totalDiscoveredLinks = ConcurrentHashMap.newKeySet();
        totalDiscoveredLinks.addAll(allLinks);

        // 1. Filter internal links for the crawler logic
        Set<String> internalLinks = allLinks.stream()
                .filter(link -> link.startsWith("http://") || link.startsWith("https://"))
                .map(link -> {
                    try {
                        return URLDecoder.decode(link, StandardCharsets.UTF_8).trim();
                    } catch (Exception e) {
                        return link.trim();
                    }
                })
                .filter(link -> !link.contains("#")) // Discard fragment only/section links
                .map(link -> getNormalizedLink(link, domain))
                .filter(link -> link.contains(domain)) // same domain only
                .collect(Collectors.toSet());

        // 2. Keyword Filtering
        Set<String> filteredLinks;
        if (internalLinks.size() <= 5) {
            filteredLinks = new HashSet<>(internalLinks);
        } else {
            filteredLinks = internalLinks.stream()
                    .filter(link -> {
                        String lowerLink = link.toLowerCase();
                        return KEYWORDS.stream().anyMatch(lowerLink::contains);
                    })
                    .collect(Collectors.toSet());
        }

        // 3. Mode Decision: Shallow vs Deep Crawl
        boolean isShallow = filteredLinks.stream().anyMatch(link -> {
            String lower = link.toLowerCase();
            return lower.contains("contact") || lower.contains("about") || lower.contains("location");
        });

        Set<String> scrapedUrls = ConcurrentHashMap.newKeySet();
        StringBuilder crawledTextBuilder = new StringBuilder();

        ExecutorService executor = Executors.newFixedThreadPool(THREADS);

        try {
            if (isShallow) {
                log.info("====== Mode Decision: SHALLOW CRAWL for domain {} ======", domain);
                extractedData.put("crawlMode", "SHALLOW_CRAWL");
                List<String> priorityLinks = filteredLinks.stream()
                        .filter(this::isPriority)
                        .limit(MAX_PAGES_TOTAL)
                        .collect(Collectors.toList());

                log.info("Shallow Crawl - Pages selected for scraping:");
                for (String link : priorityLinks) {
                    log.info("   -> {}", link);
                }
                extractedData.put("shallowCrawlPages", (Serializable) new ArrayList<>(priorityLinks));

                List<ScrapedPage> results = scrapePages(priorityLinks, scrapedUrls, executor, MAX_PAGES_TOTAL);

                List<String> newlyDiscoveredLinks = new ArrayList<>();
                for (ScrapedPage sp : results) {
                    processSingleScrapeResult(sp, crawledTextBuilder, extractedData, totalDiscoveredLinks);
                    // Queue new priority links discovered on the shallow pages
                    newlyDiscoveredLinks.addAll(sp.outboundLinks.stream()
                            .map(l -> getNormalizedLink(l, domain))
                            .filter(l -> l.contains(domain))
                            .filter(this::isPriority)
                            .collect(Collectors.toSet()));
                }

                if (scrapedUrls.size() < MAX_PAGES_TOTAL && !newlyDiscoveredLinks.isEmpty()) {
                    List<String> nextBatch = newlyDiscoveredLinks.stream()
                            .filter(l -> !scrapedUrls.contains(l))
                            .limit((long) MAX_PAGES_TOTAL - scrapedUrls.size())
                            .collect(Collectors.toList());

                    if (!nextBatch.isEmpty()) {
                        log.info("Shallow Crawl - Newly discovered priority pages selected for phase 2 scraping:");
                        for (String link : nextBatch) {
                            log.info("   -> {}", link);
                        }
                        extractedData.put("shallowCrawlPriorityPages", (Serializable) new ArrayList<>(nextBatch));

                        List<ScrapedPage> resultsPhase2 = scrapePages(nextBatch, scrapedUrls, executor, MAX_PAGES_TOTAL);
                        for (ScrapedPage sp : resultsPhase2) {
                            processSingleScrapeResult(sp, crawledTextBuilder, extractedData, totalDiscoveredLinks);
                        }
                    }
                }
            } else {
                log.info("====== Mode Decision: DEEP CRAWL for domain {} ======", domain);
                extractedData.put("crawlMode", "DEEP_CRAWL");

                // Scrape matched sub-pages
                List<String> deepLinksToScrape = filteredLinks.stream().limit(MAX_DEEP_MATCHED_PAGES).collect(Collectors.toList());
                log.info("Deep Crawl - Matched sub-pages selected for phase 1 scraping:");
                for (String link : deepLinksToScrape) {
                    log.info("   -> {}", link);
                }
                extractedData.put("deepCrawlMatchedPages", (Serializable) new ArrayList<>(deepLinksToScrape));

                // Execute Phase 1 concurrently
                List<ScrapedPage> phase1Results = scrapePages(deepLinksToScrape, scrapedUrls, executor, MAX_PAGES_TOTAL);

                List<String> newlyDiscoveredLinks = new ArrayList<>();
                for (ScrapedPage sp : phase1Results) {
                    processSingleScrapeResult(sp, crawledTextBuilder, extractedData, totalDiscoveredLinks);
                    // Queue only new priority links
                    newlyDiscoveredLinks.addAll(sp.outboundLinks.stream()
                            .map(l -> getNormalizedLink(l, domain))
                            .filter(l -> l.contains(domain))
                            .filter(this::isPriority)
                            .collect(Collectors.toSet()));
                }

                // Scrape those newly discovered high-priority links
                if (scrapedUrls.size() < MAX_PAGES_TOTAL && !newlyDiscoveredLinks.isEmpty()) {
                    List<String> nextBatch = newlyDiscoveredLinks.stream()
                            .filter(l -> !scrapedUrls.contains(l))
                            .limit((long) MAX_PAGES_TOTAL - scrapedUrls.size())
                            .collect(Collectors.toList());

                    if (!nextBatch.isEmpty()) {
                        log.info("Deep Crawl - Newly discovered priority pages selected for phase 2 scraping:");
                        for (String link : nextBatch) {
                            log.info("   -> {}", link);
                        }
                        extractedData.put("deepCrawlPriorityPages", (Serializable) new ArrayList<>(nextBatch));

                        List<ScrapedPage> results = scrapePages(nextBatch, scrapedUrls, executor, MAX_PAGES_TOTAL);
                        processScrapeResults(results, crawledTextBuilder, extractedData, totalDiscoveredLinks);
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error in pageappender -- {}", e.getMessage(), e);
        } finally {
            executor.shutdownNow();
        }

        // --- POPULATE LEGACY LINKS WITH ALL DISCOVERED LINKS ---
        populateLegacyLinks(extractedData, totalDiscoveredLinks, domain);

        if (crawledTextBuilder.length() > 0) {
            String existingRawHtml = (String) extractedData.getOrDefault(WebsiteCrawlerConstants.RAW_HTML, "");
            extractedData.put(WebsiteCrawlerConstants.RAW_HTML, existingRawHtml + "\n" + crawledTextBuilder.toString());
            log.info("raw html updated with scraped pages - {}", domain);
        }

        return extractedData;
    }

    private void populateLegacyLinks(Map<String, Serializable> extractedData, Set<String> allLinks, String domain) {
        Set<String> filteredLinks = allLinks.stream()
                .filter(link -> link.startsWith("http://") || link.startsWith("https://"))
                .collect(Collectors.toSet());

        // Update HOME_LINKS and ALL_LINKS so downstream components see everything discovered
        extractedData.put(WebsiteCrawlerConstants.Home_links, (Serializable) new HashSet<>(filteredLinks));
        extractedData.put(WebsiteCrawlerConstants.ALL_LINKS, (Serializable) new HashSet<>(allLinks));

        extractedData.put(WebsiteCrawlerConstants.CONTACT_LINKS, extractCategoryLinks(allLinks, domain, "contact"));
        extractedData.put(WebsiteCrawlerConstants.PRIVACY_LINKS, extractCategoryLinks(allLinks, domain, "privacy"));
        extractedData.put(WebsiteCrawlerConstants.ABOUT_LINKS, extractCategoryLinks(allLinks, domain, "about"));
        extractedData.put(WebsiteCrawlerConstants.TERMS_LINKS, extractCategoryLinks(allLinks, domain, "terms"));
        extractedData.put(WebsiteCrawlerConstants.Product_links, extractCategoryLinks(allLinks, domain, "product"));
        extractedData.put(WebsiteCrawlerConstants.LOCATION_LINKS, extractCategoryLinks(allLinks, domain, "location", "locations", "find-us", "store-locator", "branches"));

        String services = extractCategoryLinks(allLinks, domain, "services", "service", "what-we-do", "capabilities",
                "expertise", "consulting", "advisory", "implementation", "integration", "deployment", "managed",
                "outsourcing", "support", "maintenance", "training", "workshops", "professional-services", "it-services",
                "development-services", "engineering", "custom-development", "migration", "modernization", "cloud-services",
                "security-services", "compliance", "audit", "analytics-services", "ai-services");
        if (StringUtils.isNotBlank(services)) {
            extractedData.put(WebsiteCrawlerConstants.Service_links, services);
        }

        String ecommerce = extractCategoryLinks(allLinks, domain, "/shop", "/store", "/cart", "/checkout", "/product", "/pricing");
        if (StringUtils.isNotBlank(ecommerce)) {
            extractedData.put(WebsiteCrawlerConstants.Ecommerce_links, ecommerce);
        }
    }

    private String extractCategoryLinks(Set<String> allLinks, String domain, String... matchers) {
        Set<String> matchedLinks = allLinks.parallelStream()
                .map(String::trim)
                .map(link -> {
                    try {
                        return URLDecoder.decode(link, StandardCharsets.UTF_8);
                    } catch (Exception e) {
                        return link;
                    }
                })
                .filter(link -> {
                    String lower = link.toLowerCase();
                    boolean containsMatcher = Arrays.stream(matchers).anyMatch(lower::contains);
                    return containsMatcher && !lower.contains("google") && !lower.contains("twitter")
                            && !lower.contains("facebook") && !lower.contains("instagram");
                })
                .map(link -> getNormalizedLink(link, domain))
                .filter(link -> link.contains(domain))
                .collect(Collectors.toSet());
        return matchedLinks.isEmpty() ? "" : String.join("|", matchedLinks);
    }

    private void processScrapeResults(List<ScrapedPage> results, StringBuilder crawledTextBuilder,
                                      Map<String, Serializable> extractedData, Set<String> totalDiscoveredLinks) {
        for (ScrapedPage sp : results) {
            processSingleScrapeResult(sp, crawledTextBuilder, extractedData, totalDiscoveredLinks);
        }
    }

    private void processSingleScrapeResult(ScrapedPage sp, StringBuilder crawledTextBuilder,
                                           Map<String, Serializable> extractedData, Set<String> totalDiscoveredLinks) {
        crawledTextBuilder.append("<!-- SOURCE: ").append(sp.url).append(" -->\n").append(sp.rawHtml).append("\n");
        totalDiscoveredLinks.addAll(sp.outboundLinks);

        String lowerUrl = sp.url.toLowerCase();
        if (lowerUrl.contains("contact")) {
            appendMapString(extractedData, WebsiteCrawlerConstants.Contact_page, sp.rawHtml);
            appendLinksAll(extractedData, WebsiteCrawlerConstants.CONTACT_LINKS_ALL, sp.outboundLinks);
        } else if (lowerUrl.contains("privacy")) {
            appendMapString(extractedData, WebsiteCrawlerConstants.Privacy_page, sp.rawHtml);
            appendLinksAll(extractedData, WebsiteCrawlerConstants.PRIVACY_LINKS_ALL, sp.outboundLinks);
        } else if (lowerUrl.contains("about")) {
            appendMapString(extractedData, WebsiteCrawlerConstants.About_page, sp.rawHtml);
            appendLinksAll(extractedData, WebsiteCrawlerConstants.ABOUT_LINKS_ALL, sp.outboundLinks);
        } else if (lowerUrl.contains("terms")) {
            appendMapString(extractedData, WebsiteCrawlerConstants.Terms_page, sp.rawHtml);
            appendLinksAll(extractedData, WebsiteCrawlerConstants.TERMS_LINKS_ALL, sp.outboundLinks);
        } else if (lowerUrl.contains("location") || lowerUrl.contains("find-us") || lowerUrl.contains("store-locator")) {
            appendMapString(extractedData, WebsiteCrawlerConstants.Location_page, sp.rawHtml);
            appendLinksAll(extractedData, WebsiteCrawlerConstants.LOCATION_LINKS_ALL, sp.outboundLinks);
        } else if (lowerUrl.contains("product")) {
            appendMapString(extractedData, WebsiteCrawlerConstants.Product_Page, sp.rawHtml);
        }
    }

    @SuppressWarnings("unchecked")
    private void appendLinksAll(Map<String, Serializable> map, String key, Set<String> newLinks) {
        Set<String> validLinks = filterValidLinks(newLinks);
        if (validLinks.isEmpty()) {
            return;
        }

        Object existing = map.get(key);
        if (existing instanceof Set) {
            ((Set<String>) existing).addAll(validLinks);
        } else {
            map.put(key, new HashSet<>(validLinks));
        }
    }

    private void appendMapString(Map<String, Serializable> map, String key, String value) {
        if (StringUtils.isBlank(value)) {
            return;
        }
        String existing = (String) map.getOrDefault(key, "");
        if (StringUtils.isBlank(existing)) {
            map.put(key, value);
        } else {
            map.put(key, existing + "\n" + value);
        }
    }

    private Set<String> filterValidLinks(Set<String> links) {
        return links.stream()
                .filter(link -> link.startsWith("http://") || link.startsWith("https://"))
                .collect(Collectors.toSet());
    }

    private List<ScrapedPage> scrapePages(List<String> links, Set<String> scrapedUrls, ExecutorService executor, int maxPages) {
        List<Callable<ScrapedPage>> tasks = new ArrayList<>();
        for (String link : links) {
            if (scrapedUrls.size() >= maxPages) {
                break;
            }
            if (scrapedUrls.add(link)) {
                tasks.add(() -> {
                    log.info(">>> Actively hitting (scraping) URL: {}", link);
                    ScrapedPage res = fetchAndParse(link);
                    if (res != null) {
                        log.info("<<< Finished scraping URL: {}. Extracted {} sub-links: {}", link, res.outboundLinks.size(), res.outboundLinks);
                    }
                    return res;
                });
            }
        }

        List<ScrapedPage> completedResults = new ArrayList<>();
        if (tasks.isEmpty()) {
            return completedResults;
        }

        try {
            List<Future<ScrapedPage>> futures = executor.invokeAll(tasks, TIMEOUT_SECONDS, TimeUnit.SECONDS);
            for (Future<ScrapedPage> future : futures) {
                if (!future.isCancelled()) {
                    ScrapedPage res = future.get();
                    if (res != null && StringUtils.isNotBlank(res.rawHtml)) {
                        completedResults.add(res);
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error executing parallel scrape", e);
        }
        return completedResults;
    }

    private ScrapedPage fetchAndParse(String url) {
        try {
            String html = addressProcessFunctionInterface.extractSubLinkPageSource(url);
            String cleanedText = "";

            // Initial parse
            if (StringUtils.isNotBlank(html)) {
                cleanedText = extractText(html);
            }

            // SPA fallback: If HTML is empty or text content is < 100 characters
            if (StringUtils.isBlank(html) || cleanedText.length() < 100) {
                log.info("Jsoup failed or content < 100 chars, falling back to Playwright for {}", url);
                html = fetchWithPlaywright(url);
                if (StringUtils.isNotBlank(html)) {
                    cleanedText = extractText(html);
                }
            }

            if (StringUtils.isBlank(html)) {
                return null;
            }

            Set<String> outboundLinks = extractLinksFromHtml(html);
            return new ScrapedPage(url, html, cleanedText, outboundLinks);

        } catch (Exception e) {
            log.error("Sub-page fetch failed for {}", url, e);
            return null;
        }
    }

    private String extractText(String html) {
        try {
            Document document = Jsoup.parse(html);
            return document.text().replaceAll("\\s+", " ").trim();
        } catch (Exception e) {
            return "";
        }
    }

    private String fetchWithPlaywright(String url) {
        try {
            RequestConfig config = RequestConfig.custom()
                    .setConnectTimeout(15_000)
                    .setConnectionRequestTimeout(20_000)
                    .setSocketTimeout(75_000)
                    .build();

            try (CloseableHttpClient client = HttpClients.custom()
                    .setDefaultRequestConfig(config)
                    .build()) {

                HttpPost post = new HttpPost("http://173.212.240.94:5111/render");
                post.setHeader("Content-Type", "application/json");

                JSONObject jsonObj = new JSONObject();
                jsonObj.put("url", url);

                post.setEntity(new StringEntity(
                        jsonObj.toString(),
                        StandardCharsets.UTF_8
                ));

                log.info("Calling Playwright for {}", url);

                try (CloseableHttpResponse response = client.execute(post)) {
                    String body = EntityUtils.toString(
                            response.getEntity(),
                            StandardCharsets.UTF_8
                    );

                    JSONObject obj = new JSONObject(body);
                    return obj.optString("html", "");
                }
            }

        } catch (Exception e) {
            log.error("Playwright failed for {}", url, e);
            return "";
        }
    }

    private Set<String> extractLinksFromHtml(String html) {
        Set<String> links = new HashSet<>();
        try {
            Document doc = Jsoup.parse(html);
            Elements anchors = doc.select("a[href]");
            for (Element a : anchors) {
                String href = a.attr("abs:href").trim();
                if (StringUtils.isBlank(href)) {
                    href = a.attr("href").trim();
                }

                if (href.startsWith("http://") || href.startsWith("https://")) {
                    links.add(href);
                } else if (!href.isEmpty() && !href.startsWith("javascript") && !href.startsWith("mailto")
                        && !href.startsWith("tel") && !href.startsWith("#")) {
                    links.add(href);
                }
            }
        } catch (Exception e) {
            log.error("Error extracting links from html", e);
        }
        return links;
    }

    private String getNormalizedLink(String link, String domain) {
        if (!link.startsWith("http")) {
            String cleanLink = link.startsWith("/") ? link : "/" + link;
            link = "https://www." + domain + cleanLink;
        }
        return link;
    }
}
