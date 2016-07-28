package com.server.lang;

import com.server.spring.SpringConfig;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.logging.Logger;

/**
 * Created by Mor on 21/04/2016.
 */
@Component
public class StringsFactoryImpl implements StringsFactory {

    private HashMap<String, LangStrings> lang2StringsMap = new HashMap<>();

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
            logger.warning(String.format("Invalid locale '%s'. Assuming English", locale));
            return new EnglishStrings();
        }
        return langStrings;
    }

    public static void main(String[] args) {

        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(SpringConfig.class);
        StringsFactory stringsFactory = context.getBean(StringsFactory.class);
        List<String> locales = new ArrayList() {{
            add("en");
            add("he");
            add("iw");
        }};

        for (String locale : locales) {
            System.out.println("Locale:" + locale);
            LangStrings langStrings = stringsFactory.getStrings(locale);
            System.out.println(langStrings.oops());
            System.out.println(langStrings.media_cleared_title());
            System.out.println(langStrings.media_cleared_body());
            System.out.println(langStrings.media_undelivered_title());
            System.out.println(langStrings.media_undelivered_body());
            System.out.println(langStrings.upload_failed());
            System.out.println(langStrings.your_verification_code());
        }
    }
}
