package com.infynd.DatavesterExtractor.SingleExtractor.Fullcrawler_lite.name;

import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.*;


@Service
public class NameFinderExtractor {

    private static final int validateWordLength = 5;
    private static final int FIRST_WORD_MAX_LENGTH = 3;


    public static boolean isMatch(String inputText, String primaryDomain, int defaultLengthMatch) {

        boolean isValid = false;

        int wordLength = defaultLengthMatch;

        String normalizedBaseWord = removeTLD(primaryDomain);

        if (normalizedBaseWord.length() < defaultLengthMatch) {

            wordLength = normalizedBaseWord.length() - 1;

        }

        // equals check

        if ((wordLength > 3) && StringUtils.equalsIgnoreCase(StringUtils.substring(inputText, 0, wordLength),

                StringUtils.substring(normalizedBaseWord, 0, wordLength))) {

            isValid = true;

        }

        // contains check 1

        if (!isValid) {

            if (!(StringUtils.isBlank(inputText)) && !(StringUtils.isBlank(normalizedBaseWord))

                    && StringUtils.containsIgnoreCase(inputText, normalizedBaseWord)) {

                isValid = true;

            }

        }

        // contains check 2

        if (!isValid) {

            if (!(StringUtils.isBlank(normalizedBaseWord)) && !(StringUtils.isBlank(inputText))

                    && StringUtils.containsIgnoreCase(normalizedBaseWord, inputText)) {

                isValid = true;

            }

        }

        return isValid;

    }

    public Boolean isValidLegalName(String normalizedLegalName, String primaryDomain) {
        boolean isValid = false;
        int tmpWordLength = validateWordLength;

        if (StringUtils.containsIgnoreCase(normalizedLegalName, "c/o")) {
            int index = StringUtils.substringBefore(normalizedLegalName.toLowerCase(), "c/o").length();
            normalizedLegalName = StringUtils.substring(normalizedLegalName, 0, index);
        }

        String normalizedBaseWord = removeTLD(primaryDomain);

        if (normalizedBaseWord.length() < validateWordLength) {
            tmpWordLength = normalizedBaseWord.length() - 1;
        }

        if ((tmpWordLength > 3)
                && StringUtils.equalsIgnoreCase(StringUtils.substring(normalizedLegalName, 0, tmpWordLength),
                StringUtils.substring(normalizedBaseWord, 0, tmpWordLength))) {
            isValid = true;
        }

        if (!isValid) {
            if (!(StringUtils.isBlank(normalizedLegalName)) && !(StringUtils.isBlank(normalizedBaseWord))
                    && StringUtils.containsIgnoreCase(normalizedLegalName, normalizedBaseWord)) {
                isValid = true;
            }
        }

        if (!isValid) {
            if (!(StringUtils.isBlank(normalizedBaseWord)) && !(StringUtils.isBlank(normalizedLegalName))
                    && StringUtils.containsIgnoreCase(normalizedBaseWord, normalizedLegalName)) {
                isValid = true;
            }
        }

        if (!isValid) {

            String nonAlphaNumaralLegalName = normalizedLegalName.replaceAll("[^A-Za-z0-9]", "");

            tmpWordLength = validateWordLength;
            String nonAlphaNumaralPrimaryDomain = normalizedBaseWord.replaceAll("[^A-Za-z0-9]", "");

            if (nonAlphaNumaralPrimaryDomain.length() < validateWordLength) {
                tmpWordLength = nonAlphaNumaralPrimaryDomain.length() - 1;
            }

            if ((tmpWordLength > 3)
                    && StringUtils.equalsIgnoreCase(StringUtils.substring(nonAlphaNumaralLegalName, 0, tmpWordLength),
                    StringUtils.substring(nonAlphaNumaralPrimaryDomain, 0, tmpWordLength))) {
                isValid = true;
            }

            if (!isValid) {
                if (!(StringUtils.isBlank(nonAlphaNumaralLegalName))
                        && !(StringUtils.isBlank(nonAlphaNumaralPrimaryDomain))
                        && StringUtils.containsIgnoreCase(nonAlphaNumaralLegalName, nonAlphaNumaralPrimaryDomain)) {
                    isValid = true;
                }
            }

            if (!isValid) {
                if (!(StringUtils.isBlank(nonAlphaNumaralPrimaryDomain))
                        && !(StringUtils.isBlank(nonAlphaNumaralLegalName))
                        && StringUtils.containsIgnoreCase(nonAlphaNumaralPrimaryDomain, nonAlphaNumaralLegalName)) {
                    isValid = true;
                }
            }
        }

        if (!isValid) {
            tmpWordLength = FIRST_WORD_MAX_LENGTH;

            try {
                String firstOfAllWords = firstOfAllWords(normalizedLegalName);

                if (firstOfAllWords.length() < validateWordLength) {
                    tmpWordLength = firstOfAllWords.length();
                }

                if ((tmpWordLength >= 3)
                        && StringUtils.equalsIgnoreCase(firstOfAllWords.substring(0, tmpWordLength),
                        normalizedBaseWord.substring(0, tmpWordLength))) {
                    isValid = true;
                }

            } catch (Exception e) {
                isValid = false;
            }
        }

        if (!isValid) {
            int lDistance = LevenshteinDistanceExtractor.getLevenshteinDistance(normalizedLegalName, normalizedBaseWord);

            if (lDistance >= normalizedBaseWord.length()) {
                isValid = false;
            }
        }

        return isValid;
    }

