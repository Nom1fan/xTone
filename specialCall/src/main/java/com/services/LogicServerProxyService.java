package com.services;

import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import com.app.AppStateManager;
import com.data_objects.Constants;
import com.mediacallz.app.R;
import com.utils.BroadcastUtils;
import com.utils.SharedPrefUtils;

import java.io.IOException;
import java.util.HashMap;

import ClientObjects.ConnectionToServer;
import DataObjects.CallRecord;
import DataObjects.DataKeys;
import DataObjects.SharedConstants;
import EventObjects.EventReport;
import EventObjects.EventType;
import MessagesToServer.ActionType;
import MessagesToServer.GenericMessageToServer;


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
    //endregion

    //region Service intent keys
    public static final String DESTINATION_ID            =      "com.services.LogicServerProxyService.DESTINATION_ID";
    public static final String CALL_RECORD               =      "com.services.LogicServerProxyService.CALL_RECORD";
    public static final String SMS_CODE                  =      "com.services.LogicServerProxyService.SMS_CODE";
    public static final String INTER_PHONE               =      "com.services.LogicServerProxyService.INTER_PHONE"; // International phone number for SMS code reception
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

        final Intent intentForThread = intent;

        new Thread() {

            @Override
            public void run() {
                if (intentForThread != null) {
                    String action = intentForThread.getAction();
                    Log.i(TAG, "Action:" + action);

                    try {

                        switch (action) {

                            case ACTION_REGISTER:
                                setMidAction(true); // This flag will be marked as false after action work is complete. Otherwise, work will be retried in redeliver intent flow.
                                BroadcastUtils.sendEventReportBroadcast(getApplicationContext(), TAG, new EventReport(EventType.REGISTERING, getResources().getString(R.string.registering), null));
                                int smsCode = intentForThread.getIntExtra(SMS_CODE, 0);
                                actionRegister(openSocket(SharedConstants.LOGIC_SERVER_HOST, SharedConstants.LOGIC_SERVER_PORT), smsCode);
                                break;

                            case ACTION_UNREGISTER:
                                setMidAction(true);
                                actionUnregister(openSocket(SharedConstants.LOGIC_SERVER_HOST, SharedConstants.LOGIC_SERVER_PORT));
                                break;

                            case ACTION_GET_SMS_CODE:
                                setMidAction(true);
                                String interPhoneNumber = intentForThread.getStringExtra(INTER_PHONE);
                                actionGetSmsCode(openSocket(SharedConstants.LOGIC_SERVER_HOST, SharedConstants.LOGIC_SERVER_PORT),
                                        Constants.MY_ID(getApplicationContext()) ,interPhoneNumber);
                                break;

                            case ACTION_GET_APP_RECORD:
                                setMidAction(true);
                                actionGetAppRecord(openSocket(SharedConstants.LOGIC_SERVER_HOST, SharedConstants.LOGIC_SERVER_PORT));
                                break;

                            case ACTION_ISREGISTERED: {
                                setMidAction(true); // This flag will be marked as false after action work is complete. Otherwise, work will be retried in redeliver intent flow.
                                String destId = intentForThread.getStringExtra(DESTINATION_ID);
                                actionIsRegistered(openSocket(SharedConstants.LOGIC_SERVER_HOST, SharedConstants.LOGIC_SERVER_PORT), destId);
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
                                CallRecord callRecord = (CallRecord) intentForThread.getSerializableExtra(CALL_RECORD);
                                actionInsertMediaCallRecord(openSocket(SharedConstants.LOGIC_SERVER_HOST, SharedConstants.LOGIC_SERVER_PORT), callRecord);
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
    private void actionIsRegistered(ConnectionToServer connectionToServer, String destinationId) throws IOException {

        HashMap data = new HashMap();
        data.put(DataKeys.DESTINATION_ID, destinationId);

        GenericMessageToServer msgIsLogin = new GenericMessageToServer(Constants.MY_ID(getApplicationContext()), data, ActionType.IS_REGISTERED);
        connectionToServer.sendToServer(msgIsLogin);
    }

    private void actionGetSmsCode(ConnectionToServer connectionToServer, String localNumber, String interPhoneNumber) throws IOException {

        HashMap data = new HashMap();
        data.put(DataKeys.INTERNATIONAL_PHONE_NUMBER, interPhoneNumber);

        connectionToServer.sendToServer(new GenericMessageToServer(localNumber, data, ActionType.GET_SMS_CODE));
    }

    private void actionGetAppRecord(ConnectionToServer connectionToServer) throws IOException {

        Log.i(TAG, "Initiating actionGetAppRecord sequence...");
        connectionToServer.sendToServer(new GenericMessageToServer(Constants.MY_ID(getApplicationContext()), ActionType.GET_APP_RECORD));
    }

    private void actionRegister(ConnectionToServer connectionToServer, int smsCode) throws IOException {

        Log.i(TAG, "Initiating actionRegister sequence...");

        HashMap data = new HashMap();
        data.put(DataKeys.PUSH_TOKEN, Constants.MY_BATCH_TOKEN(getApplicationContext()));
        data.put(DataKeys.SMS_CODE, smsCode);

        GenericMessageToServer msgRegister = new GenericMessageToServer(
                Constants.MY_ID(getApplicationContext()),
                data,
                ActionType.REGISTER);

        Log.i(TAG, "Sending actionRegister message to server...");

        connectionToServer.sendToServer(msgRegister);
    }

    private void actionUnregister(ConnectionToServer connectionToServer) throws IOException {

        Log.i(TAG, "Initating actionUnregister sequence...");

        HashMap data = new HashMap();
        data.put(DataKeys.PUSH_TOKEN, Constants.MY_BATCH_TOKEN(getApplicationContext()));

        GenericMessageToServer msgUnregister = new GenericMessageToServer(
                Constants.MY_ID(getApplicationContext()),
                data,
                ActionType.UNREGISTER);

        Log.i(TAG, "Sending actionUnregister message to server...");

        connectionToServer.sendToServer(msgUnregister);
    }

    private void actionInsertMediaCallRecord(ConnectionToServer connectionToServer, CallRecord callRecord) throws IOException {

        Log.i(TAG, "Initiating actionInsertMediaCallRecord sequence...");
        HashMap data = new HashMap();
        data.put(DataKeys.CALL_RECORD, callRecord);

        GenericMessageToServer msgInsertMCrecord = new GenericMessageToServer(callRecord.get_sourceId(), data, ActionType.INSERT_MEDIA_CALL_RECORD);
        Log.i(TAG, "Sending actionInsertMediaCallRecord message to server...");
        connectionToServer.sendToServer(msgInsertMCrecord);
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