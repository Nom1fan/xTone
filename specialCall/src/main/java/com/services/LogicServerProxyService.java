package com.services;

import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import com.client.ConnectionToServer;
import com.data_objects.Constants;
import com.utils.BroadcastUtils;
import com.utils.SpecialDevicesUtils;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import DataObjects.CallRecord;
import DataObjects.DataKeys;
import EventObjects.EventReport;
import EventObjects.EventType;

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
public class LogicServerProxyService extends AbstractServerProxy {

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
    public static final String USER_RECORD = "com.services.LogicServerProxyService.USER_RECORD";
    //endregion

    //region Action URLs
    protected static final String URL_GET_SMS_AUTH = ROOT_URL + "/v1/GetSmsAuthCode";
    protected static final String URL_REGISTER = ROOT_URL + "/v1/Register";
    protected static final String URL_UNREGISTER = ROOT_URL + "/v1/UnRegister";
    protected static final String URL_ISREGISTERED = ROOT_URL + "/v1/IsRegistered";
    protected static final String URL_INSERT_CALL_RECORD = ROOT_URL + "/v1/InsertMediaCallRecord";
    protected static final String URL_GET_APP_RECORD = ROOT_URL + "/v1/GetAppMeta";
    protected static final String URL_UPDATE_USER_RECORD = ROOT_URL + "/v1/UpdateUserRecord";
    //endregion

    public LogicServerProxyService() {
        super(LogicServerProxyService.class.getSimpleName());
    }

