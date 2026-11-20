package com.infynd.DatavesterExtractor.SingleExtractor.Fullcrawler_lite.LinkExtraction;

import com.infynd.DatavesterExtractor.SingleExtractor.Fullcrawler_lite.utils.WebsiteCrawlerConstants;
import com.infynd.DatavesterExtractor.service.FunctionInterface;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.Serializable;
import java.util.*;

@Service
@Slf4j
public class LinkExtraction {
    private static final String CAREER = "career";
    private static final String ABOUT = "about";
    private static final String TEAM = "team";
    private static final String BLOG = "blog";
    private static final String CONTACT_US = "contactUs";
    private static final String PRIVACY = "privacy";
    private static final String TERMS = "terms";
    private static final String LOCATION = "location";
    private static final String[] EXCLUDED_LINK_PATHS = {"blog/", "blogs/"};
    private static final String[] TEAM_PAGE_IDENTIFIERS = {"team", "our team"};

    private static final String PASS_1_FLAG = "LINK_EXTRACTOR_PASS_1_DONE";

    @Autowired
    FunctionInterface functionInterface;

    /**
     * OPTIMIZED extract method:
     * - Pass 1: Linear O(K) extraction on homepage via document.select("a[href]")
     * - Pass 2: Parses ONLY newly discovered subpages from pageAppender, skipping the homepage completely.
     */
    @SuppressWarnings("unchecked")
    public Map<String, Serializable> extract(Map<String, Serializable> extractedData) {
        try {
            String parentUrl = (String) extractedData.get(WebsiteCrawlerConstants.PARENT_URL);
            String domain = extractedData.get(WebsiteCrawlerConstants.NORMALIZED_DOMAIN) != null
                    ? extractedData.get(WebsiteCrawlerConstants.NORMALIZED_DOMAIN).toString()
                    : "";
            String domainNameWithoutDomainExtension = functionInterface.getPartialDomainName(domain);

            boolean isPass2 = Boolean.TRUE.equals(extractedData.get(PASS_1_FLAG));

            if (!isPass2) {
                // =========================================================================
                // PASS 1: Homepage Only (Linear O(K) single-pass extraction via a[href])
                // =========================================================================
                String rawHtml = (String) extractedData.get(WebsiteCrawlerConstants.RAW_HTML);
                if (rawHtml != null) {
                    rawHtml = StringUtils.replace(rawHtml, "@|!", ",");
                }

                Set<String> allLinks = new HashSet<>();
                Map<String, Set<String>> socialSets = initSocialSets();
                Map<String, Set<String>> domainMatchedSocialSets = initSocialSets();
                Map<String, String> otherLinks = new HashMap<>();

                if (StringUtils.isNotBlank(rawHtml)) {
                    Document doc = Jsoup.parse(rawHtml, parentUrl != null ? parentUrl : "");
                    processDocumentLinksLinear(doc, allLinks, socialSets, domainMatchedSocialSets,
                            otherLinks, parentUrl, domainNameWithoutDomainExtension, domain);
                }

                Map<String, String> socialLinks = convertSetsToStringMap(socialSets);
                Map<String, String> domainMatchedSocialLinks = convertSetsToStringMap(domainMatchedSocialSets);

                extractedData.put(WebsiteCrawlerConstants.SOCIAL_LINKS, (Serializable) socialLinks);
                extractedData.put(WebsiteCrawlerConstants.DOMAIN_MATCHED_SOCIAL_LINKS, (Serializable) domainMatchedSocialLinks);
                extractedData.put(WebsiteCrawlerConstants.OTHER_LINKS, (Serializable) otherLinks);
                extractedData.put(WebsiteCrawlerConstants.ALL_LINKS, (Serializable) allLinks);

                // Mark Pass 1 completed
                extractedData.put(PASS_1_FLAG, true);
                log.info("LinkExtraction Pass 1 completed for domain {} (Discovered {} links)", domain, allLinks.size());

            } else {
                // =========================================================================
                // PASS 2: Delta Subpages Only (Never re-parse the whole homepage!)
                // =========================================================================
                Set<String> allLinks = (Set<String>) extractedData.get(WebsiteCrawlerConstants.ALL_LINKS);
                if (allLinks == null) {
                    allLinks = new HashSet<>();
                }

                Map<String, Set<String>> socialSets = loadSetsFromStringMap(
                        (Map<String, String>) extractedData.get(WebsiteCrawlerConstants.SOCIAL_LINKS));
                Map<String, Set<String>> domainMatchedSocialSets = loadSetsFromStringMap(
                        (Map<String, String>) extractedData.get(WebsiteCrawlerConstants.DOMAIN_MATCHED_SOCIAL_LINKS));
                Map<String, String> otherLinks = (Map<String, String>) extractedData.get(WebsiteCrawlerConstants.OTHER_LINKS);
                if (otherLinks == null) {
                    otherLinks = new HashMap<>();
                }

                // Collect newly fetched subpages from pageAppender
                Map<String, String> subpagesToParse = new LinkedHashMap<>();
                collectSubpage(subpagesToParse, (String) extractedData.get(WebsiteCrawlerConstants.Contact_page),
                        (String) extractedData.get(WebsiteCrawlerConstants.CONTACT_LINKS), parentUrl);
                collectSubpage(subpagesToParse, (String) extractedData.get(WebsiteCrawlerConstants.About_page),
                        (String) extractedData.get(WebsiteCrawlerConstants.ABOUT_LINKS), parentUrl);
                collectSubpage(subpagesToParse, (String) extractedData.get(WebsiteCrawlerConstants.Privacy_page),
                        (String) extractedData.get(WebsiteCrawlerConstants.PRIVACY_LINKS), parentUrl);
                collectSubpage(subpagesToParse, (String) extractedData.get(WebsiteCrawlerConstants.Terms_page),
                        (String) extractedData.get(WebsiteCrawlerConstants.TERMS_LINKS), parentUrl);
                collectSubpage(subpagesToParse, (String) extractedData.get(WebsiteCrawlerConstants.Location_page),
                        (String) extractedData.get(WebsiteCrawlerConstants.LOCATION_LINKS), parentUrl);
                collectSubpage(subpagesToParse, (String) extractedData.get(WebsiteCrawlerConstants.Product_Page),
                        null, parentUrl);

                // Fallback: If individual subpage keys were empty, check for <!-- SOURCE: url --> sections in RAW_HTML
                if (subpagesToParse.isEmpty()) {
                    String rawHtml = (String) extractedData.get(WebsiteCrawlerConstants.RAW_HTML);
                    if (rawHtml != null && rawHtml.contains("<!-- SOURCE: ")) {
                        String[] sections = rawHtml.split("<!-- SOURCE: ");
                        for (int i = 1; i < sections.length; i++) {
                            int endUrlIdx = sections[i].indexOf(" -->");
                            if (endUrlIdx > 0) {
                                String subUrl = sections[i].substring(0, endUrlIdx).trim();
                                String subHtml = sections[i].substring(endUrlIdx + 4).trim();
                                if (StringUtils.isNotBlank(subHtml)) {
                                    subpagesToParse.put(subUrl, subHtml);
                                }
                            }
                        }
                    }
                }

                // Parse each new subpage with its OWN URL as the base URI
                for (Map.Entry<String, String> entry : subpagesToParse.entrySet()) {
                    String subUrl = entry.getKey();
                    String subHtml = entry.getValue();
                    if (StringUtils.isNotBlank(subHtml)) {
                        Document subDoc = Jsoup.parse(subHtml, subUrl);
                        processDocumentLinksLinear(subDoc, allLinks, socialSets, domainMatchedSocialSets,
                                otherLinks, subUrl, domainNameWithoutDomainExtension, domain);
                    }
                }

                Map<String, String> socialLinks = convertSetsToStringMap(socialSets);
                Map<String, String> domainMatchedSocialLinks = convertSetsToStringMap(domainMatchedSocialSets);

                extractedData.put(WebsiteCrawlerConstants.SOCIAL_LINKS, (Serializable) socialLinks);
                extractedData.put(WebsiteCrawlerConstants.DOMAIN_MATCHED_SOCIAL_LINKS, (Serializable) domainMatchedSocialLinks);
                extractedData.put(WebsiteCrawlerConstants.OTHER_LINKS, (Serializable) otherLinks);
                extractedData.put(WebsiteCrawlerConstants.ALL_LINKS, (Serializable) allLinks);

                log.info("LinkExtraction Pass 2 (Delta) completed for domain {} (Parsed {} subpages, total links: {})",
                        domain, subpagesToParse.size(), allLinks.size());
            }

        } catch (Exception e) {
            log.error(String.format("Error while finding links for domain %s", extractedData.get(WebsiteCrawlerConstants.PARENT_URL)), e);
        }

        return extractedData;
    }

