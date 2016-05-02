package lang;

import ServerObjects.ILangStrings;

/**
 * Created by Mor on 18/04/2016.
 */
public class EnglishStrings implements ILangStrings {

    public static final String OOPS = "Oops!";
    public static final String UPLOAD_FAILED = "Your media to %1$s was lost on the way! Please try again.";
    public static final String MEDIA_READY_TITLE = "Media ready!";
    public static final String MEDIA_READY_BODY = "Media for %1$s is ready!";
    public static final String MEDIA_UNDELIVERED_TITLE = "Media undelivered";
    public static final String MEDIA_UNDELIVERED_BODY = "Oops! %1$s did not receive your media.";
    public static final String MEDIA_CLEARED_TITLE = "Returned to normal ring!";
    public static final String MEDIA_CLEARED_BODY = "Ring for %1$s returned to normal";
    public static final String YOUR_VERIFICATION_CODE = "Your verification code: %1%d";

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

    @Override
    public String your_verification_code() {
        return YOUR_VERIFICATION_CODE;
    }


}
