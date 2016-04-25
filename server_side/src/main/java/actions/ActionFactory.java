package actions;

import MessagesToServer.ActionType;
import log.Logged;

/**
 * Created by Mor on 23/04/2016.
 */
public class ActionFactory extends Logged {

    private static ActionFactory _instance;

    private ActionFactory() {
        super();
    }

    public static ActionFactory instance() {

        if(_instance == null)
            _instance = new ActionFactory();
        return _instance;
    }

    public Action getAction(ActionType actionType) {

        Action resultAction = null;

        switch(actionType) {

            case CLEAR_MEDIA:
                resultAction = new ActionClearMedia();
                break;
            case GET_APP_RECORD:
                resultAction = new ActionGetAppRecord();
                break;
            case GET_SMS_CODE:
                resultAction = new ActionGetSmsCode();
                break;
            case INSERT_MEDIA_CALL_RECORD:
                resultAction = new ActionInsertMediaCallRecord();
                break;
            case IS_REGISTERED:
                resultAction = new ActionIsRegistered();
                break;
            case NOTIFY_MEDIA_CLEARED:
                resultAction = new ActionNotifyMediaCleared();
                break;
            case REGISTER:
                resultAction = new ActionRegister();
                break;
            case REQUEST_DOWNLOAD:
                resultAction = new ActionRequestDownload();
                break;
            case UNREGISTER:
                resultAction = new ActionUnregister();
                break;
            case UPLOAD_FILE:
                resultAction = new ActionUploadFile();
                break;

            default:
                _logger.severe(String.format("Failure to perform action. No such _actionType: %s", actionType));

        }
        return resultAction;
    }
}
