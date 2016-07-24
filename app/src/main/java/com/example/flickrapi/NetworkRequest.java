package com.example.flickrapi;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.example.flickrtest.FlickrTestApplication;
import com.google.gson.Gson;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Map;

public class NetworkRequest {

    private static final boolean DEBUG = true;
    private static final String TAG = "NetworkRequest";

    private String url;
    private Map<String, String> params;

    public NetworkRequest(@NonNull String url) {
        this.url = url;
    }

    public void setParams(@Nullable Map<String, String> params) {
        this.params = params;
    }

    public @NonNull <T> T requestAsJson(Class<T> typeOfT) throws IOException {
        String content = requestAsString();

        T result = new Gson().fromJson(content, typeOfT);
        if (result == null)
            throw new IOException("Failed to parse JSON");

        return result;
    }

    public @NonNull String requestAsString() throws IOException {
        Context appContext = FlickrTestApplication.getContext();
        ConnectivityManager cm = (ConnectivityManager)appContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        boolean isConnected = (activeNetwork != null && activeNetwork.isConnectedOrConnecting());
        if (!isConnected)
            throw new IOException("Network is not available");

        StringBuilder urlBuilder = new StringBuilder();
        urlBuilder.append(url);

        char delim = '?';
        for (Map.Entry<String, String> entry : params.entrySet()) {
            urlBuilder.append(delim);
            urlBuilder.append(entry.getKey());
            urlBuilder.append("=");
            urlBuilder.append(URLEncoder.encode(entry.getValue(), "UTF-8"));
            delim = '&';
        }

        if (DEBUG) Log.d(TAG, "GET " + urlBuilder.toString());

        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) new URL(urlBuilder.toString()).openConnection();
            int responseCode = conn.getResponseCode();

            if (DEBUG) Log.d(TAG, "HTTP " + responseCode);

            if (responseCode != 200)
                throw new IOException("HTTP " + responseCode);

            InputStream in = conn.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(in));
            StringBuilder content = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line);
            }
            reader.close();

            if (DEBUG) Log.d(TAG, content.toString());

            return content.toString();

        } finally {
            if (conn != null)
                conn.disconnect();
        }
    }

}
