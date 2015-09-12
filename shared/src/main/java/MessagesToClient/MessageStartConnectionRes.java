package MessagesToClient;

import java.io.IOException;
import java.net.UnknownHostException;

import ClientObjects.IServerProxy;
import EventObjects.EventReport;
import EventObjects.EventType;

public class MessageStartConnectionRes extends MessageToClient {

	/**
	 * 
	 */
	private static final long serialVersionUID = 8150276781166555436L;

	@Override
	public EventReport doClientAction(IServerProxy serverProxy) throws UnknownHostException,
			IOException {
		
			return new EventReport(EventType.NO_ACTION_REQUIRED, null, null);
		
	}

}
