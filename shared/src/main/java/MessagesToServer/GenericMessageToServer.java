package MessagesToServer;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import DataObjects.DataKeys;

/**
 * Created by Mor on 23/04/2016.
 */
public class GenericMessageToServer implements Serializable {

    protected String _messageInitiaterId;
    protected HashMap<DataKeys, Object> _data;
    protected ActionType _actionType;

    public GenericMessageToServer(String messegeInitiaterId, ActionType actionType) {

        _messageInitiaterId = messegeInitiaterId;
        _actionType = actionType;
    }

    public GenericMessageToServer(String messageInitiaterId, HashMap<DataKeys, Object> data, ActionType actionType) {

        _messageInitiaterId = messageInitiaterId;
        _data = data;
        _actionType = actionType;
    }

    public String get_messageInitiaterId() {
        return _messageInitiaterId;
    }

    public Map getData() {
        return _data;
    }

    public ActionType getActionType() {
        return _actionType;
    }
}
