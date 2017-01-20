package com.dal.objects;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Created by mor on 29/09/2015.
 */
public class SQLiteManager extends SQLiteOpenHelper implements IDAL {

    private static final String TAG = SQLiteManager.class.getSimpleName();
    private static SQLiteManager _instance;
    private static final String DB_NAME = "GENERAL";

    //endregion
    //endregion


    private SQLiteManager(Context context) {
        super(context, DB_NAME, null, 1);
    }

    //region SQLiteOpenHelper methods
    @Override
    public void onCreate(SQLiteDatabase db) {

        StringBuilder query = new StringBuilder("CREATE TABLE IF NOT EXISTS ");
        query.
                append(TABLE_DOWNLOADS).
                append("(").
                append(COL_DOWNLOAD_ID)         .append(" INTEGER PRIMARY KEY").append(",").
                append(COL_SOURCE_ID)           .append(" VARCHAR").append(",").
                append(COL_DEST_ID)             .append(" VARCHAR").append(",").
                append(COL_DEST_CONTACT)        .append(" VARCHAR").append(",").
                append(COL_EXTENSION)           .append(" VARCHAR").append(",").
                append(COL_FILEPATH_ON_SRC_SD)  .append(" VARCHAR").append(",").
                append(COL_FILETYPE)            .append(" VARCHAR").append(",").
                append(COL_FILESIZE)            .append(" INTEGER").append(",").
                append(COL_MD5)                 .append(" VARCHAR").append(",").
                append(COL_SOURCE_WITH_EXT)     .append(" VARCHAR").append(",").
                append(COL_SOURCE_LOCALE)       .append(" VARCHAR").append(",").
                append(COL_FILEPATH_ON_SERVER)  .append(" VARCHAR").append(",").
                append(COL_SPECIAL_MEDIA_TYPE)  .append(" VARCHAR").append(",").
                append(COL_COMMID)              .append(" INT").
                append(");");

        db.execSQL(query.toString());
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }

//    @Override
//    public void finalize() { //TODO Due to comment text in superlcass implementation we might consider changing this override with regular method and call it at the end of each DB action
//
//        if(_instance!=null)
//            _instance.close();
//        if(_db!=null)
//            _db.close();
//
//        try {
//            super.finalize();
//        } catch (Throwable throwable) {
//            throwable.printStackTrace();
//        }
//    }
    //endregion

    //region Manager methods

    public static IDAL getInstance(Context context) {

        if(_instance==null)
            _instance = new SQLiteManager(context);
        return _instance;
    }

    private void setStringVal(String table, String whereCol, String whereRow ,String updateCol, String val) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put(updateCol,val);
        db.update(table, contentValues, whereCol + " = ? ", new String[] {whereRow});
    }

    private void setBooleanVal(String table, String whereCol, String whereRow ,String updateCol, boolean val) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put(updateCol,val);
        db.update(table,contentValues, whereCol + " = ? ",new String[] {whereRow});
    }

    private String getString(String table, String resCol, String whereCol, String val) {
        SQLiteDatabase db = getWritableDatabase();
        Cursor cursor = db.rawQuery("SELECT " + resCol + " FROM " + table + " WHERE " + whereCol + "='" + val + "'",null);
        cursor.moveToFirst();
        return cursor.getString(cursor.getColumnIndex(resCol));
    }

    private boolean getBoolean(String table, String resCol, String whereCol, String val) {
        SQLiteDatabase db = getWritableDatabase();
        Cursor cursor = db.rawQuery("SELECT " + resCol + " FROM " + table + " WHERE " + whereCol + "='" + val + "'", null);
        cursor.moveToFirst();
        return (Integer.parseInt(cursor.getString(cursor.getColumnIndex(resCol))) != 0);
    }
    //endregion

    //region IDAL methods
    @Override
    public void insertValues(String table, ContentValues values) {
        SQLiteDatabase db = getWritableDatabase();
        db.insert(table, null, values);
    }

    @Override
    public Cursor getAllValues(String table) {
        SQLiteDatabase db = getWritableDatabase();
        String query = String.format("SELECT * FROM %s", table);
        return db.rawQuery(query, null);
    }

    @Override
    public Cursor getValues(String table, String[] whereCols, String[] operators, String[] whereVals) {
        SQLiteDatabase db = getWritableDatabase();
        StringBuilder whereClause = new StringBuilder();

        for(int i=0; i < whereCols.length; ++i) {

            whereClause.
                    append(whereCols[i]).
                    append("=? ").
                    append(i < operators.length ? operators[i] + " " : "");
        }

        return db.rawQuery("SELECT * FROM " + table + " WHERE " + whereClause, whereVals);
    }

    @Override
    public Cursor getValues(String table, String whereCol, String whereVal) {
        SQLiteDatabase db = getWritableDatabase();
        return db.rawQuery("SELECT * FROM " + table + " WHERE " + whereCol + "=?", new String[] { whereVal });
    }

    @Override
    public Cursor getValuesRawQuery(String query) {
        SQLiteDatabase db = getWritableDatabase();
        return db.rawQuery(query, null);
    }

    @Override
    public Cursor getOldestRow(String table, String primaryKeyCol) {
        SQLiteDatabase db = getWritableDatabase();
        String query = String.format("SELECT * FROM %1$s WHERE %2$s = (SELECT MIN(%2$s) FROM %1$s)", table, primaryKeyCol);
        return db.rawQuery(query, null);
    }

    @Override
    public void deleteRow(String table, String[] whereCols, String[] operators, String[] whereVals) {

        SQLiteDatabase db = getWritableDatabase();
        StringBuilder whereClause = new StringBuilder();


        for(int i=0; i < whereCols.length; ++i) {

            whereClause.
                    append(whereCols[i]).
                    append("=? ").
                    append(i < operators.length ? operators[i] + " " : "");
        }
        db.delete(table, whereClause.toString(), whereVals);
    }

    @Override
    public void deleteRow(String table, String whereCol, String whereVal) {
        SQLiteDatabase db = getWritableDatabase();
        db.delete(table, whereCol + "=?", new String[] { whereVal });
    }

    //endregion
}
