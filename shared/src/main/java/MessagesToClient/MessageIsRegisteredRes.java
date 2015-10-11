package MessagesToClient;

import java.io.IOException;

import ClientObjects.IServerProxy;
import ClientObjects.UserStatus;
import EventObjects.EventReport;
import EventObjects.EventType;

public class MessageIsRegisteredRes extends MessageToClient {
		
	/**
	 * 
	 */
	private static final long serialVersionUID = -7124115668017564832L;
	private UserStatus _userStatus;
	private String _phone;
	
	public MessageIsRegisteredRes(String phone, UserStatus userStatus) {
						
		_phone = phone;
		_userStatus = userStatus;
	}
	
	@Override
	public EventReport doClientAction(IServerProxy serverProxy) throws IOException {
		
		String desc;
		switch(_userStatus)
		{
			case REGISTERED:
				desc = "User "+_phone+" is registered";
				return new EventReport(EventType.ISREGISTERED_TRUE, desc, _phone);

			case UNREGISTERED:
				desc = "User "+_phone+" is unregistered";
				return new EventReport(EventType.ISREGISTERED_FALSE, desc, _phone);
			
			default: 
					desc = "Unable to retrieve user "+_phone+" status";
					return new EventReport(EventType.ISREGISTERED_ERROR, desc, _phone);
		}
					
	}
	
}
