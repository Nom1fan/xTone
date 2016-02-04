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
		
		_logger.info(_messageInitiaterId + " is registering...");

		boolean isOK = _clientsManager.registerUser(_messageInitiaterId, pushToken);
		MessageRegisterRes msgReply = new MessageRegisterRes(isOK);
		replyToClient(msgReply);
		
		return _cont;
	}
}

