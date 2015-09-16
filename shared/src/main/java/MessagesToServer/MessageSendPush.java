package MessagesToServer;

import java.io.IOException;
import java.net.UnknownHostException;

import ServerObjects.ClientsManager;
import ServerObjects.PushSender;

/**
 * Created by Mor on 15/09/2015.
 */
public class MessageSendPush extends MessageToServer {

    private String _destId;

    public MessageSendPush(String messageInitiaterId, String destinationId) {
        super(messageInitiaterId);
        _destId = destinationId;
    }

    @Override
    public boolean doServerAction() throws IOException, ClassNotFoundException {

        initLogger();

        logger.info("Sending push to:"+_destId);

        String deviceToken = ClientsManager.getClientPushToken(_destId);
        boolean sent = PushSender.sendPush(deviceToken);

        if(sent)
            logger.info("Push sent successfully to:"+_destId);
        else
            logger.severe("Failed to send push to:"+_destId);

        return cont;
    }
}
