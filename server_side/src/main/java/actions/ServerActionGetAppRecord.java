package actions;

import com.database.AppMetaAccess;
import com.database.records.AppMetaRecord;

import java.util.HashMap;
import java.util.Map;

import DataObjects.DataKeys;
import MessagesToClient.ClientActionType;
import MessagesToClient.MessageToClient;
import MessagesToServer.ServerActionType;

/**
 * Created by Mor on 23/04/2016.
 */
public class ServerActionGetAppRecord extends ServerAction {

    public ServerActionGetAppRecord() {
        super(ServerActionType.GET_APP_RECORD);
    }

    @Override
    public void doAction(Map data) {

        AppMetaRecord appMetaRecord = AppMetaAccess.instance(_dal).getAppMeta();
        HashMap replyData = new HashMap();
        replyData.put(DataKeys.APP_VERSION, appMetaRecord.get_appVersion());
        replyData.put(DataKeys.MIN_SUPPORTED_VERSION, appMetaRecord.get_minSupportedVersion());
        replyToClient(new MessageToClient(ClientActionType.GET_APP_RECORD_RES, replyData));
    }
}
