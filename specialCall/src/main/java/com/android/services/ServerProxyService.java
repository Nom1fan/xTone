package com.android.services;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Binder;
import android.os.IBinder;
import android.os.SystemClock;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;
import java.io.IOException;
import java.net.Socket;
import java.util.Date;

import ClientObjects.ConnectionToServer;
import ClientObjects.IServerProxy;
import DataObjects.SharedConstants;
import DataObjects.TransferDetails;
import EventObjects.Event;
import EventObjects.EventReport;
import EventObjects.EventType;
import FilesManager.FileManager;
import MessagesToClient.MessageToClient;
import MessagesToServer.MessageHeartBeat;
import MessagesToServer.MessageIsLogin;
import MessagesToServer.MessageLogin;
import MessagesToServer.MessageLogout;
import MessagesToServer.MessageUploadFile;
import data_objects.Constants;
import data_objects.SharedPrefUtils;
//import MessagesToServer.MessageDownloadFile;


	  /**
	   * <pre>
	   * A Proxy that manages all server operations.
	   * Provided operations:
	   * - Login
	   * - Upload file
	   * - Check if user is online 
	   * - Periodically perform heartbeats
	   * - Listen for messages from server
	   * @author Mor
	   */
	  public class ServerProxyService extends Service implements IServerProxy {

          public static final String ACTION_HEARTBEAT = "com.android.services.HEARTBEAT";
          public static final String ACTION_START = "com.android.services.START";
          public static final String ACTION_STOP = "com.android.services.STOP";
          public static final String ACTION_RECONNECT = "com.android.services.RECONNECT";

          private ConnectionToServer connectionToServer;
          private ServerProxyService serverProxy = this;
          private ConnectivityManager connManager;
          private static final int HEARTBEAT_INTERVAL = SharedConstants.HEARTBEAT_INTERVAL;
          private static final long INITIAL_RETRY_INTERVAL = 1000 * 5;
          private static final long MAXIMUM_RETRY_INTERVAL = 1000 * 60;
          private static final long HEARTBEAT_ACK_TIMEOUT = HEARTBEAT_INTERVAL + 5000;
          private MessageHeartBeat msgHB;
          private boolean started = false;
          private final IBinder mBinder = new MyBinder();
          private static final String TAG = ServerProxyService.class.getSimpleName();


          /* Service overriding methods */

          @Override
          public IBinder onBind(Intent arg0) {
              return mBinder;
          }

          @Override
          public int onStartCommand(Intent intent, int flags, int startId) {
              super.onStartCommand(intent, flags, startId);

              Log.i(TAG, "ServerProxyService started");

              if(intent!=null) {
                  String action = intent.getAction();
                  Log.i(TAG, "Action:"+action);
                  //callInfoToast("ServerProxyService started");

                  switch (action)
                  {
                      case ACTION_START:
                          if (SharedConstants.MY_ID.equals("")) {
                              callErrToast("Can't login using empty phone number. Try restarting the app.");
                              stop();
                          } else if (SharedConstants.DEVICE_TOKEN.equals("")) {
                              callErrToast("Can't login using empty device token. Try restarting the app.");
                              stop();
                          } else
                              start();
                          break;

                      case ACTION_STOP:
                          stop();
                          break;

                      case ACTION_HEARTBEAT:
                            sendHeartBeat();
                          break;

                      case ACTION_RECONNECT:
                          reconnectIfNecessary();
                          break;

                  }
              }
              return Service.START_STICKY;
          }

          @Override
          public void onCreate() {

             Log.i(TAG, "ServerProxyService created");
             callInfoToast("ServerProxyService created");
             SharedConstants.MY_ID = SharedPrefUtils.getString(getApplicationContext(),SharedPrefUtils.GENERAL, SharedPrefUtils.MY_NUMBER);
             SharedConstants.DEVICE_TOKEN = SharedPrefUtils.getString(getApplicationContext(), SharedPrefUtils.GENERAL, SharedPrefUtils.MY_DEVICE_TOKEN);
             SharedConstants.specialCallPath = Constants.specialCallPath;

             connManager = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);

             sendEventReportBroadcast(new EventReport(EventType.SERVER_PROXY_CREATED, null, null));

             handleCrashedService();

          }

          @Override
          public void onDestroy() {

              Log.e(TAG, "ServerProxyService is being destroyed");
              callErrToast("ServerProxyService is being destroyed");
              gracefullyDisconnect();
              stop();

          }

          public class MyBinder extends Binder {
              public ServerProxyService getService() {
                  return ServerProxyService.this;
              }
          }

          /* IServerProxy operations methods */
