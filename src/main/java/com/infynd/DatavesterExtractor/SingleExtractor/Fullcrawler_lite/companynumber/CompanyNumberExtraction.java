package com.infynd.DatavesterExtractor.SingleExtractor.Fullcrawler_lite.companynumber;

import com.infynd.DatavesterExtractor.SingleExtractor.Fullcrawler_lite.utils.WebsiteCrawlerConstants;
import com.infynd.DatavesterExtractor.service.FunctionInterface;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Service
@Slf4j
public class CompanyNumberExtraction {
    public String CRN_AlphaNumeric_Pattern = "\\b((SC|NI|R0|OC|SO|NC|AC|ZC|FC|GE|LP|OC|SE|SA|SZ|SF|GS|SL|SO|SC|ES|NA" + "|NZ|NF|GN|NL|NC|NI|EN|IP|SP|IC|SI|NP|NV|RC|SR|NR|NO)\\s*" + "(\\d\\s*\\d\\s" + "*\\d\\s*\\d\\s*\\d\\s*\\d\\s" + "*[\\d\\s*]?\\s*[\\d\\s*]?))\\b";
    public String CRN_Text_Pattern = "(Registration|Registered|Register|Regd[\\.]|Regt[\\.]|Reg[\\.])\\s*(in|for|at)" + "\\s*(\\w+\\s*){1,3}(no\\s*[\\.:-]?|number\\s*[\\.:-]?)\\s*" + "(\\d\\s*\\d" + "\\s*\\d\\s*\\d\\s*\\d" + "\\s*\\d\\s*[\\d\\s*]?\\s*[\\d\\s*]?)\\b";
    public String CRN_Text_Pattern_1 = "(Company|Registration|Registered|Register|Regd[\\.]|Regt[\\.]|Reg[\\.])\\s*" + "(no\\s*[\\.:-]?|Number\\s*[\\.:-]?)\\s*" + "(\\d\\s*\\d\\s*\\d\\s*\\d\\s*\\d" + "\\s*\\d\\s*[\\d\\s*]?\\s*[\\d\\s" + "*]?)\\b";
    @Autowired
    FunctionInterface functionInterface;


