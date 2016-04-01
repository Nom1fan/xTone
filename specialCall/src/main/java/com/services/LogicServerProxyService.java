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

import ClientObjects.ConnectionToServer;
import DataObjects.CallRecord;
import DataObjects.SharedConstants;
import EventObjects.EventReport;
import EventObjects.EventType;
import MessagesToServer.MessageGetAppRecord;
import MessagesToServer.MessageGetSmsCode;
import MessagesToServer.MessageInsertMediaCallRecord;
import MessagesToServer.MessageIsRegistered;
import MessagesToServer.MessageRegister;


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
                                BroadcastUtils.sendEventReportBroadcast(getApplicationContext(), TAG, new EventReport(EventType.RECONNECT_ATTEMPT, getResources().getString(R.string.reconnecting), null));
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
                        String errMsg = "Action failed:" + action + " Exception:" + e.getMessage();
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
        MessageIsRegistered msgIsLogin = new MessageIsRegistered(Constants.MY_ID(getApplicationContext()), destinationId);
        connectionToServer.sendToServer(msgIsLogin);
    }

    private void actionGetSmsCode(ConnectionToServer connectionToServer, String localNumber, String interPhoneNumber) throws IOException {

        connectionToServer.sendToServer(new MessageGetSmsCode(localNumber, interPhoneNumber));
    }

    private void actionGetAppRecord(ConnectionToServer connectionToServer) throws IOException {

        Log.i(TAG, "Initiating actionGetAppRecord sequence...");
        connectionToServer.sendToServer(new MessageGetAppRecord(Constants.MY_ID(getApplicationContext())));
    }

    private void actionRegister(ConnectionToServer connectionToServer, int smsCode) throws IOException {

        Log.i(TAG, "Initiating actionRegister sequence...");
        MessageRegister msgRegister = new MessageRegister(Constants.MY_ID(getApplicationContext()),
                Constants.MY_BATCH_TOKEN(getApplicationContext()), smsCode);
        Log.i(TAG, "Sending actionRegister message to server...");
        connectionToServer.sendToServer(msgRegister);
    }

    private void actionInsertMediaCallRecord(ConnectionToServer connectionToServer, CallRecord callRecord) throws IOException {

        Log.i(TAG, "Initiating actionInsertMediaCallRecord sequence...");
        MessageInsertMediaCallRecord msgInsertMCrecord = new MessageInsertMediaCallRecord(callRecord.get_sourceId(), callRecord);
        Log.i(TAG, "Sending actionInsertMediaCallRecord message to server...");
        connectionToServer.sendToServer(msgInsertMCrecord);
    }
    //endregion

    //region Networking methods
    private synchronized void reconnectIfNecessary() throws IOException {

        if (isNetworkAvailable()) {
            ConnectionToServer cts = openSocket(SharedConstants.LOGIC_SERVER_HOST, SharedConstants.LOGIC_SERVER_PORT);
            if (cts != null) {
                BroadcastUtils.sendEventReportBroadcast(getApplicationContext(), TAG, new EventReport(EventType.CONNECTED, getResources().getString(R.string.connected), null));
                cancelReconnect();
                SharedPrefUtils.setLong(getApplicationContext(), SharedPrefUtils.SERVER_PROXY, SharedPrefUtils.RECONNECT_INTERVAL, INITIAL_RETRY_INTERVAL);
            }
        } else {
            scheduleReconnect(System.currentTimeMillis());
            if (!AppStateManager.getAppState(getApplicationContext()).equals(AppStateManager.STATE_DISABLED))
                BroadcastUtils.sendEventReportBroadcast(getApplicationContext(), TAG, new EventReport(EventType.DISCONNECTED, "Disconnected. Check your internet connection", null));
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

        Log.e(TAG, errMsg);

        BroadcastUtils.sendEventReportBroadcast(getApplicationContext(), TAG, new EventReport(EventType.DISCONNECTED, errMsg, null));
        scheduleReconnect(System.currentTimeMillis());
    }
    //endregion

}