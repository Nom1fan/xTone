package com.server.lang;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.logging.Logger;

/**
 * Created by Mor on 21/04/2016.
 */
@Component
public class StringsFactoryImpl implements StringsFactory {

    private final HashMap<String, LangStrings> lang2StringsMap = new HashMap<>();

    @Autowired
    private Logger logger;

    @Autowired
    private void initMap(List<LangStrings> langStringsList) {
        for (LangStrings langStrings : langStringsList) {
            lang2StringsMap.put(langStrings.getLanguage().toString(), langStrings);
        }
    }

    @Override
    public LangStrings getStrings(String locale) {
        LangStrings langStrings = lang2StringsMap.get(locale);
        if(langStrings == null) {
            logger.warning(String.format("Invalid locale '%s'. Assuming " + DEFAULT_LANG, locale));
            langStrings = lang2StringsMap.get(DEFAULT_LANG);
        }
        return langStrings;
    }
}
