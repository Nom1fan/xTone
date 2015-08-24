package ClientObjects;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

import MessagesToClient.MessageToClient;
import MessagesToServer.MessageStartConnection;
import MessagesToServer.MessageToServer;

public class ConnectionToServer {
	
	private Socket _socketToServer;
	private ObjectInputStream in;
	private ObjectOutputStream out;

	
	public ConnectionToServer(Socket socketToServer) throws IOException {
		
		_socketToServer = socketToServer;
		out = new ObjectOutputStream(new BufferedOutputStream(_socketToServer.getOutputStream()));
		sendMessage(new MessageStartConnection(""));
		in = new ObjectInputStream(new BufferedInputStream(_socketToServer.getInputStream()));
				
	}
	
	public synchronized void sendMessage(MessageToServer msg) throws IOException {
		
		out.writeObject(msg);
		out.flush();
	}
	
	public MessageToClient getMessage() throws ClassNotFoundException, IOException {
		
		return (MessageToClient) in.readObject();
	}
	
	public void closeConnection() throws IOException {
		
		_socketToServer.close();
	}
	
	public void resetOutputStream() throws IOException {
		
		out.reset();
	}
	
	public Socket getSocket() {
		return _socketToServer;
	}

}
