package com.infynd.DatavesterExtractor.SingleExtractor.Fullcrawler_lite.addressextraction;


import com.infynd.DatavesterExtractor.SingleExtractor.Fullcrawler_lite.utils.service.LocationUtilExtrator;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

@Slf4j
@Component
public class AddressProcessServiceFunctionsProcess implements AddressProcessFunctionInterfaceProcess {
    public static final String FILESEPARATOR = System.getProperty("file.separator");
    public List<String> ZIPCODE_LIST = new ArrayList<>();
    public List<String> ADDRESSLINEKEYS_LIST = new ArrayList<>();
    public List<String> COUNTY_LIST = new ArrayList<>();
    public List<String> CITY_LIST = new ArrayList<>();
    public List<String> ADDR_BEGIN_JUNK_LIST = new ArrayList<String>();
    @Autowired
    ResourceLoader resourceLoader;

    @Autowired
    LocationUtilExtrator locationUtil;

    public String spanTaggedContentZipcode(String tempOutput, List<String> zipcode, String entityName) {
        Iterator<String> itr = zipcode.iterator();
        String resultString = tempOutput;
        while (itr.hasNext()) {
            String tobeannotate = itr.next();
            try {
                resultString = resultString.replaceFirst(tobeannotate, "~:~:span id=\"annotate_events\" title=\"" + entityName + "\" style=\"background-color" + ":FF0000;\"~::~$1~:~:/span~::~");
            } catch (IllegalArgumentException | IndexOutOfBoundsException exception) {
                log.error("Pattern syntax exception in spanTaggedContentZipcode {}", exception.getLocalizedMessage());
            }
        }

        try {
            resultString = resultString.replaceAll("(?sm)(~:~:span id=\"annotate_events\" title=\"" + entityName + "\" style=\"background-color:FF0000;\"~::~)+", "$1");
        } catch (IllegalArgumentException | IndexOutOfBoundsException exception) {
            log.error("Pattern syntax exception in spanTaggedContentZipcode {}", exception.getLocalizedMessage());
        }

        try {
            Pattern regex = Pattern.compile("(<[^<>]*)~:~:span id=\"annotate_events\"[^<>]*style=\"background-color" + ":FF0000;\"~::~([^<>]*)~:~:/span~::~([^<>]*>)", Pattern.CANON_EQ | Pattern.DOTALL | Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE | Pattern.MULTILINE);
            Matcher regexMatcher = regex.matcher(resultString);
            try {
                resultString = regexMatcher.replaceAll("$1$2$3");
            } catch (IllegalArgumentException | IndexOutOfBoundsException illegalArgumentException) {
                log.error("Exception - {}", illegalArgumentException.getLocalizedMessage());
            }
        } catch (PatternSyntaxException patternSyntaxException) {
            log.error("Exception - {}", patternSyntaxException.getLocalizedMessage());
        }

        return resultString;
    }

    public String spanTaggedContent(String tempoutput, List<String> entityAttribute, String Name) {
        Iterator<String> itr = entityAttribute.iterator();
        String resultString = tempoutput;
        while (itr.hasNext()) {
            String toBeAnnotate = itr.next();
            try {
                resultString = resultString.replaceAll(toBeAnnotate, "~:~:span id=\"annotate_events\" title=\"" + Name + "\" style=\"background-color:FF0000;" + "\"~::~$1~:~:/span~::~");
            } catch (IllegalArgumentException | IndexOutOfBoundsException patternSyntaxException) {
                log.error("spanTaggedContent Error {}", patternSyntaxException.getLocalizedMessage());
            }
        }

        try {
            resultString = resultString.replaceAll("(?sm)(~:~:span id=\"annotate_events\" title=\"" + Name + "\" " + "style=\"background-color:FF0000;\"~::~)+", "$1");
        } catch (IllegalArgumentException | IndexOutOfBoundsException patternSyntaxException) {
            log.error("spanTaggedContent Error {}", patternSyntaxException.getLocalizedMessage());
        }

        try {
            Pattern regex = Pattern.compile("(<[^<>]*)~:~:span id=\"annotate_events\"[^<>]*style=\"background-color" + ":FF0000;\"~::~([^<>]*)~:~:/span~::~([^<>]*>)", Pattern.CANON_EQ | Pattern.DOTALL | Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE | Pattern.MULTILINE);
            Matcher regexMatcher = regex.matcher(resultString);
            try {
                resultString = regexMatcher.replaceAll("$1$2$3");
            } catch (IllegalArgumentException | IndexOutOfBoundsException illegalArgumentException) {
                log.error("spanTaggedContent Error {}", illegalArgumentException.getLocalizedMessage());
            }
        } catch (PatternSyntaxException patternSyntaxException) {
            log.error("spanTaggedContent Error {}", patternSyntaxException.getLocalizedMessage());
        }
        return resultString;
    }

