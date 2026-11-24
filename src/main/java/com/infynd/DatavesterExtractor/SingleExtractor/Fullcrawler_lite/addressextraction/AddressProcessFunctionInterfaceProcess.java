package com.infynd.DatavesterExtractor.SingleExtractor.Fullcrawler_lite.addressextraction;

import java.io.IOException;
import java.util.List;

public interface AddressProcessFunctionInterfaceProcess {

    String spanTaggedContentZipcode(String tempOutput, List<String> zipcode, String entityName);

    String spanTaggedContent(String tempOutput, List<String> entityAttribute, String entityName);

    boolean validateAddressBoundary(String addressBoundary);

    String removeFormattingTags(String parsedPageSource);

    String removeInjectedTags(String removedContent);

    String validateEntity(String addressEntity);

    String removeOtherAddressEntities(String county, String entity);

    String cleanAttributesEndOffset(String entity);

    String cleanAttributesBeginOffset(String entity);

    String cleanAddressJunkInBeginning(String addressLine);

    String executeMatchReplace(String content, String regPattern);

    String getRelatedEntity(String sourceContent, String entity_1, String entity_2, int length);

    String getRemainingAsAdline(String subjectString);

    boolean checkEmptyAfterCleaning(String subjectString);

    int regexMatchCount(String subjectString, String regexFormed);

    String executeStringMatchPattern(String content, String patternRegex, int groupNumber);

    String executeStringMatchPatternReturnIfCondition(String subjectString, String patternRegex, int groupNumber);

    boolean executeStringMatchPatternBooleanCondition(String subjectString, String patternRegex, int groupNumber);


    String extractSubLinkPageSource(String urlSource) throws IOException;

    String normalizePageSourceAddress(String pageSource);

    void loadAddressResources() throws IOException;
    int wordCount(String content);

}
