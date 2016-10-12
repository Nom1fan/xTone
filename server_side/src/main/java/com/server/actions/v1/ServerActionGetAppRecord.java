package com.server.actions.v1;

import com.server.actions.ServerAction;
import com.server.database.dbos.AppMetaDBO;

import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

import DataObjects.DataKeys;
import DataObjects.ResponseCodes;
import MessagesToClient.ClientActionType;
import MessagesToClient.MessageToClient;
import MessagesToServer.ServerActionType;

/**
 * Created by Mor on 23/04/2016.
 */
@Component("GET_APP_RECORD")
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class ServerActionGetAppRecord extends ServerAction {

    public ServerActionGetAppRecord() {
        super(ServerActionType.GET_APP_RECORD);
    }

    @Override
    public void doAction(Map data) {

        HashMap replyData = new HashMap();
        try {
            AppMetaDBO appMetaDBO = dao.getAppMetaRecord();

            replyData.put(DataKeys.MIN_SUPPORTED_VERSION, appMetaDBO.getLast_supported_version());
            replyToClient(new MessageToClient(ClientActionType.GET_APP_RECORD_RES, replyData));
        } catch(Exception e) {
            replyData.put(DataKeys.RESPONSE_CODE, ResponseCodes.INTERNAL_SERVER_ERR);
            String errMsg = "Failed to retrieve app meta from DB. " + (e.getMessage()!=null ? "Exception:" + e.getMessage() : "");
            replyData.put(DataKeys.ERR_MSG, errMsg);
            replyToClient(new MessageToClient(ClientActionType.GENERIC_ERROR, replyData));
        }
    }
}
