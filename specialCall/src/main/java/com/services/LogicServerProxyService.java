package com.services;

import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import com.data_objects.Constants;
import com.utils.BroadcastUtils;
import com.utils.NetworkingUtils;
import com.utils.SpecialDevicesUtils;

import java.io.IOException;
import java.util.HashMap;
import java.util.Locale;

import ClientObjects.ConnectionToServer;
import DataObjects.CallRecord;
import DataObjects.DataKeys;
import DataObjects.SharedConstants;
import EventObjects.EventReport;
import EventObjects.EventType;
import MessagesToServer.MessageToServer;
import MessagesToServer.ServerActionType;

import static com.crashlytics.android.Crashlytics.log;


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
    public static final String ACTION_REGISTER           =      "com.services.LogicServerProxyService.REGISTER";
    public static final String ACTION_UNREGISTER         =      "com.services.LogicServerProxyService.UNREGISTER";
    public static final String ACTION_ISREGISTERED       =      "com.services.LogicServerProxyService.ISREGISTERED";
    public static final String ACTION_INSERT_CALL_RECORD =      "com.services.LogicServerProxyService.INSERT_CALL_RECORD";
    public static final String ACTION_GET_APP_RECORD     =      "com.services.LogicServerProxyService.GET_APP_RECORD";
    public static final String ACTION_GET_SMS_CODE       =      "com.services.LogicServerProxyService.GET_SMS_CODE";
    public static final String ACTION_UPDATE_USER_RECORD =      "com.services.LogicServerProxyService.UPDATE_USER_RECORD";
    //endregion

    //region Service intent keys
    public static final String DESTINATION_ID            =      "com.services.LogicServerProxyService.DESTINATION_ID";
    public static final String CALL_RECORD               =      "com.services.LogicServerProxyService.CALL_RECORD";
    public static final String SMS_CODE                  =      "com.services.LogicServerProxyService.SMS_CODE";
    public static final String INTERNATIONAL_PHONE       =      "com.services.LogicServerProxyService.INTERNATIONAL_PHONE";
    public static final String USER_RECORD               =      "com.services.LogicServerProxyService.USER_RECORD";
    //endregion

    public LogicServerProxyService() {
        super(LogicServerProxyService.class.getSimpleName());
    }

    //region Overriding AbstractServerProxy
    @Override
    protected HashMap<DataKeys, Object> getDefaultMessageData() {
        HashMap<DataKeys, Object> data =  super.getDefaultMessageData();
        data.put(DataKeys.SOURCE_LOCALE, Locale.getDefault().getLanguage());
        return data;
    }
    //endregion

    //region Service methods
    @Override
    public void onCreate() {
        super.onCreate();

        host = SharedConstants.LOGIC_SERVER_HOST;
        port = SharedConstants.LOGIC_SERVER_PORT;
    }

    @Override
    public int onStartCommand(final Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        log(Log.INFO,TAG, "LogicServerProxyService started");

        boolean shouldStop = handleCrashedService(flags, startId);
        if (shouldStop)
            return START_REDELIVER_INTENT;

        if(NetworkingUtils.isNetworkAvailable(this)) {

            new Thread() {

                @Override
                public void run() {
                    if (intent != null) {
                        String action = intent.getAction();
                        log(Log.INFO, TAG, "Action:" + action);

                        HashMap<DataKeys, Object> data = getDefaultMessageData();

                        try {

                            switch (action) {

                                case ACTION_REGISTER:
                                    setMidAction(true); // This flag will be marked as false after action work is complete. Otherwise, work will be retried in redeliver intent flow.
                                    int smsCode = intent.getIntExtra(SMS_CODE, 0);
                                    actionRegister(openSocket(), smsCode, data);
                                    break;

                                case ACTION_UNREGISTER:
                                    setMidAction(true);
                                    actionUnregister(openSocket(), data);
                                    break;

                                case ACTION_GET_SMS_CODE:
                                    setMidAction(true);
                                    String interPhoneNumber = intent.getStringExtra(INTERNATIONAL_PHONE);
                                    actionGetSmsCode(openSocket(),
                                            Constants.MY_ID(getApplicationContext()), interPhoneNumber, data);
                                    break;

                                case ACTION_GET_APP_RECORD:
                                    setMidAction(true);
                                    actionGetAppRecord(openSocket(), data);
                                    break;

                                case ACTION_ISREGISTERED: {
                                    setMidAction(true); // This flag will be marked as false after action work is complete. Otherwise, work will be retried in redeliver intent flow.
                                    String destId = intent.getStringExtra(DESTINATION_ID);
                                    actionIsRegistered(openSocket(), destId, data);
                                }
                                break;

                                case ACTION_INSERT_CALL_RECORD:
                                    setMidAction(true); // This flag will be marked as false after action work is complete. Otherwise, work will be retried in redeliver intent flow.
                                    CallRecord callRecord = (CallRecord) intent.getSerializableExtra(CALL_RECORD);
                                    actionInsertMediaCallRecord(openSocket(), callRecord, data);
                                    break;

                                case ACTION_UPDATE_USER_RECORD:
                                    setMidAction(true);
                                    HashMap userRecord = (HashMap) intent.getSerializableExtra(USER_RECORD);
                                    data.putAll(userRecord);
                                    actionUpdateUserRecord(openSocket(), data);
                                    break;

                                default:
                                    setMidAction(false);
                                    log(Log.WARN, TAG, "Service started with invalid action:" + action);

                            }
                        }  catch (Exception e) {
                            e.printStackTrace();
                            String errMsg = "Action failed:" + action + " Exception:" + e.getMessage();
                            handleActionFailure();
                            log(Log.ERROR, TAG, errMsg);
                        }
                    } else
                        log(Log.WARN, TAG, "Service started with missing action");

                }
            }.start();
        } else {
            BroadcastUtils.sendEventReportBroadcast(this, TAG, new EventReport(EventType.NO_INTERNET));
        }

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
    private void actionIsRegistered(ConnectionToServer connectionToServer, String destinationId, HashMap<DataKeys,Object> data) throws IOException {

        data.put(DataKeys.DESTINATION_ID, destinationId);

        MessageToServer msgIsLogin = new MessageToServer(ServerActionType.IS_REGISTERED, Constants.MY_ID(this), data);
        connectionToServer.sendToServer(msgIsLogin);
    }

    private void actionGetSmsCode(ConnectionToServer connectionToServer, String localNumber, String interPhoneNumber, HashMap<DataKeys,Object> data) throws IOException {

        data.put(DataKeys.INTERNATIONAL_PHONE_NUMBER, interPhoneNumber);

        connectionToServer.sendToServer(new MessageToServer(ServerActionType.GET_SMS_CODE, localNumber, data));
    }

    private void actionGetAppRecord(ConnectionToServer connectionToServer, HashMap<DataKeys,Object> data) throws IOException {

        connectionToServer.sendToServer(new MessageToServer(ServerActionType.GET_APP_RECORD, Constants.MY_ID(this), data));
    }

    private void actionRegister(ConnectionToServer connectionToServer, int smsCode, HashMap<DataKeys,Object> data) throws IOException {

        log(Log.INFO,TAG, "Initiating actionRegister sequence...");

        data.put(DataKeys.DEVICE_MODEL, SpecialDevicesUtils.getDeviceName());
        data.put(DataKeys.ANDROID_VERSION, Build.VERSION.RELEASE);
        data.put(DataKeys.PUSH_TOKEN, Constants.MY_BATCH_TOKEN(this));
        data.put(DataKeys.SMS_CODE, smsCode);

        MessageToServer msgRegister = new MessageToServer(
                ServerActionType.REGISTER, Constants.MY_ID(this),
                data
        );

        connectionToServer.sendToServer(msgRegister);
    }

    private void actionUnregister(ConnectionToServer connectionToServer, HashMap<DataKeys,Object> data) throws IOException {

        log(Log.INFO,TAG, "Initating actionUnregister sequence...");

        data.put(DataKeys.PUSH_TOKEN, Constants.MY_BATCH_TOKEN(this));

        MessageToServer msgUnregister = new MessageToServer(
                ServerActionType.UNREGISTER, Constants.MY_ID(this),
                data
        );

        connectionToServer.sendToServer(msgUnregister);
    }

    private void actionInsertMediaCallRecord(ConnectionToServer connectionToServer, CallRecord callRecordDBO, HashMap<DataKeys,Object> data) throws IOException {

        log(Log.INFO,TAG, "Initiating actionInsertMediaCallRecord sequence...");
        data.put(DataKeys.CALL_RECORD, callRecordDBO);

        MessageToServer msgInsertMCrecord = new MessageToServer(ServerActionType.INSERT_MEDIA_CALL_RECORD, callRecordDBO.get_sourceId(), data);
        connectionToServer.sendToServer(msgInsertMCrecord);
    }

    private void actionUpdateUserRecord(ConnectionToServer connectionToServer, HashMap<DataKeys,Object> data) throws IOException {

        log(Log.INFO,TAG, "Initiating actionUpdateUserRecord sequence...");

        MessageToServer msgUUR = new MessageToServer(ServerActionType.UPDATE_USER_RECORD, Constants.MY_ID(this), data);
        connectionToServer.sendToServer(msgUUR);

    }

    private void handleActionFailure() {

        BroadcastUtils.sendEventReportBroadcast(this, TAG, new EventReport(EventType.LOGIC_ACTION_FAILURE));
    }
    //endregion
}