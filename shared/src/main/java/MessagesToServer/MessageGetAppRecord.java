package MessagesToServer;

import java.io.IOException;

import DataObjects.AppMetaRecord;
import MessagesToClient.MessageGetAppRecordRes;

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

        AppMetaRecord appMetaRecord = _appMetaManager.getAppMeta();
        replyToClient(new MessageGetAppRecordRes(appMetaRecord));

        return false;
    }
}
