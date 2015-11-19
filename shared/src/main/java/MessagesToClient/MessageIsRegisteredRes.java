package MessagesToClient;

import java.io.IOException;

import ClientObjects.ConnectionToServer;
import ClientObjects.UserStatus;
import EventObjects.EventReport;
import EventObjects.EventType;

public class MessageIsRegisteredRes extends MessageToClient {
		
	/**
	 * 
	 */
	private static final long serialVersionUID = -7124115668017564832L;
	private String _phone;
	private boolean _isRegistered;
	
	public MessageIsRegisteredRes(String phone, boolean isRegistered) {
						
		_phone = phone;
		_isRegistered = isRegistered;
	}
	
	@Override
	public EventReport doClientAction(ConnectionToServer connectionToServer) throws IOException {
		
		String desc;
		if(_isRegistered) {
			desc = "User "+_phone+" is registered";
			return new EventReport(EventType.USER_REGISTERED_TRUE, desc, _phone);
		}
		else {
			desc = "User "+_phone+" is unregistered";
			return new EventReport(EventType.USER_REGISTERED_FALSE, desc, _phone);
		}
	}
	
}