    private String firstOfAllWords(String line) {
        String[] arr = line.split(" |-");
        StringBuffer sb = new StringBuffer();

        for (int i = 0; i < arr.length; i++) {
            sb.append(Character.toUpperCase(arr[i].charAt(0)));
        }
        return sb.toString().trim();
    }

    public static List<String> sortByRelevency(List<String> listTobeSorted, String baseWord) {


        HashMap<String, Integer> sortedMap = new HashMap<String, Integer>();

        IntegerValueComparatorExtractor valueComparator = new IntegerValueComparatorExtractor(sortedMap);


        TreeMap<String, Integer> levenshteinScoreMap = new TreeMap<String, Integer>(valueComparator);


        for (String inputText : listTobeSorted) {

            int lDistance = LevenshteinDistanceExtractor.getLevenshteinDistance(baseWord, inputText);

            sortedMap.put(inputText, lDistance);

        }

        levenshteinScoreMap.putAll(sortedMap);

        ArrayList<String> sortedArray = new ArrayList<String>(levenshteinScoreMap.keySet());

        return sortedArray;

    }

    public static Map<String, String> sortByRelevency(Map<String, String> map, int minThreshold) {

        String SPLIT_STRING = "|";

        Map<String, String> sortedResults = new LinkedHashMap<>();

        if (MapUtils.isEmpty(map)) {

            return sortedResults;

        }

        HashMap<String, Integer> sortedMap = new HashMap<String, Integer>();

        IntegerValueComparatorExtractor valueComparator = new IntegerValueComparatorExtractor(sortedMap);

        TreeMap<String, Integer> levenshteinScoreMap = new TreeMap<String, Integer>(valueComparator);

        for (Map.Entry<String, String> entry : map.entrySet()) {

            int lDistance = LevenshteinDistanceExtractor.getLevenshteinDistance(entry.getValue(), entry.getKey());

            if (lDistance <= minThreshold) {

                sortedMap.put(entry.getKey() + SPLIT_STRING + entry.getValue(), lDistance);

            }

        }

        levenshteinScoreMap.putAll(sortedMap);

        for (Map.Entry<String, Integer> entry : levenshteinScoreMap.entrySet()) {

            String[] keyValueSplit = StringUtils.split(entry.getKey(), SPLIT_STRING);

            sortedResults.put(keyValueSplit[0], keyValueSplit[1]);

        }
        return sortedResults;

    }

    public static int getLevenshteinDistance(String baseWord, String inputText) {

        return LevenshteinDistanceExtractor.getLevenshteinDistance(baseWord, inputText);

    }

    private static String removeTLD(String inputText) {

        return StringUtils.substringBefore(inputText, ".");

    }


}

