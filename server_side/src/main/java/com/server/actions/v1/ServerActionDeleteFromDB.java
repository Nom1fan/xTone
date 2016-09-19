package com.server.actions.v1;

import com.server.actions.ServerAction;

import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Map;

import DataObjects.DataKeys;
import EventObjects.EventReport;
import EventObjects.EventType;
import MessagesToClient.ClientActionType;
import MessagesToClient.MessageToClient;
import MessagesToServer.ServerActionType;

/**
 * Created by Mor on 18/06/2016.
 */
@Component("DELETE_FROM_DB")
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class ServerActionDeleteFromDB extends ServerAction {

    public ServerActionDeleteFromDB() {
        super(ServerActionType.DELETE_FROM_DB);
    }

    @Override
    public void doAction(Map data) throws IOException {

        String table = (String) data.get(DataKeys.TABLE);
        String col = (String) data.get(DataKeys.COLUMN);
        String con = (String) data.get(DataKeys.CONDITION);
        String val = (String) data.get(DataKeys.VALUE);


        try {
            dao.delete(table, col, con, val);
            replyData.put(DataKeys.EVENT_REPORT, new EventReport(EventType.DELETE_FROM_DB_SUCCESS));
        } catch (SQLException e) {
            replyData.put(DataKeys.EVENT_REPORT, new EventReport(EventType.DELETE_FROM_DB_FAILURE));
            e.printStackTrace();
        }

        replyToClient(new MessageToClient(ClientActionType.TRIGGER_EVENT, replyData));
    }
}
