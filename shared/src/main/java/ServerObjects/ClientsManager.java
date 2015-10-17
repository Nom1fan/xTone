package ServerObjects;


import java.io.IOException;
import java.util.Date;
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
 * This class is a singleton that manages all client connections
 * @author Mor
 */
public class ClientsManager {

	// NOTE: <String,SocketWrapper> means clientId is key and a connection to the client is the value
	private static ConcurrentHashMap<String, ConnectionToClient> onlineConnections = new ConcurrentHashMap<>(); // All online connections

	// NOTE: <String,String> means clientId is key and push deviceToken is the value
	private static ConcurrentHashMap<String,String> clientsTokens = new ConcurrentHashMap<>();

	private static ClientsManager instance;

	private static Logger serverLogger = null;

	private ClientsManager() {
		
		initLoggers();
	}

	public static void initialize() {

		if(instance==null)
			instance = new ClientsManager();
	}
	
	private static void logActiveConns() { 
			
		Set<String> keys = onlineConnections.keySet();
		String str = "Connected clients:"+keys.toString();
		
		serverLogger.info(str);		
	}
	
	private void initLoggers() {
		
		serverLogger = LogsManager.getServerLogger();
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

//	public synchronized static void markClientHeartBeat(String clientId, ConnectionToClient ctc) {
//
//		Date date = new Date();
//		Long timestamp = date.getTime();
//
//		clientHeartBeats.put(clientId, timestamp);
//		onlineConnections.put(clientId, ctc);
//
//		hbLogger.info(clientId);
//	}
	
	public synchronized static UserStatus isRegistered(String clientId) {

//		Date date = new Date();
//		Long now = date.getTime();

        String deviceToken = ClientsManager.getClientPushToken(clientId);

        if (deviceToken == null || deviceToken.equals("")) {

            serverLogger.info("No device token found for user:" + clientId + ". User is " + UserStatus.UNREGISTERED.toString());
//            removeClientConnection(clientId);
            return UserStatus.UNREGISTERED;
        }

//        Long hbTimestamp = clientHeartBeats.get(clientId);
//        Long timeout;
//
//        if (hbTimestamp == null)
//            serverLogger.severe("Unable to get heartbeat from user:" + clientId);
//         else {
//            if ((timeout = now - hbTimestamp) > HEARTBEAT_TIMEOUT) {
//                serverLogger.severe("Heartbeat timeout from user:" + clientId+" Timeout="+timeout);
//            } else {
//                serverLogger.info(clientId + " is " + UserStatus.ONLINE.toString());
//                return UserStatus.ONLINE;
//            }
//        }
//
//		removeClientConnection(clientId);
//		serverLogger.info(clientId + " is " + UserStatus.OFFLINE.toString());
		return UserStatus.REGISTERED;
		
	}

//	public static void removeClientConnection(ConnectionToClient _ctc) {
//
//		onlineConnections.values().remove(_ctc);
//
//		logActiveConns();
//	}

//	public static void removeClientConnection(String clientId) {
//
//		serverLogger.info("Removing client connection. Client ID:"+clientId);
//		onlineConnections.remove(clientId);
//		clientHeartBeats.remove(clientId);
//
//		logActiveConns();
//	}
	
//	/**
//	 * @param clientId - The client id of which to send the event
//	 * @param eventReport - The event to send to the client
//	 * @return - returns 'true' if the event was sent successfully, else returns 'false' and removes the client connection from client pool
//	 */
//	public static boolean sendEventToClient(String clientId, EventReport eventReport)
//	{
//
//		try
//		{
//			ConnectionToClient ctc = ClientsManager.getClientConnection(clientId);
//
//			if(ctc==null)
//				throw(new NullPointerException("Could not get client connection for user:"+clientId));
//
//			serverLogger.info("[Sending event to user]:"+clientId+" with message:"+"\""+eventReport.desc()+"\""+" [Event Type]:"+eventReport.status().toString());
//			ctc.sendToClient(new MessageTriggerEventOnly(eventReport));
//			return true;
//		}
//		catch (IOException | NullPointerException e)
//		{
//			serverLogger.severe("[Failed to send event to user]:"+clientId+" of message:"+"\""+eventReport.desc()+"\""+" [Event Type]:"+eventReport.status().toString()+" [Exception]:"+e.getMessage()+ " Removing client connection...");
//			e.printStackTrace();
////			ClientsManager.removeClientConnection(clientId);
//			return false;
//		}
//
//	}
	
//	/**
//	 * @param clientId - The client clientId of which to send the message
//	 * @param msgToClient - The message to send to the client
//	 * @return - returns 'true' if message was sent successfully, else returns 'false' and removes the client connection from client pool
//	 */
//	public static boolean sendMessageToClient(String clientId, MessageToClient msgToClient)
//	{
//
//		try
//		{
//			ConnectionToClient ctc = ClientsManager.getClientConnection(clientId);
//			serverLogger.info("[Sending message to user]:"+clientId+" with message:"+msgToClient.getClass().getSimpleName());
//
//			if(ctc==null)
//				throw(new NullPointerException("Could not get client connection for user:"+clientId));
//
//			ctc.sendToClient(msgToClient);
//			return true;
//		}
//		catch (IOException | NullPointerException e)
//		{
//			serverLogger.severe("[Failed to send message to user]:"+clientId+" of message:"+msgToClient.getClass().getSimpleName()+" [Exception]:"+e.getMessage()+ "Terminating client connection...");
//			e.printStackTrace();
//			ClientsManager.removeClientConnection(clientId);
//			return false;
//		}
//
//	}
}
