package com.infynd.DatavesterExtractor.service;


import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

@Slf4j
@Component
@Service
public class FunctionService implements FunctionInterface {

    public static String DOMAIN_REGEX = "(^[^\\.]+)";

    public String cleanPageSource(String pageSource) {
        String normContent =
                pageSource.replaceAll("(?sim)\\s\\s+", " ").replaceAll("(?sim)&nbsp;", " ").replaceAll("(?sim)&amp;"
                                , "&").replaceAll("overflow:hidden", "overflow").replaceAll("(?sim)\\s+", " ")
                        .replaceAll("(?sim)<li\\s+[^<>]*footer[^<>]*>(.*?)</li>", "<div class=\"footer\">$1</div>")
                        .replaceAll("(?sim)<ul\\s+[^<>]*footer[^<>]*>(.*?)</ul>", "<div class=\"footer\">$1</div>");

        List<String> replaceTags = new ArrayList<>();

        String[] replaceKeys = {"(?sim)<[^<>]*placeholder[^<>]*>",
//                "(?sim)<section[^<>]*>.*?/section>",
                "(?sim)<(ul)[^<>]*(Menu|item)[^<>]*>.*?</ul>|<(li)[^<>]*(Menu|item)[^<>]*>.*?</li>", "(?sim)<[^<>]*popup" +
                "-content[^<>]*>", "(?sim)<script[^<>]*>.*?</script>", "(?sim)<form[^<>]*>.*?</form>", "(?sim)" +
                "<iframe[^<>]*?>.*?</iframe>", "(?sim)<path[^<>]*?>", "(?sim)<iframe[^<>]*?>", "(?sim)<img[^<>]*?>|</img" +
                ">", "(?sim)<style[^<>]*?>.*?</style>", "(?sim)<noscript[^<>]*?>.*?</noscript>", "<noscript/>", "(?sim)" +
                "<head>.*?</head>", "(?sim)<!--[^<>]*>", "(?sim)<!--.*?-->", "(?sim)<!--.*?-->", "(?sim)<[^<>]*(data-element|style)[^<>]*>", "(?sim)<div[^<>]*image-container[^<>]*>.*?</div>", "(?sim)<button[^<>]*>.*?</button[^<>]*>",
                "(?sim)<[^<>]*stylesheet[^<>]*>", "(?sim)<[^/][^<>]*_pb_[^<>]*>.*?</[^<>]*_pb_[^<>]*>", "\\{.*?\\}",
                "(?sim)<(div)\\s*[^<>]*(hidden|popup|item)\\b[^<>]*>.*?</div>", "(?sim)<div[^<>]*ease[^<>]*>.*?</div>", "(?sim)<(li|div)[^<>]*photo[^<>]*>.*?</(li|div)>",
                "(?sim)<font[^<>]*>"};
        replaceTags = Arrays.asList(replaceKeys);
        normContent = replaceTags.stream().reduce(normContent,
                (tempStr, strToRemove) -> tempStr.replaceAll(strToRemove, " ").replaceAll("(?sim)\\s+", " "));
        return normContent;
    }


