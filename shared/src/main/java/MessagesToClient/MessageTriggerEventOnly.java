package MessagesToClient;

import java.io.IOException;
import java.net.UnknownHostException;

import ClientObjects.ConnectionToServer;
import EventObjects.EventReport;

public class MessageTriggerEventOnly extends MessageToClient {

	private static final long serialVersionUID = -5190814867293569595L;
	private EventReport _eventReport;
	public MessageTriggerEventOnly(EventReport eventReport) {
		
		_eventReport = eventReport; 
	}

	@Override
	public EventReport doClientAction(ConnectionToServer connectionToServer) throws UnknownHostException, IOException
	{
		return _eventReport;
	}
		
}
