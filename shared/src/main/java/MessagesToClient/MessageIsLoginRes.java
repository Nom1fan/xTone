package MessagesToClient;

import java.io.IOException;
import java.net.UnknownHostException;
import ClientObjects.IServerProxy;
import ClientObjects.UserStatus;
import EventObjects.EventGenerator;
import EventObjects.EventReport;
import EventObjects.EventType;

public class MessageIsLoginRes extends MessageToClient {
		
	/**
	 * 
	 */
	private static final long serialVersionUID = -7124115668017564832L;
	private UserStatus _userStatus;
	private String _phone;
	
	public MessageIsLoginRes(String phone, UserStatus userStatus) {
						
		_phone = phone;
		_userStatus = userStatus;
	}
	
	@Override
	public EventReport doClientAction(IServerProxy serverProxy)
			throws UnknownHostException, IOException {
		
		String desc;
		switch(_userStatus)
		{
			case ONLINE: 
				desc = "User "+_phone+" is online";
				return new EventReport(EventType.ISLOGIN_ONLINE, desc, _phone);			
			
			case OFFLINE:
				desc = "User "+_phone+" is offline";
				return new EventReport(EventType.ISLOGIN_OFFLINE, desc, _phone);			
			
			case UNREGISTERED:
				desc = "User "+_phone+" is unregistered";
				return new EventReport(EventType.ISLOGIN_UNREGISTERED, desc, _phone);
			
			default: 
					desc = "Unable to retrieve user "+_phone+" status";
					return new EventReport(EventType.ISLOGIN_ERROR, desc, _phone);
		}
					
	}
	
}
