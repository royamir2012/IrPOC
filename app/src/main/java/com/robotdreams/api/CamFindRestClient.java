package com.robotdreams.api;

import android.content.Context;

import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.RequestParams;
import com.loopj.android.http.SyncHttpClient;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import cz.msebera.android.httpclient.Header;

public class CamFindRestClient {
    private static final String BASE_URL = "https://camfind.p.mashape.com/";

    SyncHttpClient client;

    public CamFindRestClient() {

        client = new SyncHttpClient();
        client.addHeader("X-Mashape-Key", "Q8K6tMbCXJmshc8PFefRaFRH120Fp1USOTvjsnMGi41mrZ5wF8");
        client.addHeader("Accept", "application/json");
    }

    private static String getAbsoluteUrl(String relativeUrl) {
        return BASE_URL + relativeUrl;
    }

    public String imageRequest(final Context context, final byte[] imageData) {

        RequestParams params = new RequestParams();
        params.put("focus[x]", "480");
        params.put("focus[y]", "640");
        params.put("image_request[altitude]", "27.912109375");
        params.put("image_request[language]", "en");
        params.put("image_request[latitude]", "35.8714220766008");
        params.put("image_request[locale]", "en_US");
        params.put("image_request[longitude]", "14.3583203002251");

        InputStream is = new ByteArrayInputStream(imageData);
        params.put("image_request[image]", is, "IMG_6707.JPG", "image/jpeg");

        final StringBuilder tokenBuf = new StringBuilder();
        client.post(context, getAbsoluteUrl("image_requests"), params, new JsonHttpResponseHandler() {

            @Override
            public void onSuccess(int statusCode, cz.msebera.android.httpclient.Header[] headers, JSONObject response) {

                try {
                    tokenBuf.append(response.getString("token"));
                } catch (Exception e) {

                }
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONObject errorResponse) {
                super.onFailure(statusCode, headers, throwable, errorResponse);
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONArray errorResponse) {
                super.onFailure(statusCode, headers, throwable, errorResponse);
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, String responseString, Throwable throwable) {
                super.onFailure(statusCode, headers, responseString, throwable);
            }
        });

        String token = tokenBuf.toString();

        final StringBuilder responseBuf = new StringBuilder();
        if (token.length() > 0) {

            int tries = 0;
            while (tries++ < 100) {

                // These code snippets use an open-source library. http://unirest.io/java
                client.get(getAbsoluteUrl("image_responses/" + token), new JsonHttpResponseHandler() {

                    @Override
                    public void onSuccess(int statusCode, cz.msebera.android.httpclient.Header[] headers, JSONObject response) {

                        try {
                            String status = response.getString("status");
                            if (status.equals("skipped")) {
                                responseBuf.append("unknown");
                            } else if (status.equals("completed") || status.equals("skipped")) {
                                responseBuf.append(response.getString("name"));
                            }
                        } catch (Exception e) {

                        }
                    }
                });

                if (responseBuf.length() > 0) {
                    break;
                }

                try {
                    Thread.sleep(1000);
                } catch (Exception e) {
                }
            }
        }

        return responseBuf.toString();
    }

}