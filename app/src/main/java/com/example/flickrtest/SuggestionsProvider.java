package com.example.flickrtest;

import android.content.SearchRecentSuggestionsProvider;

public class SuggestionsProvider extends SearchRecentSuggestionsProvider {

    public final static String AUTHORITY = "com.example.flickrtest.SuggestionsProvider";
    public final static int MODE = DATABASE_MODE_QUERIES;

    public SuggestionsProvider() {
        setupSuggestions(AUTHORITY, MODE);
    }
}