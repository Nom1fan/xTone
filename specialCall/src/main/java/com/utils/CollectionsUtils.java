package com.utils;

import java.util.List;
import java.util.Map;

import static java.util.AbstractMap.SimpleEntry;

/**
 * Created by Mor on 24/09/2016.
 */
public class CollectionsUtils<K,V> {

    public V extractValueFromSimpleEntryList(K key, List<SimpleEntry> list) {
        V result = null;
        for (SimpleEntry entry : list) {
            if(entry.getKey().equals(key)) {
                result = (V) entry.getValue();
                break;
            }
        }
        return result;
    }

    public void addMapElementsToSimpleEntryList(List<SimpleEntry> list, Map<K,?> map) {
        for (K key : map.keySet()) {
            list.add(new SimpleEntry<>(key, map.get(key)));
        }
    }
}
