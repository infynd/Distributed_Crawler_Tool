package com.infynd.DatavesterExtractor.SingleExtractor.Fullcrawler_lite;

import com.infynd.DatavesterExtractor.SingleExtractor.Fullcrawler_lite.LinkExtraction.GoogleLinkExtractor;
import com.infynd.DatavesterExtractor.SingleExtractor.Fullcrawler_lite.LinkExtraction.LinkExtraction;
import com.infynd.DatavesterExtractor.SingleExtractor.Fullcrawler_lite.SourceAppender.PageAppenderExtraction;
import com.infynd.DatavesterExtractor.SingleExtractor.Fullcrawler_lite.TechStackExtraction.JobsExtraction;
import com.infynd.DatavesterExtractor.SingleExtractor.Fullcrawler_lite.companynumber.ClearbitProcessorNameExtractor;
import com.infynd.DatavesterExtractor.SingleExtractor.Fullcrawler_lite.companynumber.CompanyNameExtraction;
import com.infynd.DatavesterExtractor.SingleExtractor.Fullcrawler_lite.companynumber.CompanyNumberExtraction;
import com.infynd.DatavesterExtractor.SingleExtractor.Fullcrawler_lite.email.EmailExtraction;
import com.infynd.DatavesterExtractor.SingleExtractor.Fullcrawler_lite.fax.FaxExtractor;
import com.infynd.DatavesterExtractor.SingleExtractor.Fullcrawler_lite.meta.MetaExtractor;
import com.infynd.DatavesterExtractor.SingleExtractor.Fullcrawler_lite.name.CopyrightNameExtraction;
import com.infynd.DatavesterExtractor.SingleExtractor.Fullcrawler_lite.name.NameFromDescriptionExtractor;
import com.infynd.DatavesterExtractor.SingleExtractor.Fullcrawler_lite.name.NameFromTitleExtractor;
import com.infynd.DatavesterExtractor.SingleExtractor.Fullcrawler_lite.phone.PhoneExtraction;
import com.infynd.DatavesterExtractor.SingleExtractor.Fullcrawler_lite.utils.WebsiteCrawlerConstants;
import com.infynd.DatavesterExtractor.entities.FullcrawlingEntity;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.io.Serializable;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@Slf4j
public class PageDetails {
    @Autowired
    Pinger pinger;

    @Autowired
    LinkExtraction linkExtractor;
    @Autowired
    GoogleLinkExtractor googleLinkExtractor;

    @Autowired
    MetaExtractor meta;

    @Autowired
    CopyrightNameExtraction copyrightNameExtractor;

    @Autowired
    NameFromTitleExtractor nameFromTitle;

    @Autowired
    NameFromDescriptionExtractor nameFromDescription;

    @Autowired
    ClearbitProcessorNameExtractor clearbitProcessorName;
    @Autowired
    PageAppenderExtraction pageAppender;

    @Autowired
    CompanyNumberExtraction companyNumberExtractor;

    @Autowired
    CompanyNameExtraction companyNameExtractor;

    @Autowired
    EmailExtraction emailExtractor;
    @Autowired
    FaxExtractor faxExtraction;

    @Autowired
    PhoneExtraction phoneExtractor;
    @Autowired
    JobsExtraction jobsExtractor;



