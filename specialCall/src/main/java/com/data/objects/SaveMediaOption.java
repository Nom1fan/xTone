package com.data.objects;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by rony on 27/02/2016.
 */
public enum SaveMediaOption {

    EMPTY(-1),
    ALWAYS(0),
    CONTACTS_ONLY(1),
    NEVER_SAVE(2);

    int value;
    static Map<Integer, SaveMediaOption> value2Enum = new HashMap<Integer, SaveMediaOption>() {{
        put(-1, EMPTY);
        put(0, ALWAYS);
        put(1, CONTACTS_ONLY);
        put(2, NEVER_SAVE);
    }};

    SaveMediaOption(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    public static SaveMediaOption fromValue(int val) {
        return value2Enum.get(val);
    }
}
