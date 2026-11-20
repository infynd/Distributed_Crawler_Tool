package com.infynd.DatavesterExtractor.SingleExtractor.Fullcrawler_lite.email;


import com.infynd.DatavesterExtractor.SingleExtractor.Fullcrawler_lite.utils.WebsiteCrawlerConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.Serializable;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@Slf4j
public class EmailExtraction {

    @Autowired
    EmailIdentifierExtractor emailIdentifier;

    public Map<String, Serializable> extract(Map<String, Serializable> extractedData) {
        Set<String> consolidatedEmails = new HashSet<>();
        String rawHtml = (String) extractedData.get(WebsiteCrawlerConstants.RAW_HTML);
        String normalized = String.valueOf(extractedData.get(WebsiteCrawlerConstants.NORMALIZED_DOMAIN));
        if (!rawHtml.equals("")) {
            try {
                Set<String> junkMails = emailIdentifier.extractJunk(rawHtml);
                Set<String> extractedMail = emailIdentifier.extractEmail(rawHtml);
                extractedMail = emailIdentifier.extractEmail3(rawHtml, extractedMail);
                extractedMail = emailIdentifier.extractEmail4(rawHtml, extractedMail);
                Optional<Set<String>> removedInvalidMailFromNew = emailIdentifier.removeInvalidMails(Optional.ofNullable(extractedMail));
                removedInvalidMailFromNew.ifPresent(consolidatedEmails::addAll);
                if (!junkMails.isEmpty()) {
                    consolidatedEmails.removeAll(junkMails);
                }
                if (!consolidatedEmails.isEmpty()) {
                    extractedData = processEmail(extractedData, consolidatedEmails, normalized);
                }
            } catch (Exception e) {
                log.error("Error in Email Extraction New Logic ..." + e.getLocalizedMessage());
            }
        }
        log.info("email module completed -- {}");
        return extractedData;
    }

