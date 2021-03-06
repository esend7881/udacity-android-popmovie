/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.ericsender.android_nanodegree.popmovie.com.ericsender.android_nanodegree.popmovie.data;

import android.content.ComponentName;
import android.content.ContentValues;
import android.content.pm.PackageManager;
import android.content.pm.ProviderInfo;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Build;
import android.test.AndroidTestCase;

import com.ericsender.android_nanodegree.popmovie.data.MovieContract;
import com.ericsender.android_nanodegree.popmovie.data.MovieDbHelper;
import com.ericsender.android_nanodegree.popmovie.data.MovieProvider;
import com.google.gson.internal.LinkedTreeMap;

import org.apache.commons.lang3.SerializationUtils;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;


/*
    Note: This is not a complete set of tests of the Sunshine ContentProvider, but it does test
    that at least the basic functionality has been implemented correctly.

    Students: Uncomment the tests in this class as you implement the functionality in your
    ContentProvider to make sure that you've implemented things reasonably correctly.
 */
public class TestProvider extends AndroidTestCase {

    public static final String LOG_TAG = TestProvider.class.getSimpleName();
    private long curr_movie_id;
    private byte[] expected_reviews, expected_trailer;
    private int expected_mins;

    /*
       This helper function deletes all records from both database tables using the ContentProvider.
       It also queries the ContentProvider to make sure that the database has been successfully
       deleted, so it cannot be used until the Query and Delete functions have been written
       in the ContentProvider.

       Students: Replace the calls to deleteAllRecordsFromDB with this one after you have written
       the delete functionality in the ContentProvider.
     */
    public void deleteAllRecordsFromProvider() {
        mContext.getContentResolver().delete(
                MovieContract.MovieEntry.CONTENT_URI,
                null,
                null
        );
        mContext.getContentResolver().delete(
                MovieContract.FavoriteEntry.CONTENT_URI,
                null,
                null
        );

        Cursor cursor = mContext.getContentResolver().query(
                MovieContract.MovieEntry.CONTENT_URI,
                null,
                null,
                null,
                null
        );
        assertEquals("Error: Records not deleted from Weather table during delete", 0, cursor.getCount());
        cursor.close();

        cursor = mContext.getContentResolver().query(
                MovieContract.FavoriteEntry.CONTENT_URI,
                null,
                null,
                null,
                null
        );
        assertEquals("Error: Records not deleted from Location table during delete", 0, cursor.getCount());
        cursor.close();
    }

    /*
        Student: Refactor this function to use the deleteAllRecordsFromProvider functionality once
        you have implemented delete functionality there.
     */
    public void deleteAllRecords() {
        deleteAllRecordsFromProvider();
    }

