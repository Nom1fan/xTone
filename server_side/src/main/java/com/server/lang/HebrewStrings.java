package com.server.lang;

import org.springframework.stereotype.Component;

/**
 * Created by Mor on 21/04/2016.
 */
@Component
public class HebrewStrings extends AbstractStrings {

    @Override
    public Languages getLanguage() {
        return Languages.HEBREW;
    }

}
