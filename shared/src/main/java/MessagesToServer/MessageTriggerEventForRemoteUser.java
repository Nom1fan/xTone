package MessagesToServer;

import java.io.IOException;
import java.net.UnknownHostException;
import EventObjects.EventReport;
import ServerObjects.ClientsManager;

public class MessageTriggerEventForRemoteUser extends MessageToServer {


	private static final long serialVersionUID = 3684945085673011673L;
	private String _remoteUserId;
	private EventReport _eventReport;

	public MessageTriggerEventForRemoteUser(String messageInitiaterId, String remoteUserId , EventReport eventReport) {
		super(messageInitiaterId);
		_remoteUserId = remoteUserId;
		_eventReport = eventReport;
		
	}

	@Override
	public boolean doServerAction() throws UnknownHostException, IOException, ClassNotFoundException {
		
		ClientsManager.sendEventToClient(_remoteUserId, _eventReport);
		return true;
	}


}
