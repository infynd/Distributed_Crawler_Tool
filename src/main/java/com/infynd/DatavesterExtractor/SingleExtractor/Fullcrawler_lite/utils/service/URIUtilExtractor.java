package com.infynd.DatavesterExtractor.SingleExtractor.Fullcrawler_lite.utils.service;

import com.google.common.net.InternetDomainName;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.net.URI;

@Service
@Slf4j
public class URIUtilExtractor {

    private static final String HTTP = "http://";
    private static final String HTTP1 = "http:";
    private static final String HTTPS = "https:";
    private static final String WWW_DOT = "www.";
    private static final String DOT = ".";

    public String getDomainNameFromWebsite(String website) {
        if (StringUtils.isBlank(website)) {
            return null;
        }
        website = website.trim();

        try {
            if (!website.startsWith(HTTP1) && !website.startsWith(HTTPS)) {
                website = HTTP + website;
            }
            URI uri = new URI(website);
            String hostName = uri.getHost();
            if (hostName == null) {
                return null;
            }
            if (hostName.startsWith(WWW_DOT)) {
                hostName = hostName.substring(4);
            } else if (hostName.indexOf(DOT) < 0) {
                return null;
            }
            return hostName;
        } catch (Exception ee) {
            //Ignore this exception - For Invalid domains, this is expected
        }
        return null;
    }

    public String getSubDomainStrippedDomainName(String website) {
        String domainName = null;
        try {
            domainName = getDomainNameFromWebsite(website);
            // strip subDomain
            if (!StringUtils.isEmpty(domainName)) {
                InternetDomainName idn = InternetDomainName.from(domainName);
                if (idn.hasPublicSuffix()) {
                    domainName = idn.topPrivateDomain().toString();
                }
            }

        } catch (Exception ex) {
            log.error(String.format("Error while getting the domain name for : %s", website), ex);
        }
        return domainName;

    }

    public static String getCleanDomain(String domain) {
        if (StringUtils.isNotBlank(domain)) {
            domain = domain.replaceAll("http://", "");
            domain = domain.replaceAll("https://", "");
            domain = domain.replaceAll("https:", "");
            domain = domain.replaceAll("http:", "");

            if (domain.startsWith("/")) {
                domain = domain.substring(1);
            }

            while (true) {
                if (domain.startsWith("www.")) {
                    int index = domain.indexOf("www.");
                    domain = domain.substring(index + 4);
                } else {
                    break;
                }
            }
            if (domain.contains("?")) {
                int index = domain.indexOf("?");
                domain = domain.substring(0, index);
            }
            if (domain.contains("/")) {
                int index = domain.indexOf("/");
                domain = domain.substring(0, index);
            }
            return domain;
        }
        return null;
    }
}
