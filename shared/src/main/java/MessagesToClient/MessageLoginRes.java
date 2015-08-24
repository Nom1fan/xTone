package MessagesToClient;

import java.io.IOException;
import java.net.UnknownHostException;

import ClientObjects.IServerProxy;
import EventObjects.EventGenerator;
import EventObjects.EventReport;
import EventObjects.EventType;

public class MessageLoginRes extends MessageToClient {

                private static final long serialVersionUID = 5071562666939252181L;

                @Override
                public EventReport doClientAction(IServerProxy serverProxy) throws UnknownHostException, IOException {
                                
                                String okMsg = "LOGIN_SUCCESS";                                                                                                                                         
                                 return new EventReport(EventType.DISPLAY_MESSAGE, okMsg, null);
                                
                }


}
