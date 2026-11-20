package com.infynd.DatavesterExtractor.SingleExtractor.Fullcrawler_lite.meta;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;


@Service
@Slf4j
public class TitleExtractor {

    public String extractTitle(Document doc, String domain, String processingId) {

        try {
            String title = null;
            Elements titleElement = doc.select("title");
            if (titleElement != null) {
                title = titleElement.text();
            }

            List<String> titleQueries = new ArrayList<>();
            titleQueries.add("meta[property=og:title]");
            titleQueries.add("meta[name=twitter:title]");
            titleQueries.add("meta[property=og:site_name]");

            title = getTitle(titleQueries, doc);

            if (StringUtils.isNotEmpty(title)) {
                return title;
            }
        } catch (Exception e) {
            log.error(String.format("Exception in getting title for domain %s", processingId), e);
            return null;
        }

        return null;
    }

    private String getTitle(List<String> titleQueries, Document doc) {
        String title = null;
        title = doc.title();
        if (StringUtils.isNotEmpty(title)) {
            return title;
        }
        for (String query : titleQueries) {
            Elements titleElement = doc.select(query);
            title = titleElement.attr("content");
            if (StringUtils.isNotEmpty(title)) {
                break;
            }
        }
        return title;
    }
}
