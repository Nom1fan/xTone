package MessagesToServer;

import java.io.IOException;
import java.net.UnknownHostException;
import ServerObjects.ClientsManager;

public class MessageHeartBeat extends MessageToServer {

	/**
	 * 
	 */
	private static final long serialVersionUID = -3086259391119843968L;

	public MessageHeartBeat(String srcPhone) {
		super(srcPhone);
		
	}

	@Override
	public boolean doServerAction() throws IOException,
			ClassNotFoundException {				
		
		ClientsManager.markClientHeartBeat(_messageInitiaterId, clientConnection);
		
		return cont;
	}

}
