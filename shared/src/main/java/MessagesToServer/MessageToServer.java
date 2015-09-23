package MessagesToServer;

import com.lloseng.ocsf.server.ConnectionToClient;
import java.io.IOException;
import java.io.Serializable;
import java.util.logging.Logger;
import LogObjects.LogsManager;

/**
 * Abstract message to the server, containing information of the source client and enables generic interface for server actions corresponding to the message
 * @author Mor
 *
 */
public abstract class MessageToServer implements Serializable  {
	
	protected String _messageInitiaterId;
	protected boolean cont = true;
	private static final long serialVersionUID = -6478414954653475710L;	
	protected com.lloseng.ocsf.server.ConnectionToClient clientConnection;
	protected Logger logger = null;
	
	public MessageToServer(String messageInitiaterId) { _messageInitiaterId = messageInitiaterId; }
	
	protected void initLogger() { logger = LogsManager.getServerLogger(); }
	abstract public boolean doServerAction() throws IOException, ClassNotFoundException;
	public void setClientConnection(ConnectionToClient cc) { clientConnection = cc; }
		
}
