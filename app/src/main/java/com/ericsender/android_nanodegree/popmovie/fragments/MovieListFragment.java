package com.ericsender.android_nanodegree.popmovie.fragments;

import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcelable;
import android.preference.PreferenceManager;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.ericsender.android_nanodegree.popmovie.R;
import com.ericsender.android_nanodegree.popmovie.activities.DetailsActivity;
import com.ericsender.android_nanodegree.popmovie.adapters.GridViewAdapter;
import com.ericsender.android_nanodegree.popmovie.application.PopMoviesApplication;
import com.ericsender.android_nanodegree.popmovie.data.MovieContract;
import com.ericsender.android_nanodegree.popmovie.data.MovieDbHelper;
import com.ericsender.android_nanodegree.popmovie.parcelable.MovieGridObj;
import com.ericsender.android_nanodegree.popmovie.utils.Utils;
import com.google.gson.internal.LinkedTreeMap;

import org.apache.commons.lang3.SerializationUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.json.JSONObject;

import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.ericsender.android_nanodegree.popmovie.application.STATE.REFRESH_GRID;

/**
 * A placeholder fragment containing a simple view.
 */
public class MovieListFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {

    public static final String LOG_TAG = MovieListFragment.class.getSimpleName();
    private ArrayAdapter<MovieGridObj> mMovieAdapter;
    private List<MovieGridObj> mMovieList = new ArrayList<>();
    private GridViewAdapter mGridViewAdapter;
    private GridView mMovieGridView;
    private String mCurrSortOrder;
    private MovieListFragment mThis;

    private String getCurrentSortPref() {
        return PreferenceManager
                .getDefaultSharedPreferences(getActivity())
                .getString(getString(R.string.pref_sort_order_key),
                        getString(R.string.most_popular_val));
    }

    private String getApiSortPref() {
        String sort = getCurrentSortPref();

        if (sort.equals(getString(R.string.most_popular_val)))
            return getString(R.string.tmdb_arg_popularity);
        else if (sort.equals(getString(R.string.highest_rated_val)))
            return getString(R.string.tmdb_arg_highestrating);
        else //if (sort.equals(getString(R.string.favorite_val)))
            return getString(R.string.tmdb_arg_favorite);
        // else throw new RuntimeException("Sort order value is not known: " + sort);
    }

    private void setTitle() {
        // getActivity().setTitle(getString(R.string.title_activity_main) + " - " + getCurrentSortPref());
    }

    public MovieListFragment() {
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelableArrayList(getString(R.string.GRIDVIEW_LIST_KEY), (ArrayList<? extends Parcelable>) mMovieList);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null)
            mMovieList = (List<MovieGridObj>) savedInstanceState.get(getString(R.string.GRIDVIEW_LIST_KEY));
        else {
            getActivity().getContentResolver().delete(MovieContract.PopularEntry.buildPopularUri(), null, null);
            getActivity().getContentResolver().delete(MovieContract.RatingEntry.buildRatingUri(), null, null);
        }
        // Add this line in order for this fragment to handle menu events.
        setHasOptionsMenu(true);
    }


    @Override
    public void onResume() {
        String foo = getCurrentSortPref();
        Log.d(getClass().getSimpleName(), "onResume with Sort =  " + foo);
        // If a change to the sort order is seen, resort the gridview and redistplay
        PopMoviesApplication Me = ((PopMoviesApplication) getActivity().getApplication());
        AtomicBoolean refreshGrid = (AtomicBoolean) Me.getStateManager().getState(REFRESH_GRID);
        Log.d(LOG_TAG, "Forced RefreshGrid status is: " + refreshGrid);
        if (refreshGrid.get() || !foo.equals(mCurrSortOrder)) { // This will also be true on inital loading.
            mCurrSortOrder = foo;
            Log.d(getClass().getSimpleName(), "Sorting on: " + mCurrSortOrder);
            Bundle b = new Bundle();
            b.putString("sort", foo);
            getLoaderManager().initLoader(0, b, this);
            setTitle();
            refreshGrid.set(false);
        }
        super.onResume();
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.movie_list_fragment, container, false);
        mMovieGridView = (GridView) rootView.findViewById(R.id.movie_grid);
        mGridViewAdapter = new GridViewAdapter(getActivity(), null, 0);
        mMovieGridView.setAdapter(mGridViewAdapter);
        // mMovieAdapter = new ArrayAdapter<String>(getActivity(), R.layout.grid_movie_posters,
        createGridItemClickCallbacks();
        Bundle b = new Bundle();
        b.putString("sort", getApiSortPref());
        getLoaderManager().initLoader(0, b, this);
        mThis = this;
        return rootView;
    }

    private void createGridItemClickCallbacks() {
        //Grid view click event
        mMovieGridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
                //Get item at position
                Cursor cursor = (Cursor) parent.getItemAtPosition(position);
                if (cursor != null) {
                    byte[] b = cursor.getBlob(1);
                    Parcelable item = SerializationUtils.deserialize(b);
                    Intent intent = new Intent(getActivity(), DetailsActivity.class);
                    ImageView imageView = (ImageView) v.findViewById(R.id.grid_item_image);

                    // Interesting data to pass across are the thumbnail size/location, the
                    // resourceId of the source bitmap, the picture description, and the
                    // orientation (to avoid returning back to an obsolete configuration if
                    // the device rotates again in the meantime)

                    int[] screenLocation = new int[2];
                    imageView.getLocationOnScreen(screenLocation);

                    //Pass the image title and url to DetailsActivity
                    // TODO: instead of putting the full movieObj in here, send just the movie_id and use a loader on the fragment to get the rest of the data.
                    intent.putExtra("left", screenLocation[0])
                            .putExtra("top", screenLocation[1])
                            .putExtra(getString(R.string.movie_id_key), ((MovieGridObj) item).id.longValue())
                            .putExtra(getString(R.string.movie_obj_key), item);

                    //Start details activity
                    startActivity(intent);
                } else
                    Toast.makeText(getActivity(), "No Movie Selected!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case R.id.action_refresh:
                Log.d(LOG_TAG, "Refreshing!");
                Bundle b = new Bundle();
                b.putString("sort", getApiSortPref());
                b.putBoolean("refresh", true);
                getLoaderManager().initLoader(0, b, this);
                return true;
//            case R.id.action_sort:
//                Log.d(LOG_TAG, "Sort Spinner");
//                handleSortSpinner();
//                return true;
        }
        return super.onOptionsItemSelected(item);
    }

