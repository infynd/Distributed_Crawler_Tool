package com.infynd.DatavesterExtractor.SingleExtractor.Fullcrawler_lite.name;


import com.infynd.DatavesterExtractor.SingleExtractor.Fullcrawler_lite.utils.WebsiteCrawlerConstants;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.Serializable;
import java.util.*;

@Service
@Slf4j
public class CopyrightNameExtraction extends BaseLegalNameFinderExtractor {

        private static final int MAX_COPYRIGHT_WHOLE_CONTENT_TEXT_LENGTH = 250;
        private static final int MIN_LEGAL_NAME_LENGTH = 3;
        private static final int LEGAL_NAME_CHECK_THRESHOLD = 3;
        private static final String COPYRIGHT_SELECTOR = "*:containsOwn(©),*:containsOwn(Copyright),div[id=copyright],meta[name=rights]";

        @Autowired
        public NameFinderExtractor nameFinder;
        Set<String> getLegalName(Document document, String domainName) {


            Elements copyrightsElements = document.select(COPYRIGHT_SELECTOR);
            Set<String> legalNames = new HashSet<String>();
            Set<String> tempLegalNames2 = new HashSet<String>();
            Set<String> tempLegalNames3 = new HashSet<String>();

            for (Element element : copyrightsElements) {
                String inputString = null;
                String tempString = convertNodeToText(element);

                for (String s : StringUtils.split(tempString, '\n')) {
                    if (StringUtils.contains(s, '©')) {
                        inputString = s;
                        break;
                    }
                    if (StringUtils.containsIgnoreCase(s, "Copyright")) {
                        inputString = s;
                        break;
                    }
                }
                if (inputString == null) {
                    inputString = StringUtils.replace(tempString, "\n", " ");
                }

                // if the conent is more than 1000 charas, skip it.
                if (inputString.length() > MAX_COPYRIGHT_WHOLE_CONTENT_TEXT_LENGTH) {
                    continue;
                }
                if (!StringUtils.isEmpty(inputString)) {
                    inputString = inputString.trim();
                }

                tempLegalNames2 = extractAllRightsReserved(inputString);
//            System.out.println("Extracted from All Rights Reserved: {}"+ tempLegalNames2);
                for (String legalName : tempLegalNames2) {
                    // Strip all left and right of copyright and year
                    tempLegalNames3.addAll(extractCopyrightAndYear(legalName));
                }

                legalNames.addAll(tempLegalNames3);

            }
            return legalNames;
        }


        private String getNameFromDomain(String domain) {
            if (StringUtils.isNotEmpty(domain)) {
                if (StringUtils.contains(domain, ".")) return StringUtils.split(domain, ".")[0];
            }
            return null;
        }

