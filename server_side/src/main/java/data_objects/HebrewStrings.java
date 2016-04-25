package data_objects;

import ServerObjects.ILangStrings;

/**
 * Created by Mor on 21/04/2016.
 */
public class HebrewStrings implements ILangStrings {

    public static final String OOPS                     =   "אופס!";
    public static final String UPLOAD_FAILED            =   "המדיה שלך ל-%1$s אבדה בדרך! נסה שוב.";
    public static final String MEDIA_READY_TITLE        =   "מדיה מוכנה!";
    public static final String MEDIA_READY_BODY         =   "מדיה עבור %1$s מוכנה!";
    public static final String MEDIA_UNDELIVERED_TITLE  =   "מדיה לא נשלחה";
    public static final String MEDIA_UNDELIVERED_BODY   =   "אופס! %1$s לא קיבל את המדיה שלך";
    public static final String MEDIA_CLEARED_TITLE      =   "שוחזר צלצול רגיל!";
    public static final String MEDIA_CLEARED_BODY       =   "צלצול עבור %1$s שוחזר לצלצול רגיל";


    @Override
    public String upload_failed() {

        return UPLOAD_FAILED;
    }

    @Override
    public String oops() {

        return OOPS;
    }

    @Override
    public String media_ready_title() {

        return MEDIA_READY_TITLE;
    }

    @Override
    public String media_ready_body() {

        return MEDIA_READY_BODY;
    }

    @Override
    public String media_undelivered_title() {

        return MEDIA_UNDELIVERED_TITLE;
    }

    @Override
    public String media_undelivered_body() {

        return MEDIA_UNDELIVERED_BODY;
    }

    @Override
    public String media_cleared_title() {
        return MEDIA_CLEARED_TITLE;
    }

    @Override
    public String media_cleared_body() {
        return MEDIA_CLEARED_BODY;
    }
}