//      TODO: implement as an optional bonus.
//    private void handleSortSpinner() {
//        final Spinner spinner = (Spinner) getActivity().findViewById(R.id.sort_spinner);
//        final ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(getActivity(),
//                R.array.pref_sort_order_entries, android.R.layout.simple_spinner_item);
//        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
//        spinner.setAdapter(adapter);
//        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
//            @Override
//            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
//                Log.d(LOG_TAG, Utils.f("onItemSelected parent (%s), view (%s), position (%s), id (%s)", parent, view, position, id));
//                // parent.removeView(spinner);
//            }
//
//            @Override
//            public void onNothingSelected(AdapterView<?> parent) {
//            }
//        });
//    }

    private Uri determineUri(String sort) {
        if (StringUtils.containsIgnoreCase(sort, "popular"))
            return MovieContract.PopularEntry.buildPopularUri();
        else if (StringUtils.containsIgnoreCase(sort, "vote"))
            return MovieContract.RatingEntry.buildRatingUri();
        else if (StringUtils.containsIgnoreCase(sort, "fav"))
            return MovieContract.FavoriteEntry.buildFavoriteUri();
        else
            throw new UnsupportedOperationException("Sort not identified: " + sort);
    }

    private void insertMovieListIntoDatabase(final String sort) {
        MovieDbHelper dbHelper = new MovieDbHelper(getActivity());
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues[] movie_ids = new ContentValues[mMovieList.size()];
        ContentValues[] cvs = new ContentValues[mMovieList.size()];
        int i = 0;
        try {
            for (MovieGridObj obj : mMovieList) {
                long movie_id = obj.id;
                byte[] blob = SerializationUtils.serialize(obj);
                ContentValues movieCv = new ContentValues();
                ContentValues idCv = new ContentValues();
                movieCv.put(MovieContract.MovieEntry.COLUMN_MOVIE_ID, movie_id);
                movieCv.put(MovieContract.MovieEntry.COLUMN_MOVIE_BLOB, blob);
                idCv.put(MovieContract.MovieEntry.COLUMN_MOVIE_ID, movie_id);
                cvs[i] = movieCv;
                movie_ids[i++] = idCv;
            }
            getActivity().getContentResolver().bulkInsert(MovieContract.MovieEntry.buildMovieUri(), cvs);
            // Deleted whatever is in rating/poppular
            if (StringUtils.containsIgnoreCase(sort, "rate") ||
                    StringUtils.containsIgnoreCase(sort, "popular")) {
                Uri uri = determineUri(sort);
                getActivity().getContentResolver().delete(uri, null, null);
                getActivity().getContentResolver().bulkInsert(uri, movie_ids);
            }
            Log.d(LOG_TAG, String.format("Just inserted movies %s", Arrays.toString(cvs)));
        } finally {
            db.close();
        }
    }

    private void getInternalData(Cursor cursor, String sort) {
        List<MovieGridObj> lMaps = new ArrayList<>();
        while (cursor.moveToNext()) {
            Long movie_id = cursor.getLong(0);
            byte[] bMovieObj = cursor.getBlob(1);
            MovieGridObj movieObj = (MovieGridObj) SerializationUtils.deserialize(bMovieObj);
            lMaps.add(movieObj);
        }
        mMovieList.clear();
        mMovieList = lMaps;
        mGridViewAdapter.setGridData(mMovieList);
    }

    private void getLiveDataAndCallLoader(final String sort) {
        if (isFav(sort))
            Snackbar.make(getView(), "Cannot Refresh When Sorting Preference is Favorites. Please choose '"
                    + getString(R.string.most_popular_title) + "' or '"
                    + getString(R.string.highest_rated_title)
                    + "'", Snackbar.LENGTH_SHORT).show();
        RequestQueue queue = Volley.newRequestQueue(getActivity());
        Uri builtUri = Uri.parse(getString(R.string.tmdb_api_base_discover_url)).buildUpon()
                .appendQueryParameter(getString(R.string.tmdb_param_sortby), sort)
                .appendQueryParameter(getString(R.string.tmdb_param_api), getString(R.string.private_tmdb_api))
                .build();
        String url = "";
        try {
            url = new URL(builtUri.toString()).toString();
        } catch (MalformedURLException e) {
            Log.e(getClass().getSimpleName(), e.getMessage(), e);
            // Toast.makeText(getActivity(), "Malformed URL " + e.getMessage(), Toast.LENGTH_SHORT).show();
            return;
        }

        Log.d(getClass().getSimpleName(), "updateMovieListVolley() - url = " + url);// .substring(0, url.length() - 16));
        final Toast t = Toast.makeText(getActivity(), "Loading Data...", Toast.LENGTH_LONG);
        final StopWatch sw = new StopWatch();
        t.show();
        sw.start();
        JsonObjectRequest jsObjRequest = new JsonObjectRequest
                (Request.Method.GET, url, (String) null, new Response.Listener<JSONObject>() {

                    @Override
                    public void onResponse(JSONObject response) {
                        Log.d(getClass().getSimpleName(), "Response received.");
                        LinkedTreeMap<String, Serializable> map = Utils.getGson().fromJson(response.toString(), LinkedTreeMap.class);
                        handleMap(map, sort);
                        insertMovieListIntoDatabase(sort);
                        t.setText("Loading Finished in: " + sw);
                        Bundle b = new Bundle();
                        b.putString("sort", sort);
                        getLoaderManager().initLoader(0, b, mThis);
                    }

                }, new Response.ErrorListener() {

                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.e(getClass().getSimpleName(), error.getMessage(), error);
                        t.setText(String.format("Error connecting to server (%s) in: %s", error.getMessage(), sw));
                        //Toast.makeText(getActivity(), "Error connecting to server.", Toast.LENGTH_SHORT).show();
                    }
                });
        queue.add(jsObjRequest);
    }

    private boolean isFav(String sort) {
        return getString(R.string.tmdb_arg_favorite).equals(sort);
    }

    private void handleMap(LinkedTreeMap<String, Serializable> map, String sort) {
        mMovieList = Utils.covertMapToMovieObjList(map);
        Log.d(getClass().getSimpleName(), "Received a set of movies. Registering them.");
        mGridViewAdapter.setGridData(mMovieList);
    }

    @Override
    public android.support.v4.content.Loader<Cursor> onCreateLoader(int id, Bundle args) {
        final String sort = args.getString("sort");
        Uri uri = determineUri(sort);
        Boolean isRefresh = args.getBoolean("refresh");
        if (isRefresh) {
            getLiveDataAndCallLoader(sort);
            return null;
        } else
            return new CursorLoader(getActivity(), uri, null, null, null, null);
    }

    @Override
    public void onLoadFinished(android.support.v4.content.Loader<Cursor> loader, Cursor data) {
        String sort = getApiSortPref();

        if (!isFav(sort) && !data.moveToFirst())
            getLiveDataAndCallLoader(sort);
        if (isFav(sort) && !data.moveToFirst())
            Snackbar.make(getView(), "Cannot Refresh When Sorting Preference is Favorites. Please choose '"
                    + getString(R.string.most_popular_title) + "' or '"
                    + getString(R.string.highest_rated_title)
                    + "'", Snackbar.LENGTH_SHORT).show();
        else
            mGridViewAdapter.swapCursor(data);
        // TODO: Implement position
//        if (mPosition != GridView.INVALID_POSITION) {
//            mMovieGridView.smoothScrollToPosition(mPosition);
//        }
    }

    @Override
    public void onLoaderReset(android.support.v4.content.Loader<Cursor> loader) {

    }
}

