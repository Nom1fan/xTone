package com.services;

import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import com.app.AppStateManager;
import com.data_objects.Constants;
import com.utils.BroadcastUtils;
import com.utils.SharedPrefUtils;
import java.io.IOException;
import ClientObjects.ConnectionToServer;
import DataObjects.SharedConstants;
import EventObjects.EventReport;
import EventObjects.EventType;
import MessagesToServer.MessageIsRegistered;
import MessagesToServer.MessageRegister;


/**
 * <pre>
 * A Proxy that manages logic server operations.
 * Provided operations:
 * - Register
 * - Check if user is registered
 * @author Mor
 */
public class LogicServerProxyService extends AbstractServerProxy {

    // Service actions
    public static final String ACTION_REGISTER = "com.services.LogicServerProxyService.REGISTER";
    public static final String ACTION_ISREGISTERED = "com.services.LogicServerProxyService.ISREGISTERED";

    // Service intent keys
    public static final String DESTINATION_ID = "com.services.LogicServerProxyService.DESTINATION_ID";

    public LogicServerProxyService() {
        super(LogicServerProxyService.class.getSimpleName());
    }

    /* Service overriding methods */

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        Log.i(TAG, "LogicServerProxyService started");

        boolean shouldStop = handleCrashedService(flags, startId);
        if(shouldStop)
            return START_REDELIVER_INTENT;

        final Intent intentForThread = intent;

        new Thread() {

            @Override
            public void run() {
                if (intentForThread != null)
                {
                    String action = intentForThread.getAction();
                    Log.i(TAG, "Action:" + action);

                    try {

                        //TODO Mor: Change all actions into methods (See StorageServerProxyService)
                        switch (action) {

                            case ACTION_REGISTER:
                                setMidAction(true); // This flag will be marked as false after action work is complete. Otherwise, work will be retried in redeliver intent flow.
                                BroadcastUtils.sendEventReportBroadcast(getApplicationContext(), TAG, new EventReport(EventType.CONNECTING, "Connecting...", null));
                                register(openSocket(SharedConstants.LOGIC_SERVER_HOST, SharedConstants.LOGIC_SERVER_PORT));
                            break;

                            case ACTION_ISREGISTERED: {
                                setMidAction(true); // This flag will be marked as false after action work is complete. Otherwise, work will be retried in redeliver intent flow.
                                String destId = intentForThread.getStringExtra(DESTINATION_ID);
                                isRegistered(openSocket(SharedConstants.LOGIC_SERVER_HOST, SharedConstants.LOGIC_SERVER_PORT), destId);
                            }
                            break;

                            case ACTION_RECONNECT:
                                setMidAction(true); // This flag will be marked as false after action work is complete. Otherwise, work will be retried in redeliver intent flow.
                                BroadcastUtils.sendEventReportBroadcast(getApplicationContext(), TAG, new EventReport(EventType.RECONNECT_ATTEMPT, "Reconnecting...", null));
                                reconnectIfNecessary();
                            break;

                            case ACTION_RESET_RECONNECT_INTERVAL:
                                setMidAction(true); // This flag will be marked as false after action work is complete. Otherwise, work will be retried in redeliver intent flow.
                                SharedPrefUtils.setLong(getApplicationContext(), SharedPrefUtils.SERVER_PROXY, SharedPrefUtils.RECONNECT_INTERVAL, INITIAL_RETRY_INTERVAL);
                            break;

                            default:
                                setMidAction(false);
                                Log.w(TAG, "Service started with invalid action:" + action);

                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                        String errMsg = "Action:"+action+" failed. Exception:"+e.getMessage();
                        Log.e(TAG, errMsg);
                        handleDisconnection(errMsg);
                    } catch(Exception e) {
                        String errMsg = "Action failed:"+action+" Exception:"+e.getMessage();
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

          /* IServerProxy operations methods */


    /**
     * Enables to check if a destination number is logged-in/online
     *
     * @param destinationId - The number of whom to check is logged-in
     */
    public void isRegistered(ConnectionToServer connectionToServer, String destinationId)  throws IOException {
        MessageIsRegistered msgIsLogin = new MessageIsRegistered(Constants.MY_ID(getApplicationContext()), destinationId);
        connectionToServer.sendToServer(msgIsLogin);
    }

          /* Internal server operations methods */

    /**
     * Enables to register to the server
     */
    private void register(ConnectionToServer connectionToServer) throws IOException {

        Log.i(TAG, "Initiating register sequence...");
        MessageRegister msgRegister = new MessageRegister(Constants.MY_ID(getApplicationContext()), Constants.MY_BATCH_TOKEN(getApplicationContext()));
        Log.i(TAG, "Sending register message to server...");
        connectionToServer.sendToServer(msgRegister);
    }

    private synchronized void reconnectIfNecessary() throws IOException {

        if (isNetworkAvailable())
        {
            ConnectionToServer cts = openSocket(SharedConstants.LOGIC_SERVER_HOST, SharedConstants.LOGIC_SERVER_PORT);
            if(cts!=null) {
                BroadcastUtils.sendEventReportBroadcast(getApplicationContext(), TAG, new EventReport(EventType.CONNECTED, "Connected", null));
                cancelReconnect();
                SharedPrefUtils.setLong(getApplicationContext(), SharedPrefUtils.SERVER_PROXY, SharedPrefUtils.RECONNECT_INTERVAL, INITIAL_RETRY_INTERVAL);
            }
        }
        else {
            scheduleReconnect(System.currentTimeMillis());
            if(!AppStateManager.getAppState(getApplicationContext()).equals(AppStateManager.STATE_DISABLED))
                BroadcastUtils.sendEventReportBroadcast(getApplicationContext(),TAG, new EventReport(EventType.DISCONNECTED, "Disconnected. Check your internet connection", null));
        }
    }


    /**
     * Deals with disconnection and schedules a reconnect
     * This method is called by ConnectionToServer connectionException() method
     * @param errMsg
     */
    @Override
    public void handleDisconnection(String errMsg) {

        Log.e(TAG, errMsg);

        BroadcastUtils.sendEventReportBroadcast(getApplicationContext(), TAG, new EventReport(EventType.DISCONNECTED, errMsg, null));
        scheduleReconnect(System.currentTimeMillis());
    }


}