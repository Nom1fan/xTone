package MessagesToServer;


import java.io.IOException;
import java.net.UnknownHostException;

import ClientObjects.UserStatus;
import MessagesToClient.MessageIsLoginRes;
import ServerObjects.ClientsManager;
import ServerObjects.PushSender;

public class MessageIsLogin extends MessageToServer {
		
	private static final long serialVersionUID = -2625228196534308145L;
	private String _id;
	private UserStatus userStatus;
	private final int ISLOGIN_RETRIES = 2;
	private final int ISLOGIN_SLEEP_INTERVAL = 1500; // milliseconds

	
	public MessageIsLogin(String srcId, String destId) {
		super(srcId);
		_id = destId;
	}

	@Override
	public boolean doServerAction() throws IOException, ClassNotFoundException {

		initLogger();

		logger.info(_messageInitiaterId + " is checking if " + _id + " is logged in...");

		// Sending push to wake up client before checking heartbeat
		String deviceToken = ClientsManager.getClientPushToken(_id);

		if (deviceToken == null || deviceToken.equals("")) {

			logger.info("No device token found for user:" + _id + ". User is " + UserStatus.UNREGISTERED.toString());
			userStatus = UserStatus.UNREGISTERED;
		}
		else
		{
			int retries = 0;
            boolean keepTrying = true;
			while (keepTrying && (retries < ISLOGIN_RETRIES))
			{
				boolean sent = PushSender.sendPush(deviceToken);
				if (!sent)
				{
					retries++;
					logger.severe("Unable to send push to user:" + _id);
					logger.severe("retries=" + retries);
				}
                else
                {
					try {
						Thread.sleep(ISLOGIN_SLEEP_INTERVAL);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					userStatus = ClientsManager.isLogin(_id);
                    switch(userStatus)
                    {
                        case OFFLINE:
                            logger.severe("retries="+retries);
                            retries++;
                        break;

                        case ONLINE:
                            keepTrying = false;
                        break;
                    }
				}
			}
		}


		MessageIsLoginRes res = new MessageIsLoginRes(_id, userStatus);

		clientConnection.writeToClient(res);
		
		logger.info("Sent response to client:"+_messageInitiaterId);
		
		return cont;
		
	}


	
	

	

}
