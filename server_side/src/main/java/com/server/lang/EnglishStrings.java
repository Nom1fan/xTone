package com.server.lang;

import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

/**
 * Created by Mor on 18/04/2016.
 */
@Component
public class EnglishStrings extends AbstractStrings {
    @Override
    public Languages getLanguage() {
        return Languages.ENGLISH;
    }
}