    public boolean validateAddressBoundary(String addressBoundary) {
        boolean matchedScenario = false;
        try {
            Pattern regex = Pattern.compile("[A-z]+", Pattern.CANON_EQ | Pattern.DOTALL | Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE | Pattern.MULTILINE);
            Matcher regexMatcher = regex.matcher(addressBoundary);
            if (regexMatcher.find()) {
                matchedScenario = true;
            }
        } catch (PatternSyntaxException patternSyntaxException) {
            log.error("Error in validateAddressBoundary {}", patternSyntaxException.getLocalizedMessage());
        }
        return matchedScenario;
    }


    public String removeFormattingTags(String parsedPageSource) {
        String removedContent = parsedPageSource;
        removedContent = removedContent.replaceAll("(?sim)(<[//]?b>|<[//]?strong>|<[//]?i>|<[//]?em>|<[//]?mark" + ">|<[//]?u>|<[//]?tt>|<[//]?strike>|<[//]?sup>|<[//]?sub>|<[//]?del>|<[//]?ins" + ">|<[//]?big" + ">|<[//]?small>|<[//]?br>|<[//]?blockquote>)", "");
        return removedContent;
    }

    public String removeInjectedTags(String removedContent) {
        removedContent = removedContent.replaceAll("(?sim)<[^<>]*annotate_events[^<>]*>", "").replaceAll("(?sim)" + "</span>", "");
        return removedContent;
    }

    @Override
    public String validateEntity(String addressEntity) {
        if (StringUtils.isNotBlank(addressEntity.trim())) {
            addressEntity = removeFormattingTags(addressEntity);
            addressEntity = removeInjectedTags(addressEntity);
            addressEntity = cleanAttributesBeginOffset(addressEntity);
            addressEntity = cleanAttributesEndOffset(addressEntity);
            addressEntity = addressEntity.replaceAll("(?sim)^.*?\"", "").replaceAll("(?sim)^\\s*[\\-]?\\s*]", "").trim();
        }
        return addressEntity.trim();
    }

    @Override
    public String removeOtherAddressEntities(String entity1, String entity2) {
        if (!entity1.trim().isEmpty()) {
            if (!entity2.trim().isEmpty()) {
                if (entity2.contains("|")) {
                    String[] splitter = entity2.split("\\|");
                    for (String temp : splitter) {
                        entity1 = entity1.replaceAll(temp + "\\s*$", "").trim();
                    }
                } else {
                    entity1 = entity1.replaceAll(entity2 + "\\s*$", "").trim();
                }
            }

        }

        entity1 = cleanAttributesEndOffset(entity1);
        entity1 = cleanAttributesBeginOffset(entity1);
        return entity1;
    }

    public String cleanAttributesEndOffset(String entity) {
        entity = entity.replaceAll("(?sim)[\\s,\\.\\|]*\\s*$", " ").trim();
        return entity;
    }

    public String cleanAttributesBeginOffset(String entity) {
        entity = entity.replaceAll("(?sim)^\\s*[\\s,\\.\\|]*", " ").trim();
        return entity;
    }

    @Override
    public String getRelatedEntity(String SourceContent, String entity1, String entity2, int checkDistance) {
        try {
            if (!entity1.trim().isEmpty()) {
                if (!entity2.trim().isEmpty()) {
                    if (entity2.contains("|")) {
                        String[] splitter = entity2.split("\\|");
                        for (String splitText : splitter) {
                            String itemFromList = splitText.replaceAll("\\+", "\\\\+");
                            itemFromList = itemFromList.replaceAll("\\)", "\\\\)");
                            itemFromList = itemFromList.replaceAll("\\(", "\\\\(");
                            String intermediateContent = executeMatchPatternDomain(SourceContent, entity1 + ".*?" + itemFromList, 0);
                            if (!intermediateContent.trim().isEmpty()) {
                                if (intermediateContent.replaceAll("(?sim)<[^<>]*>", "").trim().length() < checkDistance) {
                                    return splitText.trim();
                                }
                            }
                        }
                    } else {
                        return entity2;
                    }
                }
            }
        } catch (Exception exception) {
            log.error("Exception in related entity method {}", exception.getLocalizedMessage());
        }
        return "";
    }

