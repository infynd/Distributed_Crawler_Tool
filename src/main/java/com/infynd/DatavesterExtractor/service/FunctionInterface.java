package com.infynd.DatavesterExtractor.service;

import java.util.Set;

public interface FunctionInterface {
    String getPartialDomainName(String domainName);

    String RemoveHTMLTags(String content);

    Set<String> executeMatchPattern(String content, String patternRegex, int groupNumber);


    String executeMatchPatternDomain(String content, String patternRegex, int groupNumber);

    Set<String> regexMatchedSetData(String content, String patternRegex, int groupNumber);


    String cleanPageSource(String pageSource);

    String normalizePageSource(String content);

    String encryptedEmailNormalization(String cleanContent);
}



