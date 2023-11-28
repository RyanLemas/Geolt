package ca.brocku.geoit;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DataHandler extends SQLiteOpenHelper {
    public static final int DATABASE_VERSION = 2;
    public static final String DB_NAME = "geoit";
    public static final String DB_TABLE = "tags";
    public static final int DB_VERSION = 1;

    private static final String CREATE_TABLE = "CREATE TABLE " + DB_TABLE +
            " (_ID INTEGER PRIMARY KEY, name TEXT, lat REAL, lon REAL);";

    DataHandler(Context context) {super(context, DB_NAME, null, DATABASE_VERSION);}

    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_TABLE);
    }

    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }

}
