package com.xtone.utils;

import android.content.Context;
import android.os.Bundle;

import com.xtone.logging.Logger;
import com.xtone.logging.LoggerFactory;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * Created by Mor on 15/10/2015.
 */
public class StringUtils implements Utility {

    private static final Logger log = LoggerFactory.getLogger();

    public String toString(Bundle bundle) {
        Set<String> keySet = bundle.keySet();
        Iterator<String> iterator = keySet.iterator();

        StringBuilder builder = new StringBuilder();
        builder.append("[");

        while (iterator.hasNext()) {
            String key = iterator.next();
            builder.append("{")
                    .append(key)
                    .append("=")
                    .append(bundle.get(key))
                    .append("}");
        }
        builder.append("]");

        return builder.toString();
    }
}
