package actions;

import java.util.Map;

import DataObjects.AppMetaRecord;
import MessagesToClient.MessageGetAppRecordRes;
import MessagesToServer.ActionType;
import ServerObjects.AppMetaAccess;

/**
 * Created by Mor on 23/04/2016.
 */
public class ActionGetAppRecord extends Action {

    public ActionGetAppRecord() {
        super(ActionType.GET_APP_RECORD);
    }

    @Override
    public void doAction(Map data) {

        AppMetaRecord appMetaRecord = AppMetaAccess.instance(_dal).getAppMeta();
        replyToClient(new MessageGetAppRecordRes(appMetaRecord));
    }
}
