package com.infynd.DatavesterExtractor.SingleExtractor.Fullcrawler_lite.companynumber;


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
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@Slf4j
public class CompanyNameExtraction {
    public Map<String, Serializable> extract(Map<String, Serializable> extractedData) throws Exception {
        String rawHtml = (String) extractedData.get(WebsiteCrawlerConstants.RAW_HTML);
        String domain = (String) extractedData.get(WebsiteCrawlerConstants.NORMALIZED_DOMAIN);
        if (rawHtml != null) {
            String firstFiveLetters = domain.substring(0, Math.min(domain.length(), 3));
            rawHtml = StringUtils.replace(rawHtml, "@|!", ",");
            Document doc = Jsoup.parse(rawHtml);
            Elements elements = doc.getAllElements();
            StringBuilder plainText = new StringBuilder();
            for (Element element : elements) {
                plainText.append(element.ownText());
            }
            String text = plainText.toString();
            Pattern pattern = Pattern.compile("(?i)\\s*([\\w\\s]+)\\s*LTD|(?i)\\s*([\\w\\s]+)\\s*LIMITED");
            Matcher matcher = pattern.matcher(text);
            Set<String> matchedResults = new HashSet<>();
            Set<String> matchedNames = new HashSet<>();
            int count = 0;
            try {
                Set<String> extractedNames = new HashSet<>(); // Use a Set to store unique extracted names
                StringBuilder extractedNames1 = new StringBuilder(); // Use StringBuilder to store multiple values with pipe character
                if (matcher.find()) {
                    while (matcher.find()) {
                        String matchedText = matcher.group().trim();
                        if (matchedText.length() <= 35) {
                            count++;
                            matchedResults.add(matchedText);
                        }
                        String extractedText = "";
                        if (CollectionUtils.isNotEmpty(matchedResults)) {
                            String matc = matchedText.replaceAll("\\b(follow|copyright|for|if|more|visit|contact|not|and|few|come|2020|2021|2022|2023|details|our|name|company|we|are|then|me|will|as|give|is|by|your|a|had|have|they|find|ur|be|us|about|to)\\b", "");
                            if (matc.toLowerCase().startsWith(firstFiveLetters.toLowerCase())) {
                                extractedText = matc.substring(0, Math.min(35, matc.length())).trim();
                            }
                            if (!extractedNames.contains(extractedText)) { // Check for uniqueness
                                extractedNames.add(extractedText);
                                extractedNames1.append("|").append(extractedText); // Append to StringBuilder
                            }
                        }
                        StringBuilder sb = new StringBuilder();
                        if (matchedText.length() >= 35) {
                            count++;
                            sb.append(matchedText.substring(matchedText.length() - 34));
                            String matchedData = sb.toString();
                            if (matchedData.toLowerCase().startsWith(firstFiveLetters.toLowerCase())) {
                                extractedText = matchedData.substring(0, Math.min(35, matchedData.length())).trim();
                                if (!extractedNames.contains(extractedText)) { // Check for uniqueness
                                    extractedNames.add(extractedText);
                                    if (extractedNames1.length() > 0) {
                                        extractedNames1.append("|"); // Add pipe separator if needed
                                    }
                                    extractedNames1.append(extractedText);
                                }
                            }
                        }
                    }
                }
                List<String> matchedNames1 = extractedNames.stream()
                        .filter(name -> name != null && !name.trim().isEmpty()) // Remove null and empty values
                        .collect(Collectors.toList());
                String joinedNames = String.join("|", matchedNames1).trim(); // Join and trim any leading/trailing spaces
                if (!joinedNames.isEmpty()) {
                    extractedData.put(WebsiteCrawlerConstants.REG_COMPANY_NAME, joinedNames);
                }

                StringBuilder extractedNames4 = new StringBuilder(); // Use StringBuilder to store multiple values with pipe character
                Set<String> uniqueExtractedNames = new HashSet<>(); // Use a Set to store unique extracted names
                if (!matcher.find()) {
                    Pattern pattern2 = Pattern.compile("(?i)\\s*([\\w\\s.]+)\\s*LTD|(?i)\\s*([\\w\\s.]+)\\s*LIMITED");
                    Matcher matcher2 = pattern2.matcher(text);
                    while (matcher2.find()) {
                        String matchedText = matcher2.group().trim();
                        if (matchedText.length() <= 35) {
                            count++;
                            matchedResults.add(matchedText);
                        }
                        String extractedText = "";
                        if (CollectionUtils.isNotEmpty(matchedResults)) {
                            String matc = matchedText.replaceAll("\\b(follow|copyright|for|if|more|visit|contact|not|and|few|come|2020|2021|2022|2023|details|our|name|company|we|are|then|me|will|as|give|is|by|your|a|had|have|they|find|ur|be|us|about|to)\\b", "");
                            if (matc.toLowerCase().startsWith(firstFiveLetters.toLowerCase())) {
                                extractedText = matc.substring(0, Math.min(35, matc.length())).trim();

                                if (!uniqueExtractedNames.contains(extractedText)) {
                                    uniqueExtractedNames.add(extractedText);
                                    if (extractedNames4.length() > 0) {
                                        extractedNames4.append("|"); // Add pipe separator if there are already extracted names
                                    }
                                    extractedNames4.append(extractedText);
                                }
                            }
                        }
                        StringBuilder sb = new StringBuilder();
                        if (matchedText.length() >= 35) {
                            count++;
                            sb.append(matchedText.substring(matchedText.length() - 34));
                            String matchedData = sb.toString();
                            if (matchedData.toLowerCase().startsWith(firstFiveLetters.toLowerCase())) {
                                extractedText = matchedData.substring(0, Math.min(35, matchedData.length())).trim();

                                if (!uniqueExtractedNames.contains(extractedText)) {
                                    uniqueExtractedNames.add(extractedText);
                                    if (extractedNames4.length() > 0) {
                                        extractedNames4.append("|"); // Add pipe separator if there are already extracted names
                                    }
                                    extractedNames4.append(extractedText);
                                }
                                matchedNames.add(extractedText);
                            }
                        }
                    }
                    String joinedNames2 = extractedNames4.toString().trim(); // Trim leading and trailing spaces
                    joinedNames2 = joinedNames2.replaceAll("^\\|+", "").replaceAll("\\|+$", "");
                    if (!joinedNames2.isEmpty()) {
                        extractedData.put(WebsiteCrawlerConstants.REG_COMPANY_NAME, joinedNames2);
                    }
                }
            } catch (Exception e) {
                throw new RuntimeException("Exception in matching - {}", e);
            }
        }
        return extractedData;
    }
}