//	  	/**
//	  	 * Enables to download a file from the server
//	  	 * @param td - The transfer details contain: Source, Destination, Extension, File size
//	  	 */
//	  	public void downloadFileFromServer(TransferDetails td) 
//	  	{	  		 	  
//	  		MessageDownloadFile msgDLF = new MessageDownloadFile(SharedConstants.MY_ID, td);
//	  		try 
//	  		{	  				  			
//	  			connectionToServer.sendMessage(msgDLF);	  																						
//  			} 
//  			catch (IOException e) 
//  			{	  				
//  				String errMsg = "DOWNLOAD_FAILURE. Failed to download file:"+td.getSourceWithExtension()+". Exception:"+e.getMessage();
//  				sendEventReport(new EventReport(EventType.DOWNLOAD_FAILURE, errMsg, null));	  				
//  				e.printStackTrace();
//	  				
//  				reconnectIfNecessary();
//  			}
//	  	}

          /**
           * Enables to upload a file to the server, sending it to a destination number
           * @param managedFile   - The file to upload inside a manager wrapper
           * @param destNumber - The destination number to whom the file is for
           */
          @Override
          public void uploadFileToServer(final String destNumber, final FileManager managedFile) {
              new Thread() {
                  @Override
                  public void run() {

                      try {
                          TransferDetails td = new TransferDetails(SharedConstants.MY_ID, destNumber, managedFile);
                          MessageUploadFile msgUF = new MessageUploadFile(SharedConstants.MY_ID,td, managedFile.getFileData());

                          // Sending message upload file
                          connectionToServer.sendMessage(msgUF);
                      }
                      catch(IOException e) {
                          e.printStackTrace();
                          String errMsg = "Upload to user:" + destNumber + " failed. Exception:" + e.getMessage()+". Check your internet connection";
                          handleDisconnection(errMsg);
                      }
                      catch (Exception e) {
                          e.printStackTrace();
                          String errMsg = "UPLOAD_FAILURE. Upload to user:" + destNumber + " failed. Exception:" + e.getMessage();
                          sendEventReportBroadcast(new EventReport(EventType.UPLOAD_FAILURE, errMsg, null));
                          //sendEventReport(new EventReport(EventType.UPLOAD_FAILURE, errMsg, null));


                      }
                  }

              }.start();
          }

          /**
           * Enables to check if a destination number is logged-in/online
           *
           * @param destinationId - The number of whom to check is logged-in
           */
          @Override
          public void isLogin(final String destinationId) {
              new Thread() {
                  @Override
                  public void run() {

                      try {
                          if (connectionToServer != null)
                          {
                              MessageIsLogin msgIsLogin = new MessageIsLogin(SharedConstants.MY_ID, destinationId);
                              connectionToServer.sendMessage(msgIsLogin);
                          }


                      } catch (IOException e) {
                          e.printStackTrace();
                          String errMsg = "ISLOGIN_ERROR. Exception:" + e.getMessage()+". Check your internet connection";
                          handleDisconnection(errMsg);
                      }
                  }

              }.start();
          }

          @Override
          public ConnectionToServer getConnectionToServer() {
              return connectionToServer;
          }

          @Override
          public void markHeartBeatAck(Long timestamp) {
              Log.i(TAG, "Marking heartbeat ack");
              setHeartBeatAck(timestamp);
          }

          /* Internal server operations methods */

          /**
           * Enables initial connection to the server
           */
          private void connect() {

              new Thread() {

                  @Override
                  public void run() {
                      try {
                          sendEventReportBroadcast(new EventReport(EventType.CONNECTING, "Connecting...", null));
                          openSocket();
                          startClientActionListener();
                          startKeepAlives();
                          login();
                      } catch (IOException e) {
                          e.printStackTrace();
                          String errMsg = "Failed connect to server:" + e.getMessage();
                          handleDisconnection(errMsg);
                      }

                      // Done connecting. Resetting reconnect interval
                      if(isConnected())
                          SharedPrefUtils.setLong(getApplicationContext(), SharedPrefUtils.SERVER_PROXY, SharedPrefUtils.RECONNECT_INTERVAL, INITIAL_RETRY_INTERVAL);
                  }
              }.start();
          }

          private void gracefullyDisconnect()
          {
              new Thread() {

                  @Override
                  public void run() {
                      if(isConnected())
                      {
                          Log.i(TAG, "Gracefully disconnecting...");
                          setConnected(false);

                          if (connectionToServer != null)
                          {
                              MessageLogout messageLogout = new MessageLogout(SharedConstants.MY_ID);

                              try {
                                  connectionToServer.sendMessage(messageLogout);
                                  connectionToServer.closeConnection();
                                  connectionToServer = null;
                              } catch (IOException e) {
                                  Log.e(TAG, "Failed to log off:" + e.getMessage());
                                  e.printStackTrace();
                              }
                          }
                      }
                  }

              }.start();
          }

          /**
           * Enables to login to the server
           */
          private void login() {
              Log.i(TAG, "Initiating login sequence...");

              new Thread() {
                  @Override
                  public void run() {
                      try {

                          MessageLogin msgLogin = new MessageLogin(SharedConstants.MY_ID, SharedConstants.DEVICE_TOKEN);
                          Log.i(TAG, "Sending login message to server...");
                          connectionToServer.sendMessage(msgLogin);
                      } catch (IOException e) {
                          e.printStackTrace();
                          String errMsg = "LOGIN_FAILURE. Exception:" + e.getMessage()+". Check your internet connection";
                          handleDisconnection(errMsg);
                      }

                  }

              }.start();
          }

          private void openSocket() throws IOException {
              Log.i(TAG, "Opening socket...");
              Socket socketToServer = new Socket(SharedConstants.HOST, SharedConstants.PORT);
              connectionToServer = new ConnectionToServer(socketToServer);
              Log.i(TAG, "Socket is open");
              setConnected(true);

          }

          private void startClientActionListener() {
              Log.i(TAG, "Starting client action listener...");

              new Thread() {
                  public void run() {

                      MessageToClient msgTC;

                      while (isConnected()) {
                          try {
                              msgTC = connectionToServer.getMessage();
                              if (msgTC != null) {
                                  EventReport eventReport = msgTC
                                          .doClientAction(serverProxy);

                                  if(eventReport.status()!=EventType.NO_ACTION_REQUIRED)
                                    sendEventReportBroadcast(eventReport);

                                  //sendEventReport(eventReport);
                              }
                          } catch (IOException e) {
                              e.printStackTrace();
                              String errMsg = "Client action failure. Exception:"+e.getMessage()+". Check your internet connection";
                              handleDisconnection(errMsg);
                          }
                          catch(ClassNotFoundException e) {

                              String errMsg = "Client action failure. Exception:"+e.getMessage();
                              Log.e(TAG, "Client action failure:" + e.getMessage());
                              sendEventReportBroadcast(new EventReport(EventType.CLIENT_ACTION_FAILURE, errMsg, null));
                          }
                      }
                  }
              }.start();
          }

          private synchronized void reconnectIfNecessary() {

              new Thread() {

                  @Override
                  public void run() {

                      if (isNetworkAvailable() && started && connectionToServer == null) {

                          try {
                              String infoMsg = "Reconnecting...";
                              Log.i(TAG, infoMsg);
                              sendEventReportBroadcast(new EventReport(EventType.RECONNECT_ATTEMPT, infoMsg, null));

                              openSocket();
                              startClientActionListener();
                              startKeepAlives();
                              login();
                          } catch (IOException e) {
                              e.printStackTrace();
                          }
                      }

                      // Done reconnecting. Resetting reconnect interval
                      if(isConnected())
                          SharedPrefUtils.setLong(getApplicationContext(), SharedPrefUtils.SERVER_PROXY, SharedPrefUtils.RECONNECT_INTERVAL, INITIAL_RETRY_INTERVAL);
                  }
              }.start();
          }

          private synchronized void sendHeartBeat() {

                  // Checking last heartbeat ack
                  Date date = new Date();
                  Long now = date.getTime();
                  Long elapsed = now-getHeartBeatAck();
                  Log.i(TAG, "Checking HB ack. now-heartBeatAck="+elapsed+ " HEARTBEAT_ACK_TIMEOUT="+HEARTBEAT_ACK_TIMEOUT);
                  if(elapsed > HEARTBEAT_ACK_TIMEOUT) {
                      stopKeepAlives();
                      connect();
                  }
                  else {
                      try {
                          if (msgHB == null) {
                              String myId = SharedPrefUtils.getString(getApplicationContext(), SharedPrefUtils.GENERAL, SharedPrefUtils.MY_NUMBER);
                              msgHB = new MessageHeartBeat(myId);
                          }

                          connectionToServer.sendMessage(msgHB);
                          Log.i(TAG, "Heartbeat sent to server");

                      } catch (IOException | NullPointerException e) {
                          e.printStackTrace();
                          String errMsg = "HEARTBEAT_FAILURE. Exception:" + e.getMessage() + ". Check your internet connection";
                          handleDisconnection(errMsg);
                      }
                  }
          }

          private void startKeepAlives()
          {
              Log.i(TAG, "Starting keep alives");
              Intent i = new Intent();
              i.setClass(this, ServerProxyService.class);
              i.setAction(ACTION_HEARTBEAT);
              PendingIntent pi = PendingIntent.getService(this, 0, i, 0);
              AlarmManager alarmMgr = (AlarmManager)getSystemService(ALARM_SERVICE);
              alarmMgr.setRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                      SystemClock.elapsedRealtime() + HEARTBEAT_INTERVAL,
                      HEARTBEAT_INTERVAL, pi);
          }

          private void stopKeepAlives()
          {
              Intent i = new Intent();
              i.setClass(this, ServerProxyService.class);
              i.setAction(ACTION_HEARTBEAT);
              PendingIntent pi = PendingIntent.getService(getApplicationContext(), 0, i, 0);
              AlarmManager alarmMgr = (AlarmManager)getSystemService(ALARM_SERVICE);
              alarmMgr.cancel(pi);
          }

          private boolean isNetworkAvailable() {

              NetworkInfo wifiInfo = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
              boolean wifiConnected = (wifiInfo != null && wifiInfo.isConnected());

              NetworkInfo mobileInfo = connManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
              boolean mobileConnected = (mobileInfo != null && mobileInfo.isConnected());

              return wifiConnected || mobileConnected;
          }

          private void handleCrashedService() {

              Log.i(TAG, "Handling crashed service");
              if(wasStarted()) {
                  stopKeepAlives();
                  connect();
              }
          }

          private void handleDisconnection(String errMsg) {

              Log.e(TAG, errMsg);
              setConnected(false);
              sendEventReportBroadcast(new EventReport(EventType.DISCONNECTED, errMsg, null));
              connectionToServer=null;
              stopKeepAlives();
              scheduleReconnect(System.currentTimeMillis());
          }

          /* Shared pref helper methods */

          private synchronized boolean isConnected() {
              return SharedPrefUtils.getBoolean(getApplicationContext(), SharedPrefUtils.SERVER_PROXY, SharedPrefUtils.CONNECTED);
          }

          private synchronized void setConnected(boolean state) {
              SharedPrefUtils.setBoolean(getApplicationContext(), SharedPrefUtils.SERVER_PROXY, SharedPrefUtils.CONNECTED, state);
          }

          private boolean wasStarted() {
              return SharedPrefUtils.getBoolean(getApplicationContext(),SharedPrefUtils.SERVER_PROXY, SharedPrefUtils.WAS_STARTED);
          }

          private void setHeartBeatAck(Long timestemp) {

              SharedPrefUtils.setLong(getApplicationContext(), SharedPrefUtils.SERVER_PROXY, SharedPrefUtils.HEARTBEAT_ACK, timestemp);
          }

          private Long getHeartBeatAck() {

              return SharedPrefUtils.getLong(getApplicationContext(), SharedPrefUtils.SERVER_PROXY, SharedPrefUtils.HEARTBEAT_ACK);
          }

          private void setStarted(boolean state) {
              SharedPrefUtils.setBoolean(getApplicationContext(), SharedPrefUtils.SERVER_PROXY, SharedPrefUtils.WAS_STARTED, state);
              this.started = state;
          }

          /* Broadcast Receivers methods */

          private void sendEventReportBroadcast(EventReport report) {

              Log.i(TAG, "Broadcasting event:" + report.status().toString());
              Intent broadcastEvent = new Intent(Event.EVENT_ACTION);
              broadcastEvent.putExtra(Event.EVENT_REPORT, report);
              sendBroadcast(broadcastEvent);
          }

          /* Internal operations methods */

          private synchronized void start()
          {
              if (started)
              {
                  Log.w(TAG, "Attempt to start connection that is already active");
                  return;
              }

              setStarted(true);

              connect();
          }

          private synchronized void stop() {

              if (!started)
              {
                  Log.w(TAG, "Attempt to stop connection not active.");
                  return;
              }

              setStarted(false);

//              unregisterReceiver(mConnectivityChanged);
//              cancelReconnect();

//              gracefullyDisconnect();
              sendEventReportBroadcast(new EventReport(EventType.DISCONNECTED, "Disconnected. Check your internet connection", null));
//              stopSelf();
          }

          private void cancelReconnect()
          {
              Log.i(TAG, "Cancelling reconnect");
              Intent i = new Intent();
              i.setClass(this, ServerProxyService.class);
              i.setAction(ACTION_RECONNECT);
              PendingIntent pi = PendingIntent.getService(getApplicationContext(), 0, i, 0);
              AlarmManager alarmMgr = (AlarmManager)getSystemService(ALARM_SERVICE);
              alarmMgr.cancel(pi);
          }

          private void scheduleReconnect(long startTime)
          {
              Log.i(TAG, "Scheduling reconnect");
              long interval =
                      SharedPrefUtils.getLong(getApplicationContext(),SharedPrefUtils.SERVER_PROXY,SharedPrefUtils.RECONNECT_INTERVAL, INITIAL_RETRY_INTERVAL);

              long now = System.currentTimeMillis();
              long elapsed = now - startTime;

              if (elapsed < interval)
                  interval = Math.min(interval * 4, MAXIMUM_RETRY_INTERVAL);
              else
                  interval = INITIAL_RETRY_INTERVAL;

              Log.i(TAG, "Rescheduling connection in " + interval + "ms.");

              SharedPrefUtils.setLong(getApplicationContext(), SharedPrefUtils.SERVER_PROXY, SharedPrefUtils.RECONNECT_INTERVAL, interval);


              Intent i = new Intent();
              i.setClass(this, ServerProxyService.class);
              i.setAction(ACTION_RECONNECT);
              PendingIntent pi = PendingIntent.getService(getApplicationContext(), 0, i, 0);
              AlarmManager alarmMgr = (AlarmManager)getSystemService(ALARM_SERVICE);
              alarmMgr.set(AlarmManager.RTC_WAKEUP, now + interval, pi);
          }

          /* UI methods */

          private void callErrToast(final String text) {

              Toast toast = Toast.makeText(getApplicationContext(), text,
                      Toast.LENGTH_LONG);
              TextView v = (TextView) toast.getView().findViewById(
                      android.R.id.message);
              v.setTextColor(Color.RED);
              toast.show();
          }

          private void callInfoToast(final String text) {

              Toast toast = Toast.makeText(getApplicationContext(), text,
                      Toast.LENGTH_LONG);
              TextView v = (TextView) toast.getView().findViewById(
                      android.R.id.message);
              v.setTextColor(Color.GREEN);
              toast.show();
          }

          private void callInfoToast(final String text, final int g) {

              Toast toast = Toast.makeText(getApplicationContext(), text,
                      Toast.LENGTH_LONG);
              TextView v = (TextView) toast.getView().findViewById(
                      android.R.id.message);
              v.setTextColor(g);
              toast.show();
          }
      }