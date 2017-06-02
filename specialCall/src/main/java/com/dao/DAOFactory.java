package com.dao;

import android.content.Context;

import com.converters.MediaDataConverter;
import com.converters.MediaDataConverterImpl;
import com.utils.MediaFileUtils;
import com.utils.MediaFilesUtilsImpl;
import com.utils.Utility;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by Mor on 31/05/2017.
 */

public abstract class DAOFactory {

    private static Map<Class<? extends DAO>, DAO> class2ObjectMap = new HashMap<Class<? extends DAO>, DAO>() {{
        put(MediaDAO.class, new MediaDAOImpl());
    }};

    public static <T extends DAO> T getDAO(Class<? extends DAO> daoClass) {
        return (T) class2ObjectMap.get(daoClass);
    }

    public static SQLiteDAO getSQLiteDAO(Context context) {
        return SQLiteDAOImpl.getInstance(context);
    }
}
