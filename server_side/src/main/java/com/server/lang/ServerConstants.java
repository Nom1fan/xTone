package com.server.lang;

/**
 * Created by Mor on 26/03/2016.
 */
public abstract class ServerConstants {

    public static final int PORT = 8888;

    public static final double MIN_SUPPORTED_APP_VERSION = 0.2;
    public static final double APP_VERSION_1_13 = 1.13; // Introduced user device record support

    //region Batch push properties
    public static final String LIVE_API_KEY = "56B47A2D7AF8834E6D36C9CBC3F32E";
    //endregion

    //region Database properties
    public static final String DB_SERVER_HOST = "localhost";
    public static final int DB_SERVER_PORT = 3306;
    public static final String DB_SERVER_USER = "root";
    public static final String DB_SERVER_PWD = "egg9986";
    //endregion

    //region Directory paths properties
    public static final String UPLOAD_FOLDER = "\\server_side\\uploads\\";
    public static final String CALLER_MEDIA_FOLDER = "\\caller_media\\";
    public static final String PROFILE_MEDIA_RECEIVED_FOLDER = "\\profile_media_received\\";
    public static final String MY_DEFAULT_PROFILE_MEDIA_FOLDER = "\\my_default_profile_media\\";
    public static final String MY_DEFAULT_PROFILE_MEDIA_FILENAME = "MyDefaultProfileMedia";
}