    public FullcrawlingEntity extractCompanyInfo(FullcrawlingEntity fullcrawlingEntity) {

        log.info("Processing domain: {}", fullcrawlingEntity.getDomain());
        String domain = fullcrawlingEntity.getNormalizedDomain();
        Map<String, Serializable> extractedData = new HashMap<>();
        String hostedIp = fullcrawlingEntity.getHostIp();

        if (hostedIp != null) {

            extractedData.put(WebsiteCrawlerConstants.RAW_HTML, fullcrawlingEntity.getRawHtml());
            extractedData.put(WebsiteCrawlerConstants.NORMALIZED_DOMAIN, fullcrawlingEntity.getNormalizedDomain());
            extractedData.put(WebsiteCrawlerConstants.PROCESSING_ID, fullcrawlingEntity.getDomain());
            extractedData.put(WebsiteCrawlerConstants.PARENT_URL, fullcrawlingEntity.getUrl());
            extractedData.put(WebsiteCrawlerConstants.TITLE, fullcrawlingEntity.getTitle());
            extractedData.put(WebsiteCrawlerConstants.DOMAIN_STATUS, fullcrawlingEntity.getResolver());
            extractedData.put(WebsiteCrawlerConstants.HOST_IP, fullcrawlingEntity.getHostIp());
            extractedData.put(WebsiteCrawlerConstants.WEB_SERVER, fullcrawlingEntity.getWebServer());
            String title = (String) extractedData.get(WebsiteCrawlerConstants.TITLE);
            extractedData.put(WebsiteCrawlerConstants.TIMESTAMP, fullcrawlingEntity.getTimeStamp());
            extractedData.put(WebsiteCrawlerConstants.RESPONSE_CODE, fullcrawlingEntity.getResponseCode());
            String rawHtml = (String) extractedData.get(WebsiteCrawlerConstants.RAW_HTML);
            String domain2 = extractedData.containsKey(WebsiteCrawlerConstants.PROCESSING_ID) ? extractedData.get(WebsiteCrawlerConstants.PROCESSING_ID).toString().trim() : null;
            String redirection = extractedData.containsKey(WebsiteCrawlerConstants.NORMALIZED_DOMAIN) ? extractedData.get(WebsiteCrawlerConstants.NORMALIZED_DOMAIN).toString().trim() : null;
            assert domain2 != null;
            if (domain2.equalsIgnoreCase(redirection)) {
                extractedData.put(WebsiteCrawlerConstants.REDIRECTION, "false");
            } else {
                extractedData.put(WebsiteCrawlerConstants.REDIRECTION, "true");
            }
            try {
                if (StringUtils.isNotEmpty(fullcrawlingEntity.getRawHtml())) {
                    Document doc = Jsoup.parse(rawHtml);
                    String bodytext = "";
                    if (doc.body() != null) {
                        bodytext = doc.body().text().trim();
                    }
                    extractedData = pinger.execute(extractedData);
                    Map<String, Boolean> pingerInfo = (Map<String, Boolean>) extractedData.get(WebsiteCrawlerConstants.PINGER_INFO);
                    boolean isDomainForSale = pingerInfo.get(WebsiteCrawlerConstants.IS_DOMAIN_FOR_SALE);
                    boolean isInvalidDomain = pingerInfo.get(WebsiteCrawlerConstants.IS_INVALID_DOMAIN);
                    boolean isSecuredDomain = pingerInfo.get(WebsiteCrawlerConstants.IS_SECURED_DOMAIN);
                    boolean isprotected = pingerInfo.get(WebsiteCrawlerConstants.IS_PROTECTED_DOMAIN);
                    String response = (String) extractedData.get(WebsiteCrawlerConstants.RESPONSE_CODE);
                    finalizeDomainComments(extractedData, rawHtml, title, isDomainForSale, isInvalidDomain, isSecuredDomain, isprotected, response, bodytext, domain, doc);
                    if ("valid".equals(extractedData.get(WebsiteCrawlerConstants.COMMENTS)) || "secured_domain".equals(extractedData.get(WebsiteCrawlerConstants.COMMENTS)) || "protected".equals(extractedData.get(WebsiteCrawlerConstants.COMMENTS))) {
                        extractedData = processDomain(extractedData);
                    }
                } else {
                    //websiteCrawlerEntity.setPingerValid(true);
                    extractedData.put(WebsiteCrawlerConstants.COMMENTS, "protected");
                }

                Instant instant = Instant.now();
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                String formattedInstant = formatter.format(instant.atZone(ZoneId.of("UTC")));
                extractedData.put(WebsiteCrawlerConstants.TIMESTAMP, formattedInstant);
                log.info("Processed domain - {}", domain);
            } catch (Exception e) {
                log.error(String.format("Exception in processing entry %s", fullcrawlingEntity.getDomain()), e);
                Instant instant = Instant.now();
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                String formattedInstant = formatter.format(instant.atZone(ZoneId.of("UTC")));
                extractedData.put(WebsiteCrawlerConstants.TIMESTAMP, formattedInstant);
            }
        } else {
            extractedData.put(WebsiteCrawlerConstants.COMMENTS, "site cannot be reached");
            extractedData.put(WebsiteCrawlerConstants.DOMAIN_STATUS, "invalid");
            Instant instant = Instant.now();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            String formattedInstant = formatter.format(instant.atZone(ZoneId.of("UTC")));
            extractedData.put(WebsiteCrawlerConstants.TIMESTAMP, formattedInstant);
        }
        extractedData.remove(WebsiteCrawlerConstants.RAW_HTML);


        fullcrawlingEntity = dataAppend(extractedData, fullcrawlingEntity);


        return fullcrawlingEntity;


    }



