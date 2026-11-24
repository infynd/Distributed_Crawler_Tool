package com.infynd.DatavesterExtractor.SingleExtractor.Fullcrawler_lite.email;


import com.infynd.DatavesterExtractor.service.FunctionInterface;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Component
@Slf4j
public class EmailIdentifierExtractor {
    public String EMAIL_REGEXFORMATION = "(\\w{1,2}[\\.]\\s)?([\\w\\-]+([\\.]?[\\w\\-])*@[\\w\\-]+(\\.org)?\\.(com|co\\.uk|uk|ae|af|ax|al|dz|as|ad|ao|ai|aq|ar|am|aw|ac|au|at|az|bs|bh|bd|bb|eus|by|be|bz|bj|bm|bt|bo|bq|ba|bw|bv|br|io|vg|bn|bg|bf|mm|bi|kh|cm|ca|cv|cat|ky|cf|td|cl|cn|cx|cc|co|cd|cg|ck|cr|ci|hr|cu|cw|cy|nc|cz|dk|dj|dm|do|ae|tl|ec|eg|sv|uk|gq|er|ee|et|eu|fo|fk|fj|fi|fr|gf|pf|tf|ga|gal|gm|ps|ge|de|gh|gi|uk|gr|gl|gd|gp|gu|gt|gg|gn|gw|gy|ht|hm|nl|hn|hk|hu|is|in|id|ir|iq|ie|uk|im|il|it|jm|jp|je|jo|kz|ke|ki|kp|kr|kw|kg|la|lv|lb|ls|lr|ly|li|lt|lu|mo|mk|mg|mw|my|mv|ml|mt|mh|mq|mr|mu|yt|mx|fm|md|mc|mn|me|ms|ma|mz|mm|na|nr|np|nl|nc|nz|ni|ne|ng|nu|nf|nc|kp|mk|uk|mp|no|om|pk|pw|ps|pa|pg|py|pe|ph|pn|pl|pt|pr|qa|re|ro|ru|rw|bq|bl|sh|kn|lc|mf|vc|pm|ws|sm|st|sa|uk|sn|rs|sc|sl|sg|bq|sx|sk|si|sb|so|so|za|gs|kr|ss|es|lk|sd|sr|sj|sz|se|ch|sy|pf|tw|tj|tz|th|tg|tk|to|tt|tn|tr|tm|tc|tv|ug|ua|ae|uk|us|vi|uy|uz|vu|va|ve|vn|uk|wf|ps|eh|ye|zm|zw))\\b";
    @Autowired
    FunctionInterface functionInterface;

    public Set<String> extractEmail(String rawHtml) {
        log.info("Extracting emails from raw HTML of length: {}", rawHtml.length());
        final int TIMEOUT_SECONDS = 5;
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<Set<String>> future = executor.submit(() -> {
            Set<String> emailMatchedBlocks = functionInterface.regexMatchedSetData(rawHtml, EMAIL_REGEXFORMATION, 1);
            log.info("Matched email blocks count before removal: {}", emailMatchedBlocks.size());
            emailMatchedBlocks = removeunwantedtext(emailMatchedBlocks);
            log.info("Final matched email blocks count after removal: {}", emailMatchedBlocks.size());
            return emailMatchedBlocks;
        });

        try {
            return future.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            log.error("Email extraction timed out after {} seconds", TIMEOUT_SECONDS);
            future.cancel(true);
            return Collections.emptySet();
        } catch (ExecutionException e) {
            log.error("Error during email extraction: {}", e.getCause().getMessage());
            return Collections.emptySet();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Email extraction was interrupted");
            return Collections.emptySet();
        } finally {
            executor.shutdown();
        }
    }

    public Set<String> extractEmail3(String cleansedContent, Set<String> emails) {
        Set<String> emailMatchedBlocks = new HashSet<>();
        Pattern emailPattern = Pattern.compile("[a-zA-Z0-9.%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}|\\w+@[a-zA-Z]+?\\.[a-zA-Z]{2,3}");
        Document document = Jsoup.parse(cleansedContent);
        Elements emailLinks = document.select("a[href^=\"mailto:\"]");
        for (Element link : emailLinks) {
            String email = link.attr("href").replace("mailto:", "");
            Matcher matcher = emailPattern.matcher(email);
            if (matcher.matches()) {
                emailMatchedBlocks.add(email);
            }
        }
        emailMatchedBlocks = removeunwantedtext(emailMatchedBlocks);
        emailMatchedBlocks.addAll(emails);
        return emailMatchedBlocks;
    }

