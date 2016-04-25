package actions;

import java.io.IOException;
import java.util.Map;

import DataObjects.DataKeys;
import MessagesToClient.MessageUnregisterRes;
import MessagesToServer.ActionType;
import ServerObjects.UsersDataAccess;

/**
 * Created by Mor on 23/04/2016.
 */
public class ActionUnregister extends Action {

    public ActionUnregister() {
        super(ActionType.UNREGISTER);
    }

    @Override
    public void doAction(Map data) throws IOException {

        replyToClient(new MessageUnregisterRes(true)); //TODO remove boolean, unregister always succeeds for user as long as connection exists. Backend operations are for the server only
        String token = (String) data.get(DataKeys.PUSH_TOKEN);
        UsersDataAccess.instance(_dal).unregisterUser(_messageInitiaterId, token);
    }
}
