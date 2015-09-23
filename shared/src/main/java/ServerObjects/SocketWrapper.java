package ServerObjects;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

import MessagesToClient.MessageStartConnectionRes;
import MessagesToClient.MessageToClient;
import MessagesToServer.MessageToServer;

@Deprecated
public class SocketWrapper {
	
	private Socket _clientSocket;
	private ObjectOutputStream _out;
	private ObjectInputStream _in;
	
	public SocketWrapper(Socket clientSocket) throws IOException {
		
		_clientSocket = clientSocket;		
		_out = new ObjectOutputStream(new BufferedOutputStream(_clientSocket.getOutputStream()));		
		_in = new ObjectInputStream(new BufferedInputStream(_clientSocket.getInputStream()));
		writeToClient(new MessageStartConnectionRes());
		
	}
	
	public void writeToClient(MessageToClient msg) throws IOException {
		
		_out.writeObject(msg);
		_out.flush();
		
	}
	
	public MessageToServer getClientMessage() throws ClassNotFoundException, IOException {
		
		return (MessageToServer) _in.readObject();
	}

	public Socket getSocket() {
		
		return _clientSocket;				
	}
	
	public void closeSocket() throws IOException {
		
		_clientSocket.close();
		
	}
}
