package com.handlers;

import android.util.Log;

import com.crashlytics.android.Crashlytics;
import com.data.objects.ActivityRequestCodes;
import com.event.EventType;
import com.handlers.background_broadcast_receiver.EventClearFailureHandler;
import com.handlers.background_broadcast_receiver.EventClearSentHandler;
import com.handlers.background_broadcast_receiver.EventClearSuccessHandler;
import com.handlers.background_broadcast_receiver.EventCompressingHandler;
import com.handlers.background_broadcast_receiver.EventConnectedHandler;
import com.handlers.background_broadcast_receiver.EventDestinationDownloadCompleteHandler;
import com.handlers.background_broadcast_receiver.EventDisplayErrorHandler;
import com.handlers.background_broadcast_receiver.EventDisplayMessageHandler;
import com.handlers.background_broadcast_receiver.EventFetchingUserDataHandler;
import com.handlers.background_broadcast_receiver.EventGetSmsCodeFailureHandler;
import com.handlers.background_broadcast_receiver.EventLoadingCancelHandler;
import com.handlers.background_broadcast_receiver.EventLoadingTimeoutHandler;
import com.handlers.background_broadcast_receiver.EventNegativeEventHandler;
import com.handlers.background_broadcast_receiver.EventRegisterFailureHandler;
import com.handlers.background_broadcast_receiver.EventUnregisterSuccessHandler;
import com.handlers.background_broadcast_receiver.EventUpdateUserRecordSuccessHandler;
import com.handlers.background_broadcast_receiver.EventUserRegisteredFalseHandler;
import com.handlers.background_broadcast_receiver.EventUserRegisteredTrueHandler;
import com.handlers.logic_server_proxy_service.GetAppRecordActionHandler;
import com.handlers.logic_server_proxy_service.GetSmsActionHandler;
import com.handlers.logic_server_proxy_service.InsertCallRecordActionHandler;
import com.handlers.logic_server_proxy_service.IsRegisteredActionHandler;
import com.handlers.logic_server_proxy_service.RegisterActionHandler;
import com.handlers.logic_server_proxy_service.UnregisterActionHandler;
import com.handlers.logic_server_proxy_service.UpdateUserRecordActionHandler;
import com.handlers.select_media_activity.ActivityRequestCameraHandler;
import com.handlers.select_media_activity.ActivityRequestFileChooserHandler;
import com.handlers.select_media_activity.ActivityRequestPreviewMediaResultHandler;
import com.receivers.BackgroundBroadcastReceiver;
import com.services.LogicServerProxyService;
import com.ui.activities.MainActivity;
import com.ui.activities.SelectMediaActivity;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by Mor on 16/07/2016.
 */
public class HandlerFactory {

    private static final String TAG = HandlerFactory.class.getSimpleName();

    private static HandlerFactory ourInstance;

    private Map<EventType, Class<Handler>> mainActivityHandlers = new HashMap() {{
        //put(EventType.APP_RECORD_RECEIVED, new EventAppRecordReceivedHandler()); //TODO Mor: Add this instead of the code in MainActivity
    }};

    private Map<Integer, Class<Handler>> selectMediaActivityRequestHandlers = new HashMap() {{
        put(ActivityRequestCodes.PREVIEW_MEDIA, ActivityRequestPreviewMediaResultHandler.class);
        put(ActivityRequestCodes.FIlE_CHOOSER, ActivityRequestFileChooserHandler.class);
        put(ActivityRequestCodes.REQUEST_CAMERA, ActivityRequestCameraHandler.class);
    }};

