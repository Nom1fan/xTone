package MessagesToServer;


import java.io.IOException;

import DataObjects.UserStatus;
import MessagesToClient.MessageIsRegisteredRes;

public class MessageIsRegistered extends MessageToServer {
		
	private static final long serialVersionUID = -2625228196534308145L;
	private String _id;
	private UserStatus userStatus;

	
	public MessageIsRegistered(String srcId, String destId) {
		super(srcId);
		_id = destId;
	}

	@Override
	public boolean doServerAction() throws IOException, ClassNotFoundException {

		initLogger();

		_logger.info(_messageInitiaterId + " is checking if " + _id + " is logged in...");


		MessageIsRegisteredRes msgReply = new MessageIsRegisteredRes(_id, _clientsManager.isRegistered(_id));
		replyToClient(msgReply);
		
		return _cont;
		
	}


	
	

	

}
