package com.infynd.DatavesterExtractor.SingleExtractor.Fullcrawler_lite.fax;


import com.infynd.DatavesterExtractor.SingleExtractor.Fullcrawler_lite.utils.WebsiteCrawlerConstants;
import com.infynd.DatavesterExtractor.service.FunctionInterface;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;

@Component
@Slf4j
public class FaxExtractor {
    public String FAX_REFORMATION = "(<[^<>]*>\\s*(F(\\s*<[^<>]*>\\s*)*\\s*:|Fax(\\s*<[^<>]*>\\s*)*\\s*:|Fax" + "(\\s" +
            "*<[^<>]*>\\s*)*\\s*:|Fax\\s*Us(\\s*<[^<>]*>\\s*)*\\s*:)\\s*((<[^<>]*>\\s*)*\\s*))(((" + "(\\+44\\s?\\d{4" +
            "}|\\(?0\\d{4}\\)?)\\s?\\d{3}\\s?\\d{3})|((\\+44\\s?\\d{3}|\\(?0\\d{3}\\)?)" + "\\s?\\d{3}\\s?\\d{4})|" +
            "(\\+44\\s?\\(?0\\)\\d{2}\\s*\\d{3,4}\\s*\\d{3,4})|((\\+44\\s?\\d{2}|\\(?0\\d{2}\\)" + "?)\\s?\\d{4}\\s" +
            "?\\d{4}))(\\s?\\#(\\d{4}|\\d{3}))?|(\\+44\\s?7\\d{3}|\\(?07\\d{3}\\)?)" + "\\s?\\d{3}\\s?\\d{3}|((\\" +
            "(?0\\d{4}\\)?\\s?\\d{3}\\s?\\d{3})|(\\(?0\\d{3}\\)?\\s?\\d{3}\\s?\\d{4})|(\\" + "(?0\\d{2}\\)" +
            "?\\s?\\d{4}\\s?\\d{4}))(\\s?\\#(\\d{4}|\\d{3}))?)";
    @Autowired
    FunctionInterface functionInterface;

    public Map<String, Serializable> extract(Map<String, Serializable> extractedData) {
        String normalizedDomain = (String) extractedData.get(WebsiteCrawlerConstants.NORMALIZED_DOMAIN);
        String rawHtml = (String) extractedData.get(WebsiteCrawlerConstants.RAW_HTML);
        String contactHtml = (String) extractedData.get(WebsiteCrawlerConstants.CONTACT_US_HTML);
        StringBuilder concatenatedHtmlBuilder = new StringBuilder();
        if (rawHtml != null) {
            concatenatedHtmlBuilder.append(rawHtml);
        }
        if (contactHtml != null) {
            concatenatedHtmlBuilder.append(contactHtml);
        }
        String concatenatedHtml = concatenatedHtmlBuilder.toString();
        Set<String> faxNumber = new HashSet<>();
        String formattedFax = "";

        try {

            String cleansedContent = functionInterface.normalizePageSource(concatenatedHtml);
            cleansedContent = functionInterface.cleanPageSource(cleansedContent);


            try {
                Optional<Set<String>> extractedFax = extractFax(cleansedContent);
                extractedFax.ifPresent(faxNumber::addAll);
                formattedFax = formattedFax(extractedFax);
                log.info("Fax Module Completed.....");

            } catch (Exception exception) {
                log.error("Error in Fax Extraction Logic ..." + exception.getLocalizedMessage());
            }


            if (CollectionUtils.isNotEmpty(Collections.singleton(faxNumber))) {

                extractedData.put(WebsiteCrawlerConstants.FAX, faxNumber.parallelStream().collect(Collectors.joining(
                        "|")));

                extractedData.put(WebsiteCrawlerConstants.FAX_FORMATTED, formattedFax);

                String faxValue = (String) extractedData.get(WebsiteCrawlerConstants.FAX);
            }
        } catch (Exception exception) {
            log.error("Error in Fax Extraction for ID {} ,  - {}", normalizedDomain, exception.getLocalizedMessage());
        }
        return extractedData;
    }

    public Optional<Set<String>> extractFax(String cleansedContent) {
        Set<String> faxMatchedBlocks = functionInterface.regexMatchedSetData(cleansedContent, FAX_REFORMATION, 9);
        return Optional.ofNullable(faxMatchedBlocks);
    }

    public String formattedFax(Optional<Set<String>> extractedFaxNumber) {
        Set<String> formattedSet =
                extractedFaxNumber.get().parallelStream()
                        .map(x -> x.replaceAll("(?sim)[\\(\\-\\s),\\+\\|\\*\\.\\:]", "").trim())
                        .filter(x -> !x.equals("01625 524 510".replaceAll("(?sim)[\\(\\-\\s),\\+\\|\\*\\.\\:]", "").trim()))
                        .filter(x -> x.length() > 8)
                        .collect(Collectors.toSet());
        String formattedFax = formattedSet.parallelStream().collect(Collectors.joining("|"));
        return formattedFax;
    }


}
