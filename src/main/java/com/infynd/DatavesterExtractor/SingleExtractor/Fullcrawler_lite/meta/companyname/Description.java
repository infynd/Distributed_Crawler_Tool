package com.infynd.DatavesterExtractor.SingleExtractor.Fullcrawler_lite.meta.companyname;


import com.infynd.DatavesterExtractor.MapClass.OutputMap;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class Description {


    public Map<String, String> extract(Map<String, String> outputEntity, String doc) throws IOException {
        if (outputEntity.get(OutputMap.DOMAIN) != null) {
            try {
                String description = null;
                List<String> descriptionQueries = new ArrayList<>();
                descriptionQueries.add("meta[name=description]");
                descriptionQueries.add("meta[property=description]");
                descriptionQueries.add("meta[name=og:description]");
                descriptionQueries.add("meta[property=og:description]");
                descriptionQueries.add("meta[property=twitter:description]");
                descriptionQueries.add("meta[http-equiv=Description]");

                description = getDescription(descriptionQueries, doc);

                if (StringUtils.isNotEmpty(description)) {
                    outputEntity.put(OutputMap.DESCRIPTION, description);
                }

            } catch (Exception e) {
                log.error("Exception in getting description for domain {}", outputEntity.get("Domain"), e);
                return null;
            }

            return outputEntity;
        }

        return null; // or return outputEntity; depending on your logic
    }

    private String getDescription(List<String> descriptionQueries, String doc) {
        String description = null;
        for (String query : descriptionQueries) {
            // Use Jsoup to parse the HTML document
            Document document = Jsoup.parse(doc);
            Elements des = document.select(query);
            description = des.attr("content");
            if (StringUtils.isNotEmpty(description)) {
                break;
            }
        }
        return description;
    }
}