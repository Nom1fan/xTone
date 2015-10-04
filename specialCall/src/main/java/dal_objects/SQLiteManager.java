package dal_objects;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Created by mor on 29/09/2015.
 */
public class SQLiteManager extends SQLiteOpenHelper implements IDAL {

    private static SQLiteManager _instance;
    private static SQLiteDatabase _writeableDB;
    private static SQLiteDatabase _readableDB;
    private static final String DB_NAME = "GENERAL";

    /* State table constants */
    public static final String STATE_TABLE = "STATE_TABLE"; // Table name
    // Columns
    private static final String STATE_NAME_COLUMN = "State_Name";
    private static final String STATE_VALUE_COLUMN = "State_Value";
    // Keys
    public static final String LOGGED_IN_KEY = "Logged In";


    private SQLiteManager(Context context) {
        super(context, DB_NAME, null, 1);
    }

    /* SQLiteOpenHelper methods */

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS "+ STATE_TABLE +"("+ STATE_NAME_COLUMN +" VARCHAR,"+ STATE_VALUE_COLUMN +" VARCHAR);");
        db.execSQL("INSERT INTO " + STATE_TABLE + " VALUES('" + LOGGED_IN_KEY + "', 0)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }

    /* Manager methods */

    public static void initialize(Context context) {
        if(_instance==null) {
            _instance = new SQLiteManager(context);
            _writeableDB = _instance.getWritableDatabase();
            _readableDB = _instance.getReadableDatabase();
        }
    }

    public static IDAL getInstance() {
        return _instance;
    }

    private void setStringVal(String table, String whereCol, String whereRow ,String updateCol, String val) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(updateCol,val);
        _writeableDB.update(table, contentValues, whereCol + " = ? ", new String[] {whereRow});
    }

    private void setBooleanVal(String table, String whereCol, String whereRow ,String updateCol, boolean val) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(updateCol,val);
        _writeableDB.update(table,contentValues, whereCol + " = ? ",new String[] {whereRow});
    }

    private String getString(String table, String resCol, String whereCol, String val) {
        Cursor cursor = _readableDB.rawQuery("SELECT " + resCol + " FROM " + table + " WHERE " + whereCol + "='" + val + "'",null);
        cursor.moveToFirst();
        return cursor.getString(cursor.getColumnIndex(resCol));
    }

    private boolean getBoolean(String table, String resCol, String whereCol, String val) {
        Cursor cursor = _readableDB.rawQuery("SELECT " + resCol + " FROM " + table + " WHERE " + whereCol + "='" + val + "'", null);
        cursor.moveToFirst();
        return (Integer.parseInt(cursor.getString(cursor.getColumnIndex(resCol))) != 0);
    }

    /* IDAL methods */

    @Override
    public void setLoggedIn(boolean state) {
        setBooleanVal(STATE_TABLE, STATE_NAME_COLUMN, LOGGED_IN_KEY, STATE_VALUE_COLUMN, state);
    }

    @Override
    public boolean getLoggedIn() {
        return getBoolean(STATE_TABLE, STATE_VALUE_COLUMN, STATE_NAME_COLUMN, LOGGED_IN_KEY);
    }
}
