package com.actions;

import android.util.Log;

import MessagesToClient.ClientActionType;

/**
 * Created by Mor on 23/04/2016.
 */
public class ActionFactory {

    private static final String TAG = ActionFactory.class.getSimpleName();
    private static ActionFactory _instance;

    private ActionFactory() {
        super();
    }

    public static ActionFactory instance() {

        if(_instance == null)
            _instance = new ActionFactory();
        return _instance;
    }

    public ClientAction getAction(ClientActionType clientActionType) {

        ClientAction resultClientAction = null;

        switch(clientActionType) {


            case DOWNLOAD_FILE:
                resultClientAction = new ClientActionDownloadFile();
                break;
            case GET_APP_RECORD_RES:
                resultClientAction = new ClientActionGetAppRecordRes();
                break;
            case IS_REGISTERED_RES:
                resultClientAction = new ClientActionIsRegisteredRes();
                break;
            case REGISTER_RES:
                resultClientAction = new ClientActionRegisterRes();
                break;
            case UNREGISTER_RES:
                resultClientAction = new ClientActionUnregisterRes();
                break;
            case TRIGGER_EVENT:
                resultClientAction = new ClientActionTriggerEvent();
                break;
            default:
                Log.e(TAG, String.format("Failure to perform action. No such client action: %s", clientActionType));

        }
        return resultClientAction;
    }
}
