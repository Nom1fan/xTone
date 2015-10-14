package com.dal_objects;

import android.content.Context;

/**
 * Created by mor on 01/10/2015.
 * Generic DAL Manager. Manages all DAL implementations. Exposes static methods for DAL initialization and retrieval, for easy replacement of DAL implementation.
 * To change DAL implementation simply change initialize() and getInstance() methods to desired concrete DAL classes.
 */
public abstract class DAL_Manager {

    public static void initialize(Context context) {

        SQLiteManager.initialize(context);
    }

    public static IDAL getInstance() {

        return SQLiteManager.getInstance();
    }

}
