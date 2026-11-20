package com.infynd.DatavesterExtractor.SingleExtractor.Fullcrawler_lite.phone;

import com.google.i18n.phonenumbers.PhoneNumberMatch;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.infynd.DatavesterExtractor.SingleExtractor.Fullcrawler_lite.utils.WebsiteCrawlerConstants;
import com.infynd.DatavesterExtractor.service.FunctionInterface;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.Serializable;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@Slf4j

public class PhoneExtraction {
    private static final String phonePattern = "\\+(9[976]\\d|8[987530]\\d|6[987]\\d|5[90]\\d|42\\d|3[875]\\d|\n" + "2[98654321]\\d|9[8543210]|8[6421]|6[6543210]|5[87654321]|\n" + "4[987654310]|3[9643210]|2[70]|7|1)\n" + "\\W*\\d\\W*\\d\\W*\\d\\W*\\d\\W*\\d\\W*\\d\\W*\\d\\W*\\d\\W*(\\d{1,2})$";

    @Autowired
    PhoneIdentifierWithPatternsProcess phoneIdentifierWithPatterns;
    @Autowired
    FunctionInterface functionInterface;

    public Map<String, Serializable> extract(Map<String, Serializable> extractedData) {
        Set<String> phone = new HashSet<>();
        Set<String> consolidatedPhoneNumbers = new HashSet<>();
        Set<String> faxNumbers = new HashSet<>();
        String normalizedDomain = (String) extractedData.get(WebsiteCrawlerConstants.NORMALIZED_DOMAIN);
        String rawHtml = (String) extractedData.get(WebsiteCrawlerConstants.RAW_HTML);
        try {
            String cleansedContent = functionInterface.normalizePageSource(rawHtml);
            cleansedContent = functionInterface.cleanPageSource(cleansedContent);
            Optional<Set<String>> extractedPhonesOptional = phoneIdentifierWithPatterns.phoneExtract(cleansedContent);
            if (extractedPhonesOptional.isPresent()) {
                Set<String> extractedPhones = extractedPhonesOptional.get();
                consolidatedPhoneNumbers.addAll(extractedPhones);
            }
        } catch (Exception e) {
            log.error("Error in Phone Number Extraction from  HTML - {} : {}", normalizedDomain, e.getMessage());
        }

        try {
            Set<String> allLinks = (Set<String>) extractedData.get(WebsiteCrawlerConstants.ALL_LINKS);
            rawHtml = StringUtils.replace(rawHtml, "@|!", ",");
            String domText = Jsoup.parse(rawHtml).getElementsByTag("html").text();
            if (StringUtils.isNotEmpty(rawHtml)) {
                PhoneNumberUtil phoneUtil = PhoneNumberUtil.getInstance();
                Iterator<PhoneNumberMatch> iterator = phoneUtil.findNumbers(rawHtml, null).iterator();

                while (iterator.hasNext()) {
                    PhoneNumberMatch match = iterator.next();
                    String ph = match.rawString();
                    phone.add(ph);
                }
                phone = getPhoneFromLink(allLinks, phone);
                phone = phonePatternMatcher(domText, phone);
                Optional<Set<String>> phone1 = phoneIdentifierWithPatterns.phoneExtract(domText);
                Set<String> phone1Set = phone1.orElse(new HashSet<>());
                consolidatedPhoneNumbers.addAll(phone);
                consolidatedPhoneNumbers.addAll(phone1Set);
                String faxValue = (String) extractedData.get(WebsiteCrawlerConstants.FAX);
                if (faxValue != null) {
                    faxNumbers = Stream.of(faxValue.trim().split("\\s*\\|\\s*")).collect(Collectors.toSet());
                } else {
                    faxNumbers = new HashSet<>();
                }
                Set<String> faxRemovedPhones = phoneIdentifierWithPatterns.removeFax(consolidatedPhoneNumbers, faxNumbers);
                String formattedPhones = phoneIdentifierWithPatterns.formattedPhone(Optional.of(faxRemovedPhones));

                if (CollectionUtils.isNotEmpty(Collections.singleton(consolidatedPhoneNumbers))) {
                    String phoneNumbersAsString = Arrays.stream(formattedPhones.split("\\|")).distinct().collect(Collectors.joining("|"));
                    extractedData.put(WebsiteCrawlerConstants.PHONE, consolidatedPhoneNumbers.parallelStream().collect(Collectors.joining("|")));
                    extractedData.put(WebsiteCrawlerConstants.PHONE_FORMATTED, phoneNumbersAsString);
                    String companyNumber = String.valueOf(extractedData.get(WebsiteCrawlerConstants.COMPANY_NO));
                    extractedData = removePhoneinCrn(phoneNumbersAsString, companyNumber, extractedData);
                    log.info("phone module completed", normalizedDomain);
                }
            }
        } catch (Exception e) {
            log.error(String.format("Error while extracting phone by links %s", normalizedDomain), e);
        }
        return extractedData;
    }