    /**
     * Helper to safely collect subpage HTML with its specific URL
     */
    private void collectSubpage(Map<String, String> subpages, String pageHtml, String pageUrl, String defaultParentUrl) {
        if (StringUtils.isNotBlank(pageHtml)) {
            String url = StringUtils.isNotBlank(pageUrl) ? pageUrl.split("\\|")[0].trim() : defaultParentUrl;
            subpages.put(url, pageHtml);
        }
    }

    /**
     * UNIFIED O(K) LINEAR PROCESSING METHOD:
     * - Uses document.select("a[href]") to select only valid anchors
     * - Evaluates links, social media classification, and structural pages in a single loop pass.
     */
    private void processDocumentLinksLinear(Document document,
                                           Set<String> allLinks,
                                           Map<String, Set<String>> socialSets,
                                           Map<String, Set<String>> domainMatchedSocialSets,
                                           Map<String, String> otherLinks,
                                           String currentUrl,
                                           String domainNameWithoutDomainExtension,
                                           String normalizedDomain) {

        Elements anchors = document.select("a[href]");

        for (Element link : anchors) {
            String linkStr = link.attr("abs:href");
            if (StringUtils.isBlank(linkStr)) {
                linkStr = link.attr("href");
            }
            if (StringUtils.isBlank(linkStr)) {
                continue;
            }

            linkStr = linkStr.trim();

            // Skip anchor fragments and JavaScript calls
            if (linkStr.startsWith("#") || linkStr.toLowerCase().startsWith("javascript:") || linkStr.toLowerCase().startsWith("data:")) {
                continue;
            }

            allLinks.add(linkStr);

            // 1. Social Links & Domain Matching in the same pass
            classifySocialInLoop(linkStr, domainNameWithoutDomainExtension, normalizedDomain, socialSets, domainMatchedSocialSets);

            // 2. Structural subpages (Team, About, Contact, Privacy, Terms, Career, Blog)
            classifyOtherLinksInLoop(link, linkStr, currentUrl, otherLinks);
        }
    }

