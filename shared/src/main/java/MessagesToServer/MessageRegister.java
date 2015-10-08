package MessagesToServer;

import java.io.IOException;
import MessagesToClient.MessageRegisterRes;
import ServerObjects.ClientsManager;


public class MessageRegister extends MessageToServer {
	
	private static final long serialVersionUID = 7382209934954570169L;
	private String pushToken;
	
	public MessageRegister(String clientId, String pushToken) {
		
		super(clientId);
		this.pushToken = pushToken;
	
	}
	
	@Override
	public boolean doServerAction() throws IOException {
			
		initLogger();				
		
		logger.info(_messageInitiaterId + " is registering...");

		ClientsManager.addClientPushToken(_messageInitiaterId, pushToken);
		ClientsManager.addClientConnection(_messageInitiaterId, clientConnection);
		//ClientsManager.markClientHeartBeat(_messageInitiaterId, clientConnection);
		MessageRegisterRes msgReply = new MessageRegisterRes();
		replyToClient(msgReply);
		
		return cont;
	}
}

