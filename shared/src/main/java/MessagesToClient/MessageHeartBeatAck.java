package MessagesToClient;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.Date;

import ClientObjects.IServerProxy;
import EventObjects.EventReport;
import EventObjects.EventType;

/**
 * Created by rony on 11/10/2015.
 */
public class MessageHeartBeatAck extends MessageToClient {

    @Override
    public EventReport doClientAction(IServerProxy serverProxy) throws IOException {

        Date date = new Date();
        Long timestamp = date.getTime();

        serverProxy.markHeartBeatAck(timestamp);

        return new EventReport(EventType.NO_ACTION_REQUIRED, null , null);
    }
}
