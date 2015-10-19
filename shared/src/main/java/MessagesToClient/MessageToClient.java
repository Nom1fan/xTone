package MessagesToClient;
import java.io.IOException;
import java.io.Serializable;
import java.net.UnknownHostException;

import ClientObjects.ConnectionToServer;
import ClientObjects.IServerProxy;
import EventObjects.EventReport;

/**
 * Abstract message to the client, containing information and enables generic interface for client actions corresponding to the message
 * @author Mor
 *
 */
public abstract class MessageToClient implements Serializable {
	
	private static final long serialVersionUID = -3563686195376300090L;
	abstract public EventReport doClientAction(ConnectionToServer connectionToServer) throws IOException;
	

}