    /**
     * Classifies social profiles and domain-matched links directly inside the loop
     */
    private void classifySocialInLoop(String link,
                                      String domainNameWithoutDomainExtension,
                                      String normalizedDomain,
                                      Map<String, Set<String>> socialSets,
                                      Map<String, Set<String>> domainMatchedSocialSets) {

        String cleanLink = link.replaceAll("(?sim)\\s*/\\s*$", "");
        boolean isDomainNameInLink = cleanLink.toLowerCase().contains(domainNameWithoutDomainExtension.toLowerCase());
        boolean isDomainMatchedInLink = cleanLink.toLowerCase().contains(normalizedDomain.toLowerCase());

        if (cleanLink.contains("facebook.com") || StringUtils.containsIgnoreCase(cleanLink, "Facebook")) {
            socialSets.get("facebook").add(cleanLink);
            if (isDomainMatchedInLink) domainMatchedSocialSets.get("facebook").add(cleanLink);
        } else if (cleanLink.contains("twitter.com") || StringUtils.containsIgnoreCase(cleanLink, "Twitter") || cleanLink.contains("x.com/")) {
            socialSets.get("twitter").add(cleanLink);
            if (isDomainMatchedInLink) domainMatchedSocialSets.get("twitter").add(cleanLink);
        } else if (cleanLink.contains("linkedin.com") || StringUtils.containsIgnoreCase(cleanLink, "LinkedIn")) {
            socialSets.get("linkedin").add(cleanLink);
            if (isDomainMatchedInLink) domainMatchedSocialSets.get("linkedin").add(cleanLink);
        } else if (cleanLink.contains("plus.google.com/")) {
            socialSets.get("google_plus").add(cleanLink);
            if (isDomainMatchedInLink) domainMatchedSocialSets.get("google_plus").add(cleanLink);
        } else if (cleanLink.contains("slideshare.net/")) {
            socialSets.get("slideshare").add(cleanLink);
            if (isDomainMatchedInLink) domainMatchedSocialSets.get("slideshare").add(cleanLink);
        } else if (cleanLink.contains("instagram.com") || StringUtils.containsIgnoreCase(cleanLink, "Instagram")) {
            socialSets.get("instagram").add(cleanLink);
            if (isDomainMatchedInLink) domainMatchedSocialSets.get("instagram").add(cleanLink);
        } else if (cleanLink.contains("youtube.com/")) {
            socialSets.get("youtube").add(cleanLink);
            if (isDomainMatchedInLink) domainMatchedSocialSets.get("youtube").add(cleanLink);
        } else if (cleanLink.contains("pinterest.com/")) {
            socialSets.get("pinterest").add(cleanLink);
            if (isDomainMatchedInLink) domainMatchedSocialSets.get("pinterest").add(cleanLink);
        } else if (cleanLink.contains("vimeo.com")) {
            socialSets.get("vimeo").add(cleanLink);
            if (isDomainMatchedInLink) domainMatchedSocialSets.get("vimeo").add(cleanLink);
        }

        if (cleanLink.contains("privacy") || cleanLink.contains("privacy-policy")) {
            socialSets.get("privacy").add(cleanLink);
            if (isDomainNameInLink) domainMatchedSocialSets.get("privacy").add(cleanLink);
        } else if (cleanLink.contains("terms") || cleanLink.contains("condition")) {
            socialSets.get("terms").add(cleanLink);
            if (isDomainNameInLink) domainMatchedSocialSets.get("terms").add(cleanLink);
        }
    }

