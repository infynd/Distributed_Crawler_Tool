package com.infynd.DatavesterExtractor.SingleExtractor.Fullcrawler_lite.meta;


import com.infynd.DatavesterExtractor.SingleExtractor.Fullcrawler_lite.utils.WebsiteCrawlerConstants;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.Serializable;
import java.util.Map;


@Service
@Slf4j
public class MetaExtractor {
    @Autowired
    private DescriptionExtractor description;
    @Autowired
    private TitleExtractor title;
    @Autowired
    private LanguageExtractor language;
    @Autowired
    private ImageUrlExtractor imageUrl;
    @Autowired
    private NameExtractor name;

    public Map<String, Serializable> extract(Map<String, Serializable> extractedData) {

        String normalizedDomain = (String) extractedData.get(WebsiteCrawlerConstants.NORMALIZED_DOMAIN);
        String processingId = (String) extractedData.get(WebsiteCrawlerConstants.PROCESSING_ID);
        String redirectedUrl = (String) extractedData.get(WebsiteCrawlerConstants.PARENT_URL);

        try {
            String rawHtml = StringUtils.replace((String) extractedData.get(WebsiteCrawlerConstants.RAW_HTML), "@|!", ",");

            Document doc = Jsoup.parse(rawHtml);
            extractedData.put(WebsiteCrawlerConstants.NORMALIZED_DOMAIN, normalizedDomain);
            String des = description.extractDescription(doc, normalizedDomain, processingId);
            String title = this.title.extractTitle(doc, normalizedDomain, processingId);
            String lang = language.extractLanguage(doc, normalizedDomain, processingId);
            String image = imageUrl.extractImageUrl(doc, normalizedDomain, processingId, redirectedUrl);
            String name = this.name.extractName(doc, normalizedDomain, processingId);
            if (StringUtils.isNotEmpty(des)) {
                extractedData.put(WebsiteCrawlerConstants.DESCRIPTION, des);
            }
            if (StringUtils.isNotEmpty(title)) {
                extractedData.put(WebsiteCrawlerConstants.TITLE, title);
            }
            if (StringUtils.isNotEmpty(lang)) {
                extractedData.put(WebsiteCrawlerConstants.LANGUAGE, lang);
            }
            if (StringUtils.isNotEmpty(image)) {
                extractedData.put(WebsiteCrawlerConstants.IMAGE_URL, image);
            }
            if (StringUtils.isNotEmpty(name)) {
                extractedData.put((WebsiteCrawlerConstants.NAME_FROM_META), name);
                extractedData.put((WebsiteCrawlerConstants.TRADING_COMPANY_NAME), name);
            }

        } catch (Exception e) {
            log.error(String.format("Exception in getting meta information for domain %s", processingId), e);
        }

        return extractedData;
    }
}