    public Set<String> extractEmail4(String cleansedContent, Set<String> emails) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<Set<String>> future = executor.submit(() -> {
            Set<String> emailMatchedBlocks = new HashSet<>();
            Pattern emailPattern = Pattern.compile("[a-zA-Z0-9.%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}|\\w+@[a-zA-Z]+?\\.[a-zA-Z]{2,3}");
            Matcher matcher = emailPattern.matcher(cleansedContent);
            while (matcher.find()) {
                String email = matcher.group();
                emailMatchedBlocks.add(email);
            }
            // Process to remove unwanted text if necessary
            emailMatchedBlocks = removeunwantedtext(emailMatchedBlocks);
            emailMatchedBlocks.addAll(emails);
            return emailMatchedBlocks;
        });

        try {
            // Attempt to retrieve the result within 5 seconds
            return future.get(5, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            log.warn("Email extraction exceeded the time limit of 5 seconds.");
            return null;
        } catch (Exception e) {
            log.error("Unexpected error during email extraction: {}", e.getLocalizedMessage());
            return null;
        } finally {
            executor.shutdown();
        }
    }

    public Optional<Set<String>> removeInvalidMails(Optional<Set<String>> extractedMail) {
        Set<String> validEmails = extractedMail.orElse(Collections.emptySet())
                .parallelStream()
                .map(email -> {
                    // appear at the start of the email
                    email = email.replaceAll("^(%20|%22|u003E|u003e|mailto)", "");
                    email = email.replaceAll("^(20|22|3E)", "");
                    // when they appear within the email
                    email = email.replaceAll("(%20|%22|u003E|u003e)", "");
                    email = email.replaceFirst("^[^a-zA-Z0-9]+", "");
                    return email;
                })
                .filter(email -> email.contains("@"))
                .filter(email -> {
                    email = email.toLowerCase();
                    return !email.contains("@mail") && !email.contains("+") && !email.contains("example.com") && !email.contains("example@") && !email.contains("@sedo.com") && !email.contains("datacontroller") && !email.contains("datasecurity") && !email.contains("dpoffice")
                            && !email.contains("legal@") && !email.contains("test@") && !email.contains("copyright") && !email.contains("dpo@") && !email.contains("information") && !email.contains("dataprotect") && !email.contains(".gif") && !email.contains(".png") && !email.contains(".jpg") && !email.contains("2x.avif") && !email.contains(".webp") && !email.contains(".svg")
                            && !email.contains("google.com") && !email.contains("sentry.") && !email.contains("sentry-") && !email.contains("godaddy.com") && !email.contains("afternic.com") && !email.contains("domain.com")
                            && !email.contains("wix.com") && !email.contains("enteryour@") && !email.contains("test.com") && !email.contains(".jpeg") && !email.contains("your.address") && !email.contains("youremail") && !email.contains("@yourcompany.com") && !email.contains("@address.com") && !email.contains("noreply") && !email.contains("webmaster") && !email.contains("data.protection") && !email.contains("dataprotection") && !email.contains("complain")
                            && !email.contains("dataprivacy") && !email.contains("dataprotectionofficer") && !email.contains("abuse@") && !email.contains("website@") && !email.contains("websites@") && !email.contains("unsubscribe") && !email.contains("subscribe")
                            && !email.contains("privacyofficer") && !email.contains("data.privacy") && !email.contains("datasecurityinformation") && !email.contains("privacypolicy") && !email.contains("datamanager") && !email.contains("data@")
                            && !email.contains("compliance") && !email.contains("privacy@") && !email.contains("informationsecurity") && !email.contains("dataethics") && !email.contains("dataofficer") && !email.contains("datatransparency")
                            && !email.contains("informationprotect") && !email.contains("informationofficer") && !email.contains(".pdf") && !email.contains(".docx") && !email.contains(".jpeg ") && !email.contains("2x.gif") && !email.contains("example") && !email.contains("user@")
                            && !email.contains("zipify.com") && !email.contains("qq.com") && !email.contains("dan@") && !email.contains("abc@") && !email.contains("your@") && !email.contains("name@") && !email.contains("you@") && !email.contains("domain") && !email.contains("gdpr@");
                })
                .collect(Collectors.toSet());
        return Optional.of(validEmails);
    }

    public Set<String> extractJunk(String rawHtml) {
        Set<String> junkMails = new HashSet<>();
        String regex = "placeholder=\"(.*?[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Z|a-z]{2,})\"";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(rawHtml);
        while (matcher.find()) {
            junkMails.add(matcher.group(1));
        }
        junkMails = removeunwantedtext(junkMails);
        return junkMails;
    }

    private Set<String> removeunwantedtext(Set<String> junkMails) {
        String emailRegex = "[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Z|a-z]{2,}";
        Pattern emailPattern = Pattern.compile(emailRegex);
        return junkMails.stream()
                .map(emailPattern::matcher)
                .filter(Matcher::find)
                .map(Matcher::group)
                .collect(Collectors.toSet());
    }
}




