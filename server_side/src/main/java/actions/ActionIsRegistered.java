package actions;

import java.util.Map;

import DataObjects.DataKeys;
import MessagesToClient.MessageIsRegisteredRes;
import MessagesToServer.ActionType;
import ServerObjects.UsersDataAccess;

/**
 * Created by Mor on 23/04/2016.
 */
public class ActionIsRegistered extends Action {

    private String _id;

    public ActionIsRegistered() {
        super(ActionType.IS_REGISTERED);
    }

    @Override
    public void doAction(Map data) {

        _id = (String) data.get(DataKeys.DESTINATION_ID);
        _logger.info(_messageInitiaterId + " is checking if " + _id + " is logged in...");
        MessageIsRegisteredRes msgReply = new MessageIsRegisteredRes(_id, UsersDataAccess.instance(_dal).isRegistered(_id));
        replyToClient(msgReply);
    }
}