    // Since we want each test to start with a clean slate, run deleteAllRecords
    // in setUp (called by the test runner before each test).
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        deleteAllRecords();
    }

    /*
        This test checks to make sure that the content provider is registered correctly.
        Students: Uncomment this test to make sure you've correctly registered the MovieProvider.
     */
    public void testProviderRegistry() {
        PackageManager pm = mContext.getPackageManager();

        // We define the component name based on the package name from the context and the
        // MovieProvider class.
        ComponentName componentName = new ComponentName(mContext.getPackageName(),
                MovieProvider.class.getName());
        try {
            // Fetch the provider info using the component name from the PackageManager
            // This throws an exception if the provider isn't registered.
            ProviderInfo providerInfo = pm.getProviderInfo(componentName, 0);

            // Make sure that the registered authority matches the authority from the Contract.
            assertEquals("Error: MovieProvider registered with authority: " + providerInfo.authority +
                            " instead of authority: " + MovieContract.CONTENT_AUTHORITY,
                    providerInfo.authority, MovieContract.CONTENT_AUTHORITY);
        } catch (PackageManager.NameNotFoundException e) {
            // I guess the provider isn't registered correctly.
            assertTrue("Error: MovieProvider not registered at " + mContext.getPackageName(),
                    false);
        }
    }

    /*
            This test doesn't touch the database.  It verifies that the ContentProvider returns
            the correct type for each type of URI that it can handle.
            Students: Uncomment this test to verify that your implementation of GetType is
            functioning correctly.
         */
    public void testGetType() {
        // content://com.example.android.sunshine.app/weather/
        String type = mContext.getContentResolver().getType(MovieContract.MovieEntry.CONTENT_URI);
        // vnd.android.cursor.dir/com.example.android.sunshine.app/weather
        assertEquals("Content Type",
                MovieContract.MovieEntry.CONTENT_TYPE, type);

        Map<Long, ContentValues> raw = TestUtilities.createSortedMovieValues(getContext(), "popular");
        Long movie_id = (Long) raw.values().toArray(new ContentValues[0])[0].get(MovieContract.MovieEntry.COLUMN_MOVIE_ID);
        // content://com.example.android.sunshine.app/movie/#
        type = mContext.getContentResolver().getType(
                MovieContract.MovieEntry.buildUri(movie_id));
        // vnd.android.cursor.dir/com.example.android.sunshine.app/weather
        assertEquals("Content Type", MovieContract.MovieEntry.CONTENT_ITEM_TYPE, type);

        // content://com.example.android.sunshine.app/movie/favorite
        type = mContext.getContentResolver().getType(
                MovieContract.FavoriteEntry.buildUri());
        // vnd.android.cursor.item/com.example.android.sunshine.app/weather/1419120000
        assertEquals("Error: the MovieContract.MovieEntry CONTENT_URI with location and date should return MovieContract.MovieEntry.CONTENT_ITEM_TYPE",
                MovieContract.FavoriteEntry.CONTENT_TYPE, type);

    }


    /*
        This test uses the database directly to insert and then uses the ContentProvider to
        read out the data.  Uncomment this test to see if the basic weather query functionality
        given in the ContentProvider is working correctly.
     */
    public void testBasicMovieQuery() {
        // insert our test records into the database

        Map<Long, ContentValues> listContentValues = TestUtilities.createSortedMovieValues(getContext(), "popular");
        Map<Long, Long> locationRowIds = TestUtilities.insertMovieRow(mContext, listContentValues);


        // Test the basic content provider query
        Cursor movieCursor = mContext.getContentResolver().query(
                MovieContract.MovieEntry.CONTENT_URI,
                null,
                null,
                null,
                null
        );

        // Make sure we get the correct cursor out of the database
        TestUtilities.validateMovieCursor(movieCursor, listContentValues);
    }

    /*
        This test uses the database directly to insert and then uses the ContentProvider to
        read out the data.  Uncomment this test to see if your location queries are
        performing correctly.
     */
    public void testBasicFavoriteQueries() {
        //first insert movies:
        Map<Long, ContentValues> listContentValues = TestUtilities.createSortedMovieValues(getContext(), "popular");
        Map<Long, Long> locationRowIds = TestUtilities.insertMovieRow(mContext, listContentValues);

        // Insert random favorites
        SQLiteDatabase db = new MovieDbHelper(getContext()).getWritableDatabase();
        Set<Long> insertedMoviedIds = new HashSet<>();
        try {
            db.beginTransaction();
            while (insertedMoviedIds.isEmpty())
                for (Map.Entry<Long, ContentValues> e : listContentValues.entrySet())
                    insertedMoviedIds.add(TestUtilities.generateRandomFavoritesAndInsert(db, e.getValue()));
            insertedMoviedIds.remove(null);
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
            db.close();
        }


        // Test the basic content provider query
        Cursor favCursor = mContext.getContentResolver().query(
                MovieContract.FavoriteEntry.CONTENT_URI,
                null,
                null,
                null,
                null
        );

        // Make sure we get the correct cursor out of the database
        TestUtilities.validateFavoritesCursor(favCursor, listContentValues, insertedMoviedIds);

        // Has the NotificationUri been set correctly? --- we can only test this easily against API
        // level 19 or greater because getNotificationUri was added in API level 19.
        if (Build.VERSION.SDK_INT >= 19) {
            assertEquals("Error: Favoriate Query did not properly set NotificationUri",
                    favCursor.getNotificationUri(), MovieContract.FavoriteEntry.buildUri());
        }
    }

    // TODO: Implement requesting of both types of data, filling both Ratings and Popular table
    // TODO: Implement changing sort order of screen using this data source.

    public void testAddingPopularMoviesToTable() {
        mContext.getContentResolver().delete(MovieContract.PopularEntry.CONTENT_URI, null, null);
        Map<Long, ContentValues> listContentValues = TestUtilities.createSortedMovieValues(getContext(), "popular");
        ContentValues[] arr = (ContentValues[]) listContentValues.values().toArray(new ContentValues[0]);

        mContext.getContentResolver().bulkInsert(MovieContract.MovieEntry.CONTENT_URI, arr);

        ContentValues[] movie_ids = new ContentValues[arr.length];
        for (int i = 0; i < arr.length; i++)
            (movie_ids[i] = new ContentValues()).put(MovieContract.PopularEntry.COLUMN_MOVIE_ID,
                    arr[i].getAsLong(MovieContract.MovieEntry.COLUMN_MOVIE_ID));

        mContext.getContentResolver().bulkInsert(MovieContract.PopularEntry.buildUri(), movie_ids);

        TestUtilities.verifyPopularValuesInDatabase(listContentValues, mContext);
    }

    public void testAddingRatedMoviesToTable() {
        mContext.getContentResolver().delete(MovieContract.RatingEntry.CONTENT_URI, null, null);
        Map<Long, ContentValues> listContentValues = TestUtilities.createSortedMovieValues(getContext(), "rating");

        ContentValues[] arr = (ContentValues[]) listContentValues.values().toArray(new ContentValues[0]);

        mContext.getContentResolver().bulkInsert(MovieContract.MovieEntry.CONTENT_URI, arr);

        ContentValues[] movie_ids = new ContentValues[arr.length];
        for (int i = 0; i < arr.length; i++)
            (movie_ids[i] = new ContentValues()).put(MovieContract.RatingEntry.COLUMN_MOVIE_ID,
                    arr[i].getAsLong(MovieContract.MovieEntry.COLUMN_MOVIE_ID));

        mContext.getContentResolver().bulkInsert(MovieContract.RatingEntry.buildUri(), movie_ids);

        TestUtilities.verifyRatingValuesInDatabase(listContentValues, mContext);
    }

    public void testAddingFavoriteMoviesToTable() {
        mContext.getContentResolver().delete(MovieContract.FavoriteEntry.CONTENT_URI, null, null);
        Map<Long, ContentValues> listContentValues = TestUtilities.createSortedMovieValues(getContext(), "popular");

        ContentValues[] arr = (ContentValues[]) listContentValues.values().toArray(new ContentValues[0]);

        mContext.getContentResolver().bulkInsert(MovieContract.MovieEntry.CONTENT_URI, arr);

        ContentValues[] movie_ids = new ContentValues[arr.length];
        for (int i = 0; i < arr.length; i++)
            (movie_ids[i] = new ContentValues()).put(MovieContract.RatingEntry.COLUMN_MOVIE_ID,
                    arr[i].getAsLong(MovieContract.MovieEntry.COLUMN_MOVIE_ID));

        mContext.getContentResolver().bulkInsert(MovieContract.FavoriteEntry.buildUri(), movie_ids);

        TestUtilities.verifyFavoriteValuesInDatabase(listContentValues, mContext);
    }

    public void testGettingMovieAndMaybeFavorite() {
        mContext.getContentResolver().delete(MovieContract.FavoriteEntry.CONTENT_URI, null, null);
        Map<Long, ContentValues> listContentValues = TestUtilities.createSortedMovieValues(getContext(), "popular");

        ContentValues[] arr = (ContentValues[]) listContentValues.values().toArray(new ContentValues[0]);

        mContext.getContentResolver().bulkInsert(MovieContract.MovieEntry.CONTENT_URI, arr);

        ContentValues[] movie_ids = new ContentValues[arr.length];
        for (int i = 0; i < arr.length; i++)
            (movie_ids[i] = new ContentValues()).put(MovieContract.RatingEntry.COLUMN_MOVIE_ID,
                    arr[i].getAsLong(MovieContract.MovieEntry.COLUMN_MOVIE_ID));

        mContext.getContentResolver().bulkInsert(MovieContract.FavoriteEntry.CONTENT_URI, movie_ids);

        TestUtilities.verifyFavoriteValuesInDatabase(listContentValues, mContext);

        Long expected = movie_ids[0].getAsLong(MovieContract.MovieEntry.COLUMN_MOVIE_ID);
        Cursor c = mContext.getContentResolver().query(MovieContract.MovieEntry.buildUriUnionFavorite(expected),
                null, null, null, null);

        assertTrue(c.moveToFirst());
        assertEquals(2, c.getCount());
        assertEquals(expected.longValue(), c.getLong(0));
        assertTrue(c.moveToNext());
        assertTrue(c.getBlob(1).length > 0);
        c.close();

        mContext.getContentResolver().delete(MovieContract.FavoriteEntry.buildUri(), MovieContract.FavoriteEntry.COLUMN_MOVIE_ID + "=?", new String[]{expected.toString()});

        c = mContext.getContentResolver().query(MovieContract.MovieEntry.buildUriUnionFavorite(expected),
                null, null, null, null);

        assertTrue(c.moveToFirst());
        assertEquals(1, c.getCount());
        assertTrue(c.getBlob(1).length > 0);
    }

    public void testAddingReviewToMovieRow() {
        final TestDb testDb = new TestDb();
        TestUtilities.insertMovies(testDb, mContext);
        testAddingPopularMoviesToTable(); // add the popular movies
        LinkedTreeMap<String, Serializable> listContentValues = TestUtilities.getDataAsMap(getContext(), "review");
        curr_movie_id = Double.valueOf(listContentValues.get("id").toString()).longValue();
        Serializable reviews = listContentValues.get("results");
        expected_reviews = SerializationUtils.serialize(reviews);
        assertTrue(expected_reviews.length > 0);
        ContentValues cv = new ContentValues();
        cv.put(MovieContract.MovieEntry.COLUMN_MOVIE_REVIEWS, expected_reviews);
        Uri uri = MovieContract.MovieEntry.buildUriReviews(curr_movie_id);
        String selection = MovieContract.MovieEntry.TABLE_NAME + "." + MovieContract.MovieEntry.COLUMN_MOVIE_ID + "=?";
        String[] selectionArgs = new String[]{Long.valueOf(curr_movie_id).toString()};
        assertNotNull(mContext.getContentResolver().update(uri, cv, selection, selectionArgs));
    }

    public void testGettingReviews() {
        testAddingReviewToMovieRow();
        Cursor c = mContext.getContentResolver().query(MovieContract.MovieEntry.buildUriReviews(curr_movie_id),
                null, null, null, null);
        assertTrue("cursor returned for movie id = " + curr_movie_id, c.moveToFirst());
        byte[] blob = c.getBlob(1);
        assertNotNull("ensure object returned", blob);
        assertEquals(expected_reviews.length, blob.length);
        for (int i = 0; i < expected_reviews.length; i++)
            assertEquals(expected_reviews[i], blob[i]);
        c.close();
    }

    public void testAddingTrailersToMovieRow() {
        final TestDb testDb = new TestDb();
        TestUtilities.insertMovies(testDb, mContext);
        testAddingPopularMoviesToTable(); // add the popular movies
        LinkedTreeMap<String, Serializable> listContentValues = TestUtilities.getDataAsMap(getContext(), "trailer");
        curr_movie_id = Double.valueOf(listContentValues.get("id").toString()).longValue();
        Serializable trailers = listContentValues.get("results");
        expected_trailer = SerializationUtils.serialize(trailers);
        assertTrue(expected_trailer.length > 0);
        ContentValues cv = new ContentValues();
        cv.put(MovieContract.MovieEntry.COLUMN_MOVIE_TRAILERS, expected_trailer);
        Uri uri = MovieContract.MovieEntry.buildUriTrailers(curr_movie_id);
        String selection = MovieContract.MovieEntry.TABLE_NAME + "." + MovieContract.MovieEntry.COLUMN_MOVIE_ID + "=?";
        String[] selectionArgs = new String[]{Long.valueOf(curr_movie_id).toString()};
        assertNotNull(mContext.getContentResolver().update(uri, cv, selection, selectionArgs));
    }

    public void testGettingTrailers() {
        testAddingTrailersToMovieRow();
        Cursor c = mContext.getContentResolver().query(MovieContract.MovieEntry.buildUriTrailers(curr_movie_id),
                null, null, null, null);
        assertTrue("cursor returned for movie id = " + curr_movie_id, c.moveToFirst());
        byte[] blob = c.getBlob(1);
        assertNotNull("ensure object returned", blob);
        assertEquals(expected_trailer.length, blob.length);
        for (int i = 0; i < expected_trailer.length; i++)
            assertEquals(expected_trailer[i], blob[i]);
        c.close();
    }

    public void testAddingMinutesToMovieRow() {
        final TestDb testDb = new TestDb();
        TestUtilities.insertMovies(testDb, mContext);
        testAddingPopularMoviesToTable(); // add the popular movies
        LinkedTreeMap<String, Serializable> listContentValues = TestUtilities.getDataAsMap(getContext(), "minute");
        curr_movie_id = Double.valueOf(listContentValues.get("id").toString()).longValue();
        expected_mins = Double.valueOf(listContentValues.get("runtime").toString()).intValue();
        assertTrue(expected_mins > 0);
        ContentValues cv = new ContentValues();
        cv.put(MovieContract.MovieEntry.COLUMN_MOVIE_MINUTES, expected_mins);
        Uri uri = MovieContract.MovieEntry.buildUriMinutes(curr_movie_id);
        String selection = MovieContract.MovieEntry.TABLE_NAME + "." + MovieContract.MovieEntry.COLUMN_MOVIE_ID + "=?";
        String[] selectionArgs = new String[]{Long.valueOf(curr_movie_id).toString()};
        assertNotNull(mContext.getContentResolver().update(uri, cv, selection, selectionArgs));
    }

    public void testGettingMinutes() {
        testAddingMinutesToMovieRow();
        Cursor c = mContext.getContentResolver().query(MovieContract.MovieEntry.buildUriMinutes(curr_movie_id),
                null, null, null, null);
        assertTrue(c.moveToFirst());
        int mins = c.getInt(1);
        assertEquals(expected_mins, mins);
        c.close();
    }

