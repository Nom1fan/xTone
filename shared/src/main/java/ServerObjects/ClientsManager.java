package ServerObjects;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import ClientObjects.UserStatus;
import DataObjects.SharedConstants;
import EventObjects.EventReport;
import LogObjects.LogsManager;
import MessagesToClient.MessageToClient;
import MessagesToClient.MessageTriggerEventOnly;


/**
 * This class manages all client connections
 * @author Mor
 */
public class ClientsManager {

	// NOTE: <String,ConnectionToClient> means clientId is key and a connection to the client is the value
	private static ConcurrentHashMap<String,ConnectionToClient> onlineConnections = new ConcurrentHashMap<>(); // All online connections

	// NOTE: <String,Long> means clientId is key and (UNIX timestamp * 1000) is the value
	private static ConcurrentHashMap<String,Long> clientHeartBeats = new ConcurrentHashMap<>(); // all online client heart beats

	// NOTE: <String,String> means clientId is key and push deviceToken is the value
	private static ConcurrentHashMap<String,String> clientsTokens = new ConcurrentHashMap<>();

	private static final int HEARTBEAT_TIMEOUT = SharedConstants.HEARTBEAT_TIMEOUT; // 5 seconds more than client's heartbeat rate
	private static final int ISLOGIN_RETRIES = 3;
	private static final int ISLOGIN_SLEEP_INTERVAL = 200; // milliseconds
	private static Logger serverLogger = null;
	private static Logger hbLogger = null;

	public ClientsManager() {
		
		initLoggers();
	}
	
	private static void logActiveConns() { 
			
		Set<String> keys = onlineConnections.keySet();
		String str = "Connected clients:"+keys.toString();
		
		serverLogger.info(str);		
	}
	
	private void initLoggers() {
		
		serverLogger = LogsManager.getServerLogger();
		hbLogger = LogsManager.getNewLogger("heartbeats");
	
	}

	public synchronized static void addClientPushToken(String clientId, String token) {
		clientsTokens.put(clientId,token);
	}

	public synchronized static String getClientPushToken(String clientId) {
		return clientsTokens.get(clientId);
	}

	public synchronized static ConnectionToClient getClientConnection(String id) {
		
		return onlineConnections.get(id);
	}
	
	public synchronized static void addClientConnection(String clientId, ConnectionToClient ctc) {
						
		onlineConnections.put(clientId, ctc);
		
		logActiveConns();
	}

	public synchronized static void markClientHeartBeat(String clientId, ConnectionToClient ctc) {
		
		Date date = new Date();
		Long timestamp = date.getTime();
		
		clientHeartBeats.put(clientId, timestamp);
		onlineConnections.put(clientId, ctc);
		
		hbLogger.info(clientId);
	}
	
	public synchronized static UserStatus isLogin(String clientId) throws IOException, ClassNotFoundException {

		Date date = new Date();
		Long now = date.getTime();

        String deviceToken = ClientsManager.getClientPushToken(clientId);

        if (deviceToken == null || deviceToken.equals("")) {

            serverLogger.info("No device token found for user:" + clientId + ". User is " + UserStatus.UNREGISTERED.toString());
            removeClientConnection(clientId);
            return UserStatus.UNREGISTERED;
        }

        Long hbTimestamp = clientHeartBeats.get(clientId);

        if (hbTimestamp == null)
            serverLogger.severe("Unable to get heartbeat from user:" + clientId);
         else {
            if (now - hbTimestamp > HEARTBEAT_TIMEOUT) {
                serverLogger.severe("Heartbeat timeout from user:" + clientId);
            } else {
                serverLogger.info(clientId + " is " + UserStatus.ONLINE.toString());
                return UserStatus.ONLINE;
            }
        }

		removeClientConnection(clientId);
		serverLogger.info(clientId + " is " + UserStatus.OFFLINE.toString());
		return UserStatus.OFFLINE;
		
	}

	public static void removeClientConnection(ConnectionToClient _ctc) {
					
		onlineConnections.values().remove(_ctc);
		
		logActiveConns();
	}

	public static void removeClientConnection(String clientId) {

		serverLogger.info("Removing client connection. Client ID:"+clientId);
		onlineConnections.remove(clientId);
		clientHeartBeats.remove(clientId);
		
		logActiveConns();
	}
	
	/**
	 * @param clientId - The client id of which to send the event
	 * @param eventReport - The event to send to the client
	 * @return - returns 'true' if the event was sent successfully, else returns 'false' and removes the client connection from client pool
	 */
	public static boolean sendEventToClient(String clientId, EventReport eventReport)
	{
		
		try 
		{
			ConnectionToClient ctc = ClientsManager.getClientConnection(clientId);
			
			if(ctc==null)
				throw(new NullPointerException("Could not get client connection for user:"+clientId));
			
			serverLogger.info("[Sending event to user]:"+clientId+" with message:"+"\""+eventReport.desc()+"\""+" [Event Type]:"+eventReport.status().toString());
			ctc.writeToClient(new MessageTriggerEventOnly(eventReport));
			return true;
		} 
		catch (IOException | NullPointerException e) 
		{			
			serverLogger.severe("[Failed to send event to user]:"+clientId+" of message:"+"\""+eventReport.desc()+"\""+" [Event Type]:"+eventReport.status().toString()+" [Exception]:"+e.getMessage()+ " Removing client connection...");
			e.printStackTrace();
			ClientsManager.removeClientConnection(clientId);
			return false;
		}
		
	}
	
	/**
	 * @param clientId - The client clientId of which to send the message
	 * @param msgToClient - The message to send to the client
	 * @return - returns 'true' if message was sent successfully, else returns 'false' and removes the client connection from client pool
	 */
	public static boolean sendMessageToClient(String clientId, MessageToClient msgToClient)
	{
		
		try 
		{
			ConnectionToClient ctc = ClientsManager.getClientConnection(clientId);
			serverLogger.info("[Sending message to user]:"+clientId+" with message:"+msgToClient.getClass().getSimpleName());
			
			if(ctc==null)
				throw(new NullPointerException("Could not get client connection for user:"+clientId));
			
			ctc.writeToClient(msgToClient);
			return true;
		} 
		catch (IOException | NullPointerException e) 
		{			
			serverLogger.severe("[Failed to send message to user]:"+clientId+" of message:"+msgToClient.getClass().getSimpleName()+" [Exception]:"+e.getMessage()+ "Terminating client connection...");
			e.printStackTrace();
			ClientsManager.removeClientConnection(clientId);
			return false;
		}
		
	}
}