    private FullcrawlingEntity dataAppend(Map<String, Serializable> extractedData, FullcrawlingEntity fullcrawlingEntity) {
        fullcrawlingEntity.setDomainStatus(Optional.ofNullable((String) extractedData.get(WebsiteCrawlerConstants.DOMAIN_STATUS))
                .orElse(null));
        Object isoObj = extractedData.get(WebsiteCrawlerConstants.ISO_Present);
        fullcrawlingEntity.setISO_Flag(isoObj == null ? null : String.valueOf(isoObj));
        fullcrawlingEntity.setCertification_Keyword(Optional.ofNullable((String) extractedData.get(WebsiteCrawlerConstants.CERTIFICATION_MATCHED_KEYWORDS))
                .orElse(null));
        fullcrawlingEntity.setComments(Optional.ofNullable((String) extractedData.get(WebsiteCrawlerConstants.COMMENTS))
                .orElse(null));
        fullcrawlingEntity.setRedirectionStatus(Optional.ofNullable((String) extractedData.get(WebsiteCrawlerConstants.REDIRECTION))
                .orElse(null));
        fullcrawlingEntity.setDescription(Optional.ofNullable((String) extractedData.get(WebsiteCrawlerConstants.DESCRIPTION))
                .orElse(null));
        fullcrawlingEntity.setTradingCompanyName(Optional.ofNullable((String) extractedData.get(WebsiteCrawlerConstants.TRADING_COMPANY_NAME))
                .orElse(null));
        fullcrawlingEntity.setRegCompanyName(Optional.ofNullable((String) extractedData.get(WebsiteCrawlerConstants.REG_COMPANY_NAME))
                .orElse(null));

        fullcrawlingEntity.setCleaned_copyright_name1(Optional.ofNullable((String) extractedData.get(WebsiteCrawlerConstants.COPYRIGHT_NAME1_CLEAN))
                .orElse(null));
        fullcrawlingEntity.setCleaned_copyright_name2(Optional.ofNullable((String) extractedData.get(WebsiteCrawlerConstants.COPYRIGHT_NAME2_CLEAN))
                .orElse(null));
        fullcrawlingEntity.setCleaned_name_from_title(Optional.ofNullable((String) extractedData.get(WebsiteCrawlerConstants.Clean_name_FROM_TITLE))
                .orElse(null));


        //meta name
        fullcrawlingEntity.setMetaCompanyName(Optional.ofNullable((String) extractedData.get(WebsiteCrawlerConstants.NAME_FROM_META))
                .orElse(null));
        fullcrawlingEntity.setCopyRightCompanyName(Optional.ofNullable((String) extractedData.get(WebsiteCrawlerConstants.COPYRIGHT_NAME))
                .orElse(null));
        fullcrawlingEntity.setCopyRightCompanyName2(Optional.ofNullable((String) extractedData.get(WebsiteCrawlerConstants.COPYRIGHT_NAME2))
                .orElse(null));
        fullcrawlingEntity.setTitleCompanyName(Optional.ofNullable((String) extractedData.get(WebsiteCrawlerConstants.NAME_FROM_TITLE))
                .orElse(null));
        fullcrawlingEntity.setDescriptionCompanyName(Optional.ofNullable((String) extractedData.get(WebsiteCrawlerConstants.NAME_FROM_DESC))
                .orElse(null));
        fullcrawlingEntity.setClearBitCompanyName(Optional.ofNullable((String) extractedData.get(WebsiteCrawlerConstants.CLEARBIT_NAME))
                .orElse(null));
        fullcrawlingEntity.setCompanyNumber(Optional.ofNullable((String) extractedData.get(WebsiteCrawlerConstants.COMPANY_NO))
                .orElse(null));
        fullcrawlingEntity.setCharityNumber(Optional.ofNullable((String) extractedData.get(WebsiteCrawlerConstants.CHARITY_NO))
                .orElse(null));
        fullcrawlingEntity.setVatNumber(Optional.ofNullable((String) extractedData.get(WebsiteCrawlerConstants.VAT_NO))
                .orElse(null));
        fullcrawlingEntity.setPhoneNumber(Optional.ofNullable((String) extractedData.get(WebsiteCrawlerConstants.PHONE))
                .orElse(null));
        fullcrawlingEntity.setPhoneNumberFormatted(Optional.ofNullable((String) extractedData.get(WebsiteCrawlerConstants.PHONE_FORMATTED))
                .orElse(null));
        fullcrawlingEntity.setFaxNumber(Optional.ofNullable((String) extractedData.get(WebsiteCrawlerConstants.FAX))
                .orElse(null));
        fullcrawlingEntity.setEmails(Optional.ofNullable((String) extractedData.get(WebsiteCrawlerConstants.EMAIL))
                .orElse(null));
        fullcrawlingEntity.setGenericEmails(Optional.ofNullable((String) extractedData.get(WebsiteCrawlerConstants.GENERIC_EMAIL))
                .orElse(null));
        fullcrawlingEntity.setNonGenericEmails(Optional.ofNullable((String) extractedData.get(WebsiteCrawlerConstants.NON_GENERIC_EMAIL))
                .orElse(null));
        fullcrawlingEntity.setBranchPresent(Optional.ofNullable((String) extractedData.get(WebsiteCrawlerConstants.BRANCH_PRESENT))
                .orElse(null));
        fullcrawlingEntity.setNeaPresent(Optional.ofNullable((String) extractedData.get(WebsiteCrawlerConstants.NEA_PRESENT))
                .orElse(null));
        fullcrawlingEntity.setNeaLinks(Optional.ofNullable((String) extractedData.get(WebsiteCrawlerConstants.NEA_LINKS))
                .orElse(null));
        fullcrawlingEntity.setJobPresent(Optional.ofNullable((String) extractedData.get(WebsiteCrawlerConstants.JOb_PRESENT))
                .orElse(null));
        fullcrawlingEntity.setJobLinks(Optional.ofNullable((String) extractedData.get(WebsiteCrawlerConstants.JOB_LINKS))
                .orElse(null));
        fullcrawlingEntity.setContactLInks(Optional.ofNullable((String) extractedData.get(WebsiteCrawlerConstants.CONTACT_LINKS))
                .orElse(null));
        fullcrawlingEntity.setAboutLinks(Optional.ofNullable((String) extractedData.get(WebsiteCrawlerConstants.ABOUT_LINKS))
                .orElse(null));
        fullcrawlingEntity.setAddressPresent(Optional.ofNullable((String) extractedData.get(WebsiteCrawlerConstants.ADDRESS_PRESENT))
                .orElse(null));

        fullcrawlingEntity.setContactAllLinks(
                toPipeSeparatedString(
                        extractedData.get(WebsiteCrawlerConstants.CONTACT_LINKS_ALL)
                )
        );

        fullcrawlingEntity.setAboutAllLink(
                toPipeSeparatedString(
                        extractedData.get(WebsiteCrawlerConstants.ABOUT_LINKS_ALL)
                )
        );

        fullcrawlingEntity.setPrivacyAllLinks(
                toPipeSeparatedString(
                        extractedData.get(WebsiteCrawlerConstants.PRIVACY_LINKS_ALL)
                )
        );

        fullcrawlingEntity.setTermsAllLinks(
                toPipeSeparatedString(
                        extractedData.get(WebsiteCrawlerConstants.TERMS_LINKS_ALL)
                )
        );

        fullcrawlingEntity.setHomeAllLinks(
                toPipeSeparatedString(
                        extractedData.get(WebsiteCrawlerConstants.Home_links)
                )
        );






        Map<String, String> socialLinks = (Map<String, String>) extractedData.get(WebsiteCrawlerConstants.SOCIAL_LINKS);
        Map<String, String> domainMatchedSocialLinks = (Map<String, String>) extractedData.get(WebsiteCrawlerConstants.DOMAIN_MATCHED_SOCIAL_LINKS);
        Map<String, String> otherLinks = (Map<String, String>) extractedData.get(WebsiteCrawlerConstants.OTHER_LINKS);
        // Set LinkedIn link, using Optional to handle null values safely
        fullcrawlingEntity.setLinkedinLinks(
                Optional.ofNullable(socialLinks)
                        .map(links -> links.get("linkedin"))
                        .orElse(null)
        );
        fullcrawlingEntity.setDomainMacthedLinkedinLinks(
                Optional.ofNullable(domainMatchedSocialLinks)
                        .map(links -> links.get("linkedin"))
                        .orElse(null)
        );
        fullcrawlingEntity.setFacebookLinks(Optional.ofNullable(socialLinks)
                .map(links -> links.get("facebook"))
                .orElse(null));
        fullcrawlingEntity.setDomainMatchedFacebookLinks(Optional.ofNullable(domainMatchedSocialLinks)
                .map(links -> links.get("facebook"))
                .orElse(null));
        fullcrawlingEntity.setTwitterLinks(Optional.ofNullable(socialLinks)
                .map(links -> links.get("twitter"))
                .orElse(null));
        fullcrawlingEntity.setDomainMatchedTwitterLinks(Optional.ofNullable(domainMatchedSocialLinks)
                .map(links -> links.get("twitter"))
                .orElse(null));
        fullcrawlingEntity.setInstagramLinks(Optional.ofNullable(socialLinks)
                .map(links -> links.get("instagram"))
                .orElse(null));
        fullcrawlingEntity.setDomainMatchedInstagramLinks(Optional.ofNullable(domainMatchedSocialLinks)
                .map(links -> links.get("instagram"))
                .orElse(null));
        fullcrawlingEntity.setPrivacyLinks(Optional.ofNullable(socialLinks)
                .map(links -> links.get("privacy"))
                .orElse(null));
        fullcrawlingEntity.setDomainMatchedPrivacyLinks(Optional.ofNullable(domainMatchedSocialLinks)
                .map(links -> links.get("privacy"))
                .orElse(null));
        fullcrawlingEntity.setTermsLinks(Optional.ofNullable(socialLinks)
                .map(links -> links.get("terms"))
                .orElse(null));
        fullcrawlingEntity.setDomainMatchedTermsLinks(Optional.ofNullable(domainMatchedSocialLinks)
                .map(links -> links.get("terms"))
                .orElse(null));
        fullcrawlingEntity.setSlideShareLinks(Optional.ofNullable(socialLinks)
                .map(links -> links.get("slideshare"))
                .orElse(null));
        fullcrawlingEntity.setDomainMatchedSlideShareLinks(Optional.ofNullable(domainMatchedSocialLinks)
                .map(links -> links.get("slideshare"))
                .orElse(null));
        fullcrawlingEntity.setVimeoLinks(Optional.ofNullable(socialLinks)
                .map(links -> links.get("vimeo"))
                .orElse(null));
        fullcrawlingEntity.setDomainMatchedVimeoLinks(Optional.ofNullable(domainMatchedSocialLinks)
                .map(links -> links.get("vimeo"))
                .orElse(null));
        fullcrawlingEntity.setGooglePlusLinks(Optional.ofNullable(socialLinks)
                .map(links -> links.get("google_plus"))
                .orElse(null));
        fullcrawlingEntity.setDomainMatchedGooglePlusLinks(Optional.ofNullable(domainMatchedSocialLinks)
                .map(links -> links.get("google_plus"))
                .orElse(null));
        fullcrawlingEntity.setPinterestLinks(Optional.ofNullable(socialLinks)
                .map(links -> links.get("pinterest"))
                .orElse(null));
        fullcrawlingEntity.setDomainMatchedPinterestLinks(Optional.ofNullable(domainMatchedSocialLinks)
                .map(links -> links.get("pinterest"))
                .orElse(null));
        return fullcrawlingEntity;
    }

