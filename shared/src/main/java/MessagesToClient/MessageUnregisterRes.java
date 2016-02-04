package MessagesToClient;

import java.io.IOException;
import ClientObjects.ConnectionToServer;
import EventObjects.EventReport;
import EventObjects.EventType;
import MessagesToServer.MessageUnregister;

/**
 * Created by Mor on 29/01/2016.
 */
public class MessageUnregisterRes extends MessageToClient {

    private boolean _unregisterSuccess;

    public MessageUnregisterRes(boolean unregisterSuccess) {

        _unregisterSuccess = unregisterSuccess;
    }

    @Override
    public EventReport doClientAction(ConnectionToServer connectionToServer) throws IOException {

        if(_unregisterSuccess)
            return new EventReport(EventType.UNREGISTER_SUCCESS, null, null);
        else
            return new EventReport(EventType.UNREGISTER_FAILURE, null, null);

    }
}
