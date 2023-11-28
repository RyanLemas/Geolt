package ca.brocku.geoit;

import static java.lang.Math.sqrt;

import android.Manifest;
import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.navigation.ui.AppBarConfiguration;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.CancellationTokenSource;
import com.google.android.gms.tasks.OnSuccessListener;

import java.text.DecimalFormat;
import java.util.ArrayList;

/*
* Displays information of the marker that was selected by the user
 */

public class LocationView extends AppCompatActivity {
    EditText editName, editLat, editLng, editDis, editTime;
    Button remove, rename;
    TextView nameTitle;
    private String name;
    private int selectedID;
    double currentlat, currentlng, locationLat, locationLng, distanceLat, distanceLng; //users location values, marked location values, distance between user and location values
    private static final String TAG = "Main";
    private static final int PERMISSION_FINE_LOCATION = 420;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_location_view);
        editName = (EditText) findViewById(R.id.listName);
        editLat = (EditText) findViewById(R.id.listLat);
        editLng = (EditText) findViewById(R.id.listLong);
        remove = (Button) findViewById(R.id.remove);
        rename = (Button) findViewById(R.id.rename);
        nameTitle = (TextView) findViewById(R.id.nameTitle);
        editDis = (EditText) findViewById(R.id.distance);

        Bundle sel = getIntent().getExtras();
        //gets id value that was sent from mainActivity
        if (sel != null) {
            selectedID = sel.getInt("selectedID");
        }

        getTag(selectedID);

        list();
        showDistance();

        // Add back button support for the location view.
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

    }

    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.tasks, menu);
        return true;
    }


    public boolean onOptionsItemSelected(MenuItem item) {
        Log.d(TAG, "onOptionsItemSelected: " + item.toString());
        // If the back button is pushed then just finish the activity returning to last activity.
        // return a result so that the find it page will refresh.
        if (item.getItemId() == 16908332) {
            setResult(1);
            finish();
        }
        return ModeSwitcher.MenuBar(item, this);
    }


    /**
     * This function will fill in the edit texts
     */
    public void list() {
        editName.setText(name);

        editLat.setText(Double.toString(locationLat));
        editLng.setText(Double.toString(locationLng));
    }

    /**
    * Removes the selected marker from database
    */
    public void remove(View view) {
        DataHandler dh = new DataHandler(this);
        SQLiteDatabase dataEdit = dh.getWritableDatabase();

        dataEdit.delete(DataHandler.DB_TABLE, "_ID = " + selectedID, null);
        dataEdit.close();

        editLat.setText("");
        editLng.setText("");
        editName.setText("");
        editDis.setText("");
        Toast.makeText(LocationView.this, "Location removed", Toast.LENGTH_SHORT).show();
        finish();
    }

    /**
     * This function will update the current name for the location view.
     *
     * @param view
     */
    public void SaveName(View view) {
        // This will get the current string from the edit text
        String n = editName.getText().toString();

        // check for non equal string.
        if (n.equals("")) {
            Toast.makeText(LocationView.this, "No name has been set", Toast.LENGTH_SHORT).show();
        } else {
            // If valid name get an writable database and update.
            DataHandler dh = new DataHandler(this);
            SQLiteDatabase data = dh.getWritableDatabase();

            ContentValues values = new ContentValues();
            values.put("name", n);

            data.update(DataHandler.DB_TABLE, values, "_ID = " + selectedID, null);
            data.close();

            // Update UI and debug log.
            Log.d(TAG, "SaveName: Update   - Name: " + n);
            editName.setText(n);
        }

    }


    /**
    * This function will calculate distance between user's current location and the marked location
    *
    */
    public void showDistance() {
        Log.d(TAG, "updateDistance&Time: Start");

        // This will check for permissions. If there are no permissions found it will request permissions.
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSION_FINE_LOCATION);
            return;
        }

        // Create a cancellation token
        CancellationTokenSource cts = new CancellationTokenSource();

        // This is the updated current location.
        FusedLocationProviderClient fusedLocation = LocationServices.getFusedLocationProviderClient(this);
        fusedLocation.getCurrentLocation(100, cts.getToken()).addOnSuccessListener(this, new OnSuccessListener<Location>() {

            // On a success update current location and then calculate distance
            @Override
            public void onSuccess(@NonNull Location location) {
                if (location != null) {
                    currentlat = location.getLatitude();
                    currentlng = location.getLongitude();

                    //Haversine formula for distance
                    final int R = 6371;

                    distanceLat = Math.toRadians(locationLat - currentlat);
                    distanceLng = Math.toRadians(locationLng - currentlng);
                    double a = (Math.sin(distanceLat / 2) * Math.sin(distanceLat / 2)) + Math.cos(Math.toRadians(currentlat)) * Math.cos(Math.toRadians(locationLat)) * (Math.sin(distanceLng / 2) * Math.sin(distanceLng / 2));
                    double c;
                    double distance;

                    //checks that no negative value are square rooted
                    if (a < 0) {
                        c = 2 * Math.atan2((sqrt(-1 * a)) * -1, sqrt(1 - (-1 * a)));
                    } else {
                        c = 2 * Math.atan2(sqrt(a), sqrt(1 - a));
                    }

                    distance = (R * c);

                    //incase of negative distance
                    if (distance < 0) {
                        distance = -1 * (R * c);
                    }

                    //displays distance with only 2 decimals
                    String disTemp = new DecimalFormat("##.##").format(distance);
                    editDis.setText(disTemp + "km");
                }
            }
        });
        Log.d(TAG, "updateDistance&Time: Done");

    }


    /**
     * This function will update the current selected tag information based off of an ID.
     *
     * @param id This is the provided SQLite _ID
     */
    public void getTag(int id) {
        String[] fields = new String[]{"_ID", "name", "lat", "lon"};    // Fields for select.
        ArrayList<String> markers = new ArrayList<>();          // This is the return arraylist.

        // Get a readable database.
        DataHandler dh = new DataHandler(this);
        SQLiteDatabase datareader = dh.getReadableDatabase();

        // Create a cursor to query the database.
        Cursor cursor = datareader.query(DataHandler.DB_TABLE, fields,
                "_ID = " + id, null, null, null, null);

        DatabaseUtils dbHelp = new DatabaseUtils();

        // Move the cursor to the start and then iterate through grabbing the data.
        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            Log.d(TAG, "getTag: ID: " + cursor.getInt(0) + " name: " +
                    cursor.getString(1) + " lat: " + cursor.getString(2) +
                    " lon: " + cursor.getString(3));
            name = cursor.getString(1);
            locationLat = cursor.getDouble(2);
            locationLng = cursor.getDouble(3);
            cursor.moveToNext();
        }

        // close the cursor once it is done.
        if (cursor != null && !cursor.isClosed()) {
            cursor.close();
        }

        // Close data handler and return array list.
        datareader.close();
    }

    /**
     * Send user to google maps to find the directions from user current location to marked location
     *
     * @param view
     */
    public void navigate(View view) {
        Intent intent = new Intent(android.content.Intent.ACTION_VIEW,
                Uri.parse("http://maps.google.com/maps?saddr=" + currentlat + "," + currentlng + "&daddr=" + locationLat + "," + locationLng));
        startActivity(intent);
    }

}