    private Map<String, Serializable> processDomain(Map<String, Serializable> extractedData) throws Exception {

        extractedData = linkExtractor.extract(extractedData);
        extractedData = meta.extract(extractedData);
        extractedData = copyrightNameExtractor.extract(extractedData);
        extractedData = nameFromTitle.extract(extractedData);
        extractedData = nameFromDescription.extract(extractedData);
        extractedData = clearbitProcessorName.extract(extractedData);
        extractedData = pageAppender.extract(extractedData);
        extractedData = linkExtractor.extract(extractedData);
        extractedData = companyNumberExtractor.extract(extractedData);
        extractedData = emailExtractor.extract(extractedData);
        extractedData = faxExtraction.extract(extractedData);
        extractedData = phoneExtractor.extract(extractedData);
        extractedData = jobsExtractor.extract(extractedData);
        extractedData = companyNameExtractor.extract(extractedData);
        extractedData = googleLinkExtractor.extract(extractedData);

        return extractedData;
    }

    private void finalizeDomainComments(Map<String, Serializable> extractedData, String rawHtml, String title, boolean isDomainForSale, boolean isInvalidDomain, boolean isSecuredDomain, boolean isprotected, String response, String bodytext, String domain, Document doc) {
        String lowerCaseHtml = rawHtml.toLowerCase();
        String domainStatus = (String) extractedData.get(WebsiteCrawlerConstants.DOMAIN_STATUS);
        Elements frameElements = doc.select("frame");
        String frameElements1 = String.valueOf(doc.select("frame"));
        if ((!isDomainForSale && !isprotected && !isInvalidDomain && !domain.contains("linkedin.com") && !domain.contains("facebook.com") && !domain.contains("instagram.com") && !domain.contains("twitter.com") && !domain.matches("\\d+"))) {
            extractedData.put(WebsiteCrawlerConstants.COMMENTS, "valid");
        }
        if (frameElements != null && !frameElements1.equals("")) {
            extractedData.put(WebsiteCrawlerConstants.COMMENTS, "iframe");
        }
        if (rawHtml.contains("Diese Domain ist bereits registriert")) {
            extractedData.put(WebsiteCrawlerConstants.COMMENTS, "domain register");
        }
        if (rawHtml.contains("Dynadot.com") && rawHtml.contains("coming soon")) {
            extractedData.put(WebsiteCrawlerConstants.COMMENTS, "coming soon");
        }
        if (rawHtml.contains("https://websites.godaddy.com/categories/v4/videos/raw/video/uA41GmyyG8IMaxXdb") || rawHtml.contains("https://www.godaddy.com/websites/website-builder?isc=pwugc&amp;utm_source=wsb&amp;utm_medium=applications&amp;utm_campaign=en-gb_corp_applications_base")) {
            extractedData.put(WebsiteCrawlerConstants.COMMENTS, "coming soon");
        }
        if (rawHtml.contains("The domain name you are looking")) {
            extractedData.put(WebsiteCrawlerConstants.COMMENTS, "sale");
        }
        if (rawHtml.contains("This domain may be for sale") || rawHtml.contains("Buy this domain") || rawHtml.contains("is listed for sale") || rawHtml.contains("429: Too Many Requests") || rawHtml.contains("This domain is already taken") || rawHtml.contains("domainpark/domainpark.cgi") || rawHtml.contains("https://www.trustpilot.com/review/saw.com") || rawHtml.contains("This domain is registered")) {
            extractedData.put(WebsiteCrawlerConstants.COMMENTS, "sale");
        }
        if (title.contains("Not Found") || rawHtml.contains("Under Construction")) {
            extractedData.put(WebsiteCrawlerConstants.COMMENTS, "not found/invalid");
        }
        if (response.equals("500") && bodytext.equals("")) {
            extractedData.put(WebsiteCrawlerConstants.COMMENTS, "not found/invalid");
        }
        if (rawHtml.contains("https://www.names.co.uk/parked-domains/index")) {
            extractedData.put(WebsiteCrawlerConstants.COMMENTS, "sale");
        }
        if (rawHtml.contains("Domains on Sav come with free") || rawHtml.contains("Build your website for free on")) {
            extractedData.put(WebsiteCrawlerConstants.COMMENTS, "sale");
        }
        if (response.equals("500") && (bodytext.toLowerCase().contains("") || bodytext.toLowerCase().startsWith("error"))) {
            extractedData.put(WebsiteCrawlerConstants.COMMENTS, "not found/invalid");
        }
        if (rawHtml.contains("https://cdn.webber.forsale/favicon.ico")) {
            extractedData.put(WebsiteCrawlerConstants.COMMENTS, "sale");
        }
        if ((isInvalidDomain)) {
            extractedData.put(WebsiteCrawlerConstants.COMMENTS, "not found/invalid");
        }
        if (rawHtml.contains("This domain was recently")) {
            extractedData.put(WebsiteCrawlerConstants.COMMENTS, "sale");
        }
        if (rawHtml.contains("name=\"robots\"") && rawHtml.contains("name=\"GOOGLEBOT\"")) {
            extractedData.put(WebsiteCrawlerConstants.COMMENTS, "sale");
        }
        if (rawHtml.contains("Access Denied")) {
            extractedData.put(WebsiteCrawlerConstants.COMMENTS, "Access Denied");
        }
        if (rawHtml.contains("domains") && rawHtml.contains("for sale")) {
            extractedData.put(WebsiteCrawlerConstants.COMMENTS, "sale");
        }
        if (rawHtml.contains("https://cdn.hostinger.com/hostinger_welcome/images/hostinger-logo")) {
            extractedData.put(WebsiteCrawlerConstants.COMMENTS, "domain register");
        }
        if ((isDomainForSale && isInvalidDomain)) {
            extractedData.put(WebsiteCrawlerConstants.COMMENTS, "not found/invalid");
        }
        String lowerscasetitile = title.toLowerCase();
        String[] keywords = {"website expired", "site offline", "website Offline", "Connection timed", "Origin is unreachable", "Invalid SSL", "timeout occurred", "Web server is down"};
        for (String keyword : keywords) {
            if (lowerscasetitile.contains(keyword) && domainStatus.equals("valid")) {
                extractedData.put(WebsiteCrawlerConstants.COMMENTS, "temp_closed");
            }
        }
        if (isSecuredDomain) {
            extractedData.put(WebsiteCrawlerConstants.COMMENTS, "secured_domain");
        }
        if (rawHtml.contains("DOMAIN REGISTERED")) {
            extractedData.put(WebsiteCrawlerConstants.COMMENTS, "coming soon");
        }

        Element subtitle = doc.select(".subtitle").first();
        if (subtitle != null && subtitle.text().contains("Coming soon")) {
            extractedData.put(WebsiteCrawlerConstants.COMMENTS, "coming soon");
        }
        if (lowerCaseHtml.contains("website coming soon")) {
            extractedData.put(WebsiteCrawlerConstants.COMMENTS, "coming soon");
        }
        if (lowerCaseHtml.contains("website launches soon")) {
            extractedData.put(WebsiteCrawlerConstants.COMMENTS, "coming soon");
        }

        if (rawHtml.contains("Sorry, you have been blocked")) {
            extractedData.put(WebsiteCrawlerConstants.COMMENTS, "blocked");
        }
        if (title.equalsIgnoreCase("Parked")) {
            extractedData.put(WebsiteCrawlerConstants.COMMENTS, "sale");
        }
        if (title.contains("This store is unavailable")) {
            extractedData.put(WebsiteCrawlerConstants.COMMENTS, "unavailable");
        }
        if (rawHtml.contains("Enter using password") && rawHtml.contains("soon") && rawHtml.contains("shopify")) {
            extractedData.put(WebsiteCrawlerConstants.COMMENTS, "coming soon");
        }
        if (isprotected || title.contains("just a moment...") || title.equalsIgnoreCase("just a moment...")) {
            extractedData.put(WebsiteCrawlerConstants.COMMENTS, "protected");
        }
        if (title.contains(" soon ")) {
            extractedData.put(WebsiteCrawlerConstants.COMMENTS, "coming soon");
        }
        if (title.contains("Coming Soon")) {
            extractedData.put(WebsiteCrawlerConstants.COMMENTS, "coming soon");
        }
        if (rawHtml.contains("Website Under Maintenance") || rawHtml.contains("We will be back soon")) {
            extractedData.put(WebsiteCrawlerConstants.COMMENTS, "Maintenance");
        }
        if (rawHtml.contains("This domain has recently been registered with Namecheap")) {
            extractedData.put(WebsiteCrawlerConstants.COMMENTS, "Namecheap");
        }
        String searchPhrase = "is required by WordPress";
        Pattern pattern = Pattern.compile(searchPhrase, Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(rawHtml);
        if (matcher.find()) {
            extractedData.put(WebsiteCrawlerConstants.COMMENTS, "sale");
        }
        if ((isDomainForSale)) {
            extractedData.put(WebsiteCrawlerConstants.COMMENTS, "sale");
        }
        if (response.equals("202")) {
            extractedData.put(WebsiteCrawlerConstants.COMMENTS, "blocked");
        }
    }

    @SuppressWarnings("unchecked")
    private String toPipeSeparatedString(Object value) {

        if (value == null) return null;

        if (value instanceof String) {
            return ((String) value).trim().isEmpty() ? null : (String) value;
        }

        if (value instanceof Collection) {
            Collection<String> collection = (Collection<String>) value;
            if (collection.isEmpty()) return null;
            return String.join("|", collection);
        }

        return null;
    }

}
