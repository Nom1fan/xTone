package com.database;

import DalObjects.IDAL;

/**
 * Created by Mor on 28/03/2016.
 */
public abstract class DalFactory {

    public static IDAL getCurrentDal() {

        return new MySqlDAL();
    }
}
