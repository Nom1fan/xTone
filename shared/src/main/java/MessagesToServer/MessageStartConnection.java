package MessagesToServer;

import java.io.IOException;
import java.net.UnknownHostException;

import MessagesToClient.MessageStartConnectionRes;


public class MessageStartConnection extends MessageToServer {

	/**
	 * 
	 */
	private static final long serialVersionUID = 9054684790649495866L;

	public MessageStartConnection(String srcPhone) {
		
		super(srcPhone);
	}
	
	@Override
	public boolean doServerAction() throws IOException,
			ClassNotFoundException {
		
		initLogger();
		
		MessageStartConnectionRes msgReply = new MessageStartConnectionRes();
		replyToClient(msgReply);
		return true;
	}

}
