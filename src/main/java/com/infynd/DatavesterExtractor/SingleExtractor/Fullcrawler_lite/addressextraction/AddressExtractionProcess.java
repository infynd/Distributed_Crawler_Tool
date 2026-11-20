package com.infynd.DatavesterExtractor.SingleExtractor.Fullcrawler_lite.addressextraction;


import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Collectors;


@Slf4j
@Component
public class AddressExtractionProcess {

    @Autowired
    AddressProcessFunctionInterfaceProcess addressProcessFunctionInterface;


    public Set<String> processAddressExecution(String pageSource, String text2) {
        Set<String> finalAddressEntities = new HashSet<>();
        Set<String> parentElements;
        Set<String> countyElements;
        Set<String> cityElements;
        Set<String> addressLineElements;

        parentElements = new HashSet<>();
        countyElements = new HashSet<>();
        cityElements = new HashSet<>();
        addressLineElements = new HashSet<>();
        Set<String> finalOutputSet = new HashSet<>();
        org.jsoup.nodes.Document annotatedDocument = Jsoup.parse(pageSource);
        String sourceContent = annotatedDocument.toString().replaceAll("&quot;", "\"");
        String zipCode = "";
        String stringBlock = "";
        String zipElementString = "";
        String parentElementHtmlBlock = "";
        try {
            Pattern regex = Pattern.compile("~:~:span id=\"annotate_events\" title=\"ZIPCODE\" " + "style" + "=\"background-color:FF0000;\"~::~(.*?)\\s*~:~:/span~::~", Pattern.CANON_EQ | Pattern.DOTALL | Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE | Pattern.MULTILINE);
            Matcher regexMatcher = regex.matcher(sourceContent);

            while (regexMatcher.find()) {
                zipCode = regexMatcher.group(1);

                if (zipCode == null) {
                    log.warn("No match found for ZIPCODE pattern in the source content.");
                    continue;
                }

                Elements zipCodeElements = annotatedDocument.getElementsContainingOwnText(zipCode);
                for (int h = 0; h < zipCodeElements.size(); h++) {
                    zipElementString = zipCodeElements.get(h).outerHtml();
                    if (zipElementString.contains("ZIPCODE") && (zipElementString.contains("CITY") || zipElementString.contains("COUNTY")) && zipElementString.contains("ADLINE")) {
                        stringBlock = zipElementString.replaceAll("~:~:", "<").replaceAll("~::~", ">").replaceAll("&quot;", "").replaceAll("&nbsp;", " ");
                        if (stringBlock != null) {
                            stringBlock = stringBlock.trim();
                        }
                        String BlockContent = stringBlock.trim();
                        String MethodologyTraversal = findTraversal(BlockContent);
                        if (MethodologyTraversal.equals("Left Traversal")) {
                            identifyAddressElements(MethodologyTraversal, BlockContent, "Pattern - 1 : Address Elements Within Same HTML Element Block.", finalAddressEntities, cityElements, countyElements, addressLineElements);
                            log.info("Address Extracted in first rule ....");
                            break;
                        }

                    } else {
                        String htmlBoundaryTag = "";
                        Set<String> uniqueAddressBlocks = new HashSet<>();
                        String extractedZipBlock = zipElementString.replaceAll("(?sim)\\(", "\\\\(").replaceAll("(?sim)\\)", "\\\\)");
                        htmlBoundaryTag = addressProcessFunctionInterface.executeStringMatchPatternReturnIfCondition(sourceContent, extractedZipBlock + ".*?</([^s<>]+)>", 1);
                        String htmlBoundaryTagStartEndBlock = "";

                        try {
                            Pattern patternLevel1 = Pattern.compile("(<" + htmlBoundaryTag + " [^<>]*>).*?</" + htmlBoundaryTag + ">", Pattern.CANON_EQ | Pattern.DOTALL | Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE | Pattern.MULTILINE);
                            Matcher matcherLevel1 = patternLevel1.matcher(sourceContent);
                            while (matcherLevel1.find()) {
                                String matchedBlock = matcherLevel1.group(1);
                                htmlBoundaryTagStartEndBlock = addressProcessFunctionInterface.executeStringMatchPatternReturnIfCondition(sourceContent, matchedBlock + ".*?" + extractedZipBlock, 0);
                                if (htmlBoundaryTagStartEndBlock.contains(extractedZipBlock.replaceAll("(?sim)\\\\\\" + "(", "(").replaceAll("(?sim)\\\\\\)", ")"))) {
                                    uniqueAddressBlocks.add(htmlBoundaryTagStartEndBlock);
                                    break;
                                }
                            }
                        } catch (PatternSyntaxException patternSyntaxException) {
                            log.error("PatternSyntaxException in Finding Address Block {}", patternSyntaxException.getLocalizedMessage());
                        }

                        List<String> revOrdered = uniqueAddressBlocks.stream().sorted(Comparator.comparing(String::length).reversed()).toList();
                        for (String tempVar : revOrdered) {
                            if (tempVar.contains(extractedZipBlock)) {
                                zipElementString = tempVar;
                                break;
                            }
                        }

                        if (zipElementString.contains("ZIPCODE") && (zipElementString.contains("CITY") || zipElementString.contains("COUNTY"))) {
                            stringBlock = zipElementString.replaceAll("~:~:", "<").replaceAll("~::~", ">").replaceAll("&quot;", "").replaceAll("&nbsp;", " ");
                            String htmlBlockContent = stringBlock.trim();
                            String traversePattern = findTraversal(htmlBlockContent);
                            if (traversePattern.equalsIgnoreCase("Left Traversal")) {
                                identifyAddressElements(traversePattern, htmlBlockContent, "Pattern - 2 : Address Elements Within HTML Block as ZIPCODE ending HTML Tag as boundary.", finalAddressEntities, cityElements, countyElements, addressLineElements);
                                log.info("Address Extracted in rule 2 ....");
                            }
                        } else {
                            Elements htmlParentElements = zipCodeElements.parents();
                            for (Element element : htmlParentElements) {
                                String outerHtml = element.outerHtml();
                                parentElements.add(outerHtml);
                            }
                            boolean firstAddressFound = false;
                            Iterator<String> parentHtmlElement = parentElements.stream().sorted(Comparator.comparing(String::length)).collect(Collectors.toList()).listIterator();
                            while (parentHtmlElement.hasNext() && !firstAddressFound) {
                                parentElementHtmlBlock = parentHtmlElement.next();
                                parentElementHtmlBlock = parentElementHtmlBlock.replaceAll("~:~:", "<").replaceAll("~::~", ">").replaceAll("&quot;", "").replaceAll("&nbsp;", " ");
                                parentElementHtmlBlock = parentElementHtmlBlock.replaceAll("<meta[^<>]*.*?/>", "").replaceAll("<title" + "[^<>]*>.*?</title>", "");

                                if (parentElementHtmlBlock.contains("ZIPCODE") && (parentElementHtmlBlock.contains("CITY") || parentElementHtmlBlock.contains("COUNTY")) && parentElementHtmlBlock.contains("ADLINE")) {
                                    String addressBoundary = parentElementHtmlBlock;
                                    if (addressBoundary.contains("|")) {
                                        String[] splitText = addressBoundary.split("\\|");
                                        for (String splittedAddressBoundary : splitText) {
                                            boolean matchCase = addressProcessFunctionInterface.validateAddressBoundary(splittedAddressBoundary);
                                            if (matchCase) {
                                                try {
                                                    Pattern splitAddressBoundaryPattern = Pattern.compile("(" + splittedAddressBoundary + ").*?" + zipCode + ".*?<[^/]>", Pattern.CANON_EQ | Pattern.DOTALL | Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE | Pattern.MULTILINE);
                                                    Matcher splitAddressBoundaryMatch = splitAddressBoundaryPattern.matcher(addressBoundary);
                                                    if (splitAddressBoundaryMatch.find()) {
                                                        String companyBoundaryText = splitAddressBoundaryMatch.group();
                                                        String MethodologyTraversal = findTraversal(companyBoundaryText);
                                                        if (StringUtils.equalsIgnoreCase(MethodologyTraversal, "Left Traversal")) {
                                                            identifyAddressElements(MethodologyTraversal, companyBoundaryText, "Pattern - 3 : Address Elements Within HTML block as Outer HTML of ZIPCODE Tag.", finalAddressEntities, cityElements, countyElements, addressLineElements);

                                                            // Set the flag to true as the first address is found
                                                            firstAddressFound = true;
                                                        }
                                                    }
                                                } catch (PatternSyntaxException patternSyntaxException) {
                                                    log.error("Error in AddressBoundary with adline keyword {}", patternSyntaxException.getLocalizedMessage());
                                                }
                                            }
                                            if (firstAddressFound) break;
                                        }
                                    } else {
                                        boolean addressBoundaryMatchCase = addressProcessFunctionInterface.validateAddressBoundary(addressBoundary);
                                        if (addressBoundaryMatchCase) {
                                            try {
                                                Pattern addressBoundaryPattern = Pattern.compile("(" + addressBoundary + ").*?" + zipCode + ".*?<[^/]>", Pattern.CANON_EQ | Pattern.DOTALL | Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE | Pattern.MULTILINE);
                                                Matcher addressBoundaryMatch = addressBoundaryPattern.matcher(addressBoundary);
                                                if (addressBoundaryMatch.find()) {
                                                    String company_boundaryText = addressBoundaryMatch.group();
                                                    if (company_boundaryText.length() < 8000) {
                                                        String MethodologyTraversal = findTraversal(company_boundaryText);
                                                        if (StringUtils.equalsIgnoreCase(MethodologyTraversal, "Left Traversal")) {
                                                            identifyAddressElements(MethodologyTraversal, company_boundaryText, "Pattern - 4 : Address Elements With ADDRESS LINE keyword as boundary with Outer HTML of ZIPCODE TAG.", finalAddressEntities, cityElements, countyElements, addressLineElements);
                                                            firstAddressFound = true;
                                                        }
                                                    }
                                                } else {
                                                    String adline_boundaryText = addressProcessFunctionInterface.executeStringMatchPatternReturnIfCondition(addressBoundary, "((Address|<[^<>]*>\\s*\\d+\\s*<[^<>]*>).*?)?<[^<>]*>\\s*[^<>]*<[^>]*ADLINE[^<>]*>.*?" + zipCode + ".*?<[^<>]*>", 0);
                                                    if (StringUtils.isNotBlank(adline_boundaryText) && adline_boundaryText.length() < 8000) {
                                                        String MethodologyTraversal = findTraversal(adline_boundaryText);
                                                        if (StringUtils.equalsIgnoreCase(MethodologyTraversal, "Left Traversal")) {
                                                            identifyAddressElements(MethodologyTraversal, adline_boundaryText, "Pattern - 5 : Address Elements With ADDRESS keyword as boundary with Outer HTML of ZIPCODE TAG.", finalAddressEntities, cityElements, countyElements, addressLineElements);
                                                            firstAddressFound = true;
                                                        }
                                                    }
                                                }
                                            } catch (PatternSyntaxException patternSyntaxException) {
                                                log.error("PatternSyntaxException in finding Address boundary with Adline keyword {}", patternSyntaxException.getLocalizedMessage());
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } catch (PatternSyntaxException ex) {
            log.error("PatternSyntaxException occurred: {}", ex.getLocalizedMessage());
        } catch (IllegalStateException ex) {
            log.error("IllegalStateException occurred: {}", ex.getLocalizedMessage());
        }


        if (finalAddressEntities.size() > 0) {
            Iterator iterator = finalAddressEntities.iterator();
            while (iterator.hasNext()) {
                ArrayList<String> result = new ArrayList<String>();
                String text = iterator.next().toString();
                String[] splitterArray = text.split("(?sim)\\t");
                Collections.addAll(result, splitterArray);
                String outputZipcode = "";
                String outputCounty = "";
                String outputCity = "";
                String outputAddressLine = "";
                String outputAddressBlock = "";
                String outputAddressPattern = "";
                for (int k = 0; k < result.size(); k++) {

                    String resultText = result.get(k).trim();

                    if (StringUtils.containsIgnoreCase(resultText, "Zipcode -")) {
                        try {
                            if (outputZipcode.trim().isEmpty()) {
                                outputZipcode = resultText.replaceAll("(?sim)^zipCode\\s*-\\s*(.*?)\\s*$", "$1").trim();
                            }
                        } catch (Exception exception) {
                            log.error("Error in final splitter {} - ", exception.getLocalizedMessage());

                        }
                    }
                    if (StringUtils.containsIgnoreCase(resultText, "County -")) {
                        try {
                            if (outputCounty.trim().isEmpty()) {
                                outputCounty = resultText.replaceAll("(?sim)^County\\s*-\\s*(.*?)\\s*$", "$1").trim();
                            }
                        } catch (Exception exception) {
                            log.error("Error in final splitter {} - ", exception.getLocalizedMessage());

                        }
                    }
                    if (StringUtils.containsIgnoreCase(resultText, "City -")) {
                        try {
                            if (outputCity.trim().isEmpty()) {
                                outputCity = resultText.replaceAll("(?sim)^City\\s*-\\s*(.*?)\\s*$", "$1").trim();
                            }
                        } catch (Exception exception) {
                            log.error("Error in final splitter {} - ", exception.getLocalizedMessage());

                        }
                    }
                    if (StringUtils.containsIgnoreCase(resultText, "AddressLine -")) {
                        try {
                            outputAddressLine = resultText.replaceAll("(?sim)^AddressLine\\s*-\\s*(.*?)\\s*$", "$1").trim();
                        } catch (Exception exception) {
                            log.error("Error in final splitter {} - ", exception.getLocalizedMessage());

                        }
                    }

                    if (StringUtils.containsIgnoreCase(resultText, "AddressBlock -")) {
                        try {
                            outputAddressBlock = resultText.replaceAll("(?sim)^AddressBlock\\s*-\\s*(.*?)\\s*$", "$1").trim();
                        } catch (Exception exception) {
                            log.error("Error in final splitter {} - ", exception.getLocalizedMessage());

                        }
                    }
                    if (StringUtils.containsIgnoreCase(resultText, "AddressPattern - ")) {
                        try {
                            outputAddressPattern = resultText.trim();
                        } catch (Exception exception) {
                            log.error("Error in final splitter {} - ", exception.getLocalizedMessage());

                        }
                    }
                }

                String addressLine1 = "";
                String addressLine2 = "";
                String addressLine3 = "";

                if (StringUtils.contains(outputAddressLine, "|")) {
                    String[] SplitterLine = outputAddressLine.split("(?sim)\\|");
                    ArrayList<String> result_AddressLine = new ArrayList<String>(Arrays.asList(SplitterLine));
                    for (int i = 0; i < result_AddressLine.size(); i++) {
                        if (i == 0) {
                            addressLine1 = result_AddressLine.get(0);

                        }
                        if (i == 1) {
                            addressLine2 = result_AddressLine.get(1);
                        }
                        if (i == 2) {
                            addressLine3 = result_AddressLine.get(2);
                        }
                    }
                } else {
                    addressLine1 = outputAddressLine;
                }

                StringJoiner joinEntities = new StringJoiner("~::~");
                addressLine1 = addressProcessFunctionInterface.validateEntity(addressLine1);
                addressLine2 = addressProcessFunctionInterface.validateEntity(addressLine2);
                addressLine3 = addressProcessFunctionInterface.validateEntity(addressLine3);
                outputCity = addressProcessFunctionInterface.validateEntity(outputCity);
                outputCounty = addressProcessFunctionInterface.validateEntity(outputCounty);
                outputZipcode = addressProcessFunctionInterface.validateEntity(outputZipcode);
                addressLine1 = addressProcessFunctionInterface.removeOtherAddressEntities(addressLine1, outputZipcode);
                addressLine1 = addressProcessFunctionInterface.removeOtherAddressEntities(addressLine1, outputCounty);
                addressLine1 = addressProcessFunctionInterface.removeOtherAddressEntities(addressLine1, outputCity);
                addressLine2 = addressProcessFunctionInterface.removeOtherAddressEntities(addressLine2, outputZipcode);
                addressLine2 = addressProcessFunctionInterface.removeOtherAddressEntities(addressLine2, outputCounty);
                addressLine2 = addressProcessFunctionInterface.removeOtherAddressEntities(addressLine2, outputCity);
                addressLine3 = addressProcessFunctionInterface.removeOtherAddressEntities(addressLine3, outputZipcode);
                addressLine3 = addressProcessFunctionInterface.removeOtherAddressEntities(addressLine3, outputCounty);
                addressLine3 = addressProcessFunctionInterface.removeOtherAddressEntities(addressLine3, outputCity);
                outputCity = addressProcessFunctionInterface.removeOtherAddressEntities(outputCity, outputCounty);
                outputCounty = addressProcessFunctionInterface.removeOtherAddressEntities(outputCounty, outputCity);
                addressLine1 = addressProcessFunctionInterface.cleanAddressJunkInBeginning(addressLine1);
                addressLine2 = addressProcessFunctionInterface.cleanAddressJunkInBeginning(addressLine2);
                addressLine3 = addressProcessFunctionInterface.cleanAddressJunkInBeginning(addressLine3);
                if (addressLine1.trim().isEmpty()) {
                    addressLine1 = addressLine2;
                    addressLine2 = "";
                }
                if (addressLine2.trim().isEmpty()) {
                    addressLine2 = addressLine3;
                    addressLine3 = "";
                }


                joinEntities.add("AddressLine-1:".concat(addressLine1));
                joinEntities.add("AddressLine-2:".concat(addressLine2));
                joinEntities.add("AddressLine-3:".concat(addressLine3));
                joinEntities.add("City:".concat(outputCity));
                joinEntities.add("County:".concat(outputCounty));
                joinEntities.add("Zipcode:".concat(outputZipcode));
                if (StringUtils.containsIgnoreCase(pageSource, "Registered Office") || StringUtils.containsIgnoreCase(pageSource, "Head Office") || StringUtils.containsIgnoreCase(pageSource, "headquarters") || StringUtils.containsIgnoreCase(pageSource, "Registered Office Address")) {
                    joinEntities.add("HeadQuarters:TRUE");
                } else {
                    joinEntities.add("HeadQuarters:FALSE");
                }
                if (outputZipcode != null && text2 != null) {
                    String result2 = findFirstMatch(text2, outputZipcode);
                    if (result2 != null) {
                        joinEntities.add("AddressPattern:".concat(result2));
                    }
                }
                finalOutputSet.add(joinEntities.toString());
            }


        }

        return finalOutputSet;
    }

    public String findTraversal(String textBlockContent) {
        boolean addressPatternMatch = false;
        try {
            addressPatternMatch = addressProcessFunctionInterface.executeStringMatchPatternBooleanCondition(textBlockContent, "(ADLINE).*?(City).*?(County).*?(Zipcode)|(ADLINE).*?(County).*?" + "(City).*?(Zipcode)|(ADLINE).*?(County).*?(Zipcode).*?(City)|(ADLINE).*?(City).*?(Zipcode)" + ".*?" + "(County)", 0);

            if (addressPatternMatch) {
                return "Left Traversal";
            }
            addressPatternMatch = addressProcessFunctionInterface.executeStringMatchPatternBooleanCondition(textBlockContent, "(Zipcode).*?(County).*?(City).*?(ADLINE)|(Zipcode).*?(City).*?" + "(County).*?(ADLINE)|(City).*?(Zipcode).*?(County).*?(ADLINE)|(County).*?(Zipcode).*?(City)" + ".*?(ADLINE)", 0);
            if (addressPatternMatch) {
                return "Right Traversal";
            }
        } catch (PatternSyntaxException patternSyntaxException) {
            log.error("Exception in Traversal Identification Module - {} ", patternSyntaxException.getLocalizedMessage());
        }
        return "Left Traversal";

    }

    public void identifyAddressElements(String methodologyTraversal, String textBlockContent, String addressPattern, Set<String> finalAddressEntities, Set<String> cityElements, Set<String> countyElements, Set<String> addressLineElements) {
        try {
            Pattern regex = Pattern.compile("^(.*?)<span[^<>]*ZIPCODE[^<>]*>.*?</span>", Pattern.CANON_EQ | Pattern.DOTALL | Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE | Pattern.MULTILINE);
            Matcher regexMatcher = regex.matcher(textBlockContent);
            while (regexMatcher.find()) {
                String textBlock = regexMatcher.group();
                String zipcode = "";
                try {
                    Pattern zipRegexPattern = Pattern.compile("(,|\\||<br\\s*[/]>|<[/]?span[^<>]*>)" + "?\\s" + "*[^<>]*<[^<>]*Zipcode[^<>]*>(.*?)(,|\\||<br[/]>|<[/]?span[^<>]*>)", Pattern.CANON_EQ | Pattern.DOTALL | Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE | Pattern.MULTILINE);
                    Matcher zipRegexMatcher = zipRegexPattern.matcher(textBlock);
                    if (zipRegexMatcher.find()) {
                        zipcode = zipRegexMatcher.group(2).replaceAll("(?sim)<[^<>]*>", "").replaceAll("(?sim)\\s*," + "\\s*+", ",");
                    }
                } catch (PatternSyntaxException patternSyntaxException) {
                    log.error("Exception in regex zipcode extraction - {}", patternSyntaxException.getLocalizedMessage());
                }
                countyElements = extractionEntityCounty("COUNTY", textBlock);
                cityElements = extractionEntityCity("CITY", textBlock);
                addressLineElements = Adline_Extraction_Entity("ADLINE", textBlock);
                String countyText = "";
                String cityText = "";
                String addresslineText = "";

                countyText = countyElements.stream().map(item -> item.replaceAll("(?sim)^\\s*\\W\\s*", "").replaceAll("(?sim)\\s*\\W\\s*$", "").replaceAll("(?sim)\\|+", "|").replaceAll("(?sim)^\\|", "").replaceAll("(?sim)\\|\\s*$", "")).collect(Collectors.joining("|"));
                cityText = cityElements.stream().map(item -> item.replaceAll("(?sim)^\\s*\\W\\s*", "").replaceAll("(?sim)\\s*\\W\\s*$", "")).collect(Collectors.joining("|"));
                addresslineText = addressLineElements.stream().filter(item -> !item.contains("=")).map(item -> item.replaceAll("(?sim).*?~~~", "").replaceAll("(?sim)^\\s*\\W\\s*", "").replaceAll("(?sim)\\s*\\W\\s*$", "").replaceAll("(?sim)\\|+", "|").replaceAll("(?sim)^\\|", "").replaceAll("(?sim)\\|\\s*$", "")).sorted(Collections.reverseOrder()).collect(Collectors.joining("|"));
                textBlockContent = textBlockContent.replaceAll("(?sim)<span id=annotate_events[^<>]*>", "").replaceAll("(?sim)" + "</span>", "");
                textBlockContent = textBlockContent.replaceAll(", ,", ",").replaceAll("(?sim)\\s+", " ").trim();
                if (!addresslineText.trim().isEmpty()) {
                    try {
                        StringJoiner join = new StringJoiner("\t");
                        join.add("Zipcode - " + zipcode);
                        join.add("County - " + countyText);
                        join.add("City - " + cityText);
                        join.add("AddressLine - " + addresslineText);
                        join.add("AddressBlock - " + textBlockContent);
                        join.add("AddressPattern - " + addressPattern);
                        finalAddressEntities.add(join.toString());
                    } catch (Exception exception) {
                        log.error("Exception in StringJoiner - {} ", exception.getLocalizedMessage());
                    }
                }

            }
        } catch (Exception exception) {
            log.error("Exception in identifyAddressElements {}", exception.getLocalizedMessage());
        }
    }

    public Set<String> extractionEntityCity(String entity, String subjectString) {
        Set<String> cityElements = new HashSet<>();
        subjectString = subjectString.replaceAll("(?sim)^.*?Address[:\\-,]?", " ");
        String entityTextValue = "";
        if (entity.equalsIgnoreCase("city")) {
            entityTextValue = addressProcessFunctionInterface.executeStringMatchPattern(subjectString, "ADLINE.*?</SPAN>.*?<[^<>]*(?=(" + entity + "))" + entity + "[^<>]*>(.*?)" + "(<[^<>]*County" + "[^<>]*>)", 2);
        } else {
            entityTextValue = addressProcessFunctionInterface.executeStringMatchPattern(subjectString, "(,)" + "[^<>]*<[^<>]*(?=(" + entity + "))" + entity + "[^<>]*>(.*?)(,)", 3);
        }
        if (addressProcessFunctionInterface.checkEmptyAfterCleaning(entityTextValue)) {
            entityTextValue = addressProcessFunctionInterface.executeStringMatchPattern(subjectString, "(,\\s*" + "(<span\\s*id=annotate_events title[^<>]*>)*)[^<>]*<[^<>]*(?=(" + entity + "))" + entity + "[^<>]*>(.*?)(,)", 4);
        }
        if (addressProcessFunctionInterface.checkEmptyAfterCleaning(entityTextValue)) {
            entityTextValue = addressProcessFunctionInterface.executeStringMatchPattern(subjectString, "(<br" + "[^<>]*[/]?>\\s*(\\s*<[^<>]*>\\s*)*)[^<>]*<[^<>]*(?=(" + entity + "))" + entity + "[^<>]*>" + "([^<>]*" + "(</span>)*\\s*[^<>]*)", 4);
        }
        if (addressProcessFunctionInterface.checkEmptyAfterCleaning(entityTextValue)) {
            entityTextValue = addressProcessFunctionInterface.executeStringMatchPattern(subjectString, "((\\s" + "*<[^<>]*>\\s*)*)[^<>]*<[^<>]*(?=(" + entity + "))" + entity + "[^<>]*>([^<>]*" + "(</span>)" + "*\\s" + "*[^<>]*)", 4);
        }
        if (addressProcessFunctionInterface.checkEmptyAfterCleaning(entityTextValue)) {
            entityTextValue = addressProcessFunctionInterface.executeStringMatchPattern(subjectString, "(<br" + "[^<>]*[/]?>\\s*(\\s*<[^<>]*>\\s*)*)[^<>]*<[^<>]*(?=(" + entity + "))" + entity + "[^<>]*>" + "([^<>]*" + "(<[^<>]*span[^<>]*>)*\\s*[^<>]*)", 4);
        }
        if (addressProcessFunctionInterface.checkEmptyAfterCleaning(entityTextValue)) {
            entityTextValue = addressProcessFunctionInterface.executeStringMatchPattern(subjectString, "(<[^<>]*[/]?>\\s*(\\s*<[^<>]*>\\s*)*)[^<>]*<[^<>]*(?=(" + entity + "))" + entity + "[^<>]*>" + "([^<>]*(<[^<>]*span[^<>]*>)*\\s*[^<>]*)", 4);
        }
        if (!StringUtils.contains(entityTextValue, "|") && (StringUtils.contains(entityTextValue, "</span></span>"))) {
            Pattern regexFormatPatternForDividedText = Pattern.compile("(,)([^<>]*<[^<>]*(?=(" + entity + "))(" + entity + ")[^<>]*>(.*?))(,)", Pattern.CANON_EQ | Pattern.DOTALL | Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE | Pattern.MULTILINE);
            Matcher regexFormatMatchForDividedText = regexFormatPatternForDividedText.matcher(subjectString);
            if (regexFormatMatchForDividedText.find()) {
                entityTextValue = regexFormatMatchForDividedText.group(2).trim();
            }
        }
        Pattern regexFormatPatternForRemoveAtEnd = Pattern.compile("</span>(\\s*</span>)*,\\s*\\w+\\s*$", Pattern.CANON_EQ | Pattern.DOTALL | Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE | Pattern.MULTILINE);
        Matcher regexFormatMatchForRemoveAtEnd = regexFormatPatternForRemoveAtEnd.matcher(entityTextValue);
        if (regexFormatMatchForRemoveAtEnd.find()) {
            entityTextValue = entityTextValue.replaceAll("(?sim)</span>(\\s*</span>)*,\\s*\\w+\\s*$", " ");
        }
        entityTextValue = entityTextValue.replaceAll(", ,", ",").replaceAll("(?sim)<[^<>]*>", " ").replaceAll("(?sim)" + "^\\s*[,\\|]\\s*", "").replaceAll("(?sim)\\|\\s*$", "").replaceAll("(?sim)" + "\\s+", " ").trim();
        String[] finalEntityValue = entityTextValue.split("(?sim)\\|");
        for (String splitText : finalEntityValue) {
            if (StringUtils.equalsIgnoreCase(entity, "CITY")) {
                if (addressProcessFunctionInterface.regexMatchCount(splitText.trim(), "\\w+") < 5) {
                    cityElements.add(splitText);
                }
            }

        }
        return cityElements;
    }

    public Set<String> extractionEntityCounty(String entity, String subjectString) {
        Set<String> countyElements = new HashSet<>();
        subjectString = subjectString.replaceAll("(?sim)^.*?Address[:\\-,]?", " ");
        String entityTextValue = "";
        if (entity.equalsIgnoreCase("city")) {
            entityTextValue = addressProcessFunctionInterface.executeStringMatchPattern(subjectString, "ADLINE.*?</SPAN>.*?<[^<>]*(?=(" + entity + "))" + entity + "[^<>]*>(.*?)(<[^<>]*County" + "[^<>]*>)", 2);
        } else {
            entityTextValue = addressProcessFunctionInterface.executeStringMatchPattern(subjectString, "(,)" + "[^<>]*<[^<>]*(?=(" + entity + "))" + entity + "[^<>]*>(.*?)(,)", 3);
        }
        if (addressProcessFunctionInterface.checkEmptyAfterCleaning(entityTextValue)) {
            entityTextValue = addressProcessFunctionInterface.executeStringMatchPattern(subjectString, "(,\\s*" + "(<span\\s*id=annotate_events title[^<>]*>)*)[^<>]*<[^<>]*(?=(" + entity + "))" + entity + "[^<>]*>(.*?)(,)", 4);
        }
        if (addressProcessFunctionInterface.checkEmptyAfterCleaning(entityTextValue)) {
            entityTextValue = addressProcessFunctionInterface.executeStringMatchPattern(subjectString, "(<br" + "[^<>]*[/]?>\\s*(\\s*<[^<>]*>\\s*)*)[^<>]*<[^<>]*(?=(" + entity + "))" + entity + "[^<>]*>([^<>]*(</span>)*\\s*[^<>]*)", 4);
        }
        if (addressProcessFunctionInterface.checkEmptyAfterCleaning(entityTextValue)) {
            entityTextValue = addressProcessFunctionInterface.executeStringMatchPattern(subjectString, "((\\s" + "*<[^<>]*>\\s*)*)[^<>]*<[^<>]*(?=(" + entity + "))" + entity + "[^<>]*>([^<>]*" + "(</span>)*\\s*[^<>]*)", 4);

        }
        if (addressProcessFunctionInterface.checkEmptyAfterCleaning(entityTextValue)) {
            entityTextValue = addressProcessFunctionInterface.executeStringMatchPattern(subjectString, "(<br" + "[^<>]*[/]?>\\s*(\\s*<[^<>]*>\\s*)*)[^<>]*<[^<>]*(?=(" + entity + "))" + entity + "[^<>]*>([^<>]*(<[^<>]*span[^<>]*>)*\\s*[^<>]*)", 4);

        }
        if (addressProcessFunctionInterface.checkEmptyAfterCleaning(entityTextValue)) {
            entityTextValue = addressProcessFunctionInterface.executeStringMatchPattern(subjectString, "(<[^<>]*[/]?>\\s*(\\s*<[^<>]*>\\s*)*)[^<>]*<[^<>]*(?=(" + entity + "))" + entity + "[^<>]*>" + "([^<>]*(<[^<>]*span[^<>]*>)*\\s*[^<>]*)", 4);
        }
        if (!StringUtils.contains(entityTextValue, "|") && (StringUtils.contains(entityTextValue, "</span></span>"))) {
            Pattern regexFormatPatternForDividedText = Pattern.compile("(,)([^<>]*<[^<>]*(?=(" + entity + "))(" + entity + ")[^<>]*>(.*?))(,)", Pattern.CANON_EQ | Pattern.DOTALL | Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE | Pattern.MULTILINE);
            Matcher regexFormatMatchForDividedText = regexFormatPatternForDividedText.matcher(subjectString);
            if (regexFormatMatchForDividedText.find()) {
                entityTextValue = regexFormatMatchForDividedText.group(2).trim();
            }
        }


        entityTextValue = entityTextValue.replaceAll(", ,", ",").replaceAll("(?sim)<[^<>]*>", " ").replaceAll("(?sim)" + "^\\s*[,\\|]\\s*", "").replaceAll("(?sim)\\|\\s*$", "").replaceAll("(?sim)" + "\\s+", " ").trim();
        String[] finalEntityValue = entityTextValue.split("(?sim)\\|");
        for (String splitText : finalEntityValue) {
            if (entity.equalsIgnoreCase("county")) {
                if (addressProcessFunctionInterface.regexMatchCount(splitText.trim(), "\\w+") < 3) {
                    countyElements.add(splitText);
                }
            }
        }
        return countyElements;
    }

    public Set<String> Adline_Extraction_Entity(String entity, String subjectString) {
        subjectString = subjectString.replaceAll("(?sim)^.*?(Address[:\\-,]?.*?$)", "$1");
        Set<String> addressLineElements = new HashSet<>();
        String entityTextValue = "";
        subjectString = subjectString.replaceAll("(?sim)\\s+", " ").trim();
        if (StringUtils.containsIgnoreCase(subjectString, "unit ") || StringUtils.containsIgnoreCase(subjectString, "unit<") || StringUtils.containsIgnoreCase(subjectString, "unit,")) {
            entityTextValue = addressProcessFunctionInterface.executeStringMatchPattern(subjectString, "Unit.*?<[^<>]*(?=(" + entity + "))" + entity + "[^<>]*>(.*?)(,(\\s*[^<>]*<[^<>]*>)?)", 0);
            if (entityTextValue.trim().isEmpty()) {
                entityTextValue = addressProcessFunctionInterface.executeStringMatchPattern(subjectString, "Unit.*?<[^<>]*(?=(" + entity + "))" + entity + "[^<>]*>(.*?)(<[^<>]*(CITY|COUNTY)[^<>]*>)", 0);
            } else {
                String cleansedEntityTextValueRemoved = entityTextValue.replaceAll("(?sim)<[^<>]*>", " ").toLowerCase().replaceAll("(?sim)[^\\w]*$", "");
                String lastWord = cleansedEntityTextValueRemoved.replaceAll("^.*?(\\w+\\s*$)", "$1").trim();
                if (StringUtils.equalsIgnoreCase(lastWord.toLowerCase().trim(), "south") || StringUtils.equalsIgnoreCase(lastWord.toLowerCase().trim(), "west") || StringUtils.equalsIgnoreCase(lastWord.toLowerCase().trim(), "east") || StringUtils.equalsIgnoreCase(lastWord.toLowerCase().trim(), "north")) {
                    entityTextValue = addressProcessFunctionInterface.executeStringMatchPattern(subjectString, "Unit.*?<[^<>]*(?=(" + entity + "))" + entity + "[^<>]*>(.*?)(<[^<>]*(CITY|COUNTY)" + "[^<>]*>)", 0);
                }
            }
        }
        if (addressProcessFunctionInterface.checkEmptyAfterCleaning(entityTextValue)) {
            entityTextValue = addressProcessFunctionInterface.executeStringMatchPattern(subjectString, "(((\\d+|\\w+)\\s*)?(\\d+)?,)[^<>]*<[^<>]*(?=(" + entity + "))" + entity + "[^<>]*>(.*?)(," + "(\\s*[^<>]*<[^<>]*>){0,2})", 0);
        }
        if (addressProcessFunctionInterface.checkEmptyAfterCleaning(entityTextValue) || entityTextValue.trim().charAt(0) == ',') {
            entityTextValue = addressProcessFunctionInterface.executeStringMatchPattern(subjectString, "((\\d+\\s*)*" + ")[^<>]*<[^<>]*(?=(" + entity + "))" + entity + "[^<>]*>(.*?)(," + "(\\s*[^<>]*<[^<>]*>){0,2})", 0);
        }
        if (addressProcessFunctionInterface.checkEmptyAfterCleaning(entityTextValue) || entityTextValue.trim().charAt(0) == ',') {
            entityTextValue = addressProcessFunctionInterface.executeStringMatchPattern(subjectString, "[^<>]*<[^<>]*(?=" + "(" + entity + "))" + entity + "[^<>]*>(.*?)(,(\\s*[^<>]*<[^<>]*>)?" + "([^<>]*" + "(</span>|,)))", 0);
        }
        if (addressProcessFunctionInterface.checkEmptyAfterCleaning(entityTextValue) || entityTextValue.trim().charAt(0) == ',') {
            entityTextValue = addressProcessFunctionInterface.executeStringMatchPattern(subjectString, "(<[^<>]*[/]?>" + ".*?<[^<>]*" + entity + "[^<>]*>.*?</span>[^<>]*)", 0);
        }

        if (addressProcessFunctionInterface.checkEmptyAfterCleaning(entityTextValue) || entityTextValue.trim().charAt(0) == ',') {
            entityTextValue = addressProcessFunctionInterface.executeStringMatchPattern(subjectString, "(<br" + "[^<>]*[/]?>\\s*(\\s*<[^<>]*>\\s*)*)[^<>]*<[^<>]*(?=(" + entity + "))" + entity + "[^<>]*>" + "([^<>]*" + "(</span>)*\\s*[^<>]*)", 0);
        }

        if (addressProcessFunctionInterface.checkEmptyAfterCleaning(entityTextValue) || entityTextValue.trim().charAt(0) == ',') {
            entityTextValue = addressProcessFunctionInterface.executeStringMatchPattern(subjectString, "((\\s" + "*<[^<>]*>\\s*)*)[^<>]*<[^<>]*(?=(" + entity + "))" + entity + "[^<>]*>([^<>]*" + "(</span>)" + "*\\s" + "*[^<>]*)", 0);
        }

        if (addressProcessFunctionInterface.wordCount(entityTextValue) > 10) {
            entityTextValue = addressProcessFunctionInterface.executeStringMatchPattern(subjectString, "(<[^<>]*>(\\s*\\w+\\s*){1,3}<[^<>]*(?=(" + entity + "))" + entity + "[^<>]*>(\\s*\\w+\\s*){1," + "3})(\\s*<[^<>]*>\\s*){0,2}.*?</span>.*?<span[^<>]*annotate_events[^<>]*>", 0);
        }


        entityTextValue = entityTextValue.replaceAll("(?sim)<[^<>]*>", " ").replaceAll("(?sim)^\\s*\\|", "").replaceAll("(?sim)" + "\\|\\s*$", "").replaceAll("(?sim)^[,|]+", "").replaceAll("(?sim)" + "\\s+", " ").trim();
        if (entityTextValue.length() < 10) {
            entityTextValue = addressProcessFunctionInterface.executeStringMatchPattern(subjectString, "<br" + "[^<>]*[/]?>.*?" + entity + "[^<>]*>[^<>]*([^<>]*)", 0);
        }
        entityTextValue = entityTextValue.replaceAll("(?sim)<[^<>]*>", " ").replaceAll("(?sim)^\\s*\\|", "").replaceAll("(?sim)" + "\\|\\s*$", "").replaceAll("(?sim)\\s+", " ").replaceAll("(?sim)" + "^[,|]+", "").trim();
        if (entityTextValue.length() < 10) {
            entityTextValue = addressProcessFunctionInterface.executeStringMatchPattern(subjectString, "<[^<>]*[/]?>" + ".*?" + entity + "[^<>]*>[^<>]*([^<>]*)", 0);
        }
        entityTextValue = entityTextValue.replaceAll(", ,", ",").replaceAll("(?sim)<[^<>]*>", " ").replaceAll("(?sim)" + "^\\s*\\|", "").replaceAll("(?sim)\\|\\s*$", "").replaceAll("(?sim)^[,|]", "").replaceAll("(?sim)" + "\\s+", " ").trim();


        String[] finalEntityValue = entityTextValue.split("(?sim)\\|");
        int iteratorValue = 1;
        for (String splitTextValue : finalEntityValue) {
            if (entity.equalsIgnoreCase("Adline")) {
                if (splitTextValue.trim().isEmpty()) {
                    splitTextValue = addressProcessFunctionInterface.getRemainingAsAdline(subjectString);
                    if (splitTextValue.contains("<br")) {
                        splitTextValue = splitTextValue.replaceAll("(?sim)^.*?<br[^<>]*>", "").replaceAll("(?sim)" + "<[^<>]*>", ",");
                        splitTextValue = addressProcessFunctionInterface.cleanAddressJunkInBeginning(splitTextValue);
                    } else {
                        splitTextValue = splitTextValue.replaceAll("(?sim)^.*?[^\\d]+", "").replaceAll("(?sim)" + "<[^<>]*>", ",");
                        splitTextValue = addressProcessFunctionInterface.cleanAddressJunkInBeginning(splitTextValue);
                    }
                }
                splitTextValue = splitTextValue.replaceAll("(?sim)^.*?(Limited|Ltd)(\\.)?", " ").replaceAll("(?sim), " + ",", ", ").replaceAll("(?sim)\\s+", " ").trim();
                splitTextValue = addressProcessFunctionInterface.cleanAddressJunkInBeginning(splitTextValue);
                if (addressProcessFunctionInterface.regexMatchCount(splitTextValue.trim(), "\\w+") <= 10 && addressProcessFunctionInterface.regexMatchCount(splitTextValue.trim(), ",") <= 3) {
                    addressLineElements.add(iteratorValue + "~~~" + splitTextValue);
                    iteratorValue++;
                }
            }
        }
        return addressLineElements;
    }

    public static String findFirstMatch(String text, String zipcode) {
        Pattern pattern = Pattern.compile(zipcode);
        Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            int start = matcher.start();
            int beginIndex = Math.max(0, start - 70);
            return text.substring(beginIndex, start) + zipcode;
        }
        return null;
    }
}
