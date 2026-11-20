package com.infynd.DatavesterExtractor.dto;

import com.infynd.DatavesterExtractor.entities.FullcrawlingEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * Extracted result sent from Workers back to Master via RabbitMQ (crawler.results.queue).
 * Contains all business attributes extracted from the domain, with rawHtml excluded.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CrawlResultDto implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String input;
    private String domain;
    private String status;
    private String timeStamp;
    private String title;
    private String responseCode;
    private String url;
    private String webServer;
    private String normalizedDomain;
    private String hostIp;
    private String resolver;
    private String domainStatus;
    private String comments;
    private String redirectionStatus;
    private long loadTimeMs;
    private String ISO_Flag;
    private String Certification_Keyword;

    // Company Names
    private String tradingCompanyName;
    private String regCompanyName;
    private String metaCompanyName;
    private String copyRightCompanyName;
    private String copyRightCompanyName2;
    private String titleCompanyName;
    private String clearBitCompanyName;
    private String DescriptionCompanyName;
    private String Cleaned_copyright_name1;
    private String Cleaned_copyright_name2;
    private String cleaned_name_from_title;

    // Business Registration Numbers
    private String description;
    private String companyNumber;
    private String charityNumber;
    private String vatNumber;

    // Contact Information
    private String emails;
    private String genericEmails;
    private String nonGenericEmails;
    private String phoneNumber;
    private String phoneNumberFormatted;
    private String faxNumber;
    private String faxNumberFormatted;

    // Flags & Links
    private String neaPresent;
    private String neaLinks;
    private String branchPresent;
    private String aboutLinks;
    private String contactLInks;
    private String jobPresent;
    private String jobLinks;
    private String googleLinks;
    private String linkedinLinks;
    private String facebookLinks;
    private String twitterLinks;
    private String instagramLinks;
    private String youtubeLinks;
    private String privacyLinks;
    private String termsLinks;
    private String vimeoLinks;
    private String pinterestLinks;
    private String slideShareLinks;

    // Domain Matched Links
    private String domainMacthedLinkedinLinks;
    private String domainMatchedFacebookLinks;
    private String domainMatchedTwitterLinks;
    private String domainMatchedInstagramLinks;
    private String domainMatchedYoutubeLinks;
    private String domainMatchedPrivacyLinks;
    private String domainMatchedTermsLinks;
    private String domainMatchedVimeoLinks;
    private String domainMatchedPinterestLinks;
    private String domainMatchedSlideShareLinks;
    private String googlePlusLinks;
    private String domainMatchedGooglePlusLinks;

    // Address & Geographic Details
    private String addressPresent;
    private String IpCountry;
    private String crawledDate;
    private String cache;
    private String ContactAllLinks;
    private String AboutAllLink;
    private String PrivacyAllLinks;
    private String TermsAllLinks;
    private String HomeAllLinks;
    private String addressLine1;
    private String addressLine2;
    private String addressLine3;
    private String city;
    private String county;
    private String zipcode;
    private String isHeadQuarters;
    private String addressRelatedPhone;
    private String addressRelatedFax;
    private String addressRelatedEmail;
    private String addressPattern;
    private String splittedAddress;

    /**
     * Factory method to copy extracted fields from FullcrawlingEntity into CrawlResultDto,
     * ensuring rawHtml is never carried over.
     */
    public static CrawlResultDto fromEntity(FullcrawlingEntity entity) {
        if (entity == null) {
            return null;
        }

        return CrawlResultDto.builder()
                .id(entity.getId())
                .input(entity.getInput())
                .domain(entity.getDomain())
                .status(entity.getStatus())
                .timeStamp(entity.getTimeStamp())
                .title(entity.getTitle())
                .responseCode(entity.getResponseCode())
                .url(entity.getUrl())
                .webServer(entity.getWebServer())
                .normalizedDomain(entity.getNormalizedDomain())
                .hostIp(entity.getHostIp())
                .resolver(entity.getResolver())
                .domainStatus(entity.getDomainStatus())
                .comments(entity.getComments())
                .redirectionStatus(entity.getRedirectionStatus())
                .loadTimeMs(entity.getLoadTimeMs())
                .ISO_Flag(entity.getISO_Flag())
                .Certification_Keyword(entity.getCertification_Keyword())
                .tradingCompanyName(entity.getTradingCompanyName())
                .regCompanyName(entity.getRegCompanyName())
                .metaCompanyName(entity.getMetaCompanyName())
                .copyRightCompanyName(entity.getCopyRightCompanyName())
                .copyRightCompanyName2(entity.getCopyRightCompanyName2())
                .titleCompanyName(entity.getTitleCompanyName())
                .clearBitCompanyName(entity.getClearBitCompanyName())
                .DescriptionCompanyName(entity.getDescriptionCompanyName())
                .Cleaned_copyright_name1(entity.getCleaned_copyright_name1())
                .Cleaned_copyright_name2(entity.getCleaned_copyright_name2())
                .cleaned_name_from_title(entity.getCleaned_name_from_title())
                .description(entity.getDescription())
                .companyNumber(entity.getCompanyNumber())
                .charityNumber(entity.getCharityNumber())
                .vatNumber(entity.getVatNumber())
                .emails(entity.getEmails())
                .genericEmails(entity.getGenericEmails())
                .nonGenericEmails(entity.getNonGenericEmails())
                .phoneNumber(entity.getPhoneNumber())
                .phoneNumberFormatted(entity.getPhoneNumberFormatted())
                .faxNumber(entity.getFaxNumber())
                .faxNumberFormatted(entity.getFaxNumberFormatted())
                .neaPresent(entity.getNeaPresent())
                .neaLinks(entity.getNeaLinks())
                .branchPresent(entity.getBranchPresent())
                .aboutLinks(entity.getAboutLinks())
                .contactLInks(entity.getContactLInks())
                .jobPresent(entity.getJobPresent())
                .jobLinks(entity.getJobLinks())
                .googleLinks(entity.getGoogleLinks())
                .linkedinLinks(entity.getLinkedinLinks())
                .facebookLinks(entity.getFacebookLinks())
                .twitterLinks(entity.getTwitterLinks())
                .instagramLinks(entity.getInstagramLinks())
                .youtubeLinks(entity.getYoutubeLinks())
                .privacyLinks(entity.getPrivacyLinks())
                .termsLinks(entity.getTermsLinks())
                .vimeoLinks(entity.getVimeoLinks())
                .pinterestLinks(entity.getPinterestLinks())
                .slideShareLinks(entity.getSlideShareLinks())
                .domainMacthedLinkedinLinks(entity.getDomainMacthedLinkedinLinks())
                .domainMatchedFacebookLinks(entity.getDomainMatchedFacebookLinks())
                .domainMatchedTwitterLinks(entity.getDomainMatchedTwitterLinks())
                .domainMatchedInstagramLinks(entity.getDomainMatchedInstagramLinks())
                .domainMatchedYoutubeLinks(entity.getDomainMatchedYoutubeLinks())
                .domainMatchedPrivacyLinks(entity.getDomainMatchedPrivacyLinks())
                .domainMatchedTermsLinks(entity.getDomainMatchedTermsLinks())
                .domainMatchedVimeoLinks(entity.getDomainMatchedVimeoLinks())
                .domainMatchedPinterestLinks(entity.getDomainMatchedPinterestLinks())
                .domainMatchedSlideShareLinks(entity.getDomainMatchedSlideShareLinks())
                .googlePlusLinks(entity.getGooglePlusLinks())
                .domainMatchedGooglePlusLinks(entity.getDomainMatchedGooglePlusLinks())
                .addressPresent(entity.getAddressPresent())
                .IpCountry(entity.getIpCountry())
                .crawledDate(entity.getCrawledDate())
                .cache(entity.getCache())
                .ContactAllLinks(entity.getContactAllLinks())
                .AboutAllLink(entity.getAboutAllLink())
                .PrivacyAllLinks(entity.getPrivacyAllLinks())
                .TermsAllLinks(entity.getTermsAllLinks())
                .HomeAllLinks(entity.getHomeAllLinks())
                .addressLine1(entity.getAddressLine1())
                .addressLine2(entity.getAddressLine2())
                .addressLine3(entity.getAddressLine3())
                .city(entity.getCity())
                .county(entity.getCounty())
                .zipcode(entity.getZipcode())
                .isHeadQuarters(entity.getIsHeadQuarters())
                .addressRelatedPhone(entity.getAddressRelatedPhone())
                .addressRelatedFax(entity.getAddressRelatedFax())
                .addressRelatedEmail(entity.getAddressRelatedEmail())
                .addressPattern(entity.getAddressPattern())
                .splittedAddress(entity.getSplittedAddress())
                .build();
    }
}
