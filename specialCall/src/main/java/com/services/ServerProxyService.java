package com.services;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import com.client.ConnectionToServer;
import com.data.objects.Constants;
import com.event.EventReport;
import com.event.EventType;
import com.handlers.ActionHandler;
import com.handlers.HandlerFactory;
import com.model.request.Request;
import com.utils.BroadcastUtils;
import com.utils.RequestUtils;
import com.utils.SharedPrefUtils;

import static com.crashlytics.android.Crashlytics.log;

/**
 * Created by mor on 18/10/2015.
 */
public class ServerProxyService extends Service implements Runnable {

    private static final String TAG = ServerProxyService.class.getSimpleName();
    protected static final String HOST = Constants.SERVER_HOST;
    protected static final int PORT = Constants.SERVER_PORT;
    protected static final String ROOT_URL = "http://" + HOST + ":" + PORT;

    //region Service actions
    public static final String ACTION_REGISTER = "com.services.ServerProxyService.REGISTER";
    public static final String ACTION_UNREGISTER = "com.services.ServerProxyService.UNREGISTER";
    public static final String ACTION_ISREGISTERED = "com.services.ServerProxyService.ISREGISTERED";
    public static final String ACTION_INSERT_CALL_RECORD = "com.services.ServerProxyService.INSERT_CALL_RECORD";
    public static final String ACTION_GET_APP_RECORD = "com.services.ServerProxyService.GET_APP_RECORD";
    public static final String ACTION_GET_SMS_CODE = "com.services.ServerProxyService.GET_SMS_CODE";
    public static final String ACTION_UPDATE_USER_RECORD = "com.services.ServerProxyService.UPDATE_USER_RECORD";
    public static final String ACTION_DOWNLOAD = "com.services.ServerProxyService.DOWNLOAD";
    public static final String ACTION_NOTIFY_MEDIA_CLEARED = "com.services.ServerProxyService.NOTIFY_MEDIA_CLEARED";
    public static final String ACTION_CLEAR_MEDIA = "com.services.ServerProxyService.CLEAR_MEDIA";
    //endregion

    //region Service intent keys
    public static final String DESTINATION_ID = "DESTINATION_ID";
    public static final String CALL_RECORD = "CALL_RECORD";
    public static final String SMS_CODE = "SMS_CODE";
    public static final String INTERNATIONAL_PHONE = "INTERNATIONAL_PHONE";
    public static final String SPECIAL_MEDIA_TYPE = "SPECIAL_MEDIA_TYPE";
    public static final String TRANSFER_DETAILS = "TRANSFER_DETAILS";
    //endregion

    private Intent intent;
    private int flags;
    private int startId;

    //region Service methods
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        log(Log.INFO, TAG, "LogicServerProxyService started");

        this.intent = intent;
        this.flags = flags;
        this.startId = startId;

        boolean shouldStop = handleCrashedService(flags, startId);
        if (shouldStop)
            return START_REDELIVER_INTENT;

        new Thread(this).start();

        return START_REDELIVER_INTENT;
    }

    @Override
    public void onCreate() {
        log(Log.INFO, TAG, "Created");
    }

    @Override
    public void onDestroy() {
        log(Log.ERROR, TAG, "Being destroyed");
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
    //endregion

    @Override
    public void run() {
        if (intent != null) {
            String action = intent.getAction();
            log(Log.INFO, TAG, "Action:" + action);

            Request request = createDefaultRequest();
            ConnectionToServer connectionToServer = new ConnectionToServer();

            try {
                ActionHandler actionHandler = HandlerFactory.getInstance().getActionHandler(action);
                ActionHandler.ActionBundle actionBundle = new ActionHandler.ActionBundle();
                actionBundle.
                        setCtx(getApplication()).
                        setConnectionToServer(connectionToServer).
                        setIntent(intent).
                        setRequest(request);
                setMidAction(true);
                actionHandler.handleAction(actionBundle);
                setMidAction(false);

            } catch (Exception e) {
                e.printStackTrace();
                String errMsg = "Action failed:" + action + " Exception:" + e.getMessage();
                handleActionFailure();
                log(Log.ERROR, TAG, errMsg);
            } finally {
                connectionToServer.disconnect();
            }
        } else
            log(Log.WARN, TAG, "Service started with missing action");

        markCrashedServiceHandlingComplete(flags, startId);
    }

    private void setMidAction(boolean bool) {
        log(Log.INFO, TAG, "Setting midAction=" + bool);
        SharedPrefUtils.setBoolean(this, SharedPrefUtils.SERVER_PROXY, SharedPrefUtils.WAS_MID_ACTION, bool);
    }

    private boolean wasMidAction() {
        return SharedPrefUtils.getBoolean(this, SharedPrefUtils.SERVER_PROXY, SharedPrefUtils.WAS_MID_ACTION);
    }

    private boolean handleCrashedService(int flags, int startId) {

        boolean shouldStop = false;
        // If crash restart occurred but was not mid-action we should do nothing
        if ((flags & START_FLAG_REDELIVERY) != 0 && !wasMidAction()) {
            log(Log.INFO, TAG, "Crash restart occurred but was not mid-action (wasMidAction()=" + wasMidAction() + ". Exiting service.");
            stopSelf(startId);
            shouldStop = true;
        }

        return shouldStop;
    }

    private void markCrashedServiceHandlingComplete(int flags, int startId) {
        if ((flags & START_FLAG_REDELIVERY) != 0) { // if we took care of crash restart, mark it as completed
            stopSelf(startId);
        }
    }

    private Request createDefaultRequest() {
        Request request = new Request();
        RequestUtils.prepareDefaultRequest(getApplicationContext(), request);
        return request;
    }

    private void handleActionFailure() {
        BroadcastUtils.sendEventReportBroadcast(this, TAG, new EventReport(EventType.LOADING_TIMEOUT));
    }
    //endregion
}