    private Map<String, Serializable> removePhoneinCrn(String phoneNumbersAsString, String companyNumber, Map<String, Serializable> extractedData) {
        if (companyNumber == null) {
            return extractedData;
        }
        boolean phoneHasPipe = phoneNumbersAsString.contains("|");
        boolean companyHasPipe = companyNumber.contains("|");
        if (phoneHasPipe && companyHasPipe) {
            String[] phoneNumbersArray = phoneNumbersAsString.split("\\|");
            String[] companyNumbersArray = companyNumber.split("\\|");
            for (int i = 0; i < phoneNumbersArray.length; i++) {
                for (int j = 0; j < companyNumbersArray.length; j++) {
                    if (phoneNumbersArray[i].equals(companyNumbersArray[j])) {
                        companyNumbersArray[j] = "";
                    }
                }
            }
            companyNumber = String.join("|", companyNumbersArray);
        } else if (phoneHasPipe) {
            String[] phoneNumbersArray = phoneNumbersAsString.split("\\|");
            for (String phoneNumber : phoneNumbersArray) {
                if (companyNumber.equals(phoneNumber)) {
                    companyNumber = "";
                    break;
                }
            }
        } else if (companyHasPipe) {
            String[] companyNumbersArray = companyNumber.split("\\|");
            for (int i = 0; i < companyNumbersArray.length; i++) {
                if (phoneNumbersAsString.equals(companyNumbersArray[i])) {
                    companyNumbersArray[i] = "";
                }
            }
            companyNumber = String.join("|", companyNumbersArray);
        } else {
            if (phoneNumbersAsString.equals(companyNumber)) {
                companyNumber = "";
            }
        }
        extractedData.put(WebsiteCrawlerConstants.COMPANY_NO, companyNumber);
        return extractedData;
    }

    private Set<String> getPhoneFromLink(Set<String> allLinks, Set<String> phone) {
        if (CollectionUtils.isNotEmpty(allLinks)) {
            for (String link : allLinks) {
                if (link.contains("tel:") || link.contains("telephone:") || link.contains("phone:")) {
                    String phoneNumber = extractPhoneNumber(link);
                    if (phoneNumber != null) {
                        phone.add(phoneNumber);
                    }
                }
            }
        }
        return phone;
    }

    private String extractPhoneNumber(String link) {
        String[] parts;
        if (link.contains("tel:")) {
            parts = link.split("tel:");
        } else if (link.contains("telephone:")) {
            parts = link.split("telephone:");
        } else if (link.contains("phone:")) {
            parts = link.split("phone:");
        } else if (link.contains("whatsapp:")) {
            parts = link.split("whatsapp:");
        } else {
            return null;
        }
        if (parts.length > 1) {
            return parts[1];
        } else {
            return null;
        }
    }


    public Set<String> phonePatternMatcher(String text, Set<String> phoneSet) {

        Pattern pattern = Pattern.compile(phonePattern);
        Matcher matcher = pattern.matcher(text);
        while (matcher.find()) {
            if (StringUtils.isNotEmpty(matcher.group())) {
                phoneSet.add(matcher.group().trim());
            }
        }
        return phoneSet;
    }

}
