package com.infynd.DatavesterExtractor.SingleExtractor.Fullcrawler_lite.name;


import com.infynd.DatavesterExtractor.SingleExtractor.Fullcrawler_lite.utils.WebsiteCrawlerConstants;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.Serializable;
import java.util.*;

@Service
@Slf4j
public class NameFromTitleExtractor {

    @Autowired
    public NameFinderExtractor nameFinder;

    String[] splitChars = {"|", "-", ":", "–", "»", ">>"};
    String[] stopwords = {" to "};


    public Map<String, Serializable> extract(Map<String, Serializable> extractedData) {

        String normalizedDomain = (String) extractedData.get(WebsiteCrawlerConstants.NORMALIZED_DOMAIN);
        String processingId = (String) extractedData.get(WebsiteCrawlerConstants.PROCESSING_ID);

        try {

            String tit = (String) extractedData.get(WebsiteCrawlerConstants.TITLE);
            if (StringUtils.isNotEmpty(tit)) {
                String name = getNameFromTitle(tit, normalizedDomain);
                if (StringUtils.isNotEmpty(name)) {
                    name = removeStopWords(name, normalizedDomain);
                    if (StringUtils.isNotEmpty(name)) {
                        extractedData.put(WebsiteCrawlerConstants.NAME_FROM_TITLE, name);
                        extractedData.put(WebsiteCrawlerConstants.TRADING_COMPANY_NAME, name);
                    }
                }
            }

            String rawName = (String) extractedData.get(WebsiteCrawlerConstants.NAME_FROM_TITLE);
            if (StringUtils.isNotEmpty(rawName)) {

                // 1️⃣ Split by pipe
                String[] splitCompanies = rawName.split("\\|");
                List<String> cleanedList = new ArrayList<>();

                for (String name : splitCompanies) {
                    if (name == null) continue;

                    // 2️⃣ Basic cleaning (regex remove unwanted chars + trim + remove newlines)
                    String cleaned = name.replaceAll("[^a-zA-Z0-9\\p{Punct}À-ÿ+®° ]", "").replaceAll("[\\r\\n]", "").trim();

                    // 3️⃣ Remove invalid/junk words
                    if (!cleaned.isEmpty() && !INVALID_WORDS.contains(cleaned)) {
                        cleanedList.add(cleaned);
                    }
                }

                // 4️⃣ Optional: Split into C1–C5 (if you want to replicate SQL table)
                List<String> cList = new ArrayList<>(cleanedList);
                while (cList.size() < 5) cList.add(null); // pad to 5 elements

                // 5️⃣ Determine final company name using domain prefix
                String finalName = null;
                String domainPrefix = null;
                if (StringUtils.isNotEmpty(normalizedDomain)) {
                    domainPrefix = normalizedDomain.split("\\.")[0];
                }

                if (domainPrefix != null && domainPrefix.length() >= 3) {
                    for (String company : cleanedList) {
                        if (company == null) continue;
                        String noSpace = company.replaceAll("\\s+", "");
                        if (noSpace.length() >= 3 && domainPrefix.substring(0, 3).equalsIgnoreCase(noSpace.substring(0, 3))) {
                            finalName = company;
                            break;
                        }
                    }
                }

                // 6️⃣ Fallback: first valid name
                if (finalName == null && !cleanedList.isEmpty()) {
                    finalName = cleanedList.get(0);
                }

                // 7️⃣ Save back
                if (finalName != null && !finalName.isEmpty()) {
                    extractedData.put(WebsiteCrawlerConstants.Clean_name_FROM_TITLE, finalName);
                }
            }

        } catch (Exception e) {
            log.error("Exception in getting name from title ", e);
        }
        return extractedData;
    }

    private static final Set<String> INVALID_WORDS = new HashSet<>(Arrays.asList(
            "empty","Home","Mysite","First","Access","20","Dan.com - a GoDaddy brand","street","north","south","error",
            "not found","City Of","Central","Greater","shire","(pseudo)","road","lane","east","west","st","city","town","county","country",
            "House","Man","and","are","or","Great","Long","office","showroom","market","park",
            "Tag","SIX","ltd","limited","shop","the","this","for","that","inc","plc",
            "gmbh","uk","ceo","united kingdom","N/a","NA","Null","Facebook","twitter","instagram","youtube","blog","#Name?","none"
    ));


    private String removeStopWords(String name, String domain) {
        for (String word : stopwords) {
            String[] names = name.split(word);
            for (String companyName : names) {
                if (nameFinder.isValidLegalName(companyName, domain)) {
                    return companyName;
                }
            }
        }
        return null;
    }

    private String getNameFromTitle(String tit, String domain) {

        for (String spclChar : splitChars) {
            if (tit.contains(spclChar)) {
                String[] s;
                if (spclChar.equals("|")) {
                    s = tit.split("\\|");
                } else {
                    s = tit.split(spclChar);
                }
                for (String name : s) {
                    if (nameFinder.isValidLegalName(name, domain)) {
                        for (String spclChar1 : splitChars) {
                            if (name.contains(spclChar1)) {

                                String[] s1;
                                if (spclChar1.equals("|")) {
                                    s1 = name.split("\\|");
                                } else {
                                    s1 = name.split(spclChar1);
                                }
                                for (String name1 : s1) {
                                    if (nameFinder.isValidLegalName(name, domain)) {
                                        return name1;
                                    } else {
                                        continue;
                                    }
                                }
                            }
                        }
                        return name;
                    } else {
                        continue;
                    }
                }
            }
        }

        //match entire title
        if (nameFinder.isValidLegalName(tit, domain)) {
            return tit;
        }

        return null;
    }

}
