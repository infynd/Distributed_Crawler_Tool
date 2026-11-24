package com.infynd.DatavesterExtractor.SingleExtractor.Fullcrawler_lite.utils.service;

import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.apache.commons.collections4.MapUtils;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.TimeUnit;


@Service
@Slf4j
public class RestUtilExtractor {
    private static final OkHttpClient newClient;

    static {
        newClient = new OkHttpClient().newBuilder().connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .retryOnConnectionFailure(true)
                .build();
    }

    public static Response getAPICall(String url, Map<String, String> headers) throws IOException {
        Response response = null;
        try {
            Request.Builder requestBuilder = new Request.Builder().url(url);
            if (MapUtils.isNotEmpty(headers)) {
                for (String key : headers.keySet()) {
                    requestBuilder.addHeader(key, headers.get(key));
                }
            }
            Request request = requestBuilder.url(url).get()
                    .addHeader("content-type", "application/json")
                    .addHeader("Connection", "close")
                    .build();
            response = newClient.newCall(request).execute();
            return response;
        } catch (Exception e) {
            log.error(String.format("Exception in get call %s", url), e);
        }
        return null;
    }

    public static String postAPICall(String url, RequestBody requestBody, Map<String, String> headers) throws IOException {
        Response response = null;
        try {
            Request.Builder requestBuilder = new Request.Builder().url(url);
            if (MapUtils.isNotEmpty(headers)) {
                for (String key : headers.keySet()) {
                    requestBuilder.addHeader(key, headers.get(key));
                }
            }
            Request request = requestBuilder.url(url).post(requestBody)
                    .addHeader("content-type", "application/json")
                    .addHeader("Connection", "close")
                    .build();
            response = newClient.newCall(request).execute();
            return response.body().string();
        } catch (Exception e) {
            log.error(String.format("Exception in get call %s", url), e);
        } finally {
            if (response != null) {
                response.body().close();
            }
        }
        return null;
    }

    public static String putAPICall(String url, RequestBody requestBody, Map<String, String> headers) throws IOException {
        Response response = null;
        try {
            Request.Builder requestBuilder = new Request.Builder().url(url);
            if (MapUtils.isNotEmpty(headers)) {
                for (String key : headers.keySet()) {
                    requestBuilder.addHeader(key, headers.get(key));
                }
            }
            Request request = requestBuilder.url(url).put(requestBody)
                    .addHeader("content-type", "application/json")
                    .addHeader("Connection", "close")
                    .build();
            response = newClient.newCall(request).execute();
            return response.body().string();
        } catch (Exception e) {
            log.error(String.format("Exception in get call %s", url), e);
        } finally {
            if (response != null) {
                response.body().close();
            }
        }
        return null;
    }
}
