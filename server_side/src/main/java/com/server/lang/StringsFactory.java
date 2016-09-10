package com.server.lang;

/**
 * Created by Mor on 21/04/2016.
 */
public interface StringsFactory {

    String DEFAULT_LANG = "en";
    LangStrings getStrings(String locale);
}
