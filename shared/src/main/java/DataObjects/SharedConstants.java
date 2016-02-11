package DataObjects;

public abstract class SharedConstants {


    /* Client constants */
	public static String ROOT_FOLDER;
    public static String INCOMING_FOLDER;
    public static String OUTGOING_FOLDER;
	public static String tempFolderForCompression;

    /* Server constants*/
    public static final String UPLOAD_FOLDER = "\\server_side\\uploads\\";
    public static final String CALLER_MEDIA_FOLDER = "\\caller_media\\";
    public static final String PROFILE_MEDIA_RECEIVED_FOLDER = "\\profile_media_received\\";
    public static final String MY_DEFAULT_PROFILE_MEDIA_FOLDER = "\\my_default_profile_media\\";
    public static final String MY_DEFAULT_PROFILE_MEDIA_FILENAME = "MyDefaultProfileMedia";

    /* Shared constants */
  	public static final String LOGIC_SERVER_HOST = "epicall.no-ip.biz";
  	public static final int LOGIC_SERVER_PORT = 8888;
	public static final String STROAGE_SERVER_HOST = "epicall.no-ip.biz";
	public static final int STORAGE_SERVER_PORT = 7777;
	public static final String DB_SERVER_HOST = "localhost";
	public static final int DB_SERVER_PORT = 3306;
	public static final String DB_SERVER_USER = "root";
	public static final String DB_SERVER_PWD = "egg9986";
    public static final String APP_NAME = "MediaCallz";
    public static final String LIVE_API_KEY = "56B47A2D7AF8834E6D36C9CBC3F32E";
    public static final String DEV_API_KEY = "DEV56B47A2D7D7E553A410B64C489D";


}
