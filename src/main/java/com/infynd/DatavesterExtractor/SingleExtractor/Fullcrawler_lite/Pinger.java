package com.infynd.DatavesterExtractor.SingleExtractor.Fullcrawler_lite;


import com.infynd.DatavesterExtractor.SingleExtractor.Fullcrawler_lite.utils.WebsiteCrawlerConstants;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;

import java.io.Serializable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
public class Pinger {
    public Map<String, Serializable> execute(Map<String, Serializable> extractedData) {

        Map<String, Boolean> pingerInfo = new HashMap<>();
        if (MapUtils.isNotEmpty(extractedData)) {
            String rawHtml = (String) extractedData.get(WebsiteCrawlerConstants.RAW_HTML);
            String normalizedDomain = (String) extractedData.get(WebsiteCrawlerConstants.NORMALIZED_DOMAIN);
            String resposecode = (String) extractedData.get(WebsiteCrawlerConstants.RESPONSE_CODE);
            String title = null;
            if (StringUtils.isNotEmpty(extractedData.get(WebsiteCrawlerConstants.TITLE).toString())) {
                title = (String) extractedData.get(WebsiteCrawlerConstants.TITLE);
            }
            if (StringUtils.isNotBlank(title) && StringUtils.isNotEmpty(normalizedDomain)) {
                if (securedDomain(title, normalizedDomain, rawHtml, extractedData)) {
                    pingerInfo.put(WebsiteCrawlerConstants.IS_SECURED_DOMAIN, true);
                } else {
                    pingerInfo.put(WebsiteCrawlerConstants.IS_SECURED_DOMAIN, false);
                }
            }
            if (StringUtils.isNotBlank(rawHtml) && StringUtils.isNotBlank(normalizedDomain)) {
                assert title != null;
                if (hasSalesKeywords(rawHtml, normalizedDomain, title)) {
                    pingerInfo.put(WebsiteCrawlerConstants.IS_DOMAIN_FOR_SALE, true);
                } else {
                    pingerInfo.put(WebsiteCrawlerConstants.IS_DOMAIN_FOR_SALE, false);
                }
            }
            if (StringUtils.isNotBlank(rawHtml)) {
                if (isInvalidSite(rawHtml, title, resposecode)) {
                    pingerInfo.put(WebsiteCrawlerConstants.IS_INVALID_DOMAIN, true);
                } else {
                    pingerInfo.put(WebsiteCrawlerConstants.IS_INVALID_DOMAIN, false);
                }
            }
            if (StringUtils.isNotBlank(title)) {
                if (isprotected(title, normalizedDomain, rawHtml, resposecode)) {
                    pingerInfo.put(WebsiteCrawlerConstants.IS_PROTECTED_DOMAIN, true);
                } else {
                    pingerInfo.put(WebsiteCrawlerConstants.IS_PROTECTED_DOMAIN, false);
                }
            }
        }
        extractedData.put(WebsiteCrawlerConstants.PINGER_INFO, (Serializable) pingerInfo);
        return extractedData;
    }

