package com.services;

import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import com.app.AppStateManager;
import com.data_objects.Constants;
import com.mediacallz.app.R;
import com.utils.BroadcastUtils;
import com.utils.SharedPrefUtils;
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

    //region Service methods
    @Override
    public int onStartCommand(final Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        Log.i(TAG, "LogicServerProxyService started");

        boolean shouldStop = handleCrashedService(flags, startId);
        if (shouldStop)
            return START_REDELIVER_INTENT;

        new Thread() {

            @Override
            public void run() {
                if (intent != null) {
                    String action = intent.getAction();
                    Log.i(TAG, "Action:" + action);

                    HashMap<DataKeys, Object> data = getDefaultMessageData();

                    try {

                        switch (action) {

                            case ACTION_REGISTER:
                                setMidAction(true); // This flag will be marked as false after action work is complete. Otherwise, work will be retried in redeliver intent flow.
                                int smsCode = intent.getIntExtra(SMS_CODE, 0);
                                actionRegister(openSocket(SharedConstants.LOGIC_SERVER_HOST, SharedConstants.LOGIC_SERVER_PORT), smsCode, data);
                                break;

                            case ACTION_UNREGISTER:
                                setMidAction(true);
                                actionUnregister(openSocket(SharedConstants.LOGIC_SERVER_HOST, SharedConstants.LOGIC_SERVER_PORT), data);
                                break;

                            case ACTION_GET_SMS_CODE:
                                setMidAction(true);
                                String interPhoneNumber = intent.getStringExtra(INTERNATIONAL_PHONE);
                                actionGetSmsCode(openSocket(SharedConstants.LOGIC_SERVER_HOST, SharedConstants.LOGIC_SERVER_PORT),
                                        Constants.MY_ID(getApplicationContext()) ,interPhoneNumber , data);
                                break;

                            case ACTION_GET_APP_RECORD:
                                setMidAction(true);
                                actionGetAppRecord(openSocket(SharedConstants.LOGIC_SERVER_HOST, SharedConstants.LOGIC_SERVER_PORT), data);
                                break;

                            case ACTION_ISREGISTERED: {
                                setMidAction(true); // This flag will be marked as false after action work is complete. Otherwise, work will be retried in redeliver intent flow.
                                String destId = intent.getStringExtra(DESTINATION_ID);
                                actionIsRegistered(openSocket(SharedConstants.LOGIC_SERVER_HOST, SharedConstants.LOGIC_SERVER_PORT), destId, data);
                            }
                                break;

                            case ACTION_RECONNECT:
                                setMidAction(true); // This flag will be marked as false after action work is complete. Otherwise, work will be retried in redeliver intent flow.
                                reconnectIfNecessary();
                                break;

                            case ACTION_RESET_RECONNECT_INTERVAL:
                                setMidAction(true); // This flag will be marked as false after action work is complete. Otherwise, work will be retried in redeliver intent flow.
                                SharedPrefUtils.setLong(getApplicationContext(), SharedPrefUtils.SERVER_PROXY, SharedPrefUtils.RECONNECT_INTERVAL, INITIAL_RETRY_INTERVAL);
                                break;

                            case ACTION_INSERT_CALL_RECORD:
                                setMidAction(true); // This flag will be marked as false after action work is complete. Otherwise, work will be retried in redeliver intent flow.
                                CallRecord callRecord = (CallRecord) intent.getSerializableExtra(CALL_RECORD);
                                actionInsertMediaCallRecord(openSocket(SharedConstants.LOGIC_SERVER_HOST, SharedConstants.LOGIC_SERVER_PORT), callRecord, data);
                                break;

                            case ACTION_UPDATE_USER_RECORD:
                                setMidAction(true);
                                HashMap userRecord = (HashMap)intent.getSerializableExtra(USER_RECORD);
                                data.putAll(userRecord);
                                actionUpdateUserRecord(openSocket(SharedConstants.LOGIC_SERVER_HOST, SharedConstants.LOGIC_SERVER_PORT), data);
                                break;

                            default:
                                setMidAction(false);
                                Log.w(TAG, "Service started with invalid action:" + action);

                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                        String errMsg = "Action:" + action + " failed. Exception:" + e.getMessage();
                        Log.e(TAG, errMsg);
                        handleDisconnection(errMsg);
                    } catch (Exception e) {
                        e.printStackTrace();
                        String errMsg = "Action failed:" + action + " Exception:" + e.getMessage();
                        handleActionFailure();
                        Log.e(TAG, errMsg);
                    }
                } else
                    Log.w(TAG, "Service started with missing action");

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
    private void actionIsRegistered(ConnectionToServer connectionToServer, String destinationId, HashMap<DataKeys,Object> data) throws IOException {

        data.put(DataKeys.DESTINATION_ID, destinationId);

        MessageToServer msgIsLogin = new MessageToServer(ServerActionType.IS_REGISTERED, Constants.MY_ID(getApplicationContext()), data);
        connectionToServer.sendToServer(msgIsLogin);
    }

    private void actionGetSmsCode(ConnectionToServer connectionToServer, String localNumber, String interPhoneNumber, HashMap<DataKeys,Object> data) throws IOException {

        data.put(DataKeys.INTERNATIONAL_PHONE_NUMBER, interPhoneNumber);
        data.put(DataKeys.SOURCE_LOCALE, Locale.getDefault().getLanguage());

        connectionToServer.sendToServer(new MessageToServer(ServerActionType.GET_SMS_CODE, localNumber, data));
    }

    private void actionGetAppRecord(ConnectionToServer connectionToServer, HashMap<DataKeys,Object> data) throws IOException {

        connectionToServer.sendToServer(new MessageToServer(ServerActionType.GET_APP_RECORD, Constants.MY_ID(getApplicationContext()), data));
    }

    private void actionRegister(ConnectionToServer connectionToServer, int smsCode, HashMap<DataKeys,Object> data) throws IOException {

        Log.i(TAG, "Initiating actionRegister sequence...");

        data.put(DataKeys.DEVICE_MODEL, SpecialDevicesUtils.getDeviceName());
        data.put(DataKeys.ANDROID_VERSION, Build.VERSION.RELEASE);
        data.put(DataKeys.PUSH_TOKEN, Constants.MY_BATCH_TOKEN(getApplicationContext()));
        data.put(DataKeys.SMS_CODE, smsCode);

        MessageToServer msgRegister = new MessageToServer(
                ServerActionType.REGISTER, Constants.MY_ID(getApplicationContext()),
                data
        );

        connectionToServer.sendToServer(msgRegister);
    }

    private void actionUnregister(ConnectionToServer connectionToServer, HashMap<DataKeys,Object> data) throws IOException {

        Log.i(TAG, "Initating actionUnregister sequence...");

        data.put(DataKeys.PUSH_TOKEN, Constants.MY_BATCH_TOKEN(getApplicationContext()));

        MessageToServer msgUnregister = new MessageToServer(
                ServerActionType.UNREGISTER, Constants.MY_ID(getApplicationContext()),
                data
        );

        connectionToServer.sendToServer(msgUnregister);
    }

    private void actionInsertMediaCallRecord(ConnectionToServer connectionToServer, CallRecord callRecord, HashMap<DataKeys,Object> data) throws IOException {

        Log.i(TAG, "Initiating actionInsertMediaCallRecord sequence...");
        data.put(DataKeys.CALL_RECORD, callRecord);

        MessageToServer msgInsertMCrecord = new MessageToServer(ServerActionType.INSERT_MEDIA_CALL_RECORD, callRecord.get_sourceId(), data);
        connectionToServer.sendToServer(msgInsertMCrecord);
    }

    private void actionUpdateUserRecord(ConnectionToServer connectionToServer, HashMap<DataKeys,Object> data) throws IOException {

        Log.i(TAG, "Initiating actionUpdateUserRecord sequence...");

        MessageToServer msgUUR = new MessageToServer(ServerActionType.UPDATE_USER_RECORD, Constants.MY_ID(this), data);
        connectionToServer.sendToServer(msgUUR);

    }

    private void handleActionFailure() {

        BroadcastUtils.sendEventReportBroadcast(getApplicationContext(), TAG, new EventReport(EventType.LOGIC_ACTION_FAILURE, null, null));
    }
    //endregion

    //region Networking methods
    private synchronized void reconnectIfNecessary() throws IOException {

        if (isNetworkAvailable()) {
            BroadcastUtils.sendEventReportBroadcast(getApplicationContext(), TAG, new EventReport(EventType.RECONNECT_ATTEMPT, getResources().getString(R.string.reconnecting), null));
            try {
                reconnect(SharedConstants.LOGIC_SERVER_HOST, SharedConstants.LOGIC_SERVER_PORT);
                BroadcastUtils.sendEventReportBroadcast(getApplicationContext(), TAG, new EventReport(EventType.CONNECTED, getResources().getString(R.string.connected), null));
                cancelReconnect();
                SharedPrefUtils.setLong(getApplicationContext(), SharedPrefUtils.SERVER_PROXY, SharedPrefUtils.RECONNECT_INTERVAL, INITIAL_RETRY_INTERVAL);
            } catch(IOException e) {
                handleDisconnection("Failed to reconnect to server. [Exception]:" + (e.getMessage()!=null ? e.getMessage() : e));
            }
        } else {
            handleDisconnection("Failed to reconnect. No network connection");
        }
    }

    /**
     * Deals with disconnection and schedules a reconnect
     * This method is called by ConnectionToServer connectionException() method
     *
     * @param errMsg
     */
    @Override
    public void handleDisconnection(String errMsg) {

        Log.e(TAG, "handleDisconnection:" + errMsg);

        if (!AppStateManager.getAppState(getApplicationContext()).equals(AppStateManager.STATE_DISABLED))
            BroadcastUtils.sendEventReportBroadcast(getApplicationContext(), TAG, new EventReport(EventType.DISCONNECTED, null, null));

        scheduleReconnect(System.currentTimeMillis());
    }
    //endregion

}