package ServerObjects;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Set;
import java.util.logging.Logger;

import ClientObjects.UserStatus;
import EventObjects.EventReport;
import EventObjects.EventType;
import LogObjects.LogsManager;
import MessagesToClient.MessageToClient;
import MessagesToClient.MessageTriggerEventOnly;


/**
 * This class manages all client connections
 * @author Mor
 */
public class ClientsManager {

	// NOTE: <String,ConnectionToClient> means phone is key and a connection to the client is the value	
	private static HashMap<String,ConnectionToClient> onlineConnections = new HashMap<>(); // All online connections
	// NOTE: <String,Long> means phone is key and (UNIX timestamp * 1000) is the value	
	private static HashMap<String,Long> clientHeartBeats = new HashMap<>(); // all online client heart beats
	private static final int HEARTBEAT_TIMEOUT = 25*1000; // 25 seconds - 5 seconds more than client's heartbeat rate
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
		
		serverLogger = LogsManager.getServerLogger("Server");
		hbLogger = LogsManager.getNewLogger("heartbeats");
	
	}
	
	public synchronized static ConnectionToClient getClientConnection(String id) {
		
		return onlineConnections.get(id);
	}
	
	public synchronized static void addClientConnection(String phone, ConnectionToClient ctc) {
						
		onlineConnections.put(phone, ctc);
		
		logActiveConns();
	}

	public synchronized static void markClientHeartBeat(String phone, ConnectionToClient ctc) {
		
		Date date = new Date();
		Long timestamp = date.getTime();
		
		clientHeartBeats.put(phone, timestamp);
		onlineConnections.put(phone, ctc);
		
		hbLogger.info(phone);
	}
	
	public synchronized static UserStatus isLogin(String clientId) throws IOException, ClassNotFoundException {
								
		
		Date date = new Date();
		Long now = date.getTime();
		Long hbTimestamp =  clientHeartBeats.get(clientId);
		
		if(hbTimestamp==null)
		{
			removeClientConnection(clientId);
			serverLogger.info(clientId +" is "+UserStatus.OFFLINE.toString());
			return UserStatus.OFFLINE;
		}
		else
		{
			if(now-hbTimestamp > HEARTBEAT_TIMEOUT)
			{
				removeClientConnection(clientId);
				serverLogger.info(clientId +" is "+UserStatus.OFFLINE.toString());
				return UserStatus.OFFLINE;
			}
		}
		
		serverLogger.info(clientId +" is "+UserStatus.ONLINE.toString());		
		
		return UserStatus.ONLINE;
			
		//return UserStatus.UNREGISTERED;
		
	}

	public static void removeClientConnection(ConnectionToClient _ctc) {
					
		onlineConnections.values().remove(_ctc);
		
		logActiveConns();
	}

	public static void removeClientConnection(String clientId) {
		
		onlineConnections.remove(clientId);
		clientHeartBeats.remove(clientId);
		
		logActiveConns();
	}
	
	/**
	 * @param id - The client id of which to send the event
	 * @param eventReport - The event to send to the client
	 * @return - returns 'true' if the event was sent successfully, else returns 'false' and removes the client connection from client pool
	 */
	public static boolean sendEventToClient(String id, EventReport eventReport) 
	{
		
		try 
		{
			ConnectionToClient ctc = ClientsManager.getClientConnection(id);
			
			if(ctc==null)
				throw(new NullPointerException("Could not get client connection for user:"+id));
			
			serverLogger.info("[Sending event to user]:"+id+" with message:"+"\""+eventReport.desc()+"\""+" [Event Type]:"+eventReport.status().toString());
			ctc.writeToClient(new MessageTriggerEventOnly(eventReport));
			return true;
		} 
		catch (IOException | NullPointerException e) 
		{			
			serverLogger.severe("[Failed to send event to user]:"+id+" of message:"+"\""+eventReport.desc()+"\""+" [Event Type]:"+eventReport.status().toString()+" [Exception]:"+e.getMessage()+ " Removing client connection...");
			e.printStackTrace();
			ClientsManager.removeClientConnection(id);
			return false;
		}
		
	}
	
	/**
	 * @param id - The client id of which to send the message
	 * @param msgToClient - The message to send to the client
	 * @return - returns 'true' if message was sent successfully, else returns 'false' and removes the client connection from client pool
	 */
	public static boolean sendMessageToClient(String id, MessageToClient msgToClient) 
	{
		
		try 
		{
			ConnectionToClient ctc = ClientsManager.getClientConnection(id);
			serverLogger.info("[Sending message to user]:"+id+" with message:"+msgToClient.getClass().getSimpleName());
			
			if(ctc==null)
				throw(new NullPointerException("Could not get client connection for user:"+id));
			
			ctc.writeToClient(msgToClient);
			return true;
		} 
		catch (IOException | NullPointerException e) 
		{			
			serverLogger.severe("[Failed to send message to user]:"+id+" of message:"+msgToClient.getClass().getSimpleName()+" [Exception]:"+e.getMessage()+ "Terminating client connection...");
			e.printStackTrace();
			ClientsManager.removeClientConnection(id);
			return false;
		}
		
	}
}
