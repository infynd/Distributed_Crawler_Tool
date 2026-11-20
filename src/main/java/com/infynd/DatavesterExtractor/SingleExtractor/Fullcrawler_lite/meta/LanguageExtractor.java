package com.infynd.DatavesterExtractor.SingleExtractor.Fullcrawler_lite.meta;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class LanguageExtractor {

    public String extractLanguage(Document doc, String domain, String processingId) {
        try {
            String language = null;
            Elements lan = doc.select("html");
            language = lan.attr("lang");
            if (StringUtils.isEmpty(language)) {
                lan = doc.select("meta[name=language]");
                language = lan.attr("content");
            }
            if (StringUtils.isNotEmpty(language)) {
                return language;
            }
        } catch (Exception e) {
            log.error(String.format("Exception in getting description for domain %s", processingId), e);
            return null;
        }
        return null;
    }

}
