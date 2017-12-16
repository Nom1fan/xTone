package com_international.services;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import com_international.app.AppStateManager;
import com_international.client.ConnectionToServerImpl;
import com_international.data.objects.DefaultMediaData;
import com_international.data.objects.PendingDownloadData;
import com_international.data.objects.PushEventKeys;
import com_international.enums.SpecialMediaType;
import com_international.event.EventReport;
import com_international.event.EventType;
import com_international.handlers.ActionHandler;
import com_international.handlers.HandlerFactory;
import com_international.mediacallz.app.R;
import com_international.model.request.Request;
import com_international.utils.BroadcastUtils;
import com_international.utils.RequestUtils;
import com_international.utils.SharedPrefUtils;

import static com.crashlytics.android.Crashlytics.log;

/**
 * Created by mor on 18/10/2015.
 */
public class ServerProxyService extends Service implements Runnable {

    private static final String TAG = ServerProxyService.class.getSimpleName();

    //region Service actions
    public static final String ACTION_REGISTER = "ServerProxyService.REGISTER";
    public static final String ACTION_UNREGISTER = "ServerProxyService.UNREGISTER";
    public static final String ACTION_ISREGISTERED = "ServerProxyService.ISREGISTERED";
    public static final String ACTION_INSERT_CALL_RECORD = "ServerProxyService.INSERT_CALL_RECORD";
    public static final String ACTION_GET_APP_RECORD = "ServerProxyService.GET_APP_RECORD";
    public static final String ACTION_GET_SMS_CODE = "ServerProxyService.GET_SMS_CODE";
    public static final String ACTION_UPDATE_USER_RECORD = "ServerProxyService.UPDATE_USER_RECORD";
    public static final String ACTION_DOWNLOAD = "ServerProxyService.DOWNLOAD";
    public static final String ACTION_NOTIFY_MEDIA_CLEARED = "ServerProxyService.NOTIFY_MEDIA_CLEARED";
    public static final String ACTION_NOTIFY_MEDIA_READY = "ServerProxyService.NOTIFY_MEDIA_READY";
    public static final String ACTION_CLEAR_MEDIA = "ServerProxyService.CLEAR_MEDIA";
    public static final String ACTION_GET_REGISTERED_CONTACTS = "ServerProxyService.ACTION_GET_REGISTERED_CONTACTS";
    //endregion

    //region Service intent keys
    public static final String DESTINATION_ID = "DESTINATION_ID";
    public static final String MEDIA_CALL = "MEDIA_CALL";
    public static final String SMS_CODE = "SMS_CODE";
    public static final String INTERNATIONAL_PHONE = "INTERNATIONAL_PHONE";
    public static final String SPECIAL_MEDIA_TYPE = "SPECIAL_MEDIA_TYPE";
    public static final String CLEAR_MEDIA_DATA = "CLEAR_MEDIA_DATA";
    public static final String PENDING_DOWNLOAD_DATA = "PENDING_DOWNLOAD_DATA";
    public static final String DEFAULT_MEDIA_DATA = "DEFAULT_MEDIA_DATA";
    //endregion

    private Intent intent;
    private int flags;
    private int startId;

    //region Service methods
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        log(Log.INFO, TAG, "ServerProxyService started");

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
            ConnectionToServerImpl connectionToServer = new ConnectionToServerImpl();

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

    //region Internal handling methods
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

    // region service APIs
    public static void notifyMediaReady(Context context, PendingDownloadData pendingDownloadData) {
        Intent i = new Intent(context, ServerProxyService.class);
        i.setAction(ACTION_NOTIFY_MEDIA_READY);
        i.putExtra(PENDING_DOWNLOAD_DATA, pendingDownloadData);
        context.startService(i);
    }

    public static void sendActionDownload(Context context, PendingDownloadData pendingDownloadData) {
        Intent i = new Intent(context, ServerProxyService.class);
        i.setAction(ServerProxyService.ACTION_DOWNLOAD);
        i.putExtra(PushEventKeys.PUSH_DATA, pendingDownloadData);
        context.startService(i);
    }

    public static void sendActionDownload(Context context, PendingDownloadData pendingDownloadData, DefaultMediaData defaultMediaData) {
        Intent i = new Intent(context, ServerProxyService.class);
        i.setAction(ServerProxyService.ACTION_DOWNLOAD);
        i.putExtra(PushEventKeys.PUSH_DATA, pendingDownloadData);
        i.putExtra(DEFAULT_MEDIA_DATA, defaultMediaData);
        context.startService(i);
    }

    public static void register(Context context, int smsVerificationCode) {
        Intent i = new Intent(context, ServerProxyService.class);
        i.setAction(ServerProxyService.ACTION_REGISTER);
        i.putExtra(ServerProxyService.SMS_CODE, smsVerificationCode);
        context.startService(i);

        String timeoutMsg = context.getResources().getString(R.string.register_failure);
        String registering = context.getResources().getString(R.string.registering);
        AppStateManager.setLoadingState(context, TAG, registering, timeoutMsg);
    }

    public static void clearMedia(Context context, String destPhoneNumber, SpecialMediaType spMediaType) {
        Intent i = new Intent(context, ServerProxyService.class);
        i.setAction(ServerProxyService.ACTION_CLEAR_MEDIA);
        i.putExtra(ServerProxyService.DESTINATION_ID, destPhoneNumber);
        i.putExtra(ServerProxyService.SPECIAL_MEDIA_TYPE, spMediaType);
        context.startService(i);

        String timeoutMsg = context.getResources().getString(R.string.clearing_failed);
        String clearing = context.getResources().getString(R.string.clearing);
        AppStateManager.setLoadingState(context, TAG, clearing, timeoutMsg);
    }

    public static void getRegisteredContacts(Context context) {
        Intent i = new Intent(context, ServerProxyService.class);
        i.setAction(ServerProxyService.ACTION_GET_REGISTERED_CONTACTS);
        context.startService(i);

        String timeoutMsg = context.getResources().getString(R.string.fetch_registered_contacts_failure);
        String fetching = context.getResources().getString(R.string.fetching_registered_contacts);
        AppStateManager.setLoadingState(context, TAG, fetching, timeoutMsg);
    }

    //endregion
}
