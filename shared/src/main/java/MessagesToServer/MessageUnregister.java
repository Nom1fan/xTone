package MessagesToServer;

import java.io.IOException;

import MessagesToClient.MessageUnregisterRes;
import ServerObjects.ClientsDataAccess;

/**
 * Created by Mor on 29/01/2016.
 */
public class MessageUnregister extends MessageToServer {

    private String _myId;
    private String _myToken;

    public MessageUnregister(String myId, String myToken) {
        super(myId);

        _myId = myId;
        _myToken = myToken;
    }

    @Override
    public boolean doServerAction() throws IOException, ClassNotFoundException {

        initLogger();

        boolean isSuccessful = ClientsDataAccess.instance(_dal).unregisterUser(_myId, _myToken);
        if(isSuccessful)
            _logger.info("Unregister was successful");
        else
            _logger.info("Unregister failed. Check stack trace log for more information...");
        replyToClient(new MessageUnregisterRes(isSuccessful));

        return _cont;
    }
}
