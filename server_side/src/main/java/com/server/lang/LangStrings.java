package com.server.lang;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Paths;
import java.util.Properties;

/**
 * Created by Mor on 18/04/2016.
 */
public interface LangStrings {

    String KEY_OOPS = "oops";
    String KEY_UPLOAD_FAILED = "upload_failed";
    String KEY_MEDIA_READY_TITLE = "media_ready_title";
    String KEY_MEDIA_READY_BODY = "media_ready_body";
    String KEY_MEDIA_UNDELIVERED_TITLE = "media_undelivered_title";
    String KEY_MEDIA_UNDELIVERED_BODY = "media_undelivered_body";
    String KEY_MEDIA_CLEARED_TITLE = "media_cleared_title";
    String KEY_MEDIA_CLEARED_BODY = "media_cleared_body";
    String KEY_YOUR_VERIFICATION_CODE = "your_verification_code";

    String STRINGS_FILE = Paths.get("").toAbsolutePath().toString() + "\\server_side\\resources\\%s.properties\\";

    Languages getLanguage();

    String oops();

    String upload_failed();

    String media_ready_title();

    String media_ready_body();

    String media_undelivered_title();

    String media_undelivered_body();

    String media_cleared_title();

    String media_cleared_body();

    String your_verification_code();

    default String getString(String key) throws IOException, NullPointerException {
        String stringsFilePath = String.format(STRINGS_FILE, getLanguage().toString());
        FileInputStream fis = new FileInputStream(stringsFilePath);
        InputStreamReader isr = new InputStreamReader(fis, "UTF-8");
        Properties props = new Properties();
        props.load(isr);
        return props.getProperty(key);
    }

    enum Languages {

        ENGLISH("en"),
        HEBREW("he"),
        IVRIT("iw");

        private final String text;

        Languages(String text) {
            this.text = text;
        }

        @Override
        public String toString() {
            return text;
        }
    }
}
