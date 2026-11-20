package com.infynd.DatavesterExtractor.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@Table(name = "crawling_src_table")
public class FullcrawlingEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String input;
    private String domain;
    private String status;
    private String timeStamp;
    @Column(columnDefinition = "LONGTEXT")
    private String title;
    private String responseCode;
    @Column(columnDefinition = "LONGTEXT")
    private String url;
    private String webServer;
    @Column(columnDefinition = "TEXT")
    private String normalizedDomain;
    private String hostIp;
    private String resolver;
    private String domainStatus;
    private String comments;
    private String redirectionStatus;
    private long loadTimeMs;
    private String ISO_Flag;
    @Column(columnDefinition = "LONGTEXT")
    private String Certification_Keyword;
    private String rawHtml;
    @Column(columnDefinition = "TEXT")
    private String tradingCompanyName;
    @Column(columnDefinition = "TEXT")
    private String regCompanyName;
    @Column(columnDefinition = "TEXT")
    private String metaCompanyName;
    @Column(columnDefinition = "TEXT")
    private String copyRightCompanyName;
    @Column(columnDefinition = "TEXT")
    private String copyRightCompanyName2;
    @Column(columnDefinition = "TEXT")
    private String titleCompanyName;

    private String clearBitCompanyName;
    @Column(columnDefinition = "TEXT")
    private String DescriptionCompanyName;

    @Column(columnDefinition = "LONGTEXT")
    private String description;
    @Column(columnDefinition = "TEXT")
    private String companyNumber;
    @Column(columnDefinition = "TEXT")
    private String charityNumber;
    @Column(columnDefinition = "TEXT")
    private String vatNumber;

    @Column(columnDefinition = "LONGTEXT")
    private String emails;

    @Column(columnDefinition = "LONGTEXT")
    private String genericEmails;

    @Column(columnDefinition = "LONGTEXT")
    private String nonGenericEmails;

    @Column(columnDefinition = "LONGTEXT")
    private String phoneNumber;

    @Column(columnDefinition = "LONGTEXT")
    private String phoneNumberFormatted;
    @Column(columnDefinition = "LONGTEXT")
    private String faxNumber;
    @Column(columnDefinition = "LONGTEXT")
    private String faxNumberFormatted;

    private String neaPresent;

    @Column(columnDefinition = "LONGTEXT")
    private String neaLinks;
    private String branchPresent;
    @Column(columnDefinition = "LONGTEXT")
    private String aboutLinks;
    @Column(columnDefinition = "LONGTEXT")
    private String contactLInks;
    private String jobPresent;
    @Column(columnDefinition = "LONGTEXT")
    private String jobLinks;
    @Column(columnDefinition = "LONGTEXT")
    private String googleLinks;
    @Column(columnDefinition = "LONGTEXT")
    private String linkedinLinks;
    @Column(columnDefinition = "LONGTEXT")
    private String facebookLinks;
    @Column(columnDefinition = "LONGTEXT")
    private String twitterLinks;
    @Column(columnDefinition = "LONGTEXT")
    private String instagramLinks;
    @Column(columnDefinition = "LONGTEXT")
    private String youtubeLinks;
    @Column(columnDefinition = "LONGTEXT")
    private String privacyLinks;
    @Column(columnDefinition = "LONGTEXT")
    private String termsLinks;
    @Column(columnDefinition = "LONGTEXT")
    private String vimeoLinks;
    @Column(columnDefinition = "LONGTEXT")
    private String pinterestLinks;
    @Column(columnDefinition = "LONGTEXT")
    private String slideShareLinks;
    @Column(columnDefinition = "LONGTEXT")
    private String domainMacthedLinkedinLinks;
    @Column(columnDefinition = "LONGTEXT")
    private String domainMatchedFacebookLinks;
    @Column(columnDefinition = "LONGTEXT")
    private String domainMatchedTwitterLinks;
    @Column(columnDefinition = "LONGTEXT")
    private String domainMatchedInstagramLinks;
    @Column(columnDefinition = "LONGTEXT")
    private String domainMatchedYoutubeLinks;
    @Column(columnDefinition = "LONGTEXT")
    private String domainMatchedPrivacyLinks;
    @Column(columnDefinition = "LONGTEXT")
    private String domainMatchedTermsLinks;
    @Column(columnDefinition = "LONGTEXT")
    private String domainMatchedVimeoLinks;
    @Column(columnDefinition = "LONGTEXT")
    private String domainMatchedPinterestLinks;
    @Column(columnDefinition = "LONGTEXT")
    private String domainMatchedSlideShareLinks;
    @Column(columnDefinition = "LONGTEXT")
    private String googlePlusLinks;
    @Column(columnDefinition = "LONGTEXT")
    private String domainMatchedGooglePlusLinks;
    private String addressPresent;
    private String IpCountry;
    private String crawledDate;
    private String cache;

    @Column(columnDefinition = "LONGTEXT")
    private String ContactAllLinks;
    @Column(columnDefinition = "LONGTEXT")
    private String AboutAllLink;
    @Column(columnDefinition = "LONGTEXT")
    private String PrivacyAllLinks;
    @Column(columnDefinition = "LONGTEXT")
    private String TermsAllLinks;
    @Column(columnDefinition = "LONGTEXT")
    private String HomeAllLinks;
    private String Cleaned_copyright_name1;
    private String Cleaned_copyright_name2;
    private String cleaned_name_from_title;
//    private String postcode_present;
//    private String matched_postcode;
//    @Column(columnDefinition = "LONGTEXT")
//    private String postcode_match_text;
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

}
