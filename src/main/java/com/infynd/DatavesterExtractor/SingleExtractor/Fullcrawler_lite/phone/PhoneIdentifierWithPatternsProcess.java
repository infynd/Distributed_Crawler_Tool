package com.infynd.DatavesterExtractor.SingleExtractor.Fullcrawler_lite.phone;


import com.infynd.DatavesterExtractor.service.FunctionInterface;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@Slf4j
public class PhoneIdentifierWithPatternsProcess {
    private static final String PHONE_NUMBER_REGEX_1 = "((<[^<>]*>\\s*(T(\\s*<[^<>]*>\\s*)*\\s*:|Tel(\\s*<[^<>]*>\\s*)*\\s*:|P(\\s*<[^<>]*>\\s*)*\\s*:|Ph(\\s*<[^<>]*>\\s*)*\\s*:|Phone(\\s*<[^<>]*>\\s*)*\\s*:|Telephone))\\s*((<[^<>]*>)*\\s*)*)((((\\+44\\s?\\d{4}|\\(?0\\d{4}\\)?)\\s?\\d{3}\\s?\\d{3})|((\\+44\\s?\\d{3}|\\(?0\\d{3}\\)?)\\s?\\d{3}\\s?\\d{4})|((\\+44\\s?\\d{2}|\\(?0\\d{2}\\)?)\\s?\\d{4}\\s?\\d{4}))(\\s?\\#(\\d{4}|\\d{3}))?|(\\+44\\s?7\\d{3}|\\(?07\\d{3}\\)?)\\s?\\d{3}\\s?\\d{3}|((\\(?0\\d{4}\\)?\\s?\\d{3}\\s?\\d{3})|(\\(?0\\d{3}\\)?\\s?\\d{3}\\s?\\d{4})|(\\(?0\\d{2}\\)?\\s?\\d{4}\\s?\\d{4}))(\\s?\\#(\\d{4}|\\d{3}))?)";
    private static final String PHONE_NUMBER_REGEX_2 = "<[^<>]*>\\s*((((\\+44\\s?\\d{4}|\\(?0\\d{4}\\)?)\\s?\\d{3}\\s?\\d{3})|((\\+44\\s?\\d{3}|\\(?0\\d{3}\\)?)\\s?\\d{3,4}\\s?\\d{3,4})|((\\+44\\s?\\d{2}|\\(?0\\d{2}\\)?)\\s?\\d{4}\\s?\\d{4}))(\\s?\\#(\\d{4}|\\d{3}))?|(\\+44\\s?7\\d{3}|\\(?07\\d{3}\\)?)\\s?\\d{3}\\s?\\d{3}|((\\(?0\\d{4}\\)?\\s?\\d{3}\\s?\\d{3})|(\\(?0\\d{3}\\)?\\s?\\d{3}\\s?\\d{4})|(\\(?0\\d{2}\\)?\\s?\\d{4}\\s?\\d{4}))(\\s?\\#(\\d{4}|\\d{3}))?)";
    private static final String PHONE_NUMBER_REGEX_3 = "(<[^<>]*>\\s*)*(([\\(]?\\+44[\\)]?\\s?[\\(]?\\d+[\\)]?\\s?\\d+\\s?\\d+(\\s?\\d+))|(:\\s*\\d{4,5}[\\-\\s]?\\d{6})(?!\\b-)|(?<!\\d)\\d{5}\\s\\d{6}(?!\\d))";
    private static final String PHONE_NUMBER_REGEX_4 = "(<[^<>]*>|\\s)\\s*(((\\d{4}\\s\\d{3}\\s\\d{2}\\s\\d{2})|(\\+44\\d{10})|((\\+44\\s?\\d{4}|\\(?0\\d{4}\\)?)\\s?\\d{3}\\s?\\d{3})|((\\+44\\s?\\d{3}|\\(?0\\d{3}\\)?)\\s?\\d{3,4}\\s?\\d{3,4})|((\\+44\\s?\\d{2}|\\(?0\\d{2}\\)?)\\s?\\d{4}\\s?\\d{4}))(\\s?\\#(\\d{4}|\\d{3}))?|(\\+44\\s?\\d{3}|\\(?07\\d{3}\\)?)\\s?\\d{3}\\s?\\d{3}|((\\(?0\\d{4}\\)?\\s?\\d{3}\\s?\\d{3})|(\\(?0\\d{3}\\)?\\s?\\d{3}\\s?\\d{4})|(\\(?0\\d{2}\\)?\\s?\\d{4}\\s?\\d{4}))(\\s?\\#(\\d{4}|\\d{3}))?)";
    private static final String PHONE_NUMBER_REGEX_5 = "(?:(?:\\(?(?:0(?:0|11)\\)?[\\s-]?\\(?|\\+)44\\)?[\\s-]?(?:\\(?0\\)?[\\s-]?)?)|(?:\\(?0))(?:(?:\\d{5}\\)?[\\s-]?\\d{4,5})|(?:\\d{4}\\)?[\\s-]?(?:\\d{5}|\\d{3}[\\s-]?\\d{3}))|(?:\\d{3}\\)?[\\s-]?\\d{3}[\\s-]?\\d{3,4})|(?:\\d{2}\\)?[\\s-]?\\d{4}[\\s-]?\\d{4}))(?:[\\s-]?(?:x|ext\\.?|\\#)\\d{3,4})?$";
    //private static final String PHONE_NUMBER_REGEX_6 = "\\+(9[976]\\d|8[987530]\\d|6[987]\\d|5[90]\\d|42\\d|3[875]\\d|\n" +
    //  "2[98654321]\\d|9[8543210]|8[6421]|6[6543210]|5[87654321]|\n" +
    //"4[987654310]|3[9643210]|2[70]|7|1)\n" +
    // "\\W*\\d\\W*\\d\\W*\\d\\W*\\d\\W*\\d\\W*\\d\\W*\\d\\W*\\d\\W*(?:\\d{1,2})$";
    @Autowired
    FunctionInterface functionInterface;

