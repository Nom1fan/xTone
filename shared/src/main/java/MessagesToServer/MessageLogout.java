package MessagesToServer;

import java.io.IOException;
import java.net.UnknownHostException;
import ServerObjects.ClientsManager;

/**
 * Created by Mor on 04/09/2015.
 */
public class MessageLogout extends MessageToServer {

    public MessageLogout(String messageInitiaterId) {
        super(messageInitiaterId);
    }

    @Override
    public boolean doServerAction() throws UnknownHostException, IOException, ClassNotFoundException {

        logger.info("User logging out:"+_messageInitiaterId);
        ClientsManager.removeClientConnection(_messageInitiaterId);

        cont = false;
        return cont;
    }
}
