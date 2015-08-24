package ClientObjects;

public interface IServerProxy {

	public void connect();	
//	public void downloadFileFromServer(TransferDetails td);
	public void uploadFileToServer(final byte[] fileData, String extension, final String destNumber);
	public void isLogin(final String phoneNumber);
	public ConnectionToServer getConnectionToServer();		
	public void startHeartBeatThread();	
	
}
