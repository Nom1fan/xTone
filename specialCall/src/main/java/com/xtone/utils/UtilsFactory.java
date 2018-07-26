package com.xtone.utils;

import com.xtone.logging.Logger;
import com.xtone.logging.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by Mor on 02/06/2017.
 */

public class UtilsFactory {

    private static final String TAG = UtilsFactory.class.getSimpleName();

    private static final Logger log = LoggerFactory.getLogger();

    private Map<Class<? extends Utility>, Utility> class2ObjectMap;

    private static UtilsFactory instance;

    private UtilsFactory() {
        class2ObjectMap = new HashMap<Class<? extends Utility>, Utility>() {{
            put(BitmapUtils.class, new BitmapUtilsImpl());
            put(ContactsUtils.class, new ContactsUtilsImpl());
            put(StringUtils.class, new StringUtils());
            put(BroadcastUtils.class, new BroadcastUtils());
        }};
    }

    public static UtilsFactory instance() {
        if (instance == null) {
            instance = new UtilsFactory();
        }
        return instance;
    }

    public <T extends Utility> T getUtility(Class<? extends Utility> interfaceClass) {

        Utility utility = class2ObjectMap.get(interfaceClass);
        if (utility == null) {
            log.warn(TAG, String.format("Unable to find utility for of interface:%s", interfaceClass));
            return null;
        }

        return (T) utility;
    }
}
