package MessagesToServer;


import java.io.IOException;

import ClientObjects.UserStatus;
import MessagesToClient.MessageIsLoginRes;
import ServerObjects.ClientsManager;
import ServerObjects.PushSender;

public class MessageIsLogin extends MessageToServer {
		
	private static final long serialVersionUID = -2625228196534308145L;
	private String _id;
	private UserStatus userStatus;

	
	public MessageIsLogin(String srcId, String destId) {
		super(srcId);
		_id = destId;
	}

	@Override
	public boolean doServerAction() throws IOException, ClassNotFoundException {

		initLogger();

		logger.info(_messageInitiaterId + " is checking if " + _id + " is logged in...");

        userStatus = ClientsManager.isLogin(_id);

		MessageIsLoginRes res = new MessageIsLoginRes(_id, userStatus);

		clientConnection.sendToClient(res);
		
		logger.info("Sent response to client:"+_messageInitiaterId);
		
		return cont;
		
	}


	
	

	

}
