package MessagesToClient;

import java.io.IOException;

import ClientObjects.ConnectionToServer;
import EventObjects.EventReport;
import EventObjects.EventType;

public class MessageRegisterRes extends MessageToClient {

    private static final long serialVersionUID = 5071562666939252181L;

    @Override
    public EventReport doClientAction(ConnectionToServer connectionToServer) throws IOException {

        String okMsg = "REGISTER_SUCCESS";
        return new EventReport(EventType.REGISTER_SUCCESS, okMsg, null);
    }

}