    private Map<EventType, Class<Handler>> bgEventHandlers = new HashMap() {{
        put(EventType.FETCHING_USER_DATA, EventFetchingUserDataHandler.class);
        put(EventType.CLEAR_FAILURE, EventClearFailureHandler.class);
        put(EventType.CLEAR_SENT, EventClearSentHandler.class);
        put(EventType.CLEAR_SUCCESS, EventClearSuccessHandler.class);
        put(EventType.ISREGISTERED_ERROR, EventNegativeEventHandler.class);
        put(EventType.STORAGE_ACTION_FAILURE, EventNegativeEventHandler.class);
        put(EventType.UNREGISTER_FAILURE, EventNegativeEventHandler.class);
        put(EventType.USER_REGISTERED_FALSE, EventUserRegisteredFalseHandler.class);
        put(EventType.USER_REGISTERED_TRUE, EventUserRegisteredTrueHandler.class);
        put(EventType.UNREGISTER_SUCCESS, EventUnregisterSuccessHandler.class);
        put(EventType.UPDATE_USER_RECORD_SUCCESS, EventUpdateUserRecordSuccessHandler.class);
        put(EventType.REGISTER_FAILURE, EventRegisterFailureHandler.class);
        put(EventType.DESTINATION_DOWNLOAD_COMPLETE, EventDestinationDownloadCompleteHandler.class);
        put(EventType.CONNECTED, EventConnectedHandler.class);
        put(EventType.COMPRESSING, EventCompressingHandler.class);
        put(EventType.DISPLAY_ERROR, EventDisplayErrorHandler.class);
        put(EventType.DISPLAY_MESSAGE, EventDisplayMessageHandler.class);
        put(EventType.GET_SMS_CODE_FAILURE, EventGetSmsCodeFailureHandler.class);
        put(EventType.LOADING_CANCEL, EventLoadingCancelHandler.class);
        put(EventType.LOADING_TIMEOUT, EventLoadingTimeoutHandler.class);
    }};

    private Map<String, Map> class2HandlerMap = new HashMap() {{
        put(BackgroundBroadcastReceiver.class.getSimpleName(), bgEventHandlers);
        put(SelectMediaActivity.class.getSimpleName(), selectMediaActivityRequestHandlers);
        put(MainActivity.class.getSimpleName(), mainActivityHandlers);
    }};

    private Map<String, Class<ActionHandler>> class2ActionHandlerMap = new HashMap(){{
        put(LogicServerProxyService.ACTION_REGISTER, RegisterActionHandler.class);
        put(LogicServerProxyService.ACTION_GET_SMS_CODE, GetSmsActionHandler.class);
        put(LogicServerProxyService.ACTION_GET_APP_RECORD, GetAppRecordActionHandler.class);
        put(LogicServerProxyService.ACTION_ISREGISTERED, IsRegisteredActionHandler.class);
        put(LogicServerProxyService.ACTION_INSERT_CALL_RECORD, InsertCallRecordActionHandler.class);
        put(LogicServerProxyService.ACTION_UPDATE_USER_RECORD, UpdateUserRecordActionHandler.class);
        put(LogicServerProxyService.ACTION_UNREGISTER, UnregisterActionHandler.class);
    }};

    public static HandlerFactory getInstance() {
        if (ourInstance == null)
            ourInstance = new HandlerFactory();
        return ourInstance;
    }

    public <T> Handler getHandler(String className, T key) {
        Handler resultHandler = null;
        Class<Handler> handlerClass;
        try {
            Map handlers = class2HandlerMap.get(className);
            if (handlers == null) {
                throw new Exception("No handler map for class:" + className);
            }

            handlerClass = (Class<Handler>) handlers.get(key);
            if (handlerClass == null) {
                throw new Exception("No handler for key:" + key);
            }

            resultHandler = handlerClass.newInstance();

        } catch (Exception e) {
            Crashlytics.log(Log.WARN, TAG, e.getMessage());

        }
        return resultHandler;
    }

    public ActionHandler getActionHandler(String action) {
        ActionHandler resultHandler = null;
        Class<ActionHandler> handlerClass;
        try {

            handlerClass = class2ActionHandlerMap.get(action);
            if (handlerClass == null) {
                throw new Exception("No handler for action:" + action);
            }

            resultHandler = handlerClass.newInstance();

        } catch (Exception e) {
            Crashlytics.log(Log.WARN, TAG, e.getMessage());

        }
        return resultHandler;
    }
}