    /**
     * Evaluates link text and link URL for internal structural pages in the same loop
     */
    private void classifyOtherLinksInLoop(Element link, String linkUrl, String currentUrl, Map<String, String> otherLinks) {
        String text = link.text();

        // Check anchor text matches
        if (isTeamPage(text)) {
            if (!StringUtils.containsIgnoreCase(currentUrl, "team")) updateOtherLinks(link, TEAM, otherLinks);
        } else if (StringUtils.containsIgnoreCase(text, ABOUT)) {
            if (!StringUtils.containsIgnoreCase(currentUrl, "about")) updateOtherLinks(link, ABOUT, otherLinks);
        } else if (StringUtils.containsIgnoreCase(text, "contact")) {
            if (!StringUtils.containsIgnoreCase(currentUrl, "contact")) updateOtherLinks(link, CONTACT_US, otherLinks);
        } else if (StringUtils.containsIgnoreCase(text, "privacy")) {
            if (!StringUtils.containsIgnoreCase(currentUrl, "privacy")) updateOtherLinks(link, PRIVACY, otherLinks);
        } else if (StringUtils.containsIgnoreCase(text, "terms")) {
            if (!StringUtils.containsIgnoreCase(currentUrl, "terms")) updateOtherLinks(link, TERMS, otherLinks);
        } else if (StringUtils.containsIgnoreCase(text, "reach")) {
            if (!StringUtils.containsIgnoreCase(currentUrl, "reach")) updateOtherLinks(link, CONTACT_US, otherLinks);
        } else if (StringUtils.containsIgnoreCase(text, "location") || StringUtils.containsIgnoreCase(text, "find us") || StringUtils.containsIgnoreCase(text, "store locator")) {
            if (!StringUtils.containsIgnoreCase(currentUrl, "location")) updateOtherLinks(link, LOCATION, otherLinks);
        } else if (StringUtils.containsIgnoreCase(text, CAREER)) {
            if (!StringUtils.containsIgnoreCase(currentUrl, "career")) updateOtherLinks(link, CAREER, otherLinks);
        } else if (StringUtils.containsIgnoreCase(text, "job")) {
            if (!StringUtils.containsIgnoreCase(currentUrl, "job")) updateOtherLinks(link, CAREER, otherLinks);
        } else if (StringUtils.containsIgnoreCase(text, BLOG)) {
            if (!StringUtils.containsIgnoreCase(currentUrl, "blog")) updateOtherLinks(link, BLOG, otherLinks);
        }

        // Check anchor URL matches
        if (StringUtils.containsIgnoreCase(linkUrl, TEAM)) {
            if (!StringUtils.containsIgnoreCase(currentUrl, "team")) updateOtherLinks(link, TEAM, otherLinks);
        } else if (StringUtils.containsIgnoreCase(linkUrl, ABOUT)) {
            if (!StringUtils.containsIgnoreCase(currentUrl, "about")) updateOtherLinks(link, ABOUT, otherLinks);
        } else if (StringUtils.containsIgnoreCase(linkUrl, "contact")) {
            if (!StringUtils.containsIgnoreCase(currentUrl, "contact")) updateOtherLinks(link, CONTACT_US, otherLinks);
        } else if (StringUtils.containsIgnoreCase(linkUrl, "reach")) {
            if (!StringUtils.containsIgnoreCase(currentUrl, "reach")) updateOtherLinks(link, CONTACT_US, otherLinks);
        } else if (StringUtils.containsIgnoreCase(linkUrl, "location") || StringUtils.containsIgnoreCase(linkUrl, "find-us") || StringUtils.containsIgnoreCase(linkUrl, "store-locator")) {
            if (!StringUtils.containsIgnoreCase(currentUrl, "location")) updateOtherLinks(link, LOCATION, otherLinks);
        } else if (StringUtils.containsIgnoreCase(linkUrl, CAREER)) {
            if (!StringUtils.containsIgnoreCase(currentUrl, "career")) updateOtherLinks(link, CAREER, otherLinks);
        } else if (StringUtils.containsIgnoreCase(linkUrl, "job")) {
            if (!StringUtils.containsIgnoreCase(currentUrl, "job")) updateOtherLinks(link, CAREER, otherLinks);
        } else if (StringUtils.containsIgnoreCase(linkUrl, BLOG)) {
            if (!StringUtils.containsIgnoreCase(currentUrl, "blog")) updateOtherLinks(link, BLOG, otherLinks);
        }
    }

