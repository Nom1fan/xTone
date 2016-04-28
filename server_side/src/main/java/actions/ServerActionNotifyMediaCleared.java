package actions;

import java.util.HashMap;
import java.util.Map;

import DataObjects.DataKeys;
import DataObjects.PushEventKeys;
import EventObjects.EventReport;
import EventObjects.EventType;
import MessagesToClient.ClientActionType;
import MessagesToClient.MessageToClient;
import MessagesToServer.ServerActionType;
import pushservice.BatchPushSender;
import ServerObjects.ILangStrings;
import com.database.UsersDataAccess;
import lang.StringsFactory;

/**
 * Created by Mor on 23/04/2016.
 */
public class ServerActionNotifyMediaCleared extends ServerAction {

    public ServerActionNotifyMediaCleared() {
        super(ServerActionType.NOTIFY_MEDIA_CLEARED);
    }

    @Override
    public void doAction(Map data) {

        String clearRequesterId = (String) data.get(DataKeys.SOURCE_ID);
        String clearerId = _messageInitiaterId;
        String clearerName = (String) data.get(DataKeys.DESTINATION_CONTACT_NAME);

        _logger.info("Informing [Clear media requester]:" + clearRequesterId +
                " that [User]:" + clearerId +
                " cleared his media of [SpecialMediaType]: " + data.get(DataKeys.SPECIAL_MEDIA_TYPE));

        String clearRequesterToken = UsersDataAccess.instance(_dal).getUserPushToken(clearRequesterId);

        // Informing clear requester that media was cleared
        String sourceLocale = (String) data.get(DataKeys.SOURCE_LOCALE);
        ILangStrings strings;

        if(sourceLocale!=null)
            strings = StringsFactory.instance().getStrings(sourceLocale);
        else
            strings = StringsFactory.instance().getStrings(ILangStrings.ENGLISH);

        String title = strings.media_cleared_title();
        String msgBody = String.format(strings.media_cleared_body(), clearerName!=null && !clearerName.equals("") ? clearerName : clearerId);
        boolean sent = BatchPushSender.sendPush(clearRequesterToken, PushEventKeys.CLEAR_SUCCESS, title, msgBody, data);

        if(!sent) {
            _logger.severe("Failed to inform [Clear media requester]:" + clearRequesterId +
                    "that [User]:" + clearerId +
                    " cleared his media of [SpecialMediaType]:" + data.get(DataKeys.SPECIAL_MEDIA_TYPE));
        }

        HashMap<DataKeys, Object> replyData = new HashMap();
        replyData.put(DataKeys.EVENT_REPORT, new EventReport(EventType.NO_ACTION_REQUIRED, null, null));
        replyToClient(new MessageToClient(ClientActionType.TRIGGER_EVENT, replyData));
    }
}