    @Override
    public String getRemainingAsAdline(String subjectString) {
        String matchedValue = "";
        matchedValue = executeMatchPatternDomain(subjectString, "^(.*?)<span id=\"annotate[^<>]*>", 1);
        return matchedValue;
    }

    public String executeMatchPatternDomain(String content, String patternregex, int groupNumber) {
        String matchedData = "";
        try {
            Pattern regex = Pattern.compile(String.valueOf(patternregex), Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL | Pattern.UNICODE_CASE | Pattern.CANON_EQ);
            Matcher regexMatcher = regex.matcher(content);
            if (regexMatcher.find()) {
                matchedData = regexMatcher.group(groupNumber).trim();
            }
        } catch (PatternSyntaxException patternSyntaxException) {
            log.error("Error in executeMatchPatternDomain function {}. Exception - {}", patternSyntaxException.getPattern(), patternSyntaxException.toString());

        }
        return matchedData;
    }

    public boolean checkEmptyAfterCleaning(String subjectString) {
        return subjectString.replaceAll("(?sim)<[^<>]*>", "").replaceAll("(?sim)^\\s*\\|", "").replaceAll("(?sim)\\|\\s*$", "").replaceAll("(?sim)\\s+", " ").trim().isEmpty();
    }

    public int regexMatchCount(String Sentence, String regexFormed) {
        int count = 0;
        try {
            Pattern regex = Pattern.compile(regexFormed, Pattern.MULTILINE | Pattern.DOTALL | Pattern.UNICODE_CASE | Pattern.CANON_EQ);
            Matcher regexMatcher = regex.matcher(Sentence);
            while (regexMatcher.find()) {
                count++;
            }
        } catch (Exception exception) {
            log.error("Error in word count - {}", exception.getLocalizedMessage());
        }
        return count;
    }

    public String executeStringMatchPattern(String subjectString, String patternRegex, int groupNumber) {
        StringBuilder entityTextValue = new StringBuilder();
        try {
            Pattern regexFormatPattern = Pattern.compile(String.valueOf(patternRegex), Pattern.CANON_EQ | Pattern.DOTALL | Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE | Pattern.MULTILINE);
            Matcher regexFormatMatch = regexFormatPattern.matcher(subjectString);
            int i = 1;
            while (regexFormatMatch.find()) {
                String matchedText = regexFormatMatch.group(groupNumber).trim();
                entityTextValue.append(matchedText);
                if (i > 1) {
                    if (!StringUtils.containsIgnoreCase(entityTextValue.toString().trim(), matchedText.trim())) {
                        entityTextValue.append(matchedText).append("|");
                    }
                }
                i++;

            }
        } catch (PatternSyntaxException patternSyntaxException) {
            log.error("Exception in executeStringMatchPattern - {}", patternSyntaxException.getLocalizedMessage());
        }
        return entityTextValue.toString();
    }

    public String executeStringMatchPatternReturnIfCondition(String subjectString, String patternRegex, int groupNumber) {
        String entityTextValue = "";
        try {
            Pattern regexFormatPattern = Pattern.compile(String.valueOf(patternRegex), Pattern.CANON_EQ | Pattern.DOTALL | Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE | Pattern.MULTILINE);
            Matcher regexFormatMatch = regexFormatPattern.matcher(subjectString);
            if (regexFormatMatch.find()) {
                entityTextValue = regexFormatMatch.group(groupNumber).trim();
            }
        } catch (PatternSyntaxException patternSyntaxException) {
            log.error("Exception in executeStringMatchPatternReturnIfCondition - {}", patternSyntaxException.getLocalizedMessage());
        }
        return entityTextValue;
    }

