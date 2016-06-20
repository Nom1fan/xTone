package actions;

import MessagesToServer.ServerActionType;
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

    public ServerAction getAction(ServerActionType serverServerActionType) {

        ServerAction resultServerAction = null;

        switch(serverServerActionType) {

            case CLEAR_MEDIA:
                resultServerAction = new ServerActionClearMedia();
                break;
            case GET_APP_RECORD:
                resultServerAction = new ServerActionGetAppRecord();
                break;
            case GET_SMS_CODE:
                resultServerAction = new ServerActionGetSmsCode();
                break;
            case INSERT_MEDIA_CALL_RECORD:
                resultServerAction = new ServerActionInsertMediaCallRecord();
                break;
            case IS_REGISTERED:
                resultServerAction = new ServerActionIsRegistered();
                break;
            case NOTIFY_MEDIA_CLEARED:
                resultServerAction = new ServerActionNotifyMediaCleared();
                break;
            case REGISTER:
                resultServerAction = new ServerActionRegister();
                break;
            case REQUEST_DOWNLOAD:
                resultServerAction = new ServerActionRequestDownload();
                break;
            case UNREGISTER:
                resultServerAction = new ServerActionUnregister();
                break;
            case UPLOAD_FILE:
                resultServerAction = new ServerActionUploadFile();
                break;
            case UPDATE_USER_RECORD:
                resultServerAction = new ServerActionUpdateUserRecord();
                break;
            case GET_SMS_CODE_FOR_LOAD_TEST:
                resultServerAction = new ServerActionGetSmsCodeForLoadTest();
                break;
            case DELETE_FROM_DB:
                resultServerAction = new ServerActionDeleteFromDB();
                break;

            default:
                _logger.severe(String.format("Failure to perform action. No such serverActionType: %s", serverServerActionType));

        }
        return resultServerAction;
    }
}
