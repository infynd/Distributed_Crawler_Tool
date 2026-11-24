package com.infynd.DatavesterExtractor.SingleExtractor.Fullcrawler_lite.companynumber;


import com.infynd.DatavesterExtractor.SingleExtractor.Fullcrawler_lite.utils.WebsiteCrawlerConstants;
import com.infynd.DatavesterExtractor.SingleExtractor.Fullcrawler_lite.utils.service.RestUtilExtractor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Response;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class ClearbitProcessorNameExtractor {
    @Autowired
    RestUtilExtractor RestUtil;

    public Map<String, Serializable> extract(Map<String, Serializable> extractedData) throws IOException {
        String domain = (String) extractedData.get(WebsiteCrawlerConstants.NORMALIZED_DOMAIN);

        String url = "https://autocomplete.clearbit.com/v1/companies/suggest?query=" + domain;
        Response apiResponse = null;
        Response logoResponse = null;
        try {
            apiResponse = RestUtilExtractor.getAPICall(url, null);
            if (apiResponse != null) {
                String apiResponseStr = apiResponse.body().string();
                JSONArray jsonArray = new JSONArray(apiResponseStr);
                List<String> namesList = new ArrayList<>();
                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject jsonObject = jsonArray.getJSONObject(i);
                    String name = jsonObject.getString("name");
                    namesList.add(name);
                }
                String joinedNames = String.join(" | ", namesList);
                extractedData.put(WebsiteCrawlerConstants.CLEARBIT_NAME, joinedNames);
                extractedData.put(WebsiteCrawlerConstants.TRADING_COMPANY_NAME, joinedNames);
            }
        } catch (JSONException e) {
            throw new RuntimeException(e);
        } finally {
            if (apiResponse != null) {
                apiResponse.body().close();
            }
        }
        return extractedData;
    }
}