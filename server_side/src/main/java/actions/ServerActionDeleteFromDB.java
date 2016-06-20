package actions;

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
            _dao.delete(table, col, con, val);
            _replyData.put(DataKeys.EVENT_REPORT, new EventReport(EventType.DELETE_FROM_DB_SUCCESS));
        } catch (SQLException e) {
            _replyData.put(DataKeys.EVENT_REPORT, new EventReport(EventType.DELETE_FROM_DB_FAILURE));
            e.printStackTrace();
        }

        replyToClient(new MessageToClient(ClientActionType.TRIGGER_EVENT, _replyData));
    }
}
