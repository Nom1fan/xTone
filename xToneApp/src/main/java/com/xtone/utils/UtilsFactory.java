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

    private Map<Class<? extends Utility>, Class<? extends Utility>> class2ImplMap;

    private static UtilsFactory instance;

    private UtilsFactory() {
        class2ImplMap = new HashMap<Class<? extends Utility>, Class<? extends Utility>>() {{
            put(BitmapUtils.class, BitmapUtilsImpl.class);
            put(ContactsUtils.class, ContactsUtilsImpl.class);
            put(StringUtils.class, StringUtils.class);
            put(BroadcastUtils.class, BroadcastUtils.class);
            put(SharedPrefUtils.class, SharedPrefUtils.class);
            put(CallSessionUtils.class, CallSessionUtilsImpl.class);
            put(Phone2MediaUtils.class, Phone2MediaUtilsImpl.class);
            put(PermissionsUtils.class, PermissionsUtils.class);
            put(StandOutWindowUtils.class, StandOutWindowUtilsImpl.class);
        }};
    }

    public static UtilsFactory instance() {
        if (instance == null) {
            instance = new UtilsFactory();
        }
        return instance;
    }

    public <T extends Utility> T getUtility(Class<? extends Utility> interfaceClass) {
        try {

            Class<? extends Utility> utilClass = class2ImplMap.get(interfaceClass);
            if (utilClass == null) {
                log.warn(TAG, String.format("Unable to find utility for of:[%s]", interfaceClass));
                return null;
            }

            return (T) utilClass.newInstance();
        } catch (Exception e) {
            log.error(TAG, String.format("Failed to instantiate util class:[%s]", interfaceClass));
            return null;
        }
    }
}
