package MessagesToServer;

import java.io.IOException;
import MessagesToClient.MessageLoginRes;
import ServerObjects.ClientsManager;


public class MessageLogin extends MessageToServer {
	
	private static final long serialVersionUID = 7382209934954570169L;
	private String pushToken;
	
	public MessageLogin(String clientId, String pushToken) {
		
		super(clientId);
		this.pushToken = pushToken;
	
	}
	
	@Override
	public boolean doServerAction() throws IOException {
			
		initLogger();				
		
		logger.info(_messageInitiaterId + " is logging in...");

		ClientsManager.addClientPushToken(_messageInitiaterId, pushToken);
		ClientsManager.addClientConnection(_messageInitiaterId, clientConnection);
		ClientsManager.markClientHeartBeat(_messageInitiaterId, clientConnection);
		MessageLoginRes msg = new MessageLoginRes();		
		ClientsManager.sendMessageToClient(_messageInitiaterId, msg);
		
		return cont;
	}
}

