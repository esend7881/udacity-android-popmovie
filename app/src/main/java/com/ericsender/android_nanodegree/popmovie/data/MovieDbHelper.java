package com.ericsender.android_nanodegree.popmovie.data;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Created by Eric K. Sender on 9/1/2015.
 */
public class MovieDbHelper extends SQLiteOpenHelper {

    // If you change the database schema, you must increment the database version.
    private static final int DATABASE_VERSION = 2;

    public static final String DATABASE_NAME = "movies.db";
    private final String SQL_CREATE_MOVIE_TABLE;
    private final String SQL_CREATE_POPULAR_TABLE;
    private final String SQL_CREATE_RATING_TABLE;
    private final String SQL_CREATE_FAVORITE_TABLE;

    public MovieDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);

        SQL_CREATE_MOVIE_TABLE = "CREATE TABLE " + MovieContract.MovieEntry.TABLE_NAME + " (" +
                MovieContract.MovieEntry._ID + " INTEGER PRIMARY KEY," +
                MovieContract.MovieEntry.COLUMN_MOVIE_ID + " INTEGER UNIQUE NOT NULL, " +
                MovieContract.MovieEntry.COLUMN_MOVIE_BLOB + " BLOB NOT NULL, " +
                MovieContract.MovieEntry.COLUMN_MOVIE_TRAILERS + " BLOB, " +
                MovieContract.MovieEntry.COLUMN_MOVIE_REVIEWS + " BLOB, " +
                MovieContract.MovieEntry.COLUMN_MOVIE_MINUTES + " INTEGER, " +
                " UNIQUE (" + MovieContract.MovieEntry._ID + ") ON CONFLICT REPLACE);";

        SQL_CREATE_POPULAR_TABLE = "CREATE TABLE " + MovieContract.PopularEntry.TABLE_NAME + " (" +
                MovieContract.PopularEntry._ID + " INTEGER PRIMARY KEY," +
                MovieContract.PopularEntry.COLUMN_MOVIE_ID + " INTEGER UNIQUE NOT NULL, " +
                " FOREIGN KEY (" + MovieContract.PopularEntry.COLUMN_MOVIE_ID + ") REFERENCES " +
                MovieContract.PopularEntry.TABLE_NAME + " (" + MovieContract.MovieEntry.COLUMN_MOVIE_ID + "), " +
                " UNIQUE (" + MovieContract.PopularEntry.COLUMN_MOVIE_ID + ") ON CONFLICT IGNORE);";

        SQL_CREATE_RATING_TABLE = "CREATE TABLE " + MovieContract.RatingEntry.TABLE_NAME + " (" +
                MovieContract.RatingEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                MovieContract.RatingEntry.COLUMN_MOVIE_ID + " INTEGER UNIQUE NOT NULL, " +
                " FOREIGN KEY (" + MovieContract.RatingEntry.COLUMN_MOVIE_ID + ") REFERENCES " +
                MovieContract.RatingEntry.TABLE_NAME + " (" + MovieContract.MovieEntry.COLUMN_MOVIE_ID + "), " +
                " UNIQUE (" + MovieContract.RatingEntry.COLUMN_MOVIE_ID + ") ON CONFLICT IGNORE);";

        SQL_CREATE_FAVORITE_TABLE = "CREATE TABLE " + MovieContract.FavoriteEntry.TABLE_NAME + " (" +
                MovieContract.FavoriteEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                MovieContract.FavoriteEntry.COLUMN_MOVIE_ID + " INTEGER UNIQUE NOT NULL, " +
                " FOREIGN KEY (" + MovieContract.FavoriteEntry.COLUMN_MOVIE_ID + ") REFERENCES " +
                MovieContract.FavoriteEntry.TABLE_NAME + " (" + MovieContract.MovieEntry.COLUMN_MOVIE_ID + "), " +
                " UNIQUE (" + MovieContract.FavoriteEntry.COLUMN_MOVIE_ID + ") ON CONFLICT IGNORE);";
    }

    public void emptyFavorites(SQLiteDatabase db) {
        boolean doClose = false;
        if (db == null) {
            doClose = true;
            db = getWritableDatabase();
        }
        db.beginTransaction();
        try {
            db.execSQL("DROP TABLE IF EXISTS " + MovieContract.FavoriteEntry.TABLE_NAME);
            createFavoriteTable(db);
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
            if (doClose) db.close();
        }
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.beginTransaction();
        try {
            createMovieTable(db);
            createFavoriteTable(db);
            createPopularTable(db);
            createRatingTable(db);
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    public void createMovieTable(SQLiteDatabase db) {
        db.execSQL(SQL_CREATE_MOVIE_TABLE);
    }

    public void createPopularTable(SQLiteDatabase db) {
        db.execSQL(SQL_CREATE_POPULAR_TABLE);
    }

    public void createRatingTable(SQLiteDatabase db) {
        db.execSQL(SQL_CREATE_RATING_TABLE);
    }

    public void createFavoriteTable(SQLiteDatabase db) {
        db.execSQL(SQL_CREATE_FAVORITE_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // This database is only a cache for online data, so its upgrade policy is
        // to simply to discard the data and start over
        // Note that this only fires if you change the version number for your database.
        // It does NOT depend on the version number for your application.
        // If you want to update the schema without wiping data, commenting out the next 2 lines
        // should be your top priority before modifying this method.
        db.execSQL("DROP TABLE IF EXISTS " + MovieContract.MovieEntry.TABLE_NAME);
        db.execSQL("DROP TABLE IF EXISTS " + MovieContract.PopularEntry.TABLE_NAME);
        db.execSQL("DROP TABLE IF EXISTS " + MovieContract.FavoriteEntry.TABLE_NAME);
        db.execSQL("DROP TABLE IF EXISTS " + MovieContract.RatingEntry.TABLE_NAME);
        onCreate(db);
    }
}
