package com.server.lang;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;

/**
 * Created by Mor on 7/28/2016.
 */
@Component
public abstract class AbstractStrings implements LangStrings {

    @Autowired
    protected Logger logger;

    private String oops;
    private String upload_failed;
    private String media_ready_title;
    private String media_ready_body;
    private String media_undelivered_title;
    private String media_undelivered_body;
    private String media_cleared_title;
    private String media_cleared_body;
    private String your_verification_code;


    @PostConstruct
    public void init() {
        try {
            oops = getString(KEY_OOPS);
            upload_failed = getString(KEY_UPLOAD_FAILED);
            media_ready_title = getString(KEY_MEDIA_READY_TITLE);
            media_ready_body = getString(KEY_MEDIA_READY_BODY);
            media_undelivered_title = getString(KEY_MEDIA_UNDELIVERED_TITLE);
            media_undelivered_body = getString(KEY_MEDIA_UNDELIVERED_BODY);
            media_cleared_title = getString(KEY_MEDIA_CLEARED_TITLE);
            media_cleared_body = getString(KEY_MEDIA_CLEARED_BODY);
            your_verification_code = getString(KEY_YOUR_VERIFICATION_CODE);

        } catch (IOException e) {
            e.printStackTrace();
            logger.log(Level.SEVERE, "Failed to load strings from properties file for language:" + getLanguage(), e);
        }
    }

    @Override
    public Languages getLanguage() {
        return null;
    }

    @Override
    public String oops() {
        return oops;
    }

    @Override
    public String upload_failed() {
        return upload_failed;
    }

    @Override
    public String media_ready_title() {
        return media_ready_title;
    }

    @Override
    public String media_ready_body() {
        return media_ready_body;
    }

    @Override
    public String media_undelivered_title() {
        return media_undelivered_title;
    }

    @Override
    public String media_undelivered_body() {
        return media_undelivered_body;
    }

    @Override
    public String media_cleared_title() {
        return media_cleared_title;
    }

    @Override
    public String media_cleared_body() {
        return media_cleared_body;
    }

    @Override
    public String your_verification_code() {
        return your_verification_code;
    }
}
