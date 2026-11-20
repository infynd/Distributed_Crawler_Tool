package com.infynd.DatavesterExtractor.SingleExtractor.Fullcrawler_lite.addressextraction;


import com.infynd.DatavesterExtractor.SingleExtractor.Fullcrawler_lite.utils.WebsiteCrawlerConstants;
import com.infynd.DatavesterExtractor.service.FunctionInterface;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;


@Slf4j
public class AddressBlockIdentificationExtraction {
    @Autowired
    AddressProcessFunctionInterfaceProcess addressProcessFunctionInterface;

    @Autowired
    AddressProcessServiceFunctionsProcess addressProcessServiceFunctions;
    @Autowired
    AddressExtractionProcess addressExtraction;

    @Autowired
    FunctionInterface functionInterface;


    public Map<String, Serializable> extract(Map<String, Serializable> extractedData) throws IOException {
        log.info("extraction of address and splitter module - {}", extractedData.get(WebsiteCrawlerConstants.NORMALIZED_DOMAIN));
        addressProcessFunctionInterface.loadAddressResources();
        String rawHtml = (String) extractedData.get(WebsiteCrawlerConstants.RAW_HTML);
        Document doc = Jsoup.parse(rawHtml);
        String text = doc.text().replaceAll("[\\[\\]()]", "");
        Set<String> addressSetValue = null;
        String address_pattern = null;
        if (rawHtml != null) {
            String addressLine1;
            String addressLine2;
            String addressLine3;
            String city;
            String county;
            String zipCode;
            String headQuarters;
            String extractedPhoneNumbers = "";
            String addressRelatedPhone = "";
            String extractedFaxNumbers = "";
            String addressRelatedFax = "";
            String extractedEmail = "";
            String addressRelatedEmail = "";
            String cleansedSubLinkPageSource = addressProcessFunctionInterface.normalizePageSourceAddress(rawHtml);
            cleansedSubLinkPageSource = functionInterface.cleanPageSource(cleansedSubLinkPageSource);
            cleansedSubLinkPageSource = cleansedSubLinkPageSource.replaceAll("(?sim)<font class[^<>]*>.*?</font>|<[^<>]*\\(.*?</[^<>]*>", " ").replaceAll("(?sim)" + "<div[^<>]*ease[^<>]*>.*?</div>", " ").replaceAll("(?sim)<a\\s*[^<>]*map[^<>]*>", " ").replaceAll("(?sim)<a\\s*[^<>]*map[^<>]*>", " ").replaceAll("(?sim)\\b(Wycliffe|Water Lane|Wilmslow|Cheshire|SK9 5AF)\\b", "").replaceAll("(?sim)\\b(PO Box 10 094|The Terrace|Wellington 6143)\\b", "").replaceAll("[\\[\\]()]", "").replaceAll("\\s+", " ");
            addressSetValue = this.addressBlockIdentification(cleansedSubLinkPageSource, text);
            if (addressSetValue == null || addressSetValue.isEmpty()) {
                if (extractedData.get(WebsiteCrawlerConstants.MATCHED_ZIPCODES) != null) {
                    Set<String> addressSet = (Set<String>) extractedData.get(WebsiteCrawlerConstants.MATCHED_ZIPCODES);
                    if (addressSet != null && !addressSet.isEmpty()) {
                        List<String> addressList = new ArrayList<>(addressSet);
                        String firstZipCode = addressList.get(0);
                        address_pattern = AddressExtractionProcess.findFirstMatch(text, firstZipCode);
                        log.info("First ZipCode Matched: {}", address_pattern);
                    }
                }
            }
            if (addressSetValue != null) {
                for (String s : addressSetValue) {
                    if (!s.contains("Wycliffe") && !s.contains("SK9 5AF")) {
                        String[] splitterArray = s.split("~::~");
                        addressLine1 = splitterArray[0].replaceFirst("AddressLine-1:", "");
                        addressLine2 = splitterArray[1].replaceFirst("AddressLine-2:", "");
                        addressLine3 = splitterArray[2].replaceFirst("AddressLine-3:", "");
                        city = splitterArray[3].replaceFirst("City:", "");
                        county = splitterArray[4].replaceFirst("County:", "");
                        zipCode = splitterArray[5].replaceFirst("Zipcode:", "");
                        headQuarters = splitterArray[6].replaceFirst("HeadQuarters:", "");
                        if (splitterArray.length > 7) {
                            if (address_pattern == null) {
                                address_pattern = splitterArray[7].replaceFirst("AddressPattern:", "");
                            }
                        }
                        Serializable phoneValue = extractedData.get(WebsiteCrawlerConstants.PHONE);
                        if (phoneValue != null) {
                            extractedPhoneNumbers = phoneValue.toString();
                        }

                        Serializable faxValue = extractedData.get(WebsiteCrawlerConstants.FAX);
                        if (faxValue != null) {
                            extractedFaxNumbers = faxValue.toString();
                        }
                        Serializable emailValue = extractedData.get(WebsiteCrawlerConstants.EMAIL);
                        if (emailValue != null) {
                            extractedEmail = emailValue.toString();
                        }
                        addressRelatedPhone = addressProcessFunctionInterface.getRelatedEntity(extractedData.get(WebsiteCrawlerConstants.RAW_HTML).toString(), zipCode, extractedPhoneNumbers, 50);
                        addressRelatedFax = addressProcessFunctionInterface.getRelatedEntity(extractedData.get(WebsiteCrawlerConstants.RAW_HTML).toString(), zipCode, extractedFaxNumbers, 80);
                        addressRelatedEmail = addressProcessFunctionInterface.getRelatedEntity(extractedData.get(WebsiteCrawlerConstants.RAW_HTML).toString(), zipCode, extractedEmail, 120);
                        if (StringUtils.containsIgnoreCase(rawHtml, "Registered Office") || StringUtils.containsIgnoreCase(rawHtml, "Head Office") || StringUtils.containsIgnoreCase(rawHtml, "headquarters") || StringUtils.containsIgnoreCase(rawHtml, "Registered Office Address")) {
                            headQuarters = "TRUE";
                        } else {
                            headQuarters = "FALSE";
                        }

                        List<String> headQuartersList = new ArrayList<>();
                        headQuartersList.add(headQuarters);
                        extractedData.put(WebsiteCrawlerConstants.ADDRESS_LINE_1, addressLine1);
                        extractedData.put(WebsiteCrawlerConstants.ADDRESS_LINE_2, addressLine2);
                        extractedData.put(WebsiteCrawlerConstants.ADDRESS_LINE_3, addressLine3);
                        extractedData.put(WebsiteCrawlerConstants.CITY, city);
                        extractedData.put(WebsiteCrawlerConstants.COUNTY, county);
                        extractedData.put(WebsiteCrawlerConstants.ZIPCODE, zipCode);
                        extractedData.put(WebsiteCrawlerConstants.IS_HEAD_QUARTERS, headQuarters);
//                        System.out.println( headQuarters);
                        extractedData.put(WebsiteCrawlerConstants.ADDRESS_RELATED_PHONE, addressRelatedPhone);
                        extractedData.put(WebsiteCrawlerConstants.ADDRESS_RELATED_FAX, addressRelatedFax);
                        extractedData.put(WebsiteCrawlerConstants.ADDRESS_RELATED_EMAIL, addressRelatedEmail);
                        if (address_pattern != null && !address_pattern.isEmpty()) {
                            extractedData.put(WebsiteCrawlerConstants.ADDRESS_PATTERN, address_pattern);
                        }
                        break;
                    }
                }
            }
        }
        if (addressSetValue != null) {
            extractedData.put(WebsiteCrawlerConstants.SPLITTED_ADDRESS, addressSetValue.stream().map(x -> x.replaceAll("~::~", "||")).collect(Collectors.joining("~~~")));
        }
        if (address_pattern != null && !address_pattern.isEmpty()) {
            extractedData.put(WebsiteCrawlerConstants.ADDRESS_PATTERN, address_pattern);
        } else {
            log.warn("Address Pattern is null or empty");
        }
        return extractedData;
    }

    public Set<String> addressBlockIdentification(String pageSource, String text) {
        log.info("Inside Address Extraction Module .....");
        Set<String> addressData;
        Document parsedSource = Jsoup.parse(pageSource);
        String parsedPageSource = parsedSource.toString();
        String annotatedZipContent = addressProcessFunctionInterface.spanTaggedContentZipcode(parsedPageSource, addressProcessServiceFunctions.ZIPCODE_LIST, "ZIPCODE");
        String annotatedAddressLineContent = addressProcessFunctionInterface.spanTaggedContent(annotatedZipContent, addressProcessServiceFunctions.ADDRESSLINEKEYS_LIST, "ADLINE");
        String annotateCityContent = addressProcessFunctionInterface.spanTaggedContent(annotatedAddressLineContent, addressProcessServiceFunctions.CITY_LIST, "CITY");
        String annotatedCountyContent = addressProcessFunctionInterface.spanTaggedContent(annotateCityContent, addressProcessServiceFunctions.COUNTY_LIST, "COUNTY");
        addressData = this.addressExtraction.processAddressExecution(annotatedCountyContent, text);
        return addressData;
    }
}