        public Map<String, Serializable> extract(Map<String, Serializable> extractedData) {

            String normalizedDomain = (String) extractedData.get(WebsiteCrawlerConstants.NORMALIZED_DOMAIN);
            String processingId = (String) extractedData.get(WebsiteCrawlerConstants.PROCESSING_ID);

            try {
                boolean cnameFound = true;
                String domainName = getNameFromDomain(normalizedDomain);
                if (extractedData.get(WebsiteCrawlerConstants.RAW_HTML) != null) {
                    String rawHtml = StringUtils.replace((String) extractedData.get(WebsiteCrawlerConstants.RAW_HTML), "@|!", ",");
                    Document document = Jsoup.parse(rawHtml);
                    Set<String> legalNames = getLegalName(document, domainName);
//                System.out.println(  "legalNames" + legalNames);
                    if (legalNames != null & legalNames.size() > 0) {
                        List<String> arrayListNames = new ArrayList<>(legalNames);


                        List<String> validNames = new ArrayList<>();

                        for (String candidate : arrayListNames) {
                            if (candidate == null || candidate.trim().isEmpty() || candidate.trim().equals("-")) {
                                continue; // Skip invalid names
                            }
                            candidate = candidate.trim();
//                        System.out.println("Valid candidate: " + candidate);
                            validNames.add(candidate);
                        }

                        if (!validNames.isEmpty()) {
                            extractedData.put(WebsiteCrawlerConstants.COPYRIGHT_NAME, String.join(" | ", validNames)); // use delimiter if needed
                            extractedData.put(WebsiteCrawlerConstants.TRADING_COMPANY_NAME, String.join(" | ", validNames));
                            cnameFound = false;
                        }
                    }


                    if (cnameFound) {
                        String copyrightText = getfulltext(document, domainName);
                        if (copyrightText != null) {
                            String normalizedName = getnormalized(copyrightText, domainName);
                            if (normalizedName != null && !normalizedName.trim().isEmpty()) {
                                extractedData.put(WebsiteCrawlerConstants.COPYRIGHT_NAME2, normalizedName);
                                extractedData.put(WebsiteCrawlerConstants.TRADING_COMPANY_NAME, normalizedName);
                                cnameFound = false;
                            }
                        }
                    }

                    if (cnameFound) {
                        String copyrightName = getfulltext(document, domainName);
                        if (copyrightName != null) {
//                        System.out.println(  "copyrightName" + copyrightName);
                            String patternMatchedName = getpatternmacher(copyrightName, domainName);
                            if (StringUtils.isNotEmpty(patternMatchedName)) {
                                if (!patternMatchedName.trim().isEmpty()) {
//                                System.out.println(  "patternMatchedName" + patternMatchedName);
                                    extractedData.put(WebsiteCrawlerConstants.COPYRIGHT_NAME2, patternMatchedName);
                                    extractedData.put(WebsiteCrawlerConstants.TRADING_COMPANY_NAME, patternMatchedName);
                                }
                            }
                        }
                    }
                }

                String rawCompany = (String) extractedData.get(WebsiteCrawlerConstants.COPYRIGHT_NAME);

                if (rawCompany != null && !rawCompany.trim().isEmpty()) {
                    String domainPrefix = null;
                    String normalizedDomain1 = (String) extractedData.get(WebsiteCrawlerConstants.NORMALIZED_DOMAIN);
                    if (normalizedDomain1 != null && !normalizedDomain1.isEmpty()) {
                        domainPrefix = normalizedDomain1.split("\\.")[0]; // get prefix before first dot
                    }

                    // Split the raw company names by pipe
                    List<String> companyList = new ArrayList<>();
                    for (String name : rawCompany.split("\\|")) {
                        String cleaned = CompanyNameCleaner.clean(name);
                        if (cleaned != null && !cleaned.isEmpty()) {
                            companyList.add(cleaned);
                        }
                    }

                    String finalCompanyName = null;

                    if (domainPrefix != null && !domainPrefix.isEmpty()) {
                        // Match first 3 letters of domain prefix with cleaned company names
                        for (String company : companyList) {
                            String companyNoSpace = company.replaceAll("\\s+", "");
                            if (companyNoSpace.length() >= 3 && domainPrefix.length() >= 3
                                    && domainPrefix.substring(0, 3).equalsIgnoreCase(companyNoSpace.substring(0, 3))) {
                                finalCompanyName = company;
                                break;
                            }
                        }
                    }

                    // fallback to first name if no match found
                    if (finalCompanyName == null && !companyList.isEmpty()) {
                        finalCompanyName = companyList.get(0);
                    }

                    // save the final cleaned company name
                    if (finalCompanyName != null && !finalCompanyName.isEmpty()) {
                        extractedData.put(WebsiteCrawlerConstants.COPYRIGHT_NAME1_CLEAN, finalCompanyName);
                    }
                }

                // Inside extract method, after processing COPYRIGHT_NAME2
                String rawCompany2 = (String) extractedData.get(WebsiteCrawlerConstants.COPYRIGHT_NAME2);

                if (rawCompany2 != null && !rawCompany2.trim().isEmpty()) {
                    String domainPrefix = null;
                    String normalizedDomain2 = (String) extractedData.get(WebsiteCrawlerConstants.NORMALIZED_DOMAIN);
                    if (normalizedDomain2 != null && !normalizedDomain2.isEmpty()) {
                        domainPrefix = normalizedDomain2.split("\\.")[0]; // get prefix before first dot
                    }

                    // Split by pipe and clean each segment
                    List<String> companyList = new ArrayList<>();
                    for (String name : rawCompany2.split("\\|")) {
                        String cleaned = CompanyNameCleaner.clean(name); // reuse the same cleaner
                        if (cleaned != null && !cleaned.isEmpty()) {
                            companyList.add(cleaned);
                        }
                    }

                    // Further cleaning: remove empty or junk names (like SQL nullifying)
                    List<String> filteredList = new ArrayList<>();
                    Set<String> invalidWords = new HashSet<>(Arrays.asList(
                            "empty","Home","Mysite","First","Access","20","Dan.com - a GoDaddy brand","street","north","south","error",
                            "not found","City Of","Central","Greater","shire","(pseudo)","road","lane","east","west","st","city","town","county","country",
                            "House","Man","and","are","or","Great","Long","office","showroom","market","park",
                            "Tag","SIX","ltd","limited","shop","the","this","for","that","inc","plc",
                            "gmbh","uk","ceo","united kingdom","N/a","NA","Null","Facebook","twitter","instagram","youtube","blog","#Name?","none"
                    ));

                    for (String company : companyList) {
                        String trimmed = company.trim();
                        if (!trimmed.isEmpty() && !invalidWords.contains(trimmed)) {
                            filteredList.add(trimmed);
                        }
                    }

                    // Determine final company name by domain prefix match
                    String finalCompanyName2 = null;
                    if (domainPrefix != null && !domainPrefix.isEmpty()) {
                        for (String company : filteredList) {
                            String companyNoSpace = company.replaceAll("\\s+", "");
                            if (companyNoSpace.length() >= 3 && domainPrefix.length() >= 3 &&
                                    domainPrefix.substring(0, 3).equalsIgnoreCase(companyNoSpace.substring(0, 3))) {
                                finalCompanyName2 = company;
                                break;
                            }
                        }
                    }

                    // fallback to first name if no match found
                    if (finalCompanyName2 == null && !filteredList.isEmpty()) {
                        finalCompanyName2 = filteredList.get(0);
                    }

                    // save cleaned COPYRIGHT_NAME2
                    if (finalCompanyName2 != null && !finalCompanyName2.isEmpty()) {
                        extractedData.put(WebsiteCrawlerConstants.COPYRIGHT_NAME2_CLEAN, finalCompanyName2);
                    }
                }

            } catch (Exception e) {
                log.error(String.format("Error while parsing company name from copyright for %s", processingId), e);
            }
            return extractedData;
        }


