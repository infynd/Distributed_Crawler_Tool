package com.infynd.DatavesterExtractor.SingleExtractor.Fullcrawler_lite.meta;

import lombok.extern.slf4j.Slf4j;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class NameExtractor {

    public String extractName(Document doc, String normalizedDomain, String processingId) {
        try {
            Elements metaTag = doc.select("meta[property=og:site_name]");
            if (metaTag != null && metaTag.hasAttr("content")) {
                return metaTag.attr("content");
            }
        } catch (Exception e) {
            log.error(String.format("Exception in getting name for domain %s", processingId), e);
        }
        try {
            Elements jsonLds = doc.select("script[type=application/ld+json]");
            for (Element jsonLd : jsonLds) {
                String jsonData = jsonLd.html();
                try {
                    // Parse JSON data
                    JSONObject jsonObject = new JSONObject(jsonData);
                    if (jsonObject.has("@graph")) {
                        JSONArray graphArray = jsonObject.getJSONArray("@graph");
                        for (int i = 0; i < graphArray.length(); i++) {
                            JSONObject item = graphArray.getJSONObject(i);
                            if (item.has("@type") && item.getString("@type").equals("WebSite") && item.has("name")) {
                                return item.getString("name");
                            }
                        }
                    }

                } catch (org.json.JSONException je) {

                    // ✅ ADDITION: handle JSON array case
                    JSONArray jsonArray = new JSONArray(jsonData);
                    for (int i = 0; i < jsonArray.length(); i++) {
                        JSONObject jsonObject = jsonArray.getJSONObject(i);
                        if (jsonObject.has("@graph")) {
                            JSONArray graphArray = jsonObject.getJSONArray("@graph");
                            for (int j = 0; j < graphArray.length(); j++) {
                                JSONObject item = graphArray.getJSONObject(j);
                                if (item.has("@type")
                                        && item.getString("@type").equals("WebSite")
                                        && item.has("name")) {
                                    return item.getString("name");
                                }
                            }
                        }
                    }

                } catch (Exception e) {
                    log.error("Exception in parsing JSON-LD for domain %s", e);
                }
            }
        } catch (Exception e) {
            log.error("Exception in getting JSON-LD data for domain %s", e);
        }
        return null;
    }
}
