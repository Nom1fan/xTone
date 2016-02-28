package com.utils;

import android.content.Context;

import java.text.NumberFormat;
import java.text.ParsePosition;

/**
 * Created by Mor on 18/02/2016.
 */
public abstract class PhoneNumberUtils {

    public static String toValidPhoneNumber(String str) {

        str = str.replaceAll("[^0-9]", "");

        // TODO deal with other countries
        if (str.startsWith("9720")) {
            str = str.replaceFirst("9720", "0");
        }
        if (str.startsWith("972")) {
            str = str.replaceFirst("972", "0");
        }
        
        return str;
    }

    public static boolean isNumeric(String str) {
        NumberFormat formatter = NumberFormat.getInstance();
        ParsePosition pos = new ParsePosition(0);
        formatter.parse(str, pos);
        return str.length() == pos.getIndex();
    }

    public static boolean isValidPhoneNumber(String destPhone) {

        boolean lengthOK = 10 == destPhone.length();
        boolean isNumeric = isNumeric(destPhone);
        boolean isValidPrefix = destPhone.startsWith("0"); //TODO deal with other countries

        return lengthOK && isNumeric && isValidPrefix;
    }
}