    //region Overriding AbstractServerProxy
    @Override
    protected List<SimpleEntry> getDefaultMessageData() {
        List<SimpleEntry> data = super.getDefaultMessageData();
        data.add(new SimpleEntry<>(DataKeys.SOURCE_LOCALE.toString(), Locale.getDefault().getLanguage()));
        return data;
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

        new Thread() {

            @Override
            public void run() {
                if (intent != null) {
                    String action = intent.getAction();
                    log(Log.INFO, TAG, "Action:" + action);

                    List<SimpleEntry> data = getDefaultMessageData();

                    try {

                        switch (action) {

                            case ACTION_REGISTER:
                                setMidAction(true); // This flag will be marked as false after action work is complete. Otherwise, work will be retried in redeliver intent flow.
                                int smsCode = intent.getIntExtra(SMS_CODE, 0);
                                actionRegister(openSocket(responseTypes.TYPE_MAP), smsCode, data);
                                break;

                            case ACTION_UNREGISTER:
                                setMidAction(true);
                                actionUnregister(openSocket(responseTypes.TYPE_MAP), data);
                                break;

                            case ACTION_GET_SMS_CODE:
                                setMidAction(true);
                                String interPhoneNumber = intent.getStringExtra(INTERNATIONAL_PHONE);
                                actionGetSmsCode(openSocket(responseTypes.TYPE_EVENT_REPORT),
                                        Constants.MY_ID(getApplicationContext()), interPhoneNumber, data);
                                break;

                            case ACTION_GET_APP_RECORD:
                                setMidAction(true);
                                actionGetAppRecord(openSocket(responseTypes.TYPE_MAP), data);
                                break;

                            case ACTION_ISREGISTERED: {
                                setMidAction(true); // This flag will be marked as false after action work is complete. Otherwise, work will be retried in redeliver intent flow.
                                String destId = intent.getStringExtra(DESTINATION_ID);
                                actionIsRegistered(openSocket(responseTypes.TYPE_MAP), destId, data);
                            }
                            break;

                            case ACTION_INSERT_CALL_RECORD:
                                setMidAction(true); // This flag will be marked as false after action work is complete. Otherwise, work will be retried in redeliver intent flow.
                                CallRecord callRecord = (CallRecord) intent.getSerializableExtra(CALL_RECORD);
                                data.add(new SimpleEntry<>(DataKeys.CALL_RECORD, callRecord));
                                actionInsertMediaCallRecord(openSocket(responseTypes.TYPE_EVENT_REPORT), data);
                                break;

                            case ACTION_UPDATE_USER_RECORD:
                                setMidAction(true);
                                HashMap userRecord = (HashMap) intent.getSerializableExtra(USER_RECORD);
                                collectionsUtils.addMapElementsToSimpleEntryList(data, userRecord);
                                actionUpdateUserRecord(openSocket(responseTypes.TYPE_MAP), data);
                                break;

                            default:
                                setMidAction(false);
                                log(Log.WARN, TAG, "Service started with invalid action:" + action);

                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                        String errMsg = "Action:" + action + " failed. Exception:" + e.getMessage();
                        log(Log.ERROR, TAG, errMsg);
                        //scheduleReconnect(System.currentTimeMillis());
                    } catch (Exception e) {
                        e.printStackTrace();
                        String errMsg = "Action failed:" + action + " Exception:" + e.getMessage();
                        handleActionFailure();
                        log(Log.ERROR, TAG, errMsg);
                    }
                } else
                    log(Log.WARN, TAG, "Service started with missing action");

            }
        }.start();

        markCrashedServiceHandlingComplete(flags, startId);

        return START_REDELIVER_INTENT;
    }


    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
    //endregion

    //region Action methods

    /**
     * Enables to check if a destination number is logged-in/online
     *
     * @param destinationId - The number of whom to check is logged-in
     */
    private void actionIsRegistered(ConnectionToServer connectionToServer, String destinationId, List<SimpleEntry> data) throws IOException {
        data.add(new SimpleEntry<>(DataKeys.DESTINATION_ID, destinationId));
        connectionToServer.sendToServer(URL_ISREGISTERED, data);
    }

    private void actionGetSmsCode(ConnectionToServer connectionToServer, String localNumber, String interPhoneNumber, List<SimpleEntry> data) throws IOException {
        data.add(new SimpleEntry<>(DataKeys.INTERNATIONAL_PHONE_NUMBER.toString(), interPhoneNumber));
        data.add(new SimpleEntry<>(DataKeys.MESSAGE_INITIATER_ID.toString(), localNumber));
        connectionToServer.sendToServer(URL_GET_SMS_AUTH, data);
    }

    private void actionGetAppRecord(ConnectionToServer connectionToServer, List<SimpleEntry> data) throws IOException {
        connectionToServer.sendToServer(URL_GET_APP_RECORD, data);
    }

    private void actionRegister(ConnectionToServer connectionToServer, int smsCode, List<SimpleEntry> data) throws IOException {

        log(Log.INFO, TAG, "Initiating actionRegister sequence...");
        data.add(new SimpleEntry<>(DataKeys.DEVICE_MODEL, SpecialDevicesUtils.getDeviceName()));
        data.add(new SimpleEntry<>(DataKeys.ANDROID_VERSION, Build.VERSION.RELEASE));
        data.add(new SimpleEntry<>(DataKeys.PUSH_TOKEN, Constants.MY_BATCH_TOKEN(this)));
        data.add(new SimpleEntry<>(DataKeys.SMS_CODE, smsCode));

        connectionToServer.sendToServer(URL_REGISTER, data);

    }

    private void actionUnregister(ConnectionToServer connectionToServer, List<SimpleEntry> data) throws IOException {
        log(Log.INFO, TAG, "Initating actionUnregister sequence...");
        data.add(new SimpleEntry<>(DataKeys.PUSH_TOKEN, Constants.MY_BATCH_TOKEN(this)));
        connectionToServer.sendToServer(URL_UNREGISTER, data);
    }

    private void actionInsertMediaCallRecord(ConnectionToServer connectionToServer, List<SimpleEntry> data) throws IOException {

        log(Log.INFO, TAG, "Initiating actionInsertMediaCallRecord sequence...");
        connectionToServer.sendToServer(URL_INSERT_CALL_RECORD, data);
    }

    private void actionUpdateUserRecord(ConnectionToServer connectionToServer, List<SimpleEntry> data) throws IOException {
        log(Log.INFO, TAG, "Initiating actionUpdateUserRecord sequence...");
        connectionToServer.sendToServer(URL_UPDATE_USER_RECORD, data);
    }

    private void handleActionFailure() {

        BroadcastUtils.sendEventReportBroadcast(this, TAG, new EventReport(EventType.LOGIC_ACTION_FAILURE));
    }
    //endregion
}