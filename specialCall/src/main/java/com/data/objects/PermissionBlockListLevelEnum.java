package com.data.objects;

/**
 * Created by rony on 27/02/2016.
 */
public enum PermissionBlockListLevelEnum {

    ALL_VALID("ALL_VALID"),
    CONTACTS_ONLY("CONTACTS_ONLY"),
    NO_ONE("NO_ONE"),
    BLACK_LIST_SPECIFIC("BLACK_LIST_SPECIFIC");

    String value;

    PermissionBlockListLevelEnum(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
