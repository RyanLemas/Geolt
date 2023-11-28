package ca.brocku.geoit;

import android.Manifest;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.CancellationTokenSource;
import com.google.android.gms.tasks.OnSuccessListener;

import java.util.ArrayList;

import ca.brocku.geoit.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "Main";
    private AppBarConfiguration appBarConfiguration;
    private ActivityMainBinding binding;
    private FusedLocationProviderClient fusedLocation;
    private static final int PERMISSION_FINE_LOCATION = 420;
    GoogleMap map;

    private double lat, lon;    // Doubles for lat and lon
    LatLng currentLocation;     // Current Location

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);

        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        appBarConfiguration = new AppBarConfiguration.Builder(navController.getGraph()).build();
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);

        //fused location
        fusedLocation = LocationServices.getFusedLocationProviderClient(this);
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.tasks, menu);
        return true;
    }


    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        return NavigationUI.navigateUp(navController, appBarConfiguration)
                || super.onSupportNavigateUp();
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        Log.d(TAG, "onOptionsItemSelected: " + item.toString());
        if (item.getItemId() == 16908332) onSupportNavigateUp();
        return ModeSwitcher.MenuBar(item, this);
    }

    /**
     * Manual create tag will get the current lat and lon and create a marker in the sql database.
     */
    public void manualCreateTag(View view) {
        Log.d(TAG, "manualCreateTag: Start Create!");

        // Get a writable database
        DataHandler dh = new DataHandler(this);
        SQLiteDatabase datachanger = dh.getWritableDatabase();

        Log.d(TAG, "manualCreateTag: Check for permissions!");

        // This will check for permissions. If there are no permissions found it will request permissions.
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSION_FINE_LOCATION);
            return;
        }

        // Create a cancellation token
        CancellationTokenSource cts = new CancellationTokenSource();

        // This is the updated current location.
        fusedLocation.getCurrentLocation(100, cts.getToken()).addOnSuccessListener(this, new OnSuccessListener<Location>() {

            // On a success grab a non empty location and create a new tag.
            @Override
            public void onSuccess(@NonNull Location location) {
                if (location != null) {
                    // update the global lat longs.
                    updateLat(location.getLatitude());
                    updateLon(location.getLongitude());
                    Log.d(TAG, "Current Location: Lat: " + location.getLatitude() + " Lon: " + location.getLongitude());
                    Log.d(TAG, "manualCreateTag: Create Lat: " + lat + " Lon: " + lon);

                    //Create a new tag
                    ContentValues newMarker = new ContentValues();
//                    newMarker.put("name", "bleh");
                    newMarker.put("lat", lat);
                    newMarker.put("lon", lon);

                    // insert new marker into the Database.
                    datachanger.insert(DataHandler.DB_TABLE, null, newMarker);
                    datachanger.close();

                    startActivity(new Intent(getBaseContext(), MainActivity.class));
                }
            }
        });
    }


    /**
     * This function will display all the markers in the SQL database.
     */
    public void listMarkers() {
        Log.d(TAG, "listMarkers: Load");
        ArrayList<String> markers = getMarkers();   // get the array of markers.
        ArrayList<Integer> ids = getIDS();          // get the array of IDs.
        ListView list = (ListView) findViewById(R.id.listMarkers);
        // Check if list is null for safety.
        if (list != null) {
            Log.d(TAG, "listMarkers: " + list);

            // Show the data on the list view.
            ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, markers);
            list.setAdapter(adapter);
            Log.d(TAG, "listMarkers: Past Markers");
            registerForContextMenu(list);

            // Create list onclick listeners.
            list.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
            list.setOnItemClickListener(new AdapterView.OnItemClickListener() {

                //Upon clicking a marker from the list view will send user to locationView class
                public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
                    // Debug log
                    Log.d(TAG, "onItemClick: SELECTED ID: " + ids.get(position));

                    // Create an intent with the selected ID.
                    Intent intent = new Intent(MainActivity.this, LocationView.class);
                    intent.putExtra("selectedID", ids.get(position));
                    startActivityForResult(intent, 1);
                }
            });
        }
    }

    /**
     * This function will wait for the result code of the new intents from the list view.
     *
     * @param requestCode
     * @param resultCode
     * @param data
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // If the result code is 1 then refresh the list markers.
        if (requestCode == 1) {
            Log.d(TAG, "onActivityResult: code 1");
            listMarkers();
        }
    }


    /**
     * This function will generate an array list containing all the markers in the sql database.
     *
     * @return Will return an array of all the markers.
     */
    public ArrayList<String> getMarkers() {
        String[] fields = new String[]{"_ID", "name", "lat", "lon"};    // Fields for select.
        ArrayList<String> markers = new ArrayList<>();          // This is the return arraylist.

        // Get a readable database.
        DataHandler dh = new DataHandler(this);
        SQLiteDatabase datareader = dh.getReadableDatabase();

        // Create a cursor to query the database.
        Cursor cursor = datareader.query(DataHandler.DB_TABLE, fields,
                null, null, null, null, null);

        DatabaseUtils dbHelp = new DatabaseUtils();
        Log.d(TAG, "getMarkers: " + dbHelp.dumpCursorToString(cursor));

        // Move the cursor to the start and then iterate through grabbing the data.
        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            if (cursor.getString(1) != null) {
                markers.add(cursor.getString(1) + " \nLat:" + cursor.getString(2) +
                        " \tLong: " + cursor.getString(3));
                Log.d(TAG, "getMarkers: Get " + "Latitude: " + cursor.getString(2) +
                        " Longitude: " + cursor.getString(3));

            } else {
                markers.add("Lat: " + cursor.getString(2) +
                        " \tLong: " + cursor.getString(3));
                Log.d(TAG, "getMarkers: Get " + "Latitude: " + cursor.getString(2) +
                        " Longitude: " + cursor.getString(3));

            }
            cursor.moveToNext();
        }

        // close the cursor once it is done.
        if (cursor != null && !cursor.isClosed()) {
            cursor.close();
        }

        // Close data handler and return array list.
        datareader.close();
        return markers;
    }

    /**
     * This function will generate an array list containing all the IDS in the sql database.
     *
     * @return Will return an array of all IDS.
     */
    public ArrayList<Integer> getIDS() {
        String[] fields = new String[]{"_ID", "name", "lat", "lon"};    // Fields for select.
        ArrayList<Integer> ids = new ArrayList<>();          // This is the return arraylist.

        // Get a readable database.
        DataHandler dh = new DataHandler(this);
        SQLiteDatabase datareader = dh.getReadableDatabase();

        // Create a cursor to query the database.
        Cursor cursor = datareader.query(DataHandler.DB_TABLE, fields,
                null, null, null, null, null);

        DatabaseUtils dbHelp = new DatabaseUtils();

        // Move the cursor to the start and then iterate through grabbing the data.
        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            ids.add(cursor.getInt(0));
            cursor.moveToNext();
        }

        // close the cursor once it is done.
        if (cursor != null && !cursor.isClosed()) {
            cursor.close();
        }

        // Close data handler and return array list.
        datareader.close();
        return ids;
    }

    /**
     * This function will generate an array list containing all the markers in the sql database.
     *
     * @return Will return an array of all the markers.
     */
    public ArrayList<LatLng> getMarkersData() {
        String[] fields = new String[]{"_ID", "name", "lat", "lon"};    // Fields for select.
        ArrayList<LatLng> markers = new ArrayList<>();          // This is the return arraylist.
        LatLng tempAdd;

        // Get a readable database.
        DataHandler dh = new DataHandler(this);
        SQLiteDatabase datareader = dh.getReadableDatabase();

        // Create a cursor to query the database.
        Cursor cursor = datareader.query(DataHandler.DB_TABLE, fields,
                null, null, null, null, null);

        // Move the cursor to the start and then iterate through grabbing the data.
        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            tempAdd = new LatLng(Double.parseDouble(cursor.getString(2)), Double.parseDouble(cursor.getString(3)));
            markers.add(tempAdd);
            Log.d(TAG, "listMarkers: Get " + "Latitude: " + cursor.getString(1) +
                    " Longitude: " + cursor.getString(2));
            cursor.moveToNext();
        }

        // close the cursor once it is done.
        if (cursor != null && !cursor.isClosed()) {
            cursor.close();
        }

        // Close data handler and return array list.
        datareader.close();
        return markers;
    }

    /**
     * This function will show all the markers on a given Google Map.
     */
    public void showMarkers(GoogleMap googleMap) {
        map = googleMap;
        String[] fields = new String[]{"_ID", "name", "lat", "lon"};    // Fields for select.
        LatLng tempAdd;
        String tempName;

        // Get a readable database.
        DataHandler dh = new DataHandler(this);
        SQLiteDatabase datareader = dh.getReadableDatabase();

        // Create a cursor to query the database.
        Cursor cursor = datareader.query(DataHandler.DB_TABLE, fields,
                null, null, null, null, null);

        // Move the cursor to the start and then iterate through grabbing the data.
        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            tempName = cursor.getString(1);
            tempAdd = new LatLng(Double.parseDouble(cursor.getString(2)), Double.parseDouble(cursor.getString(3)));
            if (tempName != null) {
                googleMap.addMarker(new MarkerOptions().position(tempAdd).title(tempName));
            } else {
                googleMap.addMarker(new MarkerOptions().position(tempAdd));
            }
            cursor.moveToNext();
        }

        // close the cursor once it is done.
        if (cursor != null && !cursor.isClosed()) {
            cursor.close();
        }

        Log.d(TAG, "showMarkers: " + currentLocation);
        showCurrentLocation(googleMap);
        // Close data handler and return array list.
        datareader.close();

    }

    /**
     * This function will center the google map on the current location.
     *
     * @param googleMap
     */
    public void showCurrentLocation(GoogleMap googleMap) {
        Log.d(TAG, "updateCurrentLocation: Start");

        // This will check for permissions. If there are no permissions found it will request permissions.
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSION_FINE_LOCATION);
            return;
        }

        // Enable location
        googleMap.setMyLocationEnabled(true);

        // Create a cancellation token
        CancellationTokenSource cts = new CancellationTokenSource();

        // This is the updated current location.
        fusedLocation.getCurrentLocation(100, cts.getToken()).addOnSuccessListener(this, new OnSuccessListener<Location>() {

            // On a success update current location and then move the camera.
            @Override
            public void onSuccess(@NonNull Location location) {
                if (location != null) {
                    currentLocation = new LatLng(location.getLatitude(), location.getLongitude());
                    googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLocation, 9));
                    Log.d(TAG, "onSuccess: Done");
                }
            }
        });
        Log.d(TAG, "updateCurrentLocation: Done");
    }

    /**
     * This will update the global Latitude Variable.
     *
     * @param val
     */
    public void updateLat(double val) {
        Log.d(TAG, "updateLat: " + val);
        lat = val;
    }

    /**
     * This will update the global Longitude Variable.
     *
     * @param val
     */
    public void updateLon(double val) {
        Log.d(TAG, "updateLon: " + val);
        lon = val;
    }

}