    private boolean isprotected(String title, String normailized, String rawhtml, String responsecode) {
        Document doc = Jsoup.parse(rawhtml);
        String bodyContent = "";
        String bodyhtml = "";
        String bodytext = "";
        if (doc.body() != null) {
            bodyContent = String.valueOf(doc.body());
            bodyhtml = doc.body().html();
            bodytext = doc.body().text().trim();
        }
        if (bodyContent.equals("") && title.toLowerCase().contains("loading")) {
            return true;
        }
        if (bodyContent.equals("")) {
            return true;
        }
        String scriptPattern = "window\\.onload=\\s*function\\(\\)\\s*\\{\\s*window\\.location\\.href=\"/lander\"\\s*\\}";
        Pattern pattern = Pattern.compile(scriptPattern);
        Matcher matcher = pattern.matcher(rawhtml);
        boolean hasRedirectScript = matcher.find();

        String targetUrl = "https://www.names.co.uk/parked-domains/index?";
        Element iframe = doc.select("iframe").first();
        if (bodytext.equals("") && (responsecode.equals("200") || responsecode.equals("202")) && !hasRedirectScript && iframe != null && !iframe.attr("src").contains(targetUrl)) {
            return true;
        }
        if (bodyhtml.equals("") && responsecode.equals("200") && bodytext.equals("") && !hasRedirectScript) {
            return true;
        }

        String[] keywords = {"just a moment...", "cloudflare", "welcome to nginx!", "just a moment",};
        String lctitle = title.toLowerCase();
        for (String keyword : keywords) {
            if (lctitle.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private boolean hasSalesKeywords(String rawHtml, String normalizeddomain, String title) {
        Document doc = Jsoup.parse(rawHtml);
        String lowercasetitle = title.toLowerCase();

        String scriptPattern = "window\\.onload=\\s*function\\(\\)\\s*\\{\\s*window\\.location\\.href=\"/lander\"\\s*\\}";
        Pattern pattern = Pattern.compile(scriptPattern);
        Matcher matcher = pattern.matcher(rawHtml);
        boolean hasRedirectScript = matcher.find();
        if (hasRedirectScript) {
            return true;
        }
        Elements scriptTags = doc.getElementsByTag("script");
        for (Element scriptTag : scriptTags) {
            if (scriptTag.html().contains("window.park")) {
                return true;
            }
        }
        String targetUrl = "https://www.names.co.uk/parked-domains/index?";
        Element iframe = doc.select("iframe").first();
        if (iframe != null && iframe.attr("src").contains(targetUrl)) {
            return true;
        }
        if (title.contains("This domain may be for sale")) {
            return true;
        }
        if (normalizeddomain.contains("sawsells.com")) {
            return true;
        }
        if (rawHtml.contains("https://img1.wsimg.com/parking-lander/static/") || rawHtml.contains("//static.parastorage.com/services/wix-public")) {
            return true;
        }
        if (lowercasetitle.equals("loading...") && rawHtml.contains("window.location.replace")) {
            return true;
        }
        if (rawHtml.toLowerCase().contains("godaddy operating company")) {
            return true;
        }
        if (lowercasetitle.contains("wordpress.com") || lowercasetitle.contains("holding page") || lowercasetitle.contains("ecommerce software by shopify") || lowercasetitle.contains("welcome to your new website") || lowercasetitle.contains("inactive") || lowercasetitle.contains("iis windows server") || lowercasetitle.contains("your website is ready to use") || lowercasetitle.contains("this site is brand new") || lowercasetitle.contains("wix.com") || lowercasetitle.contains("domain parked") || lowercasetitle.contains("godaddy") || lowercasetitle.contains("parked domain")) {
            return true;
        }
        String[] keywords = {"is for sale", "welcome to your new website", "holding page", "ecommerce software by shopify", "this account has been suspended", "this site is brand new", "your website is ready to use", "iis windows server", "cname cross-user banned", "indexed, not submitted in sitemap", "submitted URL blocked by robots", "crawl anomaly", "manual action", "web hosting", "googlebot", "domain not claimed", "default page", "domain name", "free domain", "domain for sale", "website sale", "sale website", "godaddy", "https://img1.wsimg.com/parking-lander/static/", "static.parastorage.com/services/wix-public", "window.park", "wix.com", "godaddy", "domain parked", "parked domain", "hugedomains", "wordpress.com", "clubspark.ltorg.uk", "web hosting", "parking-lander", "parking domain", "parking website", "buy a domain", "buy domain", "domain buying", "hosted by", "plesk page", "parking page", "default web", "account suspended", "This domain may be for sale"};

        for (String keyword : keywords) {
            if (lowercasetitle.contains(keyword)) {
                return true;
            }
        }
        if (normalizeddomain != null) {
            if (normalizeddomain.contains("hugedomains") || normalizeddomain.equalsIgnoreCase("names.co.uk") || normalizeddomain.contains("wordpress.com") || normalizeddomain.contains("clubspark.lta.org.uk") || normalizeddomain.contains("web hosting") || normalizeddomain.contains("godaddy.com")) {
                return true;
            }
            String[] keywords1 = {"94.136.40.51", "accounts.google.com", "sell.sawbrokers.com", "sites.google.com", "79.170.44.8", "google.com", "partnership.sjp.co.uk", "just-eat.co.uk", "expiredwixdomain.com", "login.microsoftonline.com", "lcn.com", "sedo.com", "epageuk.com", "wbinfo.prositehosting.co.uk", "brandbucket.com", "data:,", "websitebuilder.prositehosting.co.uk"};
            for (String keyword : keywords1) {
                if (normalizeddomain.contains(keyword)) {
                    return true;
                }
            }
        }
        Elements script1 = doc.select("a href");
        for (Element element : script1) {
            String text = element.text();
            if (StringUtils.isNotBlank(text) && text.contains("parking-lander") && text.contains("window.park")) {
                return true;
            }
        }
        Elements script2 = doc.select("script");
        for (Element element : script2) {
            String text = element.text();
            if (StringUtils.isNotBlank(text) && text.contains("parking-lander") && text.contains("window.park")) {
                return true;
            }
        }
        String[] salesKeywords = new String[]{"Parkingcrew", "source=parked", "gdforsale", "domain for sale", "domain name to you if a sale is made", "is parked domain", "buy domain", "This Domain Isn't Connected To A Website", "is parked " + normalizeddomain.toLowerCase(), normalizeddomain.toLowerCase() + " may be available", normalizeddomain.toLowerCase() + " for sale", "buy " + normalizeddomain.toLowerCase(), "this site is not published or does not have a domain assigned to it,window.parl"};
        String lowerCaseHtml = rawHtml.toLowerCase();
        for (String keyword : salesKeywords) {
            if (lowerCaseHtml.contains(keyword.toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    private boolean isInvalidSite(String rawHtml, String title, String responsecode) {
        Document doc = Jsoup.parse(rawHtml);
        Elements script = doc.select("title");
        String bodyContent = "";
        String bodytext = "";
        if (doc.body() != null) {
            bodyContent = doc.body().text().trim();
            bodytext = doc.body().text().trim();
        }
        if (bodytext.equals("") && !responsecode.equals("200")) {
            return true;
        }
        String lowercasetitle = title.toLowerCase();
        String[] keywords = {"404 error", "dns resolution error", "error: the request could not be satisfied", "500 - internal server error", "runtime error", "500 internal server error", "internal server", "unknown domain", "error: active domain connection for this domain not found", "origin dns error", "502 proxy error", "500 - internal server error", "500: internal server error", "error 502", "404 error", "domain error", "server error", "database error", "not found", "application error", "unavailable", "service interruption", "error | drupal", "iis 10.0 detailed error", "coverage issue", "url not found", "page removed from the index", "new index coverage issue found", "url is not on google", "redirect error", "domain name expired", "domain expired", "there has been an error processing your request", "domain not available", "website is no longer available", "you are not authorized", "service not available", "suspended domain", "site is not available"};
        for (String keyword : keywords) {
            if (lowercasetitle.contains(keyword) || bodyContent.toLowerCase().equals(keyword)) {
                return true;
            }
        }
        if (bodyContent.toLowerCase().startsWith("not found") || bodyContent.toLowerCase().startsWith("no such forward entry") || bodyContent.toLowerCase().startsWith("The site you are looking for is unreachable or does not exist") || bodyContent.toLowerCase().startsWith("this account has been suspended")) {
            return true;
        }
        for (Element element : script) {
            String text = element.text();
            if (StringUtils.isNotBlank(text) && text.contains("Page not found") || text.contains("SITE NOT FOUND")) {
                return true;
            }
        }
        String bodyContent1 = "";
        int bodyStartIndex = rawHtml.indexOf("<body");
        if (bodyStartIndex != -1) {
            int bodyEndIndex = rawHtml.indexOf("</body>", bodyStartIndex);
            if (bodyEndIndex != -1) {
                bodyContent1 = rawHtml.substring(bodyStartIndex, bodyEndIndex + 7); // 7 is the length of "</body>"
            }
        }
        bodyContent1 = bodyContent1.replaceAll("<[^>]*>", "").trim().toLowerCase();
        return bodyContent1.contains("no such forward entry") || bodyContent1.contains("the site you are looking for is unreachable or does not exist") || bodyContent1.contains("this account has been suspended") || bodyContent1.contains("there has been a critical error on this website") || bodyContent1.contains("the business is now closed") && !responsecode.equals("200");
    }

    private boolean securedDomain(String title, String domain, String rawHtml, Map<String, Serializable> extractedData) {
        Document doc = Jsoup.parse(rawHtml);
        String ldomain = domain.toLowerCase();
        String bodyContent = "";
        if (doc.body() != null) {
            bodyContent = doc.body().text().trim();
        }
        String lowercasetitle = title.toLowerCase();
        // Check for absence of <script> elements
        boolean noScriptElements = !rawHtml.contains("<script");

        // Extract the content inside the <body> tag
        String bodyContent1 = "";
        int bodyStartIndex = rawHtml.indexOf("<body");
        if (bodyStartIndex != -1) {
            int bodyEndIndex = rawHtml.indexOf("</body>", bodyStartIndex);
            if (bodyEndIndex != -1) {
                bodyContent1 = rawHtml.substring(bodyStartIndex, bodyEndIndex + 7); // 7 is the length of "</body>"
            }
        }
        boolean isBodyEmpty = bodyContent1.replaceAll("<[^>]*>", "").trim().isEmpty();
        String targetUrl = "https://www.names.co.uk/parked-domains/index?";
        Element iframe = doc.select("iframe").first();
        if (iframe != null && noScriptElements && isBodyEmpty && !iframe.attr("src").contains(targetUrl)) {
            extractedData.put(WebsiteCrawlerConstants.VALIDATION_COMMENTS, "NO script and body content present");
            return true;
        }
        if (domain != null) {
            String[] equalsKeywords = {"names.co.uk", "google.co.uk", "uk.com", "epageuk.com", "ajg.com", "iongroup.com", "twitter.com", "ekmholdingdomain.com", "amazon.co.uk"};
            String[] containsKeywords = {"instagram.com", "facebook.com", "79.170.44.8", "94.136.40.51", "linkedin.com", "accounts.google.com", "sites.google.com", "lcn.com", "ageuk.org.uk", "wbinfo.prositehosting.co.uk", "perfectdomain.com", "google.com", "login.microsoftonline.com:443", "business.google.com", "theaccessgroup.com", "planetradio.co.uk", "fairstone.co.uk", "login.microsoftonline.com", "linktr.ee", "bk-partners1.co.uk", "portfolio.adobe.com", "accenture.com", "mrisoftware.com", "civica.com", "consent.youtube.com", "domains.squadhelp.com"};
            // Check for exact matches (equals)
            String matchedDomain = Arrays.stream(equalsKeywords).filter(ldomain::equalsIgnoreCase).findFirst().orElse(null);

            // Check for partial matches (contains)
            if (matchedDomain == null) {
                matchedDomain = Arrays.stream(containsKeywords).filter(ldomain::contains).findFirst().orElse(null);
            }
            if (matchedDomain != null) {
                extractedData.put(WebsiteCrawlerConstants.VALIDATION_COMMENTS, "Normalized domain matched: " + matchedDomain);
                return true;
            }
        }

        if (lowercasetitle.contains("private site") || lowercasetitle.contains("page redirection") || lowercasetitle.contains("welcome to nginx!") || lowercasetitle.contains("error") || lowercasetitle.contains("forbidden") || lowercasetitle.contains("this account has been suspended.") || lowercasetitle.contains("404 error: page not found") || lowercasetitle.contains("access forbidden!") || lowercasetitle.contains("403 forbidden") || lowercasetitle.contains("network error") || lowercasetitle.contains("403 - forbidden: access is denied.") || lowercasetitle.contains("attention required! ") || lowercasetitle.contains("dns resolution error ") || lowercasetitle.contains("404 error page") || lowercasetitle.contains("access denied") || lowercasetitle.contains("edge ip restricted") || lowercasetitle.contains("500 - internal server error.") || lowercasetitle.contains("runtime error") || lowercasetitle.contains("500 internal server error") || lowercasetitle.contains("403 - forbidden") || lowercasetitle.contains("forbidden") || lowercasetitle.contains("there has been an error processing your request") || lowercasetitle.contains("cname cross-user banned") || lowercasetitle.contains("fastly error: unknown domain ") || lowercasetitle.contains("error: active domain connection for this domain not found") || lowercasetitle.contains("origin dns error") || lowercasetitle.contains("tripod - succeed online") || lowercasetitle.contains("dns points to prohibited ip") || lowercasetitle.contains("application error") || lowercasetitle.contains("502 proxy error") || lowercasetitle.contains("500 - internal server error") || lowercasetitle.contains(" 500: internal server error") || lowercasetitle.equalsIgnoreCase("403 - ") || lowercasetitle.contains(": 403 - restricted access") || lowercasetitle.contains("error 502") || lowercasetitle.equalsIgnoreCase("- 404 Error") || lowercasetitle.contains("domain error") || lowercasetitle.contains("server error!") || lowercasetitle.contains("Database Error")) {
            return true;

        }
        if (lowercasetitle.contains("503 service temporarily unavailable") || lowercasetitle.contains("not found") || lowercasetitle.contains("temporarily unavailable") || lowercasetitle.contains("temporarily") || lowercasetitle.contains("under working") || lowercasetitle.contains("forbidden") || lowercasetitle.contains("unavailable") || lowercasetitle.contains("down for maintenance") || lowercasetitle.contains("site maintenance") || lowercasetitle.contains("site under maintenance") || lowercasetitle.contains("currently unavailable") || lowercasetitle.contains("site update") || lowercasetitle.contains("maintenance mode") || lowercasetitle.contains("maintenance page") || lowercasetitle.contains("site down") || lowercasetitle.contains("unavailable due to maintenance") || lowercasetitle.contains("system maintenance") || lowercasetitle.contains("service maintenance") || lowercasetitle.contains("temporarily closed") || lowercasetitle.contains("service interruption")) {
            return true;
        }
        if (bodyContent.equalsIgnoreCase("too many requests") || bodyContent.toLowerCase().contains("502 bad gateway") || bodyContent.startsWith("502") || bodyContent.toLowerCase().startsWith("virtual host not found") || bodyContent.toLowerCase().contains("the site you are looking for is unreachable or does not exist.")) {
            return true;
        }
        if (bodyContent.toLowerCase().contains("hosting") && (bodyContent.toLowerCase().contains("timeout") || bodyContent.toLowerCase().contains("currently disabled"))) {
            return true;
        }
        if (rawHtml.toLowerCase().contains("hosting server connect timeout")) {
            return true;
        }

        String[] keywords = {"page redirection", "private site", "secured domain", "this account has been suspended", "access is denied", "403.14 - forbidden", "attention required", "access denied", "edge ip restricted", "forbidden", "there has been an error processing your request", "cname cross-user banned", "forbidden", "dns points to prohibited ip", ": 403 - restricted access", "fastly error", "unusual traffic from your computer network", "sitemap could not be read", "indexed, not submitted in sitemap", "submitted URL blocked by robots", "crawl anomaly", "security issue", "manual action", "edge ip restricted", "you are not authorized", "Error 503: Service Unavailable", "Tripod - Succeed Online | Error", "503 Service Temporarily Unavailable", "Temporarily", "under working", "under maintenance", "down for maintenance", "site maintenance", "site under maintenance", "this site is under mantenance", "site is undergoing maintenance", "site is offline", "currently unavailable", "site update", "maintenance mode", "maintenance page", "site down", "unavailable due to maintenance", "system maintenance", "service maintenance", "temporarily closed", "504 gateway", "under construction", "Site offline", "504", "502", "500", "loading", "loading...", "dns points to prohibited IP", "403 -", ": 403 - restricted access", "fastly error", "unusual traffic from your computer network", "sitemap could not be read", "currently", "unavailabe", "sorry", "coming soon", "site not installed - ovhcloud", "this domain is brand new"

        };

        for (String keyword : keywords) {
            if (lowercasetitle.contains(keyword)) {
                return true;
            }
        }

        Elements script = doc.select("title");
        for (Element element : script) {
            String text = element.text();
            if (text.contains("Error | Drupal")) {
                log.info("117 - {}", title);
                return true;
            }
        }
        return false;
    }
}