package MessagesToServer;

import java.io.IOException;

import MessagesToClient.MessageUnregisterRes;
import ServerObjects.UsersDataAccess;

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

        replyToClient(new MessageUnregisterRes(true)); //TODO remove boolean, unregister always succeeds for user as long as connection exists. Backend operations are for the server only
        UsersDataAccess.instance(_dal).unregisterUser(_myId, _myToken);
        return _cont;
    }
}
