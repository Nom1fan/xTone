package com.server.actions.v1;

import com.server.actions.ServerAction;

import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

import DataObjects.CallRecord;
import DataObjects.DataKeys;
import EventObjects.EventReport;
import EventObjects.EventType;
import MessagesToClient.ClientActionType;
import MessagesToClient.MessageToClient;
import MessagesToServer.ServerActionType;

/**
 * Created by Mor on 23/04/2016.
 */
@Component("INSERT_MEDIA_CALL_RECORD")
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class ServerActionInsertMediaCallRecord extends ServerAction {

    public ServerActionInsertMediaCallRecord() {
        super(ServerActionType.INSERT_MEDIA_CALL_RECORD);
    }

    @Override
    public void doAction(Map data) {

        CallRecord callRecord = (CallRecord) data.get(DataKeys.CALL_RECORD);
        logger.info("Inserting call record" + callRecord.toString());
        try {
            dao.insertMediaCallRecord(callRecord);
        } catch (SQLException e) {
            e.printStackTrace();
            logger.log(Level.SEVERE, e.getMessage(), e);
        }
        HashMap<DataKeys, Object> replyData = new HashMap<>();
        replyData.put(DataKeys.EVENT_REPORT, new EventReport(EventType.NO_ACTION_REQUIRED));
        replyToClient(new MessageToClient(ClientActionType.TRIGGER_EVENT, replyData));
    }
}