//    /*
//        This test uses the provider to insert and then update the data. Uncomment this test to
//        see if your update location is functioning correctly.
//     */
//    public void testUpdateLocation() {
//        // Create a new map of values, where column names are the keys
//        ContentValues values = TestUtilities.createNorthPoleLocationValues();
//
//        Uri locationUri = mContext.getContentResolver().
//                insert(MovieContract.FavoriteEntry.CONTENT_URI, values);
//        long locationRowId = ContentUris.parseId(locationUri);
//
//        // Verify we got a row back.
//        assertTrue(locationRowId != -1);
//        Log.d(LOG_TAG, "New row id: " + locationRowId);
//
//        ContentValues updatedValues = new ContentValues(values);
//        updatedValues.put(MovieContract.FavoriteEntry._ID, locationRowId);
//        updatedValues.put(MovieContract.FavoriteEntry.COLUMN_CITY_NAME, "Santa's Village");
//
//        // Create a cursor with observer to make sure that the content provider is notifying
//        // the observers as expected
//        Cursor locationCursor = mContext.getContentResolver().query(MovieContract.FavoriteEntry.CONTENT_URI, null, null, null, null);
//
//        TestUtilities.TestContentObserver tco = TestUtilities.getTestContentObserver();
//        locationCursor.registerContentObserver(tco);
//
//        int count = mContext.getContentResolver().update(
//                MovieContract.FavoriteEntry.CONTENT_URI, updatedValues, MovieContract.FavoriteEntry._ID + "= ?",
//                new String[]{Long.toString(locationRowId)});
//        assertEquals(count, 1);
//
//        // Test to make sure our observer is called.  If not, we throw an assertion.
//        //
//        // Students: If your code is failing here, it means that your content provider
//        // isn't calling getContext().getContentResolver().notifyChange(uri, null);
//        tco.waitForNotificationOrFail();
//
//        locationCursor.unregisterContentObserver(tco);
//        locationCursor.close();
//
//        // A cursor is your primary interface to the query results.
//        Cursor cursor = mContext.getContentResolver().query(
//                MovieContract.FavoriteEntry.CONTENT_URI,
//                null,   // projection
//                MovieContract.FavoriteEntry._ID + " = " + locationRowId,
//                null,   // Values for the "where" clause
//                null    // sort order
//        );
//
//        TestUtilities.validateMovieCursor("testUpdateLocation.  Error validating location entry update.",
//                cursor, updatedValues);
//
//        cursor.close();
//    }
//
//
//    // Make sure we can still delete after adding/updating stuff
//    //
//    // Student: Uncomment this test after you have completed writing the insert functionality
//    // in your provider.  It relies on insertions with testInsertReadProvider, so insert and
//    // query functionality must also be complete before this test can be used.
//    public void testInsertReadProvider() {
//        ContentValues testValues = TestUtilities.createNorthPoleLocationValues();
//
//        // Register a content observer for our insert.  This time, directly with the content resolver
//        TestUtilities.TestContentObserver tco = TestUtilities.getTestContentObserver();
//        mContext.getContentResolver().registerContentObserver(MovieContract.FavoriteEntry.CONTENT_URI, true, tco);
//        Uri locationUri = mContext.getContentResolver().insert(MovieContract.FavoriteEntry.CONTENT_URI, testValues);
//
//        // Did our content observer get called?  Students:  If this fails, your insert location
//        // isn't calling getContext().getContentResolver().notifyChange(uri, null);
//        tco.waitForNotificationOrFail();
//        mContext.getContentResolver().unregisterContentObserver(tco);
//
//        long locationRowId = ContentUris.parseId(locationUri);
//
//        // Verify we got a row back.
//        assertTrue(locationRowId != -1);
//
//        // Data's inserted.  IN THEORY.  Now pull some out to stare at it and verify it made
//        // the round trip.
//
//        // A cursor is your primary interface to the query results.
//        Cursor cursor = mContext.getContentResolver().query(
//                MovieContract.FavoriteEntry.CONTENT_URI,
//                null, // leaving "columns" null just returns all the columns.
//                null, // cols for "where" clause
//                null, // values for "where" clause
//                null  // sort order
//        );
//
//        TestUtilities.validateMovieCursor("testInsertReadProvider. Error validating MovieContract.FavoriteEntry.",
//                cursor, testValues);
//
//        // Fantastic.  Now that we have a location, add some weather!
//        ContentValues weatherValues = TestUtilities.createWeatherValues(locationRowId);
//        // The TestContentObserver is a one-shot class
//        tco = TestUtilities.getTestContentObserver();
//
//        mContext.getContentResolver().registerContentObserver(MovieContract.MovieEntry.CONTENT_URI, true, tco);
//
//        Uri weatherInsertUri = mContext.getContentResolver()
//                .insert(MovieContract.MovieEntry.CONTENT_URI, weatherValues);
//        assertTrue(weatherInsertUri != null);
//
//        // Did our content observer get called?  Students:  If this fails, your insert weather
//        // in your ContentProvider isn't calling
//        // getContext().getContentResolver().notifyChange(uri, null);
//        tco.waitForNotificationOrFail();
//        mContext.getContentResolver().unregisterContentObserver(tco);
//
//        // A cursor is your primary interface to the query results.
//        Cursor weatherCursor = mContext.getContentResolver().query(
//                MovieContract.MovieEntry.CONTENT_URI,  // Table to Query
//                null, // leaving "columns" null just returns all the columns.
//                null, // cols for "where" clause
//                null, // values for "where" clause
//                null // columns to group by
//        );
//
//        TestUtilities.validateMovieCursor("testInsertReadProvider. Error validating MovieContract.MovieEntry insert.",
//                weatherCursor, weatherValues);
//
//        // Add the location values in with the weather data so that we can make
//        // sure that the join worked and we actually get all the values back
//        weatherValues.putAll(testValues);
//
//        // Get the joined Weather and Location data
//        weatherCursor = mContext.getContentResolver().query(
//                MovieContract.MovieEntry.buildWeatherLocation(TestUtilities.TEST_LOCATION),
//                null, // leaving "columns" null just returns all the columns.
//                null, // cols for "where" clause
//                null, // values for "where" clause
//                null  // sort order
//        );
//        TestUtilities.validateMovieCursor("testInsertReadProvider.  Error validating joined Weather and Location Data.",
//                weatherCursor, weatherValues);
//
//        // Get the joined Weather and Location data with a start date
//        weatherCursor = mContext.getContentResolver().query(
//                MovieContract.MovieEntry.buildWeatherLocationWithStartDate(
//                        TestUtilities.TEST_LOCATION, TestUtilities.TEST_DATE),
//                null, // leaving "columns" null just returns all the columns.
//                null, // cols for "where" clause
//                null, // values for "where" clause
//                null  // sort order
//        );
//        TestUtilities.validateMovieCursor("testInsertReadProvider.  Error validating joined Weather and Location Data with start date.",
//                weatherCursor, weatherValues);
//
//        // Get the joined Weather data for a specific date
//        weatherCursor = mContext.getContentResolver().query(
//                MovieContract.MovieEntry.buildWeatherLocationWithDate(TestUtilities.TEST_LOCATION, TestUtilities.TEST_DATE),
//                null,
//                null,
//                null,
//                null
//        );
//        TestUtilities.validateMovieCursor("testInsertReadProvider.  Error validating joined Weather and Location data for a specific date.",
//                weatherCursor, weatherValues);
//    }
//
//    // Make sure we can still delete after adding/updating stuff
//    //
//    // Student: Uncomment this test after you have completed writing the delete functionality
//    // in your provider.  It relies on insertions with testInsertReadProvider, so insert and
//    // query functionality must also be complete before this test can be used.
//    public void testDeleteRecords() {
//        testInsertReadProvider();
//
//        // Register a content observer for our location delete.
//        TestUtilities.TestContentObserver locationObserver = TestUtilities.getTestContentObserver();
//        mContext.getContentResolver().registerContentObserver(MovieContract.FavoriteEntry.CONTENT_URI, true, locationObserver);
//
//        // Register a content observer for our weather delete.
//        TestUtilities.TestContentObserver weatherObserver = TestUtilities.getTestContentObserver();
//        mContext.getContentResolver().registerContentObserver(MovieContract.MovieEntry.CONTENT_URI, true, weatherObserver);
//
//        deleteAllRecordsFromProvider();
//
//        // Students: If either of these fail, you most-likely are not calling the
//        // getContext().getContentResolver().notifyChange(uri, null); in the ContentProvider
//        // delete.  (only if the insertReadProvider is succeeding)
//        locationObserver.waitForNotificationOrFail();
//        weatherObserver.waitForNotificationOrFail();
//
//        mContext.getContentResolver().unregisterContentObserver(locationObserver);
//        mContext.getContentResolver().unregisterContentObserver(weatherObserver);
//    }
//
//
//    static private final int BULK_INSERT_RECORDS_TO_INSERT = 10;
//
//    static ContentValues[] createBulkInsertWeatherValues(long locationRowId) {
//        long currentTestDate = TestUtilities.TEST_DATE;
//        long millisecondsInADay = 1000 * 60 * 60 * 24;
//        ContentValues[] returnContentValues = new ContentValues[BULK_INSERT_RECORDS_TO_INSERT];
//
//        for (int i = 0; i < BULK_INSERT_RECORDS_TO_INSERT; i++, currentTestDate += millisecondsInADay) {
//            ContentValues weatherValues = new ContentValues();
//            weatherValues.put(MovieContract.MovieEntry.COLUMN_MOVIE_ID, locationRowId);
//            weatherValues.put(MovieContract.MovieEntry.COLUMN_MOVIE_BLOB, currentTestDate);
//            returnContentValues[i] = weatherValues;
//        }
//        return returnContentValues;
//    }
//
//    // Student: Uncomment this test after you have completed writing the BulkInsert functionality
//    // in your provider.  Note that this test will work with the built-in (default) provider
//    // implementation, which just inserts records one-at-a-time, so really do implement the
//    // BulkInsert ContentProvider function.
//    public void testBulkInsert() {
//        // first, let's create a location value
//        ContentValues testValues = TestUtilities.createNorthPoleLocationValues();
//        Uri locationUri = mContext.getContentResolver().insert(MovieContract.FavoriteEntry.CONTENT_URI, testValues);
//        long locationRowId = ContentUris.parseId(locationUri);
//
//        // Verify we got a row back.
//        assertTrue(locationRowId != -1);
//
//        // Data's inserted.  IN THEORY.  Now pull some out to stare at it and verify it made
//        // the round trip.
//
//        // A cursor is your primary interface to the query results.
//        Cursor cursor = mContext.getContentResolver().query(
//                MovieContract.FavoriteEntry.CONTENT_URI,
//                null, // leaving "columns" null just returns all the columns.
//                null, // cols for "where" clause
//                null, // values for "where" clause
//                null  // sort order
//        );
//
//        TestUtilities.validateMovieCursor("testBulkInsert. Error validating MovieContract.FavoriteEntry.",
//                cursor, testValues);
//
//        // Now we can bulkInsert some weather.  In fact, we only implement BulkInsert for weather
//        // entries.  With ContentProviders, you really only have to implement the features you
//        // use, after all.
//        ContentValues[] bulkInsertContentValues = createBulkInsertWeatherValues(locationRowId);
//
//        // Register a content observer for our bulk insert.
//        TestUtilities.TestContentObserver weatherObserver = TestUtilities.getTestContentObserver();
//        mContext.getContentResolver().registerContentObserver(MovieContract.MovieEntry.CONTENT_URI, true, weatherObserver);
//
//        int insertCount = mContext.getContentResolver().bulkInsert(MovieContract.MovieEntry.CONTENT_URI, bulkInsertContentValues);
//
//        // Students:  If this fails, it means that you most-likely are not calling the
//        // getContext().getContentResolver().notifyChange(uri, null); in your BulkInsert
//        // ContentProvider method.
//        weatherObserver.waitForNotificationOrFail();
//        mContext.getContentResolver().unregisterContentObserver(weatherObserver);
//
//        assertEquals(insertCount, BULK_INSERT_RECORDS_TO_INSERT);
//
//        // A cursor is your primary interface to the query results.
//        cursor = mContext.getContentResolver().query(
//                MovieContract.MovieEntry.CONTENT_URI,
//                null, // leaving "columns" null just returns all the columns.
//                null, // cols for "where" clause
//                null, // values for "where" clause
//                MovieContract.MovieEntry.COLUMN_DATE + " ASC"  // sort order == by DATE ASCENDING
//        );
//
//        // we should have as many records in the database as we've inserted
//        assertEquals(cursor.getCount(), BULK_INSERT_RECORDS_TO_INSERT);
//
//        // and let's make sure they match the ones we created
//        cursor.moveToFirst();
//        for (int i = 0; i < BULK_INSERT_RECORDS_TO_INSERT; i++, cursor.moveToNext()) {
//            TestUtilities.validateMovieCurrentRecord("testBulkInsert.  Error validating MovieContract.MovieEntry " + i,
//                    cursor, bulkInsertContentValues[i]);
//        }
//        cursor.close();
//    }
}
