package com.utils;

import com.converters.MediaDataConverter;
import com.converters.MediaDataConverterImpl;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by Mor on 02/06/2017.
 */

public abstract class UtilityFactory {

    private static Map<Class<? extends Utility>, Utility> class2ObjectMap = new HashMap<Class<? extends Utility>, Utility>() {{
        put(MediaFileUtils.class, new MediaFilesUtilsInstance());
        put(MediaDataConverter.class, new MediaDataConverterImpl());

    }};

    public static <T extends Utility> T getUtility(Class aClass) {
        return (T) class2ObjectMap.get(aClass);
    }
}
