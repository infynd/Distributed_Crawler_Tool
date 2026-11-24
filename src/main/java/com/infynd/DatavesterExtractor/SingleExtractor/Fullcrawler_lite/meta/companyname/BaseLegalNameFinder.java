package com.infynd.DatavesterExtractor.SingleExtractor.Fullcrawler_lite.meta.companyname;

import org.apache.commons.lang3.StringUtils;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.NodeTraversor;
import org.jsoup.select.NodeVisitor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


@Service
public abstract class BaseLegalNameFinder {


    private final Pattern addressRegex = Pattern.compile("(?<legalName>(([a-z]{1,}\\d*)[\\s*\\-\\.&',®-]*){1,})",
            Pattern.CASE_INSENSITIVE);
    private final Pattern copyrightTextRegex = Pattern.compile(
            "(?<copyrightTextLeft>.*)copyright(s)?(?<copyrightTextRight>.*)", Pattern.CASE_INSENSITIVE);
    private final Pattern copyrightSymbolTextRegex = Pattern.compile("(?<copyrightTextLeft>.*)©(?<copyrightTextRight>.*)",
            Pattern.CASE_INSENSITIVE);
    private final Pattern yearRegex = Pattern
            .compile(
                    "(?<copyrightTextLeft>.*)(((20\\d\\d|19\\d\\d)(.*)(20\\d\\d|19\\d\\d))|(20\\d\\d|19\\d\\d))(?<copyrightTextRight>.*)",
                    Pattern.CASE_INSENSITIVE);
    private final Pattern allRightsReservedRegex = Pattern.compile(
            "(?<stripContentLeft>.*)((all|some)\\s*.*right(s)?.*reser(e)?ved(\\sto)?\\.?)(?<stripContentRight>.*)",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern stripWebsiteByRegex = Pattern.compile("(?<strippedContent>.*)(web?site)(.*)(by)(.*)",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern stripPoweredByRegex = Pattern.compile("(?<strippedContent>.*)(power)(.*)(by)(.*)",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern stripPresentedByRegex = Pattern.compile("(?<strippedContent>.*)(present)(.*)(by)(.*)",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern stripMaintainedByRegex = Pattern.compile("(?<strippedContent>.*)(maintain)(.*)(by)(.*)",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern stripMaintenanceByRegex = Pattern.compile("(?<strippedContent>.*)(Maintenance)(.*)(by)(.*)",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern stripRegisteredByRegex = Pattern.compile("(?<strippedContent>.*)(register)(.*)(by)(.*)",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern stripOperatedByRegex = Pattern.compile("(?<strippedContent>.*)(operate)(.*)(by)(.*)",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern stripDevelopedByRegex = Pattern.compile("(?<strippedContent>.*)(development)(.*)(by)(.*)",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern stripCreatedByRegex = Pattern.compile("(?<strippedContent>.*)(create)(.*)(by)(.*)",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern stripDesignedByRegex = Pattern.compile("(?<strippedContent>.*)(design)(.*)(by)(.*)",
            Pattern.CASE_INSENSITIVE);

    private static final ArrayList<Pattern> patterns = new ArrayList<Pattern>();

    private static final ArrayList<Integer> invalidUnicodeList = new ArrayList<Integer>();

    private static ArrayList<String> irreleventList;

    private static ArrayList<String> addressSeparators;

    static {
        patterns.add(stripWebsiteByRegex);
        patterns.add(stripPoweredByRegex);
        patterns.add(stripDevelopedByRegex);
        patterns.add(stripCreatedByRegex);
        patterns.add(stripDesignedByRegex);
        patterns.add(stripMaintainedByRegex);
        patterns.add(stripRegisteredByRegex);
        patterns.add(stripOperatedByRegex);
        patterns.add(stripMaintenanceByRegex);
        patterns.add(stripPresentedByRegex);

        String[] splitWordsSpring = {"\\\\|", "comments", "sitemap", " site", "incorporated", " from", " at", "home", "privacy", "terms of use", "disclaimer", "privacy policy", "contact us", "a chapter of", "policy", " - ", "privately owned", "theme ", "site map", " developed ", " is ", "wordpress", "drupal", "webcomic", " author", "by ", " at ", " to ", "member login ", "all submitted content", "and beyond", "and cookies", "terms and conditions", "about us", "trademarks of", "registered in", "company no", "company reg. no", "company number", "terms & conditions", "reserved", "division of", "part of,subsidiary of", "trade mark of", "trademark of"};
        addressSeparators = new ArrayList<>(Arrays.asList(splitWordsSpring));
        String[] irrelevantWords = {"(c)", "by ", "sitemap", "site map", "contact", "contact us", "revised", "-", "|", ":", "::", ".", ",", " ", "copyright", "fax", "tel", "privacy statement", "statement", "(,)", "why cookies", "of ", "to ", "design"};
        irreleventList = new ArrayList<>(Arrays.asList(irrelevantWords));
    }


    public String convertNodeToText(Element element) {
        final StringBuilder buffer = new StringBuilder();
        NodeTraversor traversor = new NodeTraversor();
        NodeVisitor visitor = new NodeVisitor() {
            boolean isNewline = true;

            @Override
            public void head(Node node, int depth) {
                if (node instanceof TextNode textNode) {
                    String text = textNode.text().replace('\u00A0', ' ');
                    if (!text.isEmpty()) {
                        buffer.append(text);
                        isNewline = false;
                    }
                } else if (node instanceof Element element) {
                    if (!isNewline) {
                        if ((element.isBlock() || element.tagName().equals("br"))) {
                            buffer.append("\n");
                            isNewline = true;
                        }
                    }
                }
            }

            @Override
            public void tail(Node node, int depth) {
            }
        };
        NodeTraversor.traverse(visitor, element);
        if (buffer.length() == 0) {
            if (element.hasAttr("content")) {
                buffer.append(element.attr("content"));
            }
        }
        return buffer.toString();
    }

    protected String normalizeAndTrim(String text) {
        if (text != null) {
            text = text.replace(String.valueOf((char) 160), " ").trim();
        }
        return text;
    }

    protected String stripAddress(String inputString) {
        // inputString = stripInvalidCharacters(inputString);
        String legalName = null;
        Matcher addressMatcher = addressRegex.matcher(inputString);
        if (addressMatcher.find()) {
            String tmpLegalName = addressMatcher.group("legalName");
            if (!StringUtils.isEmpty(tmpLegalName)) {
                legalName = tmpLegalName.trim();
            }
        }
        return legalName;
    }

    protected String stripSiteDevelopedByContent(String inputText) {
        String legalName = inputText;

        for (Pattern pattern : patterns) {
            Matcher matcher = pattern.matcher(legalName);
            if (matcher.find()) {
                legalName = matcher.group("strippedContent");
                if (!StringUtils.isEmpty(legalName)) {
                    legalName = legalName.trim();
                }
            }
        }

        return legalName;
    }

    protected String stripInvalidCharacters(String inputString) {

        String content = inputString;
        if (content.length() > 0) {
            for (int i : invalidUnicodeList) {
                if ((content.charAt(0)) == i) {
                    content = content.substring(1);
                }

                if (content.charAt(content.length() - 1) == i) {
                    content = content.substring(0, content.length() - 1);
                }
            }

        }
        return content;

    }

    protected String stripStopWord(String inputText) {
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

    protected Set<String> splitAddressSaperators(String input, String domain) {
        Set<String> words = new HashSet<String>();
        Boolean noSaperatorFound = true;
        for (String addressSaperator : addressSeparators) {
            if ((addressSaperator.length() > 3) && domain.contains(addressSaperator)) {
                continue;
            }
            Pattern p = Pattern.compile(addressSaperator, Pattern.CASE_INSENSITIVE);
            String[] splitWords = p.split(input);
            if (splitWords.length >= 1) {
                noSaperatorFound = false;
                Set<String> wordList = new HashSet<>(Arrays.asList(splitWords));
                words.addAll(wordList);
            }
        }
        if (noSaperatorFound) {
            words.add(input);
        }
        return words;

    }

    private Set<String> strpiLeftRightContentFromRegex(String input, Pattern pattern, String leftName, String rightName) {
        Set<String> legalNames = new HashSet<String>();

        Matcher matcher = pattern.matcher(input);
        if (matcher.find()) {
            String left = matcher.group(leftName);
            String right = matcher.group(rightName);

            left = normalizeAndTrim(left);
            right = normalizeAndTrim(right);

            if (!StringUtils.isEmpty(left)) {
                legalNames.add(left);

            }

            if (!StringUtils.isEmpty(right)) {
                legalNames.add(right);
            }

        }
        return legalNames;
    }

    private Set<String> stripCopyrightText(String inputString) {
        Set<String> legalNames;

        Set<String> invalidLegalNamesList = new HashSet<String>();
        Set<String> tmpLegalNames = new HashSet<String>();

        legalNames = strpiLeftRightContentFromRegex(inputString, copyrightTextRegex, "copyrightTextLeft",
                "copyrightTextRight");
        if (legalNames.isEmpty()) {
            Set<String> tmp = strpiLeftRightContentFromRegex(inputString, copyrightSymbolTextRegex,
                    "copyrightTextLeft", "copyrightTextRight");
            legalNames.addAll(tmp);

        }

        for (String string : legalNames) {
            Set<String> tmp = strpiLeftRightContentFromRegex(string, copyrightSymbolTextRegex, "copyrightTextLeft",
                    "copyrightTextRight");
            if (isStrippedLeftRightContentFromRegexFound(string, copyrightSymbolTextRegex, "copyrightTextLeft",
                    "copyrightTextRight")) {
                invalidLegalNamesList.add(string);
                tmpLegalNames.addAll(tmp);

            }
        }
        legalNames.removeAll(invalidLegalNamesList);
        legalNames.addAll(tmpLegalNames);

        return legalNames;

    }

    private Boolean isStrippedLeftRightContentFromRegexFound(String input, Pattern pattern, String leftName,
                                                             String rightName) {

        Matcher matcher = pattern.matcher(input);
        return matcher.find();
    }

    protected Set<String> extractCopyrightAndYear(String inputText) {

        Set<String> legalNames = stripCopyrightText(inputText);
        // if copyright symbol is not there
        if (legalNames.isEmpty()) {
            legalNames.add(inputText);
        }
        Set<String> invalidLegalNamesList = new HashSet<String>();
        Set<String> tmpLegalNames = new HashSet<String>();

        for (String lName : legalNames) {
            Set<String> tmp = strpiLeftRightContentFromRegex(lName, yearRegex, "copyrightTextLeft",
                    "copyrightTextRight");
            if (isStrippedLeftRightContentFromRegexFound(lName, yearRegex, "copyrightTextLeft", "copyrightTextRight")) {
                invalidLegalNamesList.add(lName);
                tmpLegalNames.addAll(tmp);
            }
        }

        legalNames.removeAll(invalidLegalNamesList);
        legalNames.addAll(tmpLegalNames);

        return legalNames;
    }

    protected Set<String> extractAllRightsReserved(String inputText) {
        Set<String> output = new HashSet<String>();
        Matcher matcher = allRightsReservedRegex.matcher(inputText);

        if (matcher.find()) {
            String leftContent = matcher.group("stripContentLeft");
            if (!StringUtils.isEmpty(leftContent)) {
                output.add(leftContent);
            }
            String rightContent = matcher.group("stripContentRight");
            if (!StringUtils.isEmpty(rightContent)) {
                output.add(rightContent);
            }
        } else {
            output.add(inputText);
        }
        return output;
    }


}
