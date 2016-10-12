package com.server.actions.v1;

import com.server.actions.ServerAction;
import com.server.database.dbos.UserDBO;

import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import DataObjects.DataKeys;
import MessagesToClient.ClientActionType;
import MessagesToClient.MessageToClient;
import MessagesToServer.ServerActionType;

/**
 * Created by Mor on 23/04/2016.
 */
@Component("UPDATE_USER_RECORD")
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class ServerActionUpdateUserRecord extends ServerAction {

    public ServerActionUpdateUserRecord() {
        super(ServerActionType.UPDATE_USER_RECORD);
    }

    @Override
    public void doAction(Map data) {

        logger.info(messageInitiaterId + " is updating its record...");

        String androidVersion = (String)data.get(DataKeys.ANDROID_VERSION);
        UserDBO userRecord = new UserDBO();
        userRecord.setAndroidVersion(androidVersion);

        HashMap<DataKeys,Object> replyData = new HashMap<>();
        try {
            dao.updateUserRecord(messageInitiaterId, userRecord);
            replyData.put(DataKeys.IS_UPDATE_SUCCESS, true);
        } catch (SQLException e) {
            e.printStackTrace();
            logger.severe("Failed to update record of user: " + messageInitiaterId + ". [Exception]:" + (e.getMessage()!=null ? e.getMessage() : e));
            replyData.put(DataKeys.IS_UPDATE_SUCCESS, false);
        }

        replyToClient(new MessageToClient(ClientActionType.UPDATE_RES, replyData));
    }

}