    public Optional<Set<String>> phoneExtract(String concatenatedHtml) {
        Set<String> consolidatedPhoneNumbers = new HashSet<>();
        Set<String> phoneMatchedBlocks_1 = functionInterface.regexMatchedSetData(concatenatedHtml, PHONE_NUMBER_REGEX_1, 9);
        Set<String> phoneMatchedBlocks_2 = functionInterface.regexMatchedSetData(concatenatedHtml, PHONE_NUMBER_REGEX_2, 2);
        Set<String> phoneMatchedBlocks_3 = functionInterface.regexMatchedSetData(concatenatedHtml, PHONE_NUMBER_REGEX_3, 2);
        Set<String> phoneMatchedBlocks_4 = functionInterface.regexMatchedSetData(concatenatedHtml, PHONE_NUMBER_REGEX_4, 2);
        Set<String> phoneMatchedBlocks_5 = functionInterface.regexMatchedSetData(concatenatedHtml, PHONE_NUMBER_REGEX_5, 8);
        addPhoneNumbers(phoneMatchedBlocks_1, consolidatedPhoneNumbers);
        addPhoneNumbers(phoneMatchedBlocks_2, consolidatedPhoneNumbers);
        addPhoneNumbers(phoneMatchedBlocks_3, consolidatedPhoneNumbers);
        addPhoneNumbers(phoneMatchedBlocks_4, consolidatedPhoneNumbers);
        addPhoneNumbers(phoneMatchedBlocks_5, consolidatedPhoneNumbers);

        return Optional.ofNullable(consolidatedPhoneNumbers);
    }

    private void addPhoneNumbers(Set<String> phoneMatchedBlocks, Set<String> consolidatedPhoneNumbers) {
        phoneMatchedBlocks.stream()
                .map(this::cleanPhone)
                .filter(phoneNumber -> isValidPhoneNumber(phoneNumber) && !phoneNumber.isEmpty())
                .distinct()
                .forEach(consolidatedPhoneNumbers::add);
    }

    private String cleanPhone(String extractedPhoneNumber) {
        String cleanedPhoneNumber = extractedPhoneNumber.replaceAll("(?sim)^.*?:", "").trim();
        return cleanedPhoneNumber.replaceAll("[\\s-]+", "");
    }

    private boolean isValidPhoneNumber(String phoneNumber) {
        phoneNumber = cleanPhone(phoneNumber);
        String[] excludedNumbers = {
                "0303 123 1113",
                "01625 545 745"
        };
        for (String excludedNumber : excludedNumbers) {
            String cleanedExcludedNumber = cleanPhone(excludedNumber);
            if (phoneNumber.equals(cleanedExcludedNumber)) {
                return false;
            }
        }
        return !(phoneNumber.startsWith("1999")
                || phoneNumber.startsWith("0000")
                || phoneNumber.length() < 10);
    }

    public String formattedPhone(Optional<Set<String>> extractedPhoneNumber) {
        return extractedPhoneNumber.orElse(new HashSet<>()).stream()
                .filter(phone -> phone.trim().length() > 8)
                .map(phone -> phone.replaceAll("(?sim)[\\(\\-\\s),\\+\\|\\*\\.\\:]|%20", "").trim())
                .collect(Collectors.joining("|"));
    }

    public Set<String> removeFax(Set<String> extractedPhones, Set<String> extractedFax) {
        extractedPhones.removeIf(extractedFax::contains);
        return extractedPhones;
    }

}