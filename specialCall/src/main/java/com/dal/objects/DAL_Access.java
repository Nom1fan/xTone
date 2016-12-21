package com.dal.objects;

import android.content.Context;

/**
 * Generic DAL access. Manages all DAL implementations access. Exposes static methods for DAL retrieval, for easy replacement of DAL implementation.
 * To change DAL implementation simply change getInstance() method to desired concrete DAL classes.
 */
public abstract class DAL_Access {

    public static IDAL getInstance(Context context) {

        return SQLiteManager.getInstance(context);
    }

}
