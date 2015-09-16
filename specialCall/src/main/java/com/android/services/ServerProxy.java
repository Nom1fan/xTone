package com.android.services;

import android.app.Service;
import android.content.Intent;
import android.graphics.Color;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import com.parse.ParseInstallation;

import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;

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
import MessagesToServer.MessageSendPush;
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
	  public class ServerProxy extends Service implements IServerProxy {

          private ConnectionToServer connectionToServer;
          private ServerProxy serverProxy = this;
          private final int HEARTBEAT_INTERVAL = SharedConstants.HEARTBEAT_INTERVAL;
          private final int RECONNECT_INTERVAL = 5000; // milliseconds
          private final int PUSH_SLEEP_TIME = 1000; // milliseconds
          private MessageHeartBeat msgHB;
          private final IBinder mBinder = new MyBinder();
          private final String TAG = "SERVER_PROXY";


		/* Service operations methods */

          @Override
          public int onStartCommand(Intent intent, int flags, int startId) {
              super.onStartCommand(intent, flags, startId);


              Log.i(TAG, "ServerProxy service started");
              callInfoToast("ServerProxy service started");


              if(connectionToServer!=null) {

                  new Thread() {

                      @Override
                      public void run() {
                          sendHeartBeat();
                      }
                  }.start();
              }

              return Service.START_STICKY;
          }

          @Override
          public IBinder onBind(Intent arg0) {
              return mBinder;
          }


          @Override
          public void onCreate() {

             Log.i(TAG, "ServerProxy service created");
             callInfoToast("ServerProxy service created");
             SharedConstants.MY_ID = SharedPrefUtils.getString(getApplicationContext(),SharedPrefUtils.GENERAL, SharedPrefUtils.MY_NUMBER);
             SharedConstants.DEVICE_TOKEN = SharedPrefUtils.getString(getApplicationContext(), SharedPrefUtils.GENERAL, SharedPrefUtils.MY_DEVICE_TOKEN);
             SharedConstants.specialCallPath = Constants.specialCallPath;

             if(SharedConstants.MY_ID.equals(""))
                callErrToast("Can't login using empty phone number");
             else if(SharedConstants.DEVICE_TOKEN.equals(""))
                callErrToast("Can't login using empty device token");
             else
                connect();

          }

          @Override
          public void onDestroy() {

              Log.e(TAG, "ServerProxy service is being destroyed");
              callErrToast("ServerProxy service is being destroyed");
              gracefullyDisconnect();
          }

          public class MyBinder extends Binder {
              public ServerProxy getService() {
                  return ServerProxy.this;
              }
          }


	  	/* General server operations methods */

          /**
           * Enables initial connection to the server
           */
          public void connect() {

              new Thread() {

                  @Override
                  public void run() {
                      try {
                          openSocket();
                          startClientActionListener();
                          startHeartBeatThread();
                          login();
                      } catch (IOException e) {
                          String infoMsg = "Failed connect to server:" + e.getMessage();
                          Log.e(TAG, infoMsg);
                          sendEventReportBroadcast(new EventReport(EventType.DISPLAY_ERROR, infoMsg, null));
                          //sendEventReport(new EventReport(EventType.DISPLAY_ERROR, infoMsg, null));
                          attemptToReconnect();
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
                      } catch (Exception e1) {
                          Log.e(TAG, "Login failed:" + e1.getMessage());
                          String errMsg = "LOGIN_FAILURE. Exception:" + e1.getMessage();
                          //sendEventReport(new EventReport(EventType.DISPLAY_ERROR, errMsg, null));
                          sendEventReportBroadcast(new EventReport(EventType.DISPLAY_ERROR,errMsg,null));
                          e1.printStackTrace();

                          attemptToReconnect();
                      }

                  }

              }.start();
          }

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
//  				attemptToReconnect();
//  			}
//	  	}

          /**
           * Enables to upload a file to the server, sending it to a destination number
           *
           * @param fileData   - The data of the file being uploaded
           * @param extension  - The extension/file type of the file being uploaded
           * @param destNumber - The destination number to whom the file is for
           */
          public void uploadFileToServer(final byte[] fileData, final String extension, final FileManager.FileType fileType, final String destNumber, final String fullFilePath) {
              new Thread() {
                  @Override
                  public void run() {

                      TransferDetails td = new TransferDetails(SharedConstants.MY_ID, destNumber, fileData.length, extension, fileType, fullFilePath);

                      try {


                          MessageUploadFile msgUF = new MessageUploadFile(SharedConstants.MY_ID, td, fileData);

                          // Sending message upload file
                          connectionToServer.sendMessage(msgUF);
                      } catch (Exception e) {
                          String errMsg = "UPLOAD_FAILURE. Upload to user:" + destNumber + " failed. Exception:" + e.getMessage();
                          sendEventReportBroadcast(new EventReport(EventType.UPLOAD_FAILURE, errMsg, null));
                          //sendEventReport(new EventReport(EventType.UPLOAD_FAILURE, errMsg, null));
                          e.printStackTrace();

                          attemptToReconnect();
                      }
                  }

              }.start();
          }

          /**
           * Enables to check if a destination number is logged-in/online
           *
           * @param destinationId - The number of whom to check is logged-in
           */
          public void isLogin(final String destinationId) {
              new Thread() {
                  @Override
                  public void run() {

                      try {
                          if (connectionToServer != null)
                          {
                              MessageSendPush msgSendPush = new MessageSendPush(SharedConstants.MY_ID, destinationId);
                              connectionToServer.sendMessage(msgSendPush);

                              Thread.sleep(PUSH_SLEEP_TIME);

                              MessageIsLogin msgIsLogin = new MessageIsLogin(SharedConstants.MY_ID, destinationId);
                              connectionToServer.sendMessage(msgIsLogin);
                          }

                      } catch (IOException | InterruptedException e) {
                          String errMsg = "ISLOGIN_ERROR. Exception:" + e.getMessage();
                          //sendEventReport(new EventReport(EventType.ISLOGIN_ERROR, errMsg, destinationId));
                          sendEventReportBroadcast(new EventReport(EventType.ISLOGIN_ERROR, errMsg, destinationId));
                          e.printStackTrace();

                          attemptToReconnect();
                      }
                  }

              }.start();
          }

          public ConnectionToServer getConnectionToServer() {
              return connectionToServer;
          }

          public void gracefullyDisconnect()
          {

              new Thread() {

                  @Override
                  public void run() {
                      if(!isConnected())
                      {
                          setConnected(false);

                          if (connectionToServer != null)
                          {
                              MessageLogout messageLogout = new MessageLogout(SharedConstants.MY_ID);

                              try {
                                  connectionToServer.sendMessage(messageLogout);
                              } catch (IOException e) {
                                  Log.e(TAG, "Failed to log off:" + e.getMessage());
                                  e.printStackTrace();
                              }

                              Socket s = connectionToServer.getSocket();
                              if (s != null) {
                                  try {
                                      s.close();
                                  } catch (IOException e) {
                                      e.printStackTrace();
                                  }
                              }
                          }
                      }
                  }

              }.start();
          }
	  	
	  	/* Internal server operations methods */

          private synchronized boolean isConnected() {
              return SharedPrefUtils.getBoolean(getApplicationContext(), SharedPrefUtils.GENERAL, SharedPrefUtils.CONNECTED);
          }

          private synchronized void setConnected(boolean state) {
              SharedPrefUtils.setBoolean(getApplicationContext(), SharedPrefUtils.GENERAL, SharedPrefUtils.CONNECTED, state);
          }

          private synchronized boolean isReconnecting() {

              return SharedPrefUtils.getBoolean(getApplicationContext(), SharedPrefUtils.GENERAL, SharedPrefUtils.RECONNECTING);
          }

          private synchronized void setReconnecting(boolean state) {

              SharedPrefUtils.setBoolean(getApplicationContext(), SharedPrefUtils.GENERAL, SharedPrefUtils.RECONNECTING, state);
          }


          private void openSocket() throws UnknownHostException, IOException {
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

                      MessageToClient msgTC = null;

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
                          } catch (ClassNotFoundException | IOException e) {
                              Log.e(TAG, "Client action failure:" + e.getMessage());
                              e.printStackTrace();

                              attemptToReconnect();
                          }
                      }
                  }
              }.start();
          }

          public void startHeartBeatThread() {
              Log.i(TAG, "Starting heartbeat thread...");

              new Thread() {
                  @Override
                  public void run() {

                      while (isConnected())
                          sendHeartBeat();
                  }


              }.start();
          }

          private void attemptToReconnect() {
              new Thread() {
                  @Override
                  public void run() {
                      if (!isReconnecting()) {
                          setReconnecting(true);
                          setConnected(false);

                          while (!isConnected()) {
                              try {
                                  String infoMsg = "Reconnecting...";
                                  Log.i(TAG, infoMsg);
                                  sendEventReportBroadcast(new EventReport(EventType.DISPLAY_ERROR, infoMsg, null));
                                  //sendEventReport(new EventReport(EventType.DISPLAY_ERROR, infoMsg, null));

                                  Thread.sleep(RECONNECT_INTERVAL);
                                  openSocket();
                                  startClientActionListener();
                                  startHeartBeatThread();
                                  login();
                              } catch (IOException | InterruptedException e) {
                                  Log.e(TAG, e.getMessage());
                                  e.printStackTrace();
                              }

                          }
                          // Done reconnecting
                          setReconnecting(false);
                      }
                  }

              }.start();
          }




