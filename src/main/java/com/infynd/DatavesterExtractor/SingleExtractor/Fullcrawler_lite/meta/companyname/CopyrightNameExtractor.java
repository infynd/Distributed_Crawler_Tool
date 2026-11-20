package com.infynd.DatavesterExtractor.SingleExtractor.Fullcrawler_lite.meta.companyname;

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
public class CopyrightNameExtractor extends BaseLegalNameFinder {

    private static final int MAX_COPYRIGHT_WHOLE_CONTENT_TEXT_LENGTH = 250;
    private static final int MIN_LEGAL_NAME_LENGTH = 3;
    private static final int LEGAL_NAME_CHECK_THRESHOLD = 3;
    private static final String COPYRIGHT_SELECTOR = "*:containsOwn(©),*:containsOwn(Copyright),div[id=copyright],meta[name=rights]";

    @Autowired
    public NameFinder nameFinder;

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
//           System.out.println("Raw Copyright Input: {}"+ inputString);

            tempLegalNames2 = extractAllRightsReserved(inputString);
//            System.out.println("Extracted from All Rights Reserved: {}"+ tempLegalNames2);
            for (String legalName : tempLegalNames2) {
                // Strip all left and right of copyright and year
                tempLegalNames3.addAll(extractCopyrightAndYear(legalName));
            }
//            System.out.println("After extractCopyrightAndYear: {}"+ tempLegalNames3);


            tempLegalNames2.clear();
            for (String string : tempLegalNames3) {
                tempLegalNames2.addAll(splitAddressSaperators(string, domainName));
            }
//            System.out.println("After splitAddressSeparators: {}"+ tempLegalNames2);

            tempLegalNames3.clear();
            // Strip the address
            for (String legalName : tempLegalNames2) {
                if (!StringUtils.isEmpty(legalName)) {
                    legalName = stripAddress(legalName);
                    if (!StringUtils.isEmpty(legalName)) {
                        tempLegalNames3.add(legalName);
//                        System.out.println("After stripAddress: {}"+ legalName);
                    }
                }
            }



            // reusing the old arrays
            tempLegalNames2.clear();
            for (String legalName : tempLegalNames3) {
                if (!StringUtils.isEmpty(legalName)) {
                    // Strip the address
                    // reference
                    String tmpLegalName = stripSiteDevelopedByContent(legalName);
                    if (!StringUtils.isEmpty(tmpLegalName)) {
                        tempLegalNames2.add(tmpLegalName);
                    }
                }
            }

            // reusing the old arrays
            tempLegalNames3.clear();
            for (String legalName : tempLegalNames2) {
                String tmpLegalName = stripInvalidCharacters(legalName);
                tmpLegalName = stripAddress(tmpLegalName);
                tmpLegalName = stripStopWord(tmpLegalName.trim());
//                System.out.println("Before Final Add "+ legalName);
//                System.out.println("Original"+ tmpLegalName);


                if (!StringUtils.isEmpty(tmpLegalName) && (tmpLegalName.length() >= MIN_LEGAL_NAME_LENGTH)) {
                    tempLegalNames3.add(tmpLegalName);
                }
            }

            // remove the irrelevant legal names
            legalNames.addAll(tempLegalNames3);
//            System.out.println("Original"+ tempLegalNames3);
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
                    for (int i = 0; i < arrayListNames.size() && i < LEGAL_NAME_CHECK_THRESHOLD; i++) {
                        if (nameFinder.isValidLegalName(arrayListNames.get(i), normalizedDomain)) {
                            if (arrayListNames.get(i) != null && !arrayListNames.get(i).isEmpty()) {
//                                System.out.println(  "arrayListNames.get(i)" + arrayListNames.get(i));
                                extractedData.put(WebsiteCrawlerConstants.COPYRIGHT_NAME, arrayListNames.get(i));
                                extractedData.put(WebsiteCrawlerConstants.TRADING_COMPANY_NAME, arrayListNames.get(i));
                            }
                            cnameFound = false;
                        }
                    }
                }
                if (cnameFound) {
                    String copyrightText = getfulltext(document, domainName);
                    if (copyrightText != null) {
                        String normalizedName = getnormalized(copyrightText, domainName);
                        if (normalizedName != null && !normalizedName.trim().isEmpty()) {
//                            System.out.println(  "normalizedName" + normalizedName);
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


        } catch (Exception e) {
            log.error(String.format("Error while parsing company name from copyright for %s", processingId), e);
        }
        return extractedData;
    }

    private String getpatternmacher(String copyrightName, String normalizedDomain) {
        // Step 1: Clean input - remove symbols like ©, (C), Copyright, etc.
        copyrightName = copyrightName
                .replaceAll("(?i)©|\\(c\\)|copyright", "") // case-insensitive
                .replaceAll("\\s+", " ") // clean multiple spaces
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



    private String getnormalized(String copyrightText, String normalizedDomain) {
        // Clean up: remove symbols like ©, Copyright
        String cleanedText = copyrightText
                .replaceAll("(?i)©|\\(c\\)|copyright", "")
                .replaceAll("\\s+", " ")
                .trim();

        String[] words = cleanedText.split(" ");

        for (int i = 0; i < words.length; i++) {
            String cleanWord = words[i].replaceAll("[^a-zA-Z0-9]", "").toLowerCase();

            if (normalizedDomain.toLowerCase().contains(cleanWord)) {
                // Start collecting possible company name
                StringBuilder sb = new StringBuilder();
                sb.append(words[i]);

                // Add the next few words if they look like part of a company name
                for (int j = i + 1; j < words.length; j++) {
                    String suffix = words[j].replaceAll("[^a-zA-Z]", "").toLowerCase();

                    if (suffix.matches("ltd|limited|llp|plc|inc|corp|company")) {
                        sb.append(" ").append(words[j]);
                        break;
                    } else if (suffix.matches("all|rights|reserved")) {
                        break; // stop if it goes beyond company name
                    } else {
                        sb.append(" ").append(words[j]);
                    }
                }

                return sb.toString().trim();
            }
        }

        return null; // nothing matched
    }
}
