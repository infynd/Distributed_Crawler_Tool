package com.infynd.DatavesterExtractor.SingleExtractor.Fullcrawler_lite.TechStackExtraction;

import com.infynd.DatavesterExtractor.SingleExtractor.Fullcrawler_lite.addressextraction.AddressProcessFunctionInterfaceProcess;
import com.infynd.DatavesterExtractor.SingleExtractor.Fullcrawler_lite.utils.WebsiteCrawlerConstants;
import com.infynd.DatavesterExtractor.SingleExtractor.Fullcrawler_lite.utils.service.LocationUtilExtrator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.Serializable;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class JobsExtraction {
    @Autowired
    AddressProcessFunctionInterfaceProcess addressProcessFunctionInterface;
    @Autowired
    LocationUtilExtrator locationUtil;

    public Map<String, Serializable> extract(Map<String, Serializable> extractedData) {
        try {
            String domain = (String) extractedData.get(WebsiteCrawlerConstants.NORMALIZED_DOMAIN);
            Set<String> allLinks = (Set<String>) extractedData.get(WebsiteCrawlerConstants.ALL_LINKS);
            Set<String> CareerPage = allLinks.parallelStream().filter(link -> {
                String careerPattern = "(career|job)(?!.*(google|twitter|facebook|instagram))";
                Pattern careerRegex = Pattern.compile(careerPattern);
                Matcher careerMatcher = careerRegex.matcher(link);
                String domainPattern = Pattern.quote(domain);
                Pattern domainRegex = Pattern.compile(domainPattern);
                Matcher domainMatcher = domainRegex.matcher(link);
                return careerMatcher.find() && domainMatcher.find();
            }).map(link -> getNormalizedLink(link, domain)).collect(Collectors.toSet());
            if (!CareerPage.isEmpty()) {
                String job_urls = String.join("|", CareerPage);
                extractedData.put(WebsiteCrawlerConstants.JOb_PRESENT, "true");
                extractedData.put(WebsiteCrawlerConstants.JOB_LINKS, job_urls);
            } else {
                extractedData.put(WebsiteCrawlerConstants.JOb_PRESENT, "false");
            }
        } catch (Exception e) {

        }
        return extractedData;
    }

    private String getNormalizedLink(String link, String domain) {
        if (!link.contains("http")) {
            link = "https://www." + domain + link;
            return link;
        }
        return link;
    }
}