    public Set<String> regexMatchedSetData(String content, String patternRegex, int groupNumber) {
        if (content == null || content.isEmpty()) {
            return new HashSet<>();
        }
        final String safeContent = content.length() > 200_000 ? content.substring(0, 200_000) : content;

        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<Set<String>> future = executor.submit(() -> {
            Set<String> matchedData = new HashSet<>();
            try {
                Pattern regex = Pattern.compile(patternRegex, 232);
                Matcher regexMatcher = regex.matcher(safeContent);
                while (regexMatcher.find()) {
                    if (regexMatcher.groupCount() >= groupNumber) {
                        String matched = regexMatcher.group(groupNumber);
                        if (matched != null && matched.trim().replaceAll("<[^<>]*>", "").length() > 8) {
                            matchedData.add(matched);
                        }
                    }
                }
            } catch (Exception regexException) {
                log.error("Error in pattern matching: {}", regexException.getLocalizedMessage());
            }

            // Check for alternative pattern if no matches found
            if (matchedData.isEmpty()) {
                try {
                    Pattern regex = Pattern.compile(String.valueOf(patternRegex), 232);
                    Matcher regexMatcher = regex.matcher(safeContent);
                    while (regexMatcher.find()) {
                        if (regexMatcher.groupCount() >= 11) {
                            String matched = regexMatcher.group(11);
                            if (matched != null && matched.replaceAll("<[^<>]*>", "").trim().length() > 8) {
                                matchedData.add(matched);
                            }
                        }
                    }
                } catch (Exception regexException) {
                    log.error("Error in pattern matching: {}", regexException.getLocalizedMessage());
                }
            }
            return matchedData;
        });

        try {
            // Attempt to retrieve result within 3 seconds
            return future.get(3, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            future.cancel(true);
            log.warn("Pattern matching exceeded time limit of 3 seconds. Cancelled.");
            return new HashSet<>();
        } catch (Exception e) {
            log.error("Unexpected error in pattern matching: {}", e.getLocalizedMessage());
            return new HashSet<>();
        } finally {
            executor.shutdownNow();
        }
    }


    public String executeMatchPatternDomain(String content, String patternRegex, int groupNumber) {
        String matchedData = "";
        try {
            Pattern regex = Pattern.compile(String.valueOf(patternRegex), 234);
            Matcher regexMatcher = regex.matcher(content);
            if (regexMatcher.find()) {
                matchedData = regexMatcher.group(groupNumber).trim();
            }
        } catch (PatternSyntaxException patternSyntaxException) {
            log.error("Error in executeMatchPatternDomain function {}. Exception - {}",
                    patternSyntaxException.getPattern(), patternSyntaxException.toString());

        }
        return matchedData;
    }


    public Set<String> executeMatchPattern(String content, String patternRegex, int groupNumber) {
        Set<String> matchedData = new HashSet<>();
        try {
            Pattern regex = Pattern.compile(String.valueOf(patternRegex), 234);

            Matcher regexMatcher = regex.matcher(content);
            while (regexMatcher.find()) {
                matchedData.add(regexMatcher.group(groupNumber).replaceAll("<[^<>]*>", "").replaceAll(">.*?\\s*$",
                        "").replaceAll("\\s*/\\s*$", "").trim());
            }
        } catch (PatternSyntaxException patternSyntaxException) {
            log.error("Error in executeMatchPattern function {}. Exception - {}", patternSyntaxException.getPattern()
                    , patternSyntaxException.toString());
        }


        return matchedData;
    }


    public String RemoveHTMLTags(String content) {
        String tempContent = content;
        tempContent = tempContent.replaceAll("(?sim)<[^<>]*>", " ");
        tempContent = tempContent.replaceAll("(?sim)\\s+", " ");
        return tempContent;
    }


    public String getPartialDomainName(String domainName) {
        String normDomain = executeMatchPatternDomain(domainName, DOMAIN_REGEX, 1);
        normDomain = normDomain.replaceAll("(?sim)^\\s*(http[s]?)?(:)?(//)?(www\\.)?", "");
        return normDomain;
    }

    public String normalizePageSource(String pageSource) {
        List<String> strToReplace = Arrays.asList("(?sim)&lt;,<", "(?sim)&gt;,>", "(?sim)\\s+, ", "(?sim)&reg;,",
                "(?sim)&nbsp;, ", "(?sim)\\s+, ", "(?sim)&amp,&", "(?sim)&#9658;,", "(?sim)â;,", "(?sim)&#9658;,",
                "(?sim)™,", "(?sim)€,", "(?sim)" + "&#x201c, ", "(?sim)&#x201d,", "(?sim)&quot;,", "(?sim)\",",
                "(?sim)…,", "(?sim)\\[\\],", "(?sim)&nbsp;, ", "(?sim)\\s+, ", "(?sim)&#9658;,", "(?sim)&#x201d|Ã¢;" +
                        "|Ã‚|Ã¢|&#9658;|&quot;|Â€|Â™,", "(?sim)&#x201c, ", "(?sim)Â ,\\S", "(?sim)\"\",\"", "(?sim)" +
                        "&#039;,'", "(?sim)&#233,e", "(?sim)&#235," + "e", "(?sim)&#8211;,-", "(?sim)&#8213;,-",
                "(?sim)&#8217;,'", "(?sim)&#8220;,\"", "(?sim)" + "&#8221;,\"", "(?sim)&amp;,&", "(?sim)&bull;,6",
                "(?sim)&euro;,â‚¬", "(?sim)&euro;-,â‚¬", "(?sim)&ldquo;,\"", "(?sim)&mdash;,'", "(?sim)&nbsp;,\\S",
                "(?sim)&ndash;,-", "(?sim)&rdquo;,â€ ", "(?sim)&rsquo;,'", "(?sim)&rsquo;,", "(?sim)&rsquo;-,'",
                "(?sim)Â¿,\"", "(?sim)Â¬,â‚¬", "(?sim)" + "â‚¬â„¢,'", "(?sim)Ã¡,a", "(?sim)Ã ,A", "(?sim)Ã ,a",
                "(?sim)Ã€,A", "(?sim)Ã‚,", "(?sim)Ã¢,a", "(?sim)Ã¤,a", "(?sim)Ã„,a", "(?sim)Äƒ,a", "(?sim)Ãƒ,a",
                "(?sim)Ã£,a", "(?sim)Ã…,a", "(?sim)Ã¥,a", "(?sim)ÃƒÂ¨,e", "(?sim)ÃƒÂ´,o", "(?sim)Ã¢â€šÂ¤,Â£", "(?sim)" +
                        "Ã¢â€šÂ¬,â‚¬", "(?sim)aÂ±,n", "(?sim)ÃƒÂ±," + "n", "(?sim)ÃƒÂ±,n", "(?sim)ÃƒÂ«,e", "(?sim)" +
                        "aÂ£,a", "(?sim)Ã‚Â£,Â£", "(?sim)ÃƒÂ£,a", "(?sim)" + "ÃƒÂ¤,a", "(?sim)aÂ§,c", "(?sim)ÃƒÂ§o,c"
                , "(?sim)ÃƒÂ©,e", "(?sim)aÂ®,i", "(?sim)Ã‚Â®,Â®", "(?sim)ÃƒÂ¶,o", "(?sim)Ãƒâ€°,e", "(?sim)aâ‚¬,Ã€",
                "(?sim)Ã¢â‚¬,\"", "(?sim)Ã¢â‚¬Â ,\"", "(?sim)" + "Ã¢â‚¬?,\"", "(?sim)Ã¢â‚¬Â¦,...", "(?sim)Ã¢â‚¬Ëœ,'",
                "(?sim)Ã¢â‚¬Ëœ,\"", "(?sim)Ã¢â‚¬â€˜,-", "(?sim)Ã¢â‚¬â€œ,-", "(?sim)Ã¢â‚¬â€ ,-", "(?sim)Ã¢â‚¬Â¢-,",
                "(?sim)Ã¢â‚¬Å“,\"", "(?sim)Ã¢â‚¬â„¢,'", "(?sim)ÃƒÂ¼,u", "(?sim)ÃƒÂ³,o", "(?sim)aÂª,e", "(?sim)Ã¦,ae",
                "(?sim)Ã†,ae", "(?sim)ÃƒÂº,u", "(?sim)" + "aÅ ,e", "(?sim)Ä‡,c", "(?sim)Ä ,c", "(?sim)ÄŒ,C", "(?sim)" +
                        "Ã§,c", "(?sim)Ã°,eth", "(?sim)Ã‰,e", "(?sim)Ã©,e", "(?sim)Ã¨,e", "(?sim)Ãˆ,e", "(?sim)Ãª,e",
                "(?sim)Ã«,e", "(?sim)Ä›,e", "(?sim)Ä•,e", "(?sim)Ä™,e", "(?sim)Ã­,i", "(?sim)Ã ,i", "(?sim)Ã¬,i",
                "(?sim)Ã®,i", "(?sim)Ã¯,i", "(?sim)Å‚,l", "(?sim)Å„,n", "(?sim)Åˆ,n", "(?sim)Ã±,n", "(?sim)Ã‘,n",
                "(?sim)Ã³,o", "(?sim)Ã²,o", "(?sim)Ã´,o", "(?sim)Ã”,o", "(?sim)Ã¶,o", "(?sim)Ãµ,o", "(?sim)Å‘,o",
                "(?sim)Ã¸,o", "(?sim)Ã˜,O", "(?sim)Å™,r", "(?sim)Å¡,s", "(?sim)Å ,S", "(?sim)ÅŸ,s", "(?sim)ÃŸ,ss",
                "(?sim)Ãº,u", "(?sim)Ã¹,u", "(?sim)Ã»,u", "(?sim)Ã¼,u", "(?sim)Ãœ,u", "(?sim)Ã½,y", "(?sim)Ã¿,y",
                "(?sim)Å¼,z", "(?sim)Å¾,z", "(?sim)Å½,Z", "(?sim)Ä…,a", "(?sim)Ã¢â‚¬â€œ,-", "(?sim)&#39,'");
        pageSource = strToReplace.stream().reduce(pageSource,
                (tempStr, strToRemove) -> tempStr.replaceAll(strToRemove.substring(0, strToRemove.lastIndexOf(",")),
                        strToRemove.substring(strToRemove.indexOf(",") + 1)));
        return pageSource.trim();
    }

    public String encryptedEmailNormalization(String cleanContent) {
        List<String> strToReplace = Arrays.asList("(?sim)&#x20;, ", "(?sim)&#x21;,!", "(?sim)&#x23;,#", "(?sim)&#x24;,$", "(?sim)&#x25;,%", "(?sim)&#x26;,&", "(?sim)&#x27;,'", "(?sim)&#x28;,(", "(?sim)&#x29;,)", "(?sim)&#x30;,0", "(?sim)&#x31;,1", "(?sim)&#x32;,2", "(?sim)&#x33;,3", "(?sim)&#x34;,4", "(?sim)&#x35;,5", "(?sim)&#x36;,6", "(?sim)&#x37;,7", "(?sim)&#x38;,8", "(?sim)&#x39;,9", "(?sim)&#x40;,@", "(?sim)&#x41;,A", "(?sim)&#x42;,B", "(?sim)&#x43;,C", "(?sim)&#x44;,D", "(?sim)&#x45;,E", "(?sim)&#x46;,F", "(?sim)&#x47;,G", "(?sim)&#x48;,H", "(?sim)&#x49;,I", "(?sim)&#x50;,P", "(?sim)&#x51;,Q", "(?sim)&#x52;,R", "(?sim)&#x53;,S", "(?sim)&#x54;,T", "(?sim)&#x55;,U", "(?sim)&#x56;,V", "(?sim)&#x57;,W", "(?sim)&#x58;,X", "(?sim)&#x59;,Y", "(?sim)&#x60;,`", "(?sim)&#x61;,a", "(?sim)&#x62;,b", "(?sim)&#x63;,c", "(?sim)&#x64;,d", "(?sim)&#x65;,e", "(?sim)&#x66;,f", "(?sim)&#x67;,g", "(?sim)&#x68;,h", "(?sim)&#x69;,i", "(?sim)&#x70;,p", "(?sim)&#x71;,q", "(?sim)&#x72;,r", "(?sim)&#x73;,s", "(?sim)&#x74;,t", "(?sim)&#x75;,u", "(?sim)&#x76;,v", "(?sim)&#x77;,w", "(?sim)&#x78;,x", "(?sim)&#x79;,y", "(?sim)&#x2a;,*", "(?sim)&#x2b;,+", "(?sim)&#x2d;,-", "(?sim)&#x2e;,.", "(?sim)&#x2f;,/", "(?sim)&#x3a;,:", "(?sim)&#x3b;,;", "(?sim)&#x3c;,<", "(?sim)&#x3d;,=", "(?sim)&#x3e;,>", "(?sim)&#x3f;,?", "(?sim)&#x4a;,J", "(?sim)&#x4b;,K", "(?sim)&#x4c;,L", "(?sim)&#x4d;,M", "(?sim)&#x4e;,N", "(?sim)&#x4f;,O", "(?sim)&#x5a;,Z", "(?sim)&#x5b;,[", "(?sim)#&#x5c;,\\", "(?sim)&#x5d;,]", "(?sim)&#x5e;,^", "(?sim)&#x5f;,_", "(?sim)&#x6a;,j", "(?sim)&#x6b;,k", "(?sim)&#x6c;,l", "(?sim)&#x6d;,m", "(?sim)&#x6e;,n", "(?sim)&#x6f;,o", "(?sim)&#x7a;,z", "(?sim)&#x7b;,{", "(?sim)&#x7c;,|", "(?sim)&#x7d;,}", "(?sim)&#x7e;,~", "(?sim)&#x7f;, ", "(?sim)&#33;,!", "(?sim)&#35;,#", "(?sim)&#36;,$", "(?sim)&#37;,%", "(?sim)&#38;,&", "(?sim)&#39;,'", "(?sim)&#40;,(", "(?sim)&#41;,)", "(?sim)&#42;,*", "(?sim)&#43;,+", "(?sim)&#44;,,", "(?sim)&#45;,-", "(?sim)&#46;,.", "(?sim)&#47;,/", "(?sim)&#48;,0", "(?sim)&#49;,1", "(?sim)&#50;,2", "(?sim)&#51;,3", "(?sim)&#52;,4", "(?sim)&#53;,5", "(?sim)&#54;,6", "(?sim)&#55;,7", "(?sim)&#56;,8", "(?sim)&#57;,9", "(?sim)&#58;,:", "(?sim)&#59;,;", "(?sim)&#60;,<", "(?sim)&#61;,=", "(?sim)&#62;,>", "(?sim)&#63;,?", "(?sim)&#64;,@", "(?sim)&#65;,A", "(?sim)&#66;,B", "(?sim)&#67;,C", "(?sim)&#68;,D", "(?sim)&#69;,E", "(?sim)&#70;,F", "(?sim)&#71;,G", "(?sim)&#72;,H", "(?sim)&#73;,I", "(?sim)&#74;,J", "(?sim)&#75;,K", "(?sim)&#76;,L", "(?sim)&#77;,M", "(?sim)&#78;,N", "(?sim)&#79;,O", "(?sim)&#80;,P", "(?sim)&#81;,Q", "(?sim)&#82;,R", "(?sim)&#83;,S", "(?sim)&#84;,T", "(?sim)&#85;,U", "(?sim)&#86;,V", "(?sim)&#87;,W", "(?sim)&#88;,X", "(?sim)&#89;,Y", "(?sim)&#90;,Z", "(?sim)&#91;,[", "(?sim)&#033;,!", "(?sim)&#035;,#", "(?sim)&#036;,$", "(?sim)&#037;,%", "(?sim)&#038;,&", "(?sim)&#039;,'", "(?sim)&#040;,(", "(?sim)&#041;,)", "(?sim)&#042;,*", "(?sim)&#043;,+", "(?sim)&#044;,,", "(?sim)&#045;,-", "(?sim)&#046;,.", "(?sim)&#047;,/", "(?sim)&#048;,0", "(?sim)&#049;,1", "(?sim)&#050;,2", "(?sim)&#051;,3", "(?sim)&#052;,4", "(?sim)&#053;,5", "(?sim)&#054;,6", "(?sim)&#055;,7", "(?sim)&#056;,8", "(?sim)&#057;,9", "(?sim)&#058;,:", "(?sim)&#059;,;", "(?sim)&#060;,<", "(?sim)&#061;,=", "(?sim)&#062;,>", "(?sim)&#063;,?", "(?sim)&#064;,@", "(?sim)&#065;,A", "(?sim)&#066;,B", "(?sim)&#067;,C", "(?sim)&#068;,D", "(?sim)&#069;,E", "(?sim)&#070;,F", "(?sim)&#071;,G", "(?sim)&#072;,H", "(?sim)&#073;,I", "(?sim)&#074;,J", "(?sim)&#075;,K", "(?sim)&#076;,L", "(?sim)&#077;,M", "(?sim)&#078;,N", "(?sim)&#079;,O", "(?sim)&#080;,P", "(?sim)&#081;,Q", "(?sim)&#082;,R", "(?sim)&#083;,S", "(?sim)&#084;,T", "(?sim)&#085;,U", "(?sim)&#086;,V", "(?sim)&#087;,W", "(?sim)&#088;,X", "(?sim)&#089;,Y", "(?sim)&#090;,Z", "(?sim)&#091;,[");
        cleanContent = strToReplace.stream().reduce(cleanContent,
                (tempStr, strToRemove) -> tempStr.replaceAll(strToRemove.substring(0, strToRemove.lastIndexOf(",")),
                        strToRemove.substring(strToRemove.indexOf(",") + 1)));

        return cleanContent;
    }


}