    @Override
    public boolean executeStringMatchPatternBooleanCondition(String subjectString, String patternRegex, int groupNumber) {
        boolean patternMatch = false;
        try {
            Pattern regex = Pattern.compile(String.valueOf(patternRegex), Pattern.CANON_EQ | Pattern.DOTALL | Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE | Pattern.MULTILINE);
            Matcher regexMatcher = regex.matcher(subjectString);
            if (regexMatcher.find()) {
                patternMatch = true;
            }
        } catch (PatternSyntaxException patternSyntaxException) {
            log.error(" Regex Exception in Traversal Identification Module - {} ", patternSyntaxException.getLocalizedMessage());
        }
        return patternMatch;
    }

    public void loadAddressResources() throws IOException {

        ZIPCODE_LIST = locationUtil.getAddressZipcodes().stream().toList();
        ADDRESSLINEKEYS_LIST = locationUtil.getAddressAdLine().stream().toList();
        COUNTY_LIST = locationUtil.getAddressCounty().stream().toList();
        CITY_LIST = locationUtil.getAddressCity().stream().toList();
        ADDR_BEGIN_JUNK_LIST = locationUtil.getAddressBeginJunk().stream().toList();
    }

    @Override
    public String cleanAddressJunkInBeginning(String addressLine) {

        String finalAdLine = addressLine;
        for (String reg_pattern : ADDR_BEGIN_JUNK_LIST) {
            finalAdLine = executeMatchReplace(finalAdLine, reg_pattern);
        }
        return finalAdLine;
    }

    public String executeMatchReplace(String content, String regPattern) {
        String finalCleansedContent = "";
        try {
            finalCleansedContent = content.replaceAll(regPattern, "").trim();
            return finalCleansedContent;
        } catch (PatternSyntaxException patternSyntaxException) {
            log.error("Error in executeMatchPattern_domain function {}. Exception - {}", patternSyntaxException.getPattern(), patternSyntaxException.toString());
        }
        return content;
    }


    @Override
    public String normalizePageSourceAddress(String pageSource) {
        List<String> strToReplace = Arrays.asList("(?sim)&lt;,<", "(?sim)&gt;,>", "(?sim)\\s+, ", "&reg;, ", "&nbsp;, ", "&amp,&");
        pageSource = strToReplace.stream().reduce(pageSource, (tempStr, strToRemove) -> tempStr.replaceAll(strToRemove.substring(0, strToRemove.lastIndexOf(",")), strToRemove.substring(strToRemove.indexOf(",") + 1)));
        return pageSource.trim();
    }

    @Override
    public String extractSubLinkPageSource(String urlSource) throws IOException {
        log.info("Processing to extract html for sub link - {}", urlSource);
        String pageSource = null;

        try {
            log.info("Crawling url - {}", urlSource);
            int timeout = 5000;
            if (!urlSource.contains(".pdf")) {
                Document doc = Jsoup.connect(urlSource)
                        .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) "
                                + "AppleWebKit/537.36 (KHTML, like Gecko) "
                                + "Chrome/120.0.0.0 Safari/537.36")
                        .followRedirects(true)
                        .timeout(timeout)
                        .get();
                pageSource = doc.html();
                //  log.info("contactinexrtacter - {}",pageSource);
            }
            if (urlSource.contains(".pdf")) {
                URL url = new URL(urlSource); // Replace with the URL of the PDF document
                URLConnection connection = url.openConnection();
                BufferedInputStream bufferedInputStream = new BufferedInputStream(connection.getInputStream());
                PDDocument document = PDDocument.load(bufferedInputStream);
                PDFTextStripper textStripper = new PDFTextStripper();
                pageSource = textStripper.getText(document);
                bufferedInputStream.close();
                document.close();
            }
        } catch (Exception exception) {
            log.error(String.format("Exception in crawling sub link url %s", urlSource), exception.getLocalizedMessage());
        }
        return pageSource;
    }

    @Override
    public int wordCount(String content) {
        content = content.replaceAll("(?sim)<[^<>]*>", " ");
        int count = 0;
        try {
            Pattern regex = Pattern.compile("\\w+", Pattern.CANON_EQ | Pattern.DOTALL | Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE | Pattern.MULTILINE);
            Matcher regexMatcher = regex.matcher(content);
            while (regexMatcher.find()) {
                count++;
            }
        } catch (PatternSyntaxException patternSyntaxException) {
            log.error(" Regex Exception in wordCount function - {} ", patternSyntaxException.getLocalizedMessage());
        }
        return count;
    }
}
