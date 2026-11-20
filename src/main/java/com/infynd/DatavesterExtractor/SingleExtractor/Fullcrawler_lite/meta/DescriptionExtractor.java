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
public class DescriptionExtractor {

    public String extractDescription(Document doc, String domain, String processingId) throws Exception {

        try {

            String description = null;
            List<String> descriptionQueries = new ArrayList<>();
            descriptionQueries.add("meta[name=description]");
            descriptionQueries.add("meta[property=description]");
            descriptionQueries.add("meta[name=og:description]");
            descriptionQueries.add("meta[property=og:description]");
            descriptionQueries.add("meta[property=twitter:description]");
            descriptionQueries.add("meta[http-equiv=Description]");
            // descriptionQueries.add("name=description");

            description = getDescription(descriptionQueries, doc);

            if (StringUtils.isNotEmpty(description)) {
                return description;
            }


        } catch (Exception e) {
            log.error(String.format("Exception in getting description for domain %s", processingId), e);
            return null;
        }

        return null;
    }

    private String getDescription(List<String> descriptionQueries, Document doc) {
        String description = null;
        for (String query : descriptionQueries) {
            Elements des = doc.select(query);
            description = des.attr("content");
            if (StringUtils.isNotEmpty(description)) {
                break;
            }
        }
        return description;
    }
}
