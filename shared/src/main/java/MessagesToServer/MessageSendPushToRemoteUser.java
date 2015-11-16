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
    private String _extra;

	public MessageSendPushToRemoteUser(String messageInitiaterId, String remoteUserId, String pushEventAction, String msg) {
		super(messageInitiaterId);
		_remoteUserId = remoteUserId;
		_msg = msg;
		_pushEventAction = pushEventAction;
	}

    public MessageSendPushToRemoteUser(String messageInitiaterId, String remoteUserId, String pushEventAction, String msg, String extra) {
        super(messageInitiaterId);
        _remoteUserId = remoteUserId;
        _msg = msg;
        _pushEventAction = pushEventAction;
        _extra = extra;

    }

	@Override
	public boolean doServerAction() throws IOException, ClassNotFoundException {

		initLogger();

		String remoteToken = ClientsManager.getClientPushToken(_remoteUserId);

		if(remoteToken!=null && !remoteToken.equals("")) {

			boolean sent = false;
			if (_extra == null) {
				sent = PushSender.sendPush(remoteToken, _pushEventAction, _msg);
			} else {
				sent = PushSender.sendPush(remoteToken, _pushEventAction, _msg, _extra);
			}
			if(sent)
				logger.info("Push from:"+_messageInitiaterId+" to:"+_remoteUserId+" sent successfully");
			else
				logger.severe("Push from:" + _messageInitiaterId + " to:" +_remoteUserId+" failed to be sent");
		}
		else
			logger.severe("Push from:" + _messageInitiaterId + " to:" +_remoteUserId+" failed to be sent. Token does not exist");

		return true;
	}


}
