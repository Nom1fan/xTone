package com.database;

/**
 * Created by Mor on 28/03/2016.
 */
public abstract class DaoFactory {

    public static IDAO getCurrentDao() {

        return new MySqlDAO();
    }
}
