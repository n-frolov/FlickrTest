package com.example.data;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteStatement;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.example.flickrobjects.Photo;
import com.example.flickrtest.FlickrTestApplication;

import java.util.ArrayList;
import java.util.List;

public class Database {

    private static final boolean DEBUG = true;
    private static final String TAG = "Database";

    private static final int DATABASE_VERSION = 1;
    private static OpenHelper openHelper = new OpenHelper();

    private static final String PHOTOS_TABLE = "photos";
    private static final String PHOTOS_ID = "_id";
    private static final String PHOTOS_QUERY = "query";
    private static final String PHOTOS_FLICKR_ID = "flickr_id";
    private static final String PHOTOS_OWNER = "owner";
    private static final String PHOTOS_SECRET = "secret";
    private static final String PHOTOS_SERVER = "server";
    private static final String PHOTOS_FARM = "farm";
    private static final String PHOTOS_TITLE = "title";

    private static class OpenHelper extends SQLiteOpenHelper {

        private OpenHelper() {
            super(FlickrTestApplication.getContext(), "flickrtest.db", null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL("CREATE TABLE " + PHOTOS_TABLE + " (" +
                    PHOTOS_ID + " integer primary key, " +
                    PHOTOS_QUERY + " text, " +
                    PHOTOS_FLICKR_ID + " text, " +
                    PHOTOS_OWNER + " text, " +
                    PHOTOS_SECRET + " text, " +
                    PHOTOS_SERVER + " text, " +
                    PHOTOS_FARM + " text, " +
                    PHOTOS_TITLE + " text)");
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        }
    }

    private static String prepareInsert(String table, String[] columns) {
        StringBuilder sb = new StringBuilder();
        sb.append("INSERT INTO ").append(table).append(" (");
        for (int i = 0; i < columns.length; i++) {
            if (i > 0) sb.append(", ");
            sb.append(columns[i]);
        }
        sb.append(") VALUES (");
        for (int i = 0; i < columns.length; i++) {
            if (i > 0) sb.append(", ");
            sb.append("?");
        }
        sb.append(")");
        return sb.toString();
    }

    private static void bindStatement(SQLiteStatement stmt, String[] values) {
        stmt.clearBindings();
        for (int i = 1; i <= values.length; i++) {
            if (values[i-1] == null)
                stmt.bindNull(i);
            else
                stmt.bindString(i, values[i-1]);
        }
    }

    public static void storePhotos(@NonNull String query, @NonNull List<Photo> photos) {
        SQLiteDatabase db = openHelper.getWritableDatabase();
        if (DEBUG) Log.d(TAG, "storePhotos() " + photos.size());

        SQLiteStatement stmt = db.compileStatement(prepareInsert(PHOTOS_TABLE, new String[] { PHOTOS_QUERY,
                PHOTOS_FLICKR_ID, PHOTOS_OWNER, PHOTOS_SECRET, PHOTOS_SERVER, PHOTOS_FARM, PHOTOS_TITLE }));

        db.beginTransaction();

        try {
            // Delete leftover content for other queries
            db.delete(PHOTOS_TABLE, PHOTOS_QUERY + "!= ?", new String[] { query });

            // Store content for this query.
            // Simplification: it's ineffective to store query like that, it should ideally be a
            // separate table with query and probably timestamp, referenced by a foreign key.
            for (Photo photo : photos) {
                bindStatement(stmt, new String[] { query, photo.getId(), photo.getOwner(), photo.getSecret(),
                        photo.getServer(), photo.getFarm(), photo.getTitle() });
                stmt.executeInsert();
            }
            db.setTransactionSuccessful();

        } finally {
            stmt.close();
            db.endTransaction();
        }
    }

    public static @Nullable List<Photo> fetchPhotos(@NonNull String query) {
        SQLiteDatabase db = openHelper.getWritableDatabase();

        Cursor cursor = db.query(PHOTOS_TABLE, new String[] { PHOTOS_FLICKR_ID, PHOTOS_OWNER, PHOTOS_SECRET,
                PHOTOS_SERVER, PHOTOS_FARM, PHOTOS_TITLE }, PHOTOS_QUERY + "=?", new String[] { query },
                null, null, PHOTOS_ID);

        try {
            if (cursor.moveToFirst()) {
                List<Photo> result = new ArrayList<>();

                int colFlickrId = cursor.getColumnIndex(PHOTOS_FLICKR_ID);
                int colOwner = cursor.getColumnIndex(PHOTOS_OWNER);
                int colSecret = cursor.getColumnIndex(PHOTOS_SECRET);
                int colServer = cursor.getColumnIndex(PHOTOS_SERVER);
                int colFarm = cursor.getColumnIndex(PHOTOS_FARM);
                int colTitle = cursor.getColumnIndex(PHOTOS_TITLE);

                do {
                    String flickrId = cursor.getString(colFlickrId);
                    String owner = cursor.getString(colOwner);
                    String secret = cursor.getString(colSecret);
                    String server = cursor.getString(colServer);
                    String farm = cursor.getString(colFarm);
                    String title = cursor.getString(colTitle);

                    Photo photo = new Photo(flickrId, owner, secret, server, farm, title);
                    result.add(photo);

                } while (cursor.moveToNext());

                if (DEBUG) Log.d(TAG, "fetchPhotos() " + result.size());
                return result;
            }

        } finally {
            cursor.close();
        }

        if (DEBUG) Log.d(TAG, "fetchPhotos() no result");
        return null;
    }

}
