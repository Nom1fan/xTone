package actions;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import DataObjects.DataKeys;
import DataObjects.UserRecord;
import MessagesToClient.ClientActionType;
import MessagesToClient.MessageToClient;
import MessagesToServer.ServerActionType;

/**
 * Created by Mor on 23/04/2016.
 */
public class ServerActionUpdateUserRecord extends ServerAction {

    public ServerActionUpdateUserRecord() {
        super(ServerActionType.UPDATE_USER_RECORD);
    }

    @Override
    public void doAction(Map data) {

        _logger.info(_messageInitiaterId + " is updating its record...");

        String androidVersion = (String)data.get(DataKeys.ANDROID_VERSION);
        UserRecord userRecord = new UserRecord();
        userRecord.setAndroidVersion(androidVersion);

        HashMap<DataKeys,Object> replyData = new HashMap<>();
        try {
            _dao.updateUserRecord(_messageInitiaterId, userRecord);
            replyData.put(DataKeys.IS_UPDATE_SUCCESS, true);
        } catch (SQLException e) {
            e.printStackTrace();
            _logger.severe("Failed to update record of user: " + _messageInitiaterId + ". [Exception]:" + (e.getMessage()!=null ? e.getMessage() : e));
            replyData.put(DataKeys.IS_UPDATE_SUCCESS, false);
        }

        replyToClient(new MessageToClient(ClientActionType.UPDATE_RES, replyData));
    }

}

