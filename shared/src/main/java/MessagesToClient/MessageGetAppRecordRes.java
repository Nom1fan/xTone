package MessagesToClient;

import java.io.IOException;

import ClientObjects.ConnectionToServer;
import DataObjects.AppMetaRecord;
import DataObjects.SharedConstants;
import EventObjects.EventReport;
import EventObjects.EventType;

/**
 * Created by Mor on 26/03/2016.
 */
public class MessageGetAppRecordRes extends MessageToClient {

    private AppMetaRecord _appMetaRecord;

    public MessageGetAppRecordRes(AppMetaRecord appMetaRecord) {

        this._appMetaRecord = appMetaRecord;
    }

    @Override
    public EventReport doClientAction(ConnectionToServer connectionToServer) throws IOException {

        return new EventReport(EventType.APP_RECORD_RECEIVED, null, _appMetaRecord);

    }
}