        private static class CompanyNameCleaner {
            private static final Set<String> INVALID_WORDS = new HashSet<>(Arrays.asList(
                    "empty","Home","Mysite","First","Access","20","Dan.com - a GoDaddy brand","street","north","south","error",
                    "not found","City Of","Central","Greater","shire","(pseudo)","road","lane","east","west","st","city","town","county","country",
                    "House","Man","and","are","or","Great","Long","office","showroom","market","park",
                    "Tag","SIX","ltd","limited","shop","the","this","for","that","inc","plc",
                    "gmbh","uk","ceo","united kingdom","N/a","NA","Null","Facebook","twitter","instagram","youtube","blog","#Name?","none"
            ));

            private static String clean(String input) {
                if (input == null) return null;

                // Remove unwanted characters (like SQL REGEXP_REPLACE)
                String cleaned = input.replaceAll("[^a-zA-Z0-9\\p{Punct}À-ÿ\\s+®°]", "")
                        .replaceAll("\\r|\\n", "")
                        .trim();

                // Normalize spaces
                cleaned = StringUtils.normalizeSpace(cleaned);

                // Remove invalid words
                String[] parts = cleaned.split("\\s+");
                StringBuilder sb = new StringBuilder();
                for (String part : parts) {
                    if (!INVALID_WORDS.contains(part.trim())) {
                        sb.append(part).append(" ");
                    }
                }

                cleaned = sb.toString().trim();
                return cleaned.isEmpty() ? null : cleaned;
            }
        }



        private String getpatternmacher(String copyrightName, String normalizedDomain) {
            // Step 1: Clean input - remove symbols like ©, (C), Copyright, etc.
            copyrightName = copyrightName
                    .replaceAll("(?i)©|\\(c\\)|copyright", "") // case-insensitive
                    .replaceAll("\\s+", " ") // clean multiple spaces
                    .replaceAll("\\b\\d{4}\\b", "")
                    .replaceAll("[.,]", "")
                    .trim();

            String[] textarray = copyrightName.split(" ");

            for (int i = 0; i < textarray.length - 1; i++) {
                String modifiedWord = (textarray[i] + textarray[i + 1])
                        .replaceAll("[^a-zA-Z0-9]", "") // normalize for matching
                        .toLowerCase();

                if (modifiedWord.equals(normalizedDomain.toLowerCase())) {
                    // Start collecting from the matched index
                    StringBuilder sb = new StringBuilder();
                    sb.append(textarray[i]).append(" ").append(textarray[i + 1]);

                    // Include legal suffix if any
                    int j = i + 2;
                    while (j < textarray.length) {
                        String word = textarray[j].replaceAll("[^a-zA-Z]", "").toLowerCase();
                        if (word.matches("ltd|limited|llp|plc|inc|corp|company")) {
                            sb.append(" ").append(textarray[j]);
                            break;
                        } else if (word.equals("all") || word.equals("rights")) {
                            break;
                        }
                        j++;
                    }

                    return sb.toString().trim();
                }
            }
            return "";
        }



        private String getfulltext(Document document, String normalized) {
            String copyrighttext = null;
            Elements copyrightsElements = document.select(COPYRIGHT_SELECTOR);
            for (Element element : copyrightsElements) {
                String tempString = convertNodeToText(element);
                for (String s : StringUtils.split(tempString, '\n')) {
                    if (StringUtils.contains(s, '©')) {
                        copyrighttext = s;
                        break;
                    }
                }
            }

            return copyrighttext;
        }

        private String getnormalized(String copyrighttext, String normalized) {
            // Remove unwanted symbols like ©, years (e.g., 2024), and extra punctuation
            copyrighttext = copyrighttext.replaceAll("©", "")
                    .replaceAll("\\b\\d{4}\\b", "")
                    .replaceAll("[.,]", "")
                    .trim();

            String[] textarray = copyrighttext.split("\\s+");
            StringBuilder companyName = new StringBuilder();
            boolean foundLTD = false;

            for (int i = 0; i < textarray.length; i++) {
                String word = textarray[i];
                String wordClean = word.replaceAll("[^a-zA-Z]", "").toLowerCase();

                companyName.append(word).append(" ");

                if (wordClean.equals("ltd") || wordClean.equals("limited")) {
                    foundLTD = true;
                    break;
                }
            }

            if (foundLTD) {
                return companyName.toString().trim();
            } else {
                for (String a : textarray) {
                    if (getindex(a, normalized)) {
                        return a;
                    }
                }
            }

            return null;
        }



        private boolean getindex(String a, String normalized) {
            String lowerA = a.toLowerCase();
            String lowerNormalized = normalized.toLowerCase();
            return lowerA.contains(lowerNormalized);
        }


    }