    private Map<String, Set<String>> initSocialSets() {
        Map<String, Set<String>> map = new HashMap<>();
        String[] keys = {"facebook", "twitter", "google_plus", "linkedin", "instagram", "pinterest", "slideshare", "vimeo", "youtube", "privacy", "terms"};
        for (String k : keys) {
            map.put(k, new HashSet<>());
        }
        return map;
    }

    private Map<String, String> convertSetsToStringMap(Map<String, Set<String>> setsMap) {
        Map<String, String> result = new HashMap<>();
        for (Map.Entry<String, Set<String>> entry : setsMap.entrySet()) {
            result.put(entry.getKey(), String.join("|", entry.getValue()));
        }
        return result;
    }

    private Map<String, Set<String>> loadSetsFromStringMap(Map<String, String> stringMap) {
        Map<String, Set<String>> setsMap = initSocialSets();
        if (stringMap != null) {
            for (Map.Entry<String, String> entry : stringMap.entrySet()) {
                if (StringUtils.isNotBlank(entry.getValue())) {
                    setsMap.put(entry.getKey(), new HashSet<>(Arrays.asList(entry.getValue().split("\\|"))));
                }
            }
        }
        return setsMap;
    }

    private boolean containsExludedLinkPath(String link) {
        if (StringUtils.isNotEmpty(link)) {
            for (int i = 0; i < EXCLUDED_LINK_PATHS.length; i++) {
                if (StringUtils.containsIgnoreCase(link, EXCLUDED_LINK_PATHS[i])) {
                    return true;
                }
            }
            return false;
        }
        return true;
    }

    private boolean isTeamPage(String linkText) {
        if (StringUtils.isNotEmpty(linkText)) {
            for (int i = 0; i < TEAM_PAGE_IDENTIFIERS.length; i++) {
                if (linkText.equalsIgnoreCase(TEAM_PAGE_IDENTIFIERS[i]) || linkText.toLowerCase().startsWith(TEAM_PAGE_IDENTIFIERS[i])) {
                    return true;
                }
            }
        }
        return false;
    }

    private void updateOtherLinks(Element link, String type, Map<String, String> otherLinks) {
        String linkUrl = link.attr("abs:href");
        if (!otherLinks.containsKey(type)) {
            if (link.hasAttr("href") && StringUtils.isNotBlank(link.attr("abs:href"))) {
                if (!containsExludedLinkPath(linkUrl) || type.equals(BLOG)) {
                    otherLinks.put(type, linkUrl);
                }
            }
        }
    }

