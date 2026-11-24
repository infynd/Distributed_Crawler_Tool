package com.infynd.DatavesterExtractor.SingleExtractor.Fullcrawler_lite.meta;


import com.infynd.DatavesterExtractor.SingleExtractor.Fullcrawler_lite.utils.WebsiteCrawlerConstants;
import lombok.extern.slf4j.Slf4j;
import net.sourceforge.tess4j.ITesseract;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.Serializable;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class ImageUrlExtractor {


    public String extractImageUrl(Document doc, String domain, String processingId, String redirectedUrl) {

        try {
            String imageUrl = null;
            doc.setBaseUri(redirectedUrl);
            imageUrl = getUrl(doc, redirectedUrl);
            if (StringUtils.isBlank(imageUrl)) {
                List<String> metaImageQueries = new ArrayList<>();
                metaImageQueries.add("meta[property=og:image]");
                metaImageQueries.add("meta[name=twitter:image]");
                imageUrl = getUrlFromMeta(metaImageQueries, doc);
            }
            if (StringUtils.isNotBlank(imageUrl)) {
                return imageUrl;
            }

        } catch (Exception e) {
            log.error(String.format("Exception in getting imageUrl for domain %s", processingId), e);
            return null;
        }

        return null;
    }

    private String getUrl(Document doc, String redirectedUrl) {
        String redirecTedDomain = redirectedUrl.replaceAll("http://", "");
        redirecTedDomain = redirecTedDomain.replaceAll("https://", "");
        redirecTedDomain = redirecTedDomain.replaceAll("https:", "");
        redirecTedDomain = redirecTedDomain.replaceAll("https:", "");
        redirecTedDomain = redirecTedDomain.replaceAll("www.", "");
        if (redirecTedDomain.contains("/")) {
            int index = redirecTedDomain.indexOf("/");
            redirecTedDomain = redirecTedDomain.substring(0, index);
        }
        if (redirecTedDomain.contains("?")) {
            int index = redirecTedDomain.indexOf("?");
            redirecTedDomain = redirecTedDomain.substring(0, index);
        }
        Elements elements = doc.select("*[class*='logo']");
        for (Element element : elements) {
            if (element.tagName().equals("img")) {
                String url = element.attr("abs:src");
                if (url.contains(redirecTedDomain)) {
                    if (StringUtils.isNotBlank(url)) {
                        return url;
                    }
                }
            }
            Elements images = element.getElementsByTag("img");
            for (Element image : images) {
                String imageurl = image.attr("abs:src");
                if (imageurl.contains(redirecTedDomain)) {
                    if (StringUtils.isNotBlank(imageurl)) {
                        return imageurl;
                    }
                }
            }
        }
        return null;
    }

    private String getUrlFromMeta(List<String> imageQueries, Document doc) {

        String imageUrl = null;
        for (String query : imageQueries) {
            Elements url = doc.select(query);
            imageUrl = url.attr("content");
            if (StringUtils.isNotEmpty(imageUrl)) {
                break;
            }
        }
        return imageUrl;
    }

    public Map<String, Serializable> extract(Map<String, Serializable> extractedData) {
        try {
            String Url = (String) extractedData.get(WebsiteCrawlerConstants.IMAGE_URL);
            // URL to the image
            if (Url != null) {
                URL imageUrl = new URL(Url);
                BufferedImage image = ImageIO.read(imageUrl);
                ITesseract instance = new Tesseract();
                instance.setDatapath("C:/Program Files/Tesseract-OCR/tessdata");
                String result = instance.doOCR(image);
                if (result != null) {
                    extractedData.put(WebsiteCrawlerConstants.IMAGE_TEXT, result);
                }
            }
        } catch (Exception e) {
            log.error("Error in OCR extraction: {}", e.getMessage());
        }
        return extractedData;
    }
}
