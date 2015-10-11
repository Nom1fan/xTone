package MessagesToServer;

import java.io.IOException;
import java.net.UnknownHostException;

import DataObjects.PushEventKeys;
import EventObjects.EventReport;
import ServerObjects.ClientsManager;
import ServerObjects.PushSender;

public class MessageSendPushToRemoteUser extends MessageToServer {


	private static final long serialVersionUID = 3684945085673011673L;
	private String _remoteUserId;
	private String _msg;
	private String _pushEventAction;

	public MessageSendPushToRemoteUser(String messageInitiaterId, String remoteUserId, String pushEventAction, String msg) {
		super(messageInitiaterId);
		_remoteUserId = remoteUserId;
		_msg = msg;
		_pushEventAction = pushEventAction;
		
	}

	@Override
	public boolean doServerAction() throws IOException, ClassNotFoundException {

		initLogger();

		String remoteToken = ClientsManager.getClientPushToken(_remoteUserId);
		boolean sent = PushSender.sendPush(remoteToken, _pushEventAction, _msg);

		if(sent)
			logger.info("Push sent successfully to:"+_remoteUserId);
		else
			logger.severe("Failed to send push to:"+_remoteUserId);

		return true;
	}


}
