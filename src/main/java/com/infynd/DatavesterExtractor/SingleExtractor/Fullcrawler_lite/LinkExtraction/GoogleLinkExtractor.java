package com.infynd.DatavesterExtractor.SingleExtractor.Fullcrawler_lite.LinkExtraction;


import com.infynd.DatavesterExtractor.SingleExtractor.Fullcrawler_lite.utils.WebsiteCrawlerConstants;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Slf4j
public class GoogleLinkExtractor {

    public Map<String, Serializable> extract(Map<String, Serializable> extractedData) throws Exception {
        String rawHtml = (String) extractedData.get(WebsiteCrawlerConstants.RAW_HTML);
        if (rawHtml != null) {
            try {
                Document doc = Jsoup.parse(rawHtml);
                Set<String> googleLinks = getLinks(doc);
                Set<String> Links = new HashSet<>();
                if (googleLinks != null) {
                    for (String link : googleLinks) {
                        if (StringUtils.isNotBlank(link) && link.contains("google.com/maps")) {
                            Links.add(link);
                        }
                    }
                }
                String googleLinksJoined = Links.stream().collect(Collectors.joining("|"));
                extractedData.put(WebsiteCrawlerConstants.GOOGLE_LINKS, googleLinksJoined);
            } catch (Exception e) {
                log.error("Error in extracting google links", e);
            }
        }
        return extractedData;
    }

    private Set<String> getLinks(Document document) {
        Set<String> allLinks = new HashSet<>();
        // Extract links from <a> tags
        Elements anchorElements = document.select("a");
        for (Element element : anchorElements) {
            String link = element.attr("href");
            if (StringUtils.isNotBlank(link)) {
                allLinks.add(link);
            }
        }
        // Extract links from <iframe> tags
        Elements iframeElements = document.select("iframe");
        for (Element element : iframeElements) {
            String src = element.attr("src");
            if (StringUtils.isNotBlank(src)) {
                allLinks.add(src);
            }
        }
        if (CollectionUtils.isEmpty(allLinks)) {
            return null;
        }
        return allLinks;
    }
}