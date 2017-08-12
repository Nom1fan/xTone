package com.utils;

import com.converters.MediaDataConverter;
import com.converters.MediaDataConverterImpl;
import com.logger.Logger;
import com.logger.LoggerFactory;
import com.model.request.Request;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by Mor on 02/06/2017.
 */

public class UtilityFactory {

    private static final String TAG = UtilityFactory.class.getSimpleName();

    private Map<Class<? extends Utility>, Class<? extends Utility>> class2ObjectMap;

    private static Logger logger = LoggerFactory.getLogger();

    private static UtilityFactory instance;

    private UtilityFactory() {
        class2ObjectMap = new HashMap<Class<? extends Utility>, Class<? extends Utility>>() {{
            put(MediaFileUtils.class, MediaFilesUtilsImpl.class);
            put(MediaDataConverter.class, MediaDataConverterImpl.class);
            put(AlarmUtils.class, AlarmUtilsImpl.class);
            put(BitmapUtils.class, BitmapUtilsImpl.class);
            put(InitUtils.class, InitUtilsImpl.class);
            put(PowerManagerUtils.class, PowerManagerUtilsImpl.class);
            put(Phone2MediaPathMapperUtils.class, Phone2MediaPathMapperUtilsImpl.class);
            put(ContactsUtils.class, ContactsUtilsImpl.class);
            put(RequestUtils.class, RequestUtilsImpl.class);
        }};
    }

    public static UtilityFactory instance() {
        if(instance == null) {
            instance = new UtilityFactory();
        }
        return instance;
    }

    public <T extends Utility> T getUtility(Class<T> interfaceClass) {
        T result = null;
        try {
            Class<? extends Utility> utilityClass = class2ObjectMap.get(interfaceClass);
            if(utilityClass == null) {
                logger.warn(TAG, "Unable to find utility for of interface:" + interfaceClass);
                return null;
            }
            result = (T) utilityClass.newInstance();


        } catch (InstantiationException | IllegalAccessException e) {
            e.printStackTrace();
        }
        return result;
    }
}
