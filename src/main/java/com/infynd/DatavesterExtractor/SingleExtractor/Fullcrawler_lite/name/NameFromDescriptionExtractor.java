package com.infynd.DatavesterExtractor.SingleExtractor.Fullcrawler_lite.name;


import com.infynd.DatavesterExtractor.SingleExtractor.Fullcrawler_lite.utils.WebsiteCrawlerConstants;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Service
@Slf4j
public class NameFromDescriptionExtractor {

    @Autowired
    NameFinderExtractor nameFinder;

    String[] stopWords = {" is ", " specializes ", " operated ", " provides ", " incorporated "};
    String[] irreleventList = {"(c)", " by ", "sitemap", "site map", "contact", "contact us", "revised", "-", "|", ":", "::",
            ".", ",", "copyright", "fax", "tel", "privacy statement", "statement", "(", ")", "why cookies", " of ", " to ",
            "_", "design"};

    public Map<String, Serializable> extract(Map<String, Serializable> extractedData) {

        String normalizedDomain = (String) extractedData.get(WebsiteCrawlerConstants.NORMALIZED_DOMAIN);
        String processingId = (String) extractedData.get(WebsiteCrawlerConstants.PROCESSING_ID);

        try {

            String description = (String) extractedData.get(WebsiteCrawlerConstants.DESCRIPTION);
            if (StringUtils.isNotEmpty(description)) {
                String name = getNameFromDescription(description, normalizedDomain);
                if (StringUtils.isNotEmpty(name)) {
                    extractedData.put(WebsiteCrawlerConstants.NAME_FROM_DESC, name);
                    extractedData.put(WebsiteCrawlerConstants.TRADING_COMPANY_NAME, name);
                }
            }

        } catch (Exception e) {
            log.error(String.format("Exception in getting name from description for domain %s", processingId), e);
        }
        return extractedData;
    }

    private String getNameFromDescription(String description, String domain) {

        Set<String> names = getLegalName(description);
        for (String name : names) {
            if (nameFinder.isValidLegalName(name, domain)) {
                return name;
            }
        }
        return null;
    }

    Set<String> getLegalName(String description) {

        String descString = getFirstNWords(description);

        Set<String> legalnames = new HashSet<>();
        String legalName;
        if (!StringUtils.isEmpty(descString) && canExtractDescription(descString)) {

            legalName = extractLegalName(descString);
            if (!StringUtils.isEmpty(legalName)) {
                legalName = stripStopWord(legalName);
                legalName = normalizeAndTrim(legalName);
                if (!StringUtils.isEmpty(legalName)) {
                    legalnames.add(legalName);
                }
            }
        }
        return legalnames;
    }

    private String extractLegalName(String descriptionString) {
        String tmpLegalName = null;
        for (String stopWord : stopWords) {
            int index = StringUtils.indexOf(descriptionString.toLowerCase(), stopWord);
            if (index > 0) {
                tmpLegalName = StringUtils.substring(descriptionString, 0, index);
                break;
            }
        }

        return tmpLegalName;
    }

    private boolean canExtractDescription(String descrptionString) {
        boolean isValid = false;

        for (String stopWord : stopWords) {
            if (StringUtils.containsIgnoreCase(descrptionString, stopWord)) {
                isValid = true;
                break;
            }
        }

        return isValid;
    }

    private String getFirstNWords(String string) {

        int index;
        int count = 0;

        for (index = 0; index < string.length(); index++) {

            if (string.charAt(index) == ' ') {
                count++;
            }

            if (count >= 5) {
                break;
            }
        }

        return StringUtils.substring(string, 0, index);
    }

    private String stripStopWord(String inputText) {
        for (String irreleventWord : irreleventList) {
            if (inputText.toLowerCase().startsWith(irreleventWord)) {
                inputText = inputText.substring(irreleventWord.length());
            }

            if (inputText.toLowerCase().endsWith(irreleventWord)) {
                inputText = inputText.substring(0, inputText.length() - irreleventWord.length());
            }

            if (!StringUtils.isEmpty(inputText)) {
                inputText = StringUtils.trim(inputText);
            }
        }
        return inputText;
    }

    private String normalizeAndTrim(String text) {
        if (text != null) {
            text = text.replace(String.valueOf((char) 160), " ").trim();
        }
        return text;
    }

}