    /*
     * =========================================================================
     * PREVIOUS IMPLEMENTATION (Commented for reference as requested)
     * =========================================================================
     *
    public void findAndClassifyLinks(String rawHtml, Set<String> allLinks, Map<String, String> socialLinks, Map<String, String> otherLinks, String parentUrl, String normalizedDomain, Map<String, String> domainMatchedSocialLinks) {
        Document html = Jsoup.parse(rawHtml, parentUrl);
        Set<String> links = new HashSet<>();
        if (html != null) {
            links = getLinks(html);
            if (CollectionUtils.isNotEmpty(links)) {
                allLinks.addAll(links);
                filterSocialLinks(socialLinks, links, normalizedDomain, domainMatchedSocialLinks);
            }
            getOtherLinks1(html, otherLinks, parentUrl);
            getOtherLinks2(html, otherLinks, parentUrl);
        }
    }

    private Set<String> getLinks(Document document) {
        Elements allLinkEl = document.select("*");
        Set<String> allLinks = new HashSet<>();
        for (Element element : allLinkEl) {
            Elements links = element.getElementsByTag("a");
            for (Element link : links) {
                String linkStr = link.attr("abs:href");
                if (StringUtils.isNotBlank(linkStr)) {
                    allLinks.add(linkStr);
                } else {
                    linkStr = link.attr("href");
                    if (StringUtils.isNotBlank(linkStr)) {
                        allLinks.add(linkStr);
                    }
                }
            }
        }
        if (CollectionUtils.isEmpty(allLinks)) {
            return null;
        }
        return allLinks;
    }

    private void getOtherLinks1(Document document, Map<String, String> otherLinks, String url) {
        Elements elements = document.select("*");

        for (Element element : elements) {
            Elements links = element.getElementsByTag("a");
            for (Element link : links) {
                if (isTeamPage(link.text())) {
                    if (!StringUtils.containsIgnoreCase(url, "team")) {
                        updateOtherLinks(link, TEAM, otherLinks);
                    }
                } else if (StringUtils.containsIgnoreCase(link.text(), ABOUT)) {
                    if (!StringUtils.containsIgnoreCase(url, "about")) {
                        updateOtherLinks(link, ABOUT, otherLinks);
                    }
                } else if (StringUtils.containsIgnoreCase(link.text(), "contact")) {
                    if (!StringUtils.containsIgnoreCase(url, "contact")) {
                        updateOtherLinks(link, CONTACT_US, otherLinks);
                    }
                } else if (StringUtils.containsIgnoreCase(link.text(), "privacy")) {
                    if (!StringUtils.containsIgnoreCase(url, "privacy")) {
                        updateOtherLinks(link, PRIVACY, otherLinks);
                    }
                } else if (StringUtils.containsIgnoreCase(link.text(), "terms")) {
                    if (!StringUtils.containsIgnoreCase(url, "terms")) {
                        updateOtherLinks(link, TERMS, otherLinks);
                    }
                } else if (StringUtils.containsIgnoreCase(link.text(), "reach")) {
                    if (!StringUtils.containsIgnoreCase(url, "reach")) {
                        updateOtherLinks(link, CONTACT_US, otherLinks);
                    }
                } else if (StringUtils.containsIgnoreCase(link.text(), CAREER)) {
                    if (!StringUtils.containsIgnoreCase(url, "career")) {
                        updateOtherLinks(link, CAREER, otherLinks);
                    }
                } else if (StringUtils.containsIgnoreCase(link.text(), "job")) {
                    if (!StringUtils.containsIgnoreCase(url, "job")) {
                        updateOtherLinks(link, CAREER, otherLinks);
                    }
                } else if (StringUtils.containsIgnoreCase(link.text(), BLOG)) {
                    if (!StringUtils.containsIgnoreCase(url, "blog")) {
                        updateOtherLinks(link, BLOG, otherLinks);
                    }
                }
            }
        }
    }

    private void getOtherLinks2(Document document, Map<String, String> otherLinks, String parentUrl) {
        Elements allLinkEl = document.select("*");
        for (Element element : allLinkEl) {
            Elements links = element.getElementsByTag("a");
            for (Element link : links) {
                String linkUrl = link.attr("abs:href");
                if (StringUtils.containsIgnoreCase(linkUrl, TEAM)) {
                    if (!StringUtils.containsIgnoreCase(parentUrl, "team")) {
                        updateOtherLinks(link, TEAM, otherLinks);
                    }
                } else if (StringUtils.containsIgnoreCase(linkUrl, ABOUT)) {
                    if (!StringUtils.containsIgnoreCase(parentUrl, "about")) {
                        updateOtherLinks(link, ABOUT, otherLinks);
                    }
                } else if (StringUtils.containsIgnoreCase(linkUrl, "contact")) {
                    if (!StringUtils.containsIgnoreCase(parentUrl, "contact")) {
                        updateOtherLinks(link, CONTACT_US, otherLinks);
                    }
                } else if (StringUtils.containsIgnoreCase(link.text(), "privacy")) {
                    if (!StringUtils.containsIgnoreCase(parentUrl, "privacy")) {
                        updateOtherLinks(link, PRIVACY, otherLinks);
                    }
                } else if (StringUtils.containsIgnoreCase(link.text(), "terms")) {
                    if (!StringUtils.containsIgnoreCase(parentUrl, "terms")) {
                        updateOtherLinks(link, TERMS, otherLinks);
                    }
                } else if (StringUtils.containsIgnoreCase(linkUrl, "reach")) {
                    if (!StringUtils.containsIgnoreCase(parentUrl, "reach")) {
                        updateOtherLinks(link, CONTACT_US, otherLinks);
                    }
                } else if (StringUtils.containsIgnoreCase(linkUrl, CAREER)) {
                    if (!StringUtils.containsIgnoreCase(parentUrl, "career")) {
                        updateOtherLinks(link, CAREER, otherLinks);
                    }
                } else if (StringUtils.containsIgnoreCase(linkUrl, "job")) {
                    if (!StringUtils.containsIgnoreCase(parentUrl, "job")) {
                        updateOtherLinks(link, CAREER, otherLinks);
                    }
                } else if (StringUtils.containsIgnoreCase(linkUrl, BLOG)) {
                    if (!StringUtils.containsIgnoreCase(parentUrl, "blog")) {
                        updateOtherLinks(link, BLOG, otherLinks);
                    }
                }
            }
        }
    }

    private void filterSocialLinks(Map<String, String> socialLinks, Set<String> allLinks, String normalizedDomain, Map<String, String> domainMatchedSocialLinks) {
        String domainNameWithoutDomainExtension = functionInterface.getPartialDomainName(normalizedDomain);
        Set<String> tempListLinkedin = new HashSet<>();
        Set<String> tempListFb = new HashSet<>();
        Set<String> tempListTwitter = new HashSet<>();
        Set<String> tempListGoogleplus = new HashSet<>();
        Set<String> tempListSlideshare = new HashSet<>();
        Set<String> tempListInstagram = new HashSet<>();
        Set<String> tempListYoutube = new HashSet<>();
        Set<String> tempListPinterest = new HashSet<>();
        Set<String> tempListVimeo = new HashSet<>();
        //domainmatched
        Set<String> domainMatchedTempListLinkedin = new HashSet<>();
        Set<String> domainMatchedTempListFb = new HashSet<>();
        Set<String> domainMatchedTempListTwitter = new HashSet<>();
        Set<String> domainMatchedTempListGoogleplus = new HashSet<>();
        Set<String> domainMatchedTempListSlideshare = new HashSet<>();
        Set<String> domainMatchedTempListInstagram = new HashSet<>();
        Set<String> domainMatchedTempListYoutube = new HashSet<>();
        Set<String> domainMatchedTempListPinterest = new HashSet<>();
        Set<String> domainMatchedTempListVimeo = new HashSet<>();
        Set<String> privacypolicy = new HashSet<>();
        Set<String> Terms = new HashSet<>();
        Set<String> domainMatchedPrivacyPolicy = new HashSet<>();
        Set<String> domainMatchedTerms = new HashSet<>();
        for (String link : allLinks) {
            link = link.replaceAll("(?sim)\\s* / \\s*$", "");
            boolean isDomainNameInLink = link.toLowerCase().contains(domainNameWithoutDomainExtension.toLowerCase());
            boolean isDomainMatchedInLink = link.toLowerCase().contains(normalizedDomain.toLowerCase());
            if (link.contains("facebook.com") || StringUtils.containsIgnoreCase(link, "Facebook") || StringUtils.containsIgnoreCase(link, "facebook")) {
                tempListFb.add(link);
                if (isDomainMatchedInLink) {
                    domainMatchedTempListFb.add(link);
                }
            } else if (link.contains("twitter.com") || StringUtils.containsIgnoreCase(link, "Twitter") || StringUtils.containsIgnoreCase(link, "twitter")) {
                tempListTwitter.add(link);
                if (isDomainMatchedInLink) {
                    domainMatchedTempListTwitter.add(link);
                }
            } else if (link.contains("linkedin.com") || StringUtils.containsIgnoreCase(link, "LinkedIn") || StringUtils.containsIgnoreCase(link, "linkedin")) {
                tempListLinkedin.add(link);
                if (isDomainMatchedInLink) {
                    domainMatchedTempListLinkedin.add(link);
                }

            } else if (link.contains("plus.google.com/")) {
                tempListGoogleplus.add(link);
                if (isDomainMatchedInLink) {
                    domainMatchedTempListGoogleplus.add(link);
                }

            } else if (link.contains("slideshare.net/")) {
                tempListSlideshare.add(link);
                if (isDomainMatchedInLink) {
                    domainMatchedTempListSlideshare.add(link);
                }
            } else if (link.contains("instagram.com") || StringUtils.containsIgnoreCase(link, "Instagram") || StringUtils.containsIgnoreCase(link, "instagram")) {
                tempListInstagram.add(link);
                if (isDomainMatchedInLink) {
                    domainMatchedTempListInstagram.add(link);
                }

            } else if (link.contains("youtube.com/")) {
                tempListYoutube.add(link);
                if (isDomainMatchedInLink) {
                    domainMatchedTempListYoutube.add(link);
                }
            } else if (link.contains("pinterest.com/")) {
                tempListPinterest.add(link);
                if (isDomainMatchedInLink) {
                    domainMatchedTempListPinterest.add(link);
                }
            } else if (link.contains("vimeo.com")) {
                tempListVimeo.add(link);
                if (isDomainMatchedInLink) {
                    domainMatchedTempListVimeo.add(link);
                }
            }
            if (link.contains("privacy") || (link.contains("privacy-policy"))) {
                privacypolicy.add(link);
                if (isDomainNameInLink) {
                    domainMatchedPrivacyPolicy.add(link);
                }
            } else if (link.contains("terms") || (link.contains("condition"))) {
                Terms.add(link);
                if (isDomainNameInLink) {
                    domainMatchedTerms.add(link);
                }
            }
        }
        socialLinks.put("facebook", String.join("|", tempListFb));
        socialLinks.put("twitter", String.join("|", tempListTwitter));
        socialLinks.put("google_plus", String.join("|", tempListGoogleplus));
        socialLinks.put("linkedin", String.join("|", tempListLinkedin));
        socialLinks.put("instagram", String.join("|", tempListInstagram));
        socialLinks.put("pinterest", String.join("|", tempListPinterest));
        socialLinks.put("slideshare", String.join("|", tempListSlideshare));
        socialLinks.put("vimeo", String.join("|", tempListVimeo));
        socialLinks.put("youtube", String.join("|", tempListYoutube));
        socialLinks.put("privacy", String.join("|", privacypolicy));
        socialLinks.put("terms", String.join("|", Terms));
        domainMatchedSocialLinks.put("facebook", String.join("|", domainMatchedTempListFb));
        domainMatchedSocialLinks.put("twitter", String.join("|", domainMatchedTempListTwitter));
        domainMatchedSocialLinks.put("google_plus", String.join("|", domainMatchedTempListGoogleplus));
        domainMatchedSocialLinks.put("linkedin", String.join("|", domainMatchedTempListLinkedin));
        domainMatchedSocialLinks.put("instagram", String.join("|", domainMatchedTempListInstagram));
        domainMatchedSocialLinks.put("pinterest", String.join("|", domainMatchedTempListPinterest));
        domainMatchedSocialLinks.put("slideshare", String.join("|", domainMatchedTempListSlideshare));
        domainMatchedSocialLinks.put("vimeo", String.join("|", domainMatchedTempListVimeo));
        domainMatchedSocialLinks.put("privacy", String.join("|", domainMatchedPrivacyPolicy));
        domainMatchedSocialLinks.put("terms", String.join("|", domainMatchedTerms));
        domainMatchedSocialLinks.put("youtube", String.join("|", domainMatchedTempListYoutube));
    }
    * =========================================================================
    */
}
