package DataObjects;

public abstract class SharedConstants {

    public static String DEVICE_TOKEN;
    public static String specialCallPath;
	public static String MY_ID;
  	public static final String HOST = "morserver.no-ip.biz";
  	public static final int PORT = 8888;
	public static final int HEARTBEAT_INTERVAL = 60*1000;
	public static final int HEARTBEAT_TIMEOUT = HEARTBEAT_INTERVAL + 30*1000;


}
