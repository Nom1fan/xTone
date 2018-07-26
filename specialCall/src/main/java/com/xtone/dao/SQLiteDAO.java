package com.xtone.dao;

import android.content.ContentValues;
import android.database.Cursor;

/**
 * Created by mor on 29/09/2015.
 */
public interface SQLiteDAO {

    //region SQL operators constants
    String AND = "AND";
    String OR = "OR";

    //region Table Downloads
    String TABLE_DOWNLOADS = "downloads"; // Table name
    //region Table keys
    String COL_DOWNLOAD_ID = "download_id";
    String COL_SOURCE_ID = "source_id";
    String COL_DEST_ID = "dest_id";
    String COL_DEST_CONTACT = "dest_contact";
    String COL_EXTENSION = "extension";
    String COL_FILEPATH_ON_SRC_SD = "filepath_on_src_sd";
    String COL_FILETYPE = "file_type";
    String COL_FILESIZE = "file_size";
    String COL_MD5 = "md5";
    String COL_SOURCE_WITH_EXT = "source_with_extension";
    String COL_SOURCE_LOCALE = "source_locale";
    String COL_FILEPATH_ON_SERVER = "filepath_on_server";
    String COL_SPECIAL_MEDIA_TYPE = "special_media_type";
    String COL_COMMID = "comm_id";
    //endregion
    //endregion

    void insertValues(String table, ContentValues values);

    Cursor getAllValues(String table);

    Cursor getValues(String table, String[] whereCols, String[] operators, String[] whereVals);

    Cursor getValues(String table, String whereCol, String whereVal);

    Cursor getValuesRawQuery(String query);

    Cursor getOldestRow(String table, String primaryKeyCol);

    void deleteRow(String table, String whereCol, String whereVal);

    void deleteRow(String table, String[] whereCols, String[] operators, String[] whereVals);
}
