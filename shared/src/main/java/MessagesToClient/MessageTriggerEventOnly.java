package MessagesToClient;

import java.io.IOException;
import java.net.UnknownHostException;
import ClientObjects.IServerProxy;
import EventObjects.EventReport;

public class MessageTriggerEventOnly extends MessageToClient {

	private static final long serialVersionUID = -5190814867293569595L;
	private EventReport _eventReport;
	public MessageTriggerEventOnly(EventReport eventReport) {
		
		_eventReport = eventReport; 
	}

	@Override
	public EventReport doClientAction(IServerProxy serverProxy) throws UnknownHostException, IOException 
	{
		return _eventReport;
	}
		
}
