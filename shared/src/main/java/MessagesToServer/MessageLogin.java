package MessagesToServer;


import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;

import MessagesToClient.MessageLoginRes;
import ServerObjects.ClientsManager;


public class MessageLogin extends MessageToServer {
	
	private static final long serialVersionUID = 7382209934954570169L;				
	
	public MessageLogin(String clientId) {
		
		super(clientId);
	
	}
	
	@Override
	public boolean doServerAction() throws UnknownHostException, IOException {
			
		initLogger();				
		
		logger.info(_messageInitiaterId+" is logging in...");
		
		ClientsManager.addClientConnection(_messageInitiaterId, clientConnection);
		ClientsManager.markClientHeartBeat(_messageInitiaterId, clientConnection);
		MessageLoginRes msg = new MessageLoginRes();		
		clientConnection.writeToClient(msg);	
		
		return cont;
	}
}