//          private void sendEventReport(EventReport report) {
//
//              Log.i(TAG, "Firing event:" + report.status().toString());
//              _eventGenerator.fireEvent(report);
//          }
          private void sendHeartBeat() {

              try {
                  if(msgHB==null)
                     msgHB = new MessageHeartBeat(SharedConstants.MY_ID);

                  connectionToServer.sendMessage(msgHB);
                  Log.i(TAG, "Heartbeat sent to server");

                  Thread.sleep(HEARTBEAT_INTERVAL);
              } catch (IOException | InterruptedException e) {
                  Log.e(TAG, "Heartbeat failure:" + e.getMessage());
                  String errMsg = "HEARTBEAT_FAILURE. Exception:" + e.getMessage();
                  sendEventReportBroadcast(new EventReport(EventType.DISPLAY_ERROR, errMsg, null));
                  //sendEventReport(new EventReport(EventType.DISPLAY_ERROR, errMsg, null));
                  e.printStackTrace();

                  attemptToReconnect();
              }
          }

          private void sendEventReportBroadcast(EventReport report) {

              Log.i(TAG, "Broadcasting event:" + report.status().toString());
              Intent broadcastEvent = new Intent(Event.EVENT_ACTION);
              broadcastEvent.putExtra(Event.EVENT_REPORT, report);
              sendBroadcast(broadcastEvent);
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