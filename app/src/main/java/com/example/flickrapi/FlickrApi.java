package com.example.flickrapi;

import android.support.annotation.NonNull;

import com.example.flickrobjects.Photo;
import com.example.flickrobjects.SearchResponse;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class FlickrApi {

    private static final String API_URL = "https://api.flickr.com/services/rest/";
    private static final String API_KEY = "3e7cc266ae2b0e0d78e279ce8e361736";

    public static final int RESULTS_PER_SEARCH_PAGE = 30;

    public static @NonNull String urlForPhoto(@NonNull Photo photo) {
        return String.format("http://farm%s.static.flickr.com/%s/%s_%s.jpg",
                photo.getFarm(), photo.getServer(), photo.getId(), photo.getSecret());
    }

    public static @NonNull SearchResponse search(@NonNull String text, boolean safeSearch, int page) throws IOException {
        Map<String, String> params = new HashMap<>();
        params.put("method", "flickr.photos.search");
        params.put("text", text);
        params.put("safe_search", safeSearch ? "1" : "0");
        params.put("page", String.valueOf(page));
        params.put("per_page", String.valueOf(RESULTS_PER_SEARCH_PAGE));

        return runApiRequest(params, SearchResponse.class);
    }

    private static @NonNull <T> T runApiRequest(@NonNull Map<String, String> params, Class<T> typeOfT) throws IOException {
        params.put("api_key", API_KEY);
        params.put("format", "json");
        params.put("nojsoncallback", "1");

        NetworkRequest request = new NetworkRequest(API_URL);
        request.setParams(params);
        return request.requestAsJson(typeOfT);
    }

}
