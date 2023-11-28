package ca.brocku.geoit;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.view.MenuItem;
import android.widget.ListView;
import android.widget.Toast;

public class ModeSwitcher {
    private static SharedPreferences sp;

    /*
     * Menu class based on what was done in class will show what each menu button click will activate
     */
    public static boolean MenuBar(MenuItem item, Context from) {
        switch (item.getItemId()) {
            case R.id.Home:
                from.startActivity(new Intent(from, MainActivity.class));
                break;
            case R.id.ClearData://clear all databases
                from.startActivity(new Intent(from, MainActivity.class));
                from.deleteDatabase(DataHandler.DB_NAME);
                Toast.makeText(from, "Data Deleted", Toast.LENGTH_SHORT).show();
                break;
        }
        return true;
    }
}