    public Map<String, Serializable> extract(Map<String, Serializable> extractedData) {
        String[] companyKeys = {"Company Number:", "Company Reg", "Company Number.", "Company number:", "Company number", "Company number:", "Company number.", "company number", "company number:", "company number.", "Company Number is", "Company number is", "Registered Number", "Registered Company No.", "Registered Company Number", "registration number is", "Registration Number", "Registered Number:", "Registered Number.", "Registered number", "Registered number:", "Registered number.", "Company No", "Company No:", "Company No.", "Company no", "Company no:", "Company no.", "company no", "company no:", "company no.", "Registered No", "Registered No:", "Registered No.", "Registered No. ", "Registered no", "Registered no:", "Registered no.", "Registration number", "Registration number:", "Registration number.", "Registration No", "Registration No:", "Registration No.", "Company Registration Number", "Company Registration Number:", "Company Registration Number.", "Company Registration number", "Company Registration number:", "Company Registration number.", "Registered in Ireland", "Registered in Ireland:", "Registered in Ireland.", "UK Registered", "UK Registered:", "UK Registered.", "Company Registration ", "Company Registration: ", "Company Registration. ", "Company Reg", "Company Reg:", "Company Reg.", "Company Reg No", "Company Reg No:", "Company Reg No.", "Company Registered Number", "Company Registered Number:", "Company Registered Number.", "Reg.No", "Reg.No:", "Reg.No.", "Company Registration no", "Company Registration no:", "Company Registration no", "Company Registration No", "Company Registration No:", "Company Registration No.", "License Number", "License Number:", "License Number.", "Reference Number", "Reference Number:", "Reference Number.", "Registered in England", "Registered in England No", "Registered in England No:", "Registered in England No.", "Registered in England Number", "Registered in England Number:", "Registered in England Number.", "Registered in England and Wales No", "Registered in England and Wales No:", "Registered in England and Wales No.", "Registered in England and Wales No. ", "REGISTERED COMPANY IN ENGLAND AND WALES (", "Registered In England And Wales Company No", "Registered In England And Wales Company No:", "Registered In England And Wales Company No.", "registered in England and Wales – Number", "registered in England and Wales – Number:", "registered in England and Wales – Number.", "registered in England and Wales – Number ", "registered in England and Wales. No: ", "Registered in England & Wales", "Registered Company No", "Registered Company No:", "company under number", "Registered in England and Wales", "Registration number", "Ltd", "registration number", "Limited", "Registered as a Company in England:", " Registered in Scotland No."};
        String[] charityKeys = {"Charity registration number:", "charity number", "Registered charity no.", "charity registered", "UK Registered Charity NO", "UK Registered Charity NO:", "UK Registered Charity NO.", "Registered Charity no.", "Registered Charity no", "Reg Charity No", "Reg Charity No:", "Reg Charity No.", "Registered Charity No.", "Registered Charity No:", "Registered Charity No", "Registered Charity Number", "Registered Charity Number:", "Registered Charity Number.", "Charity no", "Charity No"};
        String[] vatKeys = {"vat no", "vat no.", "vat number", "vat number:", "vat registration no", "vat registration no:", "vat registration number", "vat registration number:", "vat registration", "vat registration:", "vat reg no", "vat reg no:", "vat reg no.", "vat reg number", "vat reg number:", "vat reg number.", "vat reg", "vat reg:", "vat reg no", "vat reg no:", "vat reg no.", "vat reg number", "vat reg number:", "vat reg number.", "vat reg", "vat reg:", "vat reg no", "vat reg no:", "vat reg no.", "vat reg number", "vat reg number:", "vat reg number.", "vat reg", "vat reg:", "vat reg no", "vat reg no:", "vat reg no.", "vat reg number", "vat reg number:", "vat reg number.", "vat reg", "vat reg:", "vat reg no", "vat reg no:", "vat reg no.", "vat reg number", "vat reg number:", "vat reg number.", "vat reg", "vat reg:", "vat reg no", "vat reg no:", "vat reg no.", "vat reg number", "vat reg number:", "vat reg number.", "vat reg", "vat reg:", "vat reg no", "vat reg no:", "vat reg no.", "vat reg number", "vat reg number:", "vat reg number.", "vat reg", "vat reg:", "vat reg no", "vat reg no:", "vat reg no.", "vat reg number", "vat reg number:", "vat reg number.", "vat reg", "vat reg:", "vat reg no", "vat reg no:", "vat reg no.", "vat reg number", "vat reg number:", "vat reg number.", "vat reg", "vat reg:", "vat reg no", "vat reg no:", "vat reg no.", "vat reg number", "vat reg number:", "vat reg number.", "vat reg", "vat reg:", "vat reg no", "vat reg no:", "vat reg no.", "vat reg number", "vat reg number:", "vat reg number.", "vat reg", "vat reg:", "vat reg no", "vat reg no:", "vat reg no.", "vat reg number", "vat reg number:", "vat reg number.", "vat reg", "vat reg:"};
        Set<String> companyNumberSet = new HashSet<>();
        Set<String> charityNumberset = new HashSet<>();
        Set<String> vatNumberSet = new HashSet<>();
        try {
            String rawHtml = (String) extractedData.get(WebsiteCrawlerConstants.RAW_HTML);
            if (rawHtml != null) {
                rawHtml = StringUtils.replace(rawHtml, "@|!", ",").replaceAll("[\\[\\]()]", "");
                Document doc = Jsoup.parse(rawHtml);
                String content = doc.toString().toLowerCase();
                String cleansedContent = functionInterface.cleanPageSource(content);
                cleansedContent = functionInterface.RemoveHTMLTags(cleansedContent).toLowerCase();
                // Extract company numbers
                for (String key : companyKeys) {
                    extractCompanyNumbers(content, key, companyNumberSet);
                }
                // Extract charity numbers
                for (String key : charityKeys) {
                    extractCharityNumbers(content, key, charityNumberset);
                }
                // Extract VAT numbers
                for (String key : vatKeys) {
                    extractVATNumbers(content, key, vatNumberSet);
                }
                // Remove charity and VAT numbers from company numbers set if found
                companyNumberSet.removeAll(charityNumberset);
                companyNumberSet.removeAll(vatNumberSet);
            }
            if (CollectionUtils.isNotEmpty(vatNumberSet)) {
                String vatNO = String.join("|", vatNumberSet);
                extractedData.put(WebsiteCrawlerConstants.VAT_NO, vatNO);
            }
            if (CollectionUtils.isNotEmpty(companyNumberSet)) {
                String cNO = String.join("|", companyNumberSet);
                extractedData.put(WebsiteCrawlerConstants.COMPANY_NO, cNO);
            }
            if (CollectionUtils.isNotEmpty(charityNumberset)) {
                String charityNO = String.join("|", charityNumberset);
                extractedData.put(WebsiteCrawlerConstants.CHARITY_NO, charityNO);
            }
        } catch (Exception e) {
            log.error("Exception in getting company number ", e);
            if (CollectionUtils.isNotEmpty(companyNumberSet)) {
                String cNO = String.join("|", companyNumberSet);
                extractedData.put(WebsiteCrawlerConstants.COMPANY_NO, cNO);
            }
            if (CollectionUtils.isNotEmpty(charityNumberset)) {
                String charityNO = String.join("|", charityNumberset);
                extractedData.put(WebsiteCrawlerConstants.CHARITY_NO, charityNO);
            }
        }
        return extractedData;
    }

