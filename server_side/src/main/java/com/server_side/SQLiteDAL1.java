package com.server_side;

import com.almworks.sqlite4java.*;
import com.sun.org.apache.xpath.internal.operations.Bool;

import java.io.File;
import java.nio.file.Paths;

import DalObjects.IDAL;

/**
 * Created by Mor on 16/11/2015.
 */
public class SQLiteDAL1 implements IDAL {

    // *** General Database ***
    public static final String GENERAL_DB_PATH = "\\server_side\\Database\\DB.db";

    // Table uid2token
    public static final String TABLE_UID2TOKEN = "uid2token";
    // Table keys under TABLE_UID2TOKEN
    public static final String COL_UID = "Uid";
    public static final String COL_TOKEN = "Token";


    private SQLiteQueue myQueue;

    public SQLiteDAL1(String inputFile) throws SQLiteException {
        SQLiteConnection db = new SQLiteConnection(new File(inputFile));
        db.open(true);

        myQueue = new SQLiteQueue(new File(inputFile));
        myQueue.start();
    }

    public boolean createTable(String tableName, String ... colNames ) throws SQLiteException {

        String query = "CREATE table " + tableName + " (";
        for(int i=0;i<colNames.length;++i) {
            query+=colNames[i];
            if(i+1<colNames.length)
                query+=",";
        }
        query+=")";

        return execQuery(query);

    }

    @Override
    public boolean registerUser(final String uid, final String token) {

        String query = "INSERT INTO " + TABLE_UID2TOKEN + " values (" + "\"" + uid + "\"" + "," + "\"" + token + "\"" + ")";
        return execQuery(query);
    }

    @Override
    public boolean unregisterUser(final String uid) {

        String query = "DELETE FROM "+TABLE_UID2TOKEN+" WHERE "+COL_UID+"="+"\""+uid+"\"";
        return execQuery(query);
    }

    @Override
    public String getUserPushToken(final String uid) {

        return myQueue.execute(new SQLiteJob<String>() {
            protected String job(SQLiteConnection connection) throws SQLiteException {
                String query = "SELECT "+COL_TOKEN+" FROM "+TABLE_UID2TOKEN+" WHERE "+COL_UID+"="+"\""+uid+"\"";
                SQLiteStatement st = null;
                String token = "";
                try {
                    st = connection.prepare(query);
                    if(st.step())
                        token = st.columnString(0);
                } catch (SQLiteException e) {
                    e.printStackTrace();
                }
                finally {
                    if (st != null)
                        st.dispose();
                }
                return token;
            }
        }).complete();
    }

    private boolean execQuery(final String query) {

        return myQueue.execute(new SQLiteJob<Boolean>() {
            protected Boolean job(SQLiteConnection connection) throws SQLiteException {
                Boolean isOK = false;

                try {
                    connection.exec(query);
                    isOK = true;
                } catch (SQLiteException e) {
                    e.printStackTrace();
                }

                return isOK;
            }
        }).complete();

    }


    public static void main(String args[]) {

        String dbPath  = Paths.get("").toAbsolutePath().toString() + GENERAL_DB_PATH;
        try {
            SQLiteDAL1 dal = new SQLiteDAL1(dbPath);
            dal.createTable(TABLE_UID2TOKEN, COL_UID, COL_TOKEN);
            dal.registerUser("000", "token");
            String token = dal.getUserPushToken("000");
            dal.unregisterUser("000");
            token = dal.getUserPushToken("000");
            System.out.println("end");

        } catch (SQLiteException e) {
            e.printStackTrace();
        }
    }
}