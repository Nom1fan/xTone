package MessagesToClient;

import java.io.IOException;

import ClientObjects.ConnectionToServer;
import EventObjects.EventReport;
import EventObjects.EventType;

public class MessageRegisterRes extends MessageToClient {

    private static final long serialVersionUID = 5071562666939252181L;
    private boolean _isRegisterSuccess;

    public MessageRegisterRes(boolean isRegisterSuccess) {

        _isRegisterSuccess = isRegisterSuccess;
    }

    @Override
    public EventReport doClientAction(ConnectionToServer connectionToServer) throws IOException {

        String msg;
        EventType eventType;
        if(_isRegisterSuccess) {
            msg = "Registration successful";
            eventType = EventType.REGISTER_SUCCESS;
        }
        else {
            msg = "Registration failed. Please reinstall.";
            eventType = EventType.DISPLAY_ERROR;
        }

        return new EventReport(eventType, msg, null);
    }

}
