package com.example.flickrtest;

import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.SearchRecentSuggestions;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.Loader;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.widget.Toast;

import com.example.data.Database;
import com.example.flickrapi.FlickrApi;
import com.example.flickrobjects.Photo;
import com.example.flickrobjects.Photos;
import com.example.flickrobjects.SearchResponse;
import com.example.flickrtest.flickrtest.R;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final boolean DEBUG = true;
    private static final String TAG = "MainActivity";

    private static final String KEY_SEARCHVIEWTEXT = "search_view_text";
    private static final String KEY_QUERYTODISPLAY = "query_to_display";

    private static final int LOADER_ID = 0;

    private PhotosAdapter adapter;
    private boolean loading;
    private boolean hasMore;
    private SearchView searchView;
    private String searchViewText;
    private String queryToDisplay;
    private View progressView;
    private RecyclerView recyclerView;

    @Override
    @SuppressWarnings("ConstantConditions")
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        progressView = findViewById(R.id.progressView);

        adapter = new PhotosAdapter();
        adapter.setLastItemBoundListener(lastItemBoundListener);

        GridLayoutManager layoutManager = new GridLayoutManager(this, 3);

        recyclerView = (RecyclerView) findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(layoutManager);

        if (Intent.ACTION_SEARCH.equals(getIntent().getAction())) {
            searchViewText = getIntent().getStringExtra(SearchManager.QUERY);
            queryToDisplay = searchViewText;
        } else if (savedInstanceState != null) {
            // Text in search view may be different from displayed result set
            searchViewText = savedInstanceState.getString(KEY_SEARCHVIEWTEXT);
            queryToDisplay = savedInstanceState.getString(KEY_QUERYTODISPLAY);
        }

        startLoading();
        getSupportLoaderManager().initLoader(LOADER_ID, null, loaderCallbacks);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            searchViewText = intent.getStringExtra(SearchManager.QUERY);
            queryToDisplay = searchViewText;
            adapter.reset();
            startLoading();
            getSupportLoaderManager().restartLoader(LOADER_ID, null, loaderCallbacks);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main, menu);

        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        searchView = (SearchView) menu.findItem(R.id.search).getActionView();
        searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
        searchView.setIconifiedByDefault(false);
        if (searchViewText != null)
            searchView.setQuery(searchViewText, false);
        searchView.setOnQueryTextListener(queryTextListener);
        searchView.setOnSuggestionListener(suggestionListener);

        return true;
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putString(KEY_SEARCHVIEWTEXT, searchViewText);
        outState.putString(KEY_QUERYTODISPLAY, queryToDisplay);
        super.onSaveInstanceState(outState);
    }

    private LoaderManager.LoaderCallbacks<SearchResponseLoaderResult> loaderCallbacks =
            new LoaderManager.LoaderCallbacks<SearchResponseLoaderResult>() {

        @Override
        public Loader<SearchResponseLoaderResult> onCreateLoader(int id, Bundle args) {
            String query = queryToDisplay;

            // Don't want to just show empty screen, this is a shortcut instead of some "home page"
            if (query == null)
                query = "kittens";

            return new SearchResponseLoader(MainActivity.this, query);
        }

        @Override
        public void onLoadFinished(Loader<SearchResponseLoaderResult> loader, SearchResponseLoaderResult data) {
            if (DEBUG) Log.d(TAG, "onLoadFinished");
            loading = false;
            progressView.setVisibility(View.GONE);
            if (data.error != null) {
                Toast.makeText(MainActivity.this, data.error, Toast.LENGTH_LONG).show();

            } else {
                hasMore = data.hasMore;
                adapter.setItems(data.photos);

                // If we assigned adapter earlier, in scenario where user returns to the app, and there
                // is some data cached in DB, but not in memory - we would assign empty adapter and lose
                // saved scroll position in RecyclerView.
                if (recyclerView.getAdapter() == null)
                    recyclerView.setAdapter(adapter);
            }
        }

        @Override
        public void onLoaderReset(Loader<SearchResponseLoaderResult> loader) {
        }
    };

    private static class SearchResponseLoaderResult {

        public final List<Photo> photos;
        public final boolean hasMore;
        public final String error;

        public SearchResponseLoaderResult(List<Photo> photos, boolean hasMore, String error) {
            this.photos = photos;
            this.hasMore = hasMore;
            this.error = error;
        }
    }

    private static class SearchResponseLoader extends AsyncTaskLoader<SearchResponseLoaderResult> {

        private String query;
        private final List<Photo> photos = new ArrayList<>();

        public SearchResponseLoader(Context context, String query) {
            super(context);
            this.query = query;
        }

        @Override
        protected void onStartLoading() {
            if (photos.size() > 0)
                deliverResult(new SearchResponseLoaderResult(photos, true, null));
            else
                forceLoad();
        }

        @Override
        protected void onReset() {
            if (DEBUG) Log.d(TAG, "onReset()");
            super.onReset();
            photos.clear();
        }

        @Override
        public SearchResponseLoaderResult loadInBackground() {
            if (DEBUG) Log.d(TAG, "loadInBackground()");

            if (TextUtils.isEmpty(query))
                return new SearchResponseLoaderResult(photos, false, null);

            if (photos.size() == 0) {
                // In case the app is reopened on the same query, load that last result set from DB
                // Skipped: since result set is volatile, we need to also expire this data after a while.
                List<Photo> fromDB = Database.fetchPhotos(query);
                if (fromDB != null && fromDB.size() > 0) {
                    photos.addAll(fromDB);
                    return new SearchResponseLoaderResult(photos, true, null);
                }
            }

            try {
                // Load results from network
                // Page number here is guessed, because we don't store it in DB. But also the result
                // on Flickr can change between our requests, so it's very approximate anyway.
                int page = (photos.size() / FlickrApi.RESULTS_PER_SEARCH_PAGE) + 1;

                SearchResponse response = FlickrApi.search(query, true, page);
                Photos photosContainer = response.getPhotos();
                boolean hasMore = false;
                if (photosContainer != null) {
                    hasMore = (photosContainer.getPage() < photosContainer.getPages());
                    List<Photo> newPhotos = photosContainer.getPhoto();
                    if (newPhotos != null) {
                        // Add to the list of photos
                        photos.addAll(newPhotos);
                        // Store in DB
                        Database.storePhotos(query, newPhotos);
                    }
                }

                return new SearchResponseLoaderResult(photos, hasMore, null);

            } catch (Exception ex) {
                String msg = ex.getMessage();
                if (msg == null)
                    msg = ex.toString();
                return new SearchResponseLoaderResult(photos, true, msg);
            }
        }
    }

    private void startLoading() {
        loading = true;
        progressView.setVisibility(View.VISIBLE);
    }

    private PhotosAdapter.OnLastItemBoundListener lastItemBoundListener = new PhotosAdapter.OnLastItemBoundListener() {
        @Override
        public void onLastItemBound() {
            // Request loader update unless it's already in progress
            if (!loading && hasMore) {
                if (DEBUG) Log.d(TAG, "onLastItemBound calling onContentChanged");
                startLoading();
                getSupportLoaderManager().getLoader(LOADER_ID).onContentChanged();
            }
        }
    };

    private SearchView.OnQueryTextListener queryTextListener = new SearchView.OnQueryTextListener() {

        @Override
        public boolean onQueryTextChange(String newText) {
            // Save updated text so it can be restored on config change
            searchViewText = newText;
            return false;
        }

        @Override
        public boolean onQueryTextSubmit(String query) {
            // Save to recent queries
            SearchRecentSuggestions suggestions = new SearchRecentSuggestions(MainActivity.this,
                    SuggestionsProvider.AUTHORITY, SuggestionsProvider.MODE);
            suggestions.saveRecentQuery(query, null);

            searchView.clearFocus();
            return false;
        }
    };

    private SearchView.OnSuggestionListener suggestionListener = new SearchView.OnSuggestionListener() {

        @Override
        public boolean onSuggestionSelect(int position) {
            return false;
        }

        @Override
        public boolean onSuggestionClick(int position) {
            // Prevent SearchView from opening suggestions again when a suggestion has been selected
            Cursor cursor = (Cursor) searchView.getSuggestionsAdapter().getItem(position);
            String suggestion = cursor.getString(cursor.getColumnIndex(SearchManager.SUGGEST_COLUMN_TEXT_1));
            searchView.setQuery(suggestion, true);
            return true;
        }
    };

}