    private void extractCompanyNumbers(String content, String key, Set<String> companyNumberSet) {
        extractNumbers(content, key, companyNumberSet, this::isValidCompanyNumber);
    }

    private void extractCharityNumbers(String content, String key, Set<String> charityNumberSet) {
        extractNumbers(content, key, charityNumberSet, this::isValidCharityNumber);
    }

    private void extractVATNumbers(String content, String key, Set<String> vatNumberSet) {
        extractNumbers(content, key, vatNumberSet, this::isValidVATNumber);
    }

    private void extractNumbers(String content, String key, Set<String> numberSet, java.util.function.Predicate<String> validator) {
        key = key.toLowerCase();
        int index = 0;
        while (true) {
            int keywordIndex = content.indexOf(key, index);
            if (keywordIndex == -1) {
                break;
            }
            int startIndex = keywordIndex;
            int endIndex = keywordIndex + key.length();
            endIndex = Math.min(endIndex, content.length());
            for (int i = 0; i < 5; i++) {
                endIndex = content.indexOf(" ", endIndex + 1);
                if (endIndex == -1) {
                    endIndex = content.length();
                    break;
                }
            }
            if (startIndex >= 0 && endIndex <= content.length()) {
                String extractedWords = content.substring(startIndex, endIndex);
                extractedWords = functionInterface.cleanPageSource(extractedWords);
                extractedWords = functionInterface.RemoveHTMLTags(extractedWords);
                extractedWords = extractedWords.replaceAll("(?<=\\d)\\s+(?=\\d)", "");
                if (isPhoneNumber(extractedWords)) {
                    extractedWords = cleanExtractedWords(extractedWords);
                }
                if (extractedWords != null) {
                    String[] words = extractedWords.split("[\\s\\.\\,]");
                    for (String word : words) {
                        if (validator.test(word)) {
                            String finalword = word.toUpperCase();
                            numberSet.add(finalword);
                        }
                    }
                }
            }
            index = keywordIndex + 1;
        }
    }

    private boolean isValidCompanyNumber(String word) {
        return word.length() >= 5 && word.length() < 11 && (word.matches("[A-Za-z]{2}\\d+$") || word.matches("\\d+"));
    }

    private boolean isValidCharityNumber(String word) {
        return word.matches("\\d{6,8}");
    }

    private boolean isValidVATNumber(String word) {
        return word.matches("\\d{9}") || word.matches("\\d{12}");
    }

    private boolean isPhoneNumber(String text) {
        return text.toLowerCase().contains("phone") || text.toLowerCase().contains("tel") || text.toLowerCase().contains("telephone");
    }

    private String cleanExtractedWords(String extractedWords) {
        extractedWords = extractedWords.replaceAll("(?i)phone.*", "");
        extractedWords = extractedWords.replaceAll("(?i)phone:.*", "");
        extractedWords = extractedWords.replaceAll("(?i)corby.*", "");
        extractedWords = extractedWords.replaceAll("(?i)tel.*", "");
        extractedWords = extractedWords.replaceAll("(?i)tel:.*", "");
        extractedWords = extractedWords.replaceAll("(?i)telephone.*", "");
        extractedWords = extractedWords.replaceAll("(?i)telephone:.*", "");
        return extractedWords;
    }
}