    private Map<String, Serializable> processEmail(Map<String, Serializable> extractedData, Set<String> consolidatedEmails, String normalized) {

        List<String> genericPatterns = List.of("contact", "team", "about", "support", "help", "customer.support", "sales", "business", "sales.inquiries", "enq", "enquires", "careers", "jobs", "media", "press", "public.relations", "partners", "partnerships", "alliances", "admin", "webmaster", "marketing", "promo", "campaigns", "publications", "london", "britain", "info", "sales", "hello", "contact", "enquiries", "support", "admin", "office", "webmaster", "privacy", "kontakt", "mail", "help", "marketing", "registrants", "secretary", "service", "bookings", "reception", "enquiry", "accounts", "customerservice", "contactus", "reservations", "impallari", "admissions", "membership", "jobs", "events", "orders", "recruitment", "filler", "Legal", "customercare", "treasurer", "studio", "chair", "clerk", "contato", "wordpress", "letstalk", "shop", "wpgod", "email", "manager", "edit,", "feedback", "safeguarding", "finance", "dataprotection", "communications", "accountwithdrawal", "customerservices", "compliance", "advertising", "inquiries", "welcome", "partnerships", "suggestions", "concerns", "legal", "customer", "publishing", "news", "complaints", "training", "20info", "booking", "connect", "abuse", "information", "chairman", "helpdesk", "care", "contacto", "commercial", "solutions", "inquiry", "dataprivacy", "security", "billing", "general", "career", "president", "administrator", "investors", "website", "corporate", "accounting", "advice", "registrar", "fundraising", "customer.service", "education", "community", "info", "sales", "casework", "enquire", "data.protection", "post", "theteam", "CustomerSupport", "headoffice", "getintouch", "websales", "sayhello", "clientservices", "unsubscribe", "questions", "webenquiries", "techsupport", "uksales", "datacontroller", "customer.information", "inbox", "noreply", "copyright", "your.address", "QFPComplaints", "customers", "subscriptions", "submissions", "customer.care", "webenquiry", "international.team", "webenquiry", "reservation", "customer.support", "accountants", "schooloffice", "webshop", "postbox", "tickets", "vacancies", "requests", "einfo", "webcontact", "clientcare", "query", "sales", "comments", "customerrelations", "data.privacy", "advertise", "enquries", "webmail", "complaint.info", "theoffice", "datarequest", "talktous", "datasecurity", "ourdata", "technicalsupport", "customersuccess", "public.relations", "Customer_Service", "clubmanager", "account", "copyrightagent", "uk.sales", "invoiceQueries", "conference", "branches", "orders", "design", "online", "ideas", "editor", "management", "grievances", "claims", "quotes", "order", "transport", "book", "store", "Social", "operations", "Guidance", "academy", "absence", "accessibility", "accommodation", "accreditation", "acquisitions", "alumni", "applications", "apply", "appointments", "archives", "artwork", "assistance", "assistant", "attendance", "b2b", "benefits", "circulation", "catering", "central", "ceo", "charity", "cheers", "Claims", "clients", "coaching", "coffee", "comercial", "communication", "comunicacion", "construction", "consultancy", "contracts", "Coordinator", "courses", "create", "customs", "cv", "developer", "developers", "development", "director", "discover", "dispatch", "distribution", "diversity", "ecommerce", "edinburgh", "employment", "engagement", "engineering", "estates", "estimates", "estimating", "exams", "executive", "EXPLORE", "export", "exports", "family", "foundation", "franchise", "franchising", "friends", "frontdesk", "functions", "funding", "furniture", "Governance", "governors", "GROUP", "groups", "head", "health", "hires", "home", "hospitality", "human.resources", "imports", "inclusion", "innovation", "insurance", "insure", "interiors", "international", "invest", "investor", "investorrelations", "Kitchen", "law", "leeds", "legal", "letters", "library", "licensing", "Life", "logistics", "management", "market", "medical", "meetings", "members", "mortgages", "music", "outreach", "parents", "paris", "partner", "parts", "payroll", "photos", "policy", "preferences", "primary", "procurement", "production", "professional", "project", "properties", "property", "publisher", "purchasing", "quality", "queries", "recruiting", "referrals", "refunds", "registration", "registry", "rentals", "report", "research", "residential", "restaurant", "resumes", "retail", "rights", "sale", "school", "schools", "shipping", "showroom", "social", "software", "solicitors", "staff", "SupplyChain", "surveyors", "sustainability", "talent", "talk", "trade", "trademarks", "transport", "updates", "us", "volunteer", "volunteering", "volunteers", "warranty", "weddings", "welfare", "wellbeing", "wholesale", "work");
        List<String> genericPatterns1 = List.of("hr", "gb", "hi", "pr", "ask", "web", "uk", "dpo", "dp");
        Set<String> allEmails = new HashSet<>();
        Set<String> genericEmailSet = new HashSet<>();
        Set<String> nongenericEmailSet = new HashSet<>();
        String normalizedDomainPrefix = "";
        try {
            for (String email : consolidatedEmails) {
                email = URLDecoder.decode(email, StandardCharsets.UTF_8);
                Matcher matcher = getEmail(email);
                if (matcher.find()) {
                    email = matcher.group();
                }
                String[] parts = email.split("@");
                if (parts.length == 2) {
                    String prefix = parts[0].toLowerCase();
                    String suffix = parts[1].toLowerCase();
                    if (normalizedDomainPrefix.isEmpty()) {
                        String[] domainParts = normalized.split("\\.");
                        if (domainParts.length > 0) {
                            normalizedDomainPrefix = domainParts[0];
                        }
                    }
                    if (!checkNumberAndSpecialCharacter(prefix) && !checkJunk(prefix) && !checkJunk2(suffix)) {
                        boolean isGeneric = genericPatterns.stream().anyMatch(pattern -> prefix.contains(pattern));
                        boolean isGeneric1 = genericPatterns1.contains(prefix);
                        boolean isGeneric2 = prefix.equals(normalizedDomainPrefix);
                        if (isGeneric || isGeneric1 || isGeneric2) {
                            genericEmailSet.add(email);
                        } else {
                            nongenericEmailSet.add(email);
                        }
                    }
                }
            }
            String genericEmails = String.join("|", genericEmailSet);
            String nonGenericEmails = String.join("|", nongenericEmailSet);
            allEmails.addAll(genericEmailSet);
            allEmails.addAll(nongenericEmailSet);
            String allEmailsString = String.join("|", allEmails);
            extractedData.put(WebsiteCrawlerConstants.EMAIL, allEmailsString);
            extractedData.put(WebsiteCrawlerConstants.GENERIC_EMAIL, genericEmails);
            extractedData.put(WebsiteCrawlerConstants.NON_GENERIC_EMAIL, nonGenericEmails);

        } catch (Exception e) {

        }
        return extractedData;
    }

    private boolean checkNumberAndSpecialCharacter(String prefix) {
        // Return true if the prefix contains only digits or special characters, otherwise return false
        return prefix.matches("[\\d!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?]+");
    }


    private boolean checkJunk2(String suffix) {
        List<String> junkList = List.of("sentry", "errors");
        return junkList.contains(suffix);
    }

    private boolean checkJunk(String prefix) {
        List<String> junkList = List.of("john.doe", "j.doe", "sample", "name", "some", "your", "you", "name", "yourname", "address", "address", "no-reply", "dpa", "domains", "someone", "test");
        return junkList.contains(prefix);
    }

    private Matcher getEmail(String content) {
        Pattern emailPattern = Pattern.compile("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}");
        Matcher matcher = emailPattern.matcher(content);
        return matcher;
    }


}