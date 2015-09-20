package EventObjects;

import java.io.Serializable;

public enum EventType implements Serializable { 
	UPLOAD_SUCCESS,
	UPLOAD_FAILURE,
	PENDING_DOWNLOAD,
	DOWNLOAD_SUCCESS,
	DOWNLOAD_FAILURE,	
	LOGIN_FAILURE,
	LOGIN_SUCCESS,
	ISLOGIN_ERROR,
	ISLOGIN_ONLINE,
	ISLOGIN_OFFLINE,
	ISLOGIN_UNREGISTERED,
	DESTINATION_DOWNLOAD_COMPLETE,
	RESPONSE_FAILURE, 	
	CLIENT_ACTION_FAILURE, 
	CLOSE_APP, 	
	NO_ACTION_REQUIRED, 
	SERVICE_STARTED, 
	DISPLAY_ERROR,
	DISPLAY_MESSAGE,
	RECONNECT_ATTEMPT, 	  
}
