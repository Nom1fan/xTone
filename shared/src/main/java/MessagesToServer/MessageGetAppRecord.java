package MessagesToServer;

import java.io.IOException;

import DataObjects.AppMetaRecord;
import MessagesToClient.MessageGetAppRecordRes;
import ServerObjects.AppMetaAccess;

/**
 * Created by Mor on 26/03/2016.
 */
public class MessageGetAppRecord extends MessageToServer {

    public MessageGetAppRecord(String messageInitiaterId) {
        super(messageInitiaterId);
    }

    @Override
    public boolean doServerAction() throws IOException, ClassNotFoundException {

        initLogger();

        AppMetaRecord appMetaRecord = AppMetaAccess.instance(_dal).getAppMeta();
        replyToClient(new MessageGetAppRecordRes(appMetaRecord));

        return false;
    }
}
