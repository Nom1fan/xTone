package actions;

import java.util.HashMap;
import java.util.Map;

import DataObjects.DataKeys;
import MessagesToClient.ClientActionType;
import MessagesToClient.MessageToClient;
import MessagesToServer.ServerActionType;
import com.database.UsersDataAccess;

/**
 * Created by Mor on 23/04/2016.
 */
public class ServerActionIsRegistered extends ServerAction {

    private String _id;

    public ServerActionIsRegistered() {
        super(ServerActionType.IS_REGISTERED);
    }

    @Override
    public void doAction(Map data) {

        _id = (String) data.get(DataKeys.DESTINATION_ID);
        _logger.info(_messageInitiaterId + " is checking if " + _id + " is logged in...");
        data.put(DataKeys.IS_REGISTERED, UsersDataAccess.instance(_dao).isRegistered(_id));
        replyToClient(new MessageToClient(ClientActionType.IS_REGISTERED_RES, (HashMap)data));
    }
}
