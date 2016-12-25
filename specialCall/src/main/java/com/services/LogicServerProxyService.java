package com.services;

import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import com.client.ConnectionToServer;
import com.data.objects.DataKeys;
import com.event.EventReport;
import com.event.EventType;
import com.handlers.ActionHandler;
import com.handlers.HandlerFactory;
import com.model.request.Request;
import com.utils.BroadcastUtils;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

import static com.crashlytics.android.Crashlytics.log;
import static java.util.AbstractMap.SimpleEntry;


/**
 * <pre>
 * A Proxy that manages logic server operations.
 * Provided operations:
 * - Register
 * - Check if user is registered
 *
 * @author Mor
 */
public class LogicServerProxyService extends AbstractServerProxy implements Runnable {

    //region Service actions
    public static final String ACTION_REGISTER = "com.services.LogicServerProxyService.REGISTER";
    public static final String ACTION_UNREGISTER = "com.services.LogicServerProxyService.UNREGISTER";
    public static final String ACTION_ISREGISTERED = "com.services.LogicServerProxyService.ISREGISTERED";
    public static final String ACTION_INSERT_CALL_RECORD = "com.services.LogicServerProxyService.INSERT_CALL_RECORD";
    public static final String ACTION_GET_APP_RECORD = "com.services.LogicServerProxyService.GET_APP_RECORD";
    public static final String ACTION_GET_SMS_CODE = "com.services.LogicServerProxyService.GET_SMS_CODE";
    public static final String ACTION_UPDATE_USER_RECORD = "com.services.LogicServerProxyService.UPDATE_USER_RECORD";
    //endregion

    //region Service intent keys
    public static final String DESTINATION_ID = "com.services.LogicServerProxyService.DESTINATION_ID";
    public static final String CALL_RECORD = "com.services.LogicServerProxyService.CALL_RECORD";
    public static final String SMS_CODE = "com.services.LogicServerProxyService.SMS_CODE";
    public static final String INTERNATIONAL_PHONE = "com.services.LogicServerProxyService.INTERNATIONAL_PHONE";
    //endregion

    public LogicServerProxyService() {
        super(LogicServerProxyService.class.getSimpleName());
    }

    private Intent intent;
    private int flags;
    private int startId;

    //region Overriding AbstractServerProxy
    @Override
    protected List<SimpleEntry> getDefaultMessageData() {
        List<SimpleEntry> data = super.getDefaultMessageData();
        data.add(new SimpleEntry<>(DataKeys.SOURCE_LOCALE.toString(), Locale.getDefault().getLanguage()));
        return data;
    }

    @Override
    protected Request prepareDefaultRequestData() {
        Request request = super.prepareDefaultRequestData();
        request.setSourceLocale(Locale.getDefault().getLanguage());
        return request;
    }
    //endregion

    //region Service methods
    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public int onStartCommand(final Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        log(Log.INFO, TAG, "LogicServerProxyService started");

        boolean shouldStop = handleCrashedService(flags, startId);
        if (shouldStop)
            return START_REDELIVER_INTENT;

        this.intent = intent;
        this.flags = flags;
        this.startId = startId;

        new Thread(this).start();

        return START_REDELIVER_INTENT;
    }


    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
    //endregion

    private void handleActionFailure() {
        BroadcastUtils.sendEventReportBroadcast(this, TAG, new EventReport(EventType.LOADING_TIMEOUT));
    }

    @Override
    public void run() {
        if (intent != null) {
            String action = intent.getAction();
            log(Log.INFO, TAG, "Action:" + action);

            Request request = prepareDefaultRequestData();
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
    //endregion
}