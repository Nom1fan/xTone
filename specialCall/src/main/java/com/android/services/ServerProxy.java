package com.android.services;

import android.app.Service;
import android.content.Intent;
import android.graphics.Color;
import android.os.Binder;
import android.os.Environment;
import android.os.IBinder;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;

import ClientObjects.ConnectionToServer;
import ClientObjects.IServerProxy;
import DataObjects.SharedConstants;
import DataObjects.TransferDetails;
import EventObjects.Event;
import EventObjects.EventGenerator;
import EventObjects.EventReport;
import EventObjects.EventType;
import MessagesToClient.MessageToClient;
import MessagesToServer.MessageHeartBeat;
import MessagesToServer.MessageIsLogin;
import MessagesToServer.MessageLogin;
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

//          private EventGenerator _eventGenerator;        // Used to fire events to listeners
          private ConnectionToServer connectionToServer;
          private ServerProxy serverProxy = this;
          private final int HEARTBEAT_INTERVAL = 20 * 1000; // 20 seconds
          private final int RECONNECT_INTERVAL = 5000; // 5 seconds
          private static volatile boolean connected = false;
          private static volatile boolean _locked = false;
          private final IBinder mBinder = new MyBinder();

          private final String tag = "SERVER_PROXY";

//          public ServerProxy(EventGenerator eventGenerator) {
//
//              _eventGenerator = eventGenerator;
//          }

		/* Service operations methods */

          @Override
          public int onStartCommand(Intent intent, int flags, int startId) {
              super.onStartCommand(intent, flags, startId);

//              Intent i = new Intent(Event.EVENT_ACTION);
//              i.putExtra("eventreport", new EventReport(EventType.SERVICE_STARTED, null, null));
//              sendBroadcast(i);
              Log.i(tag, "ServerProxy service started");
              connect();
              return Service.START_STICKY;
          }

          @Override
          public IBinder onBind(Intent arg0) {
              return mBinder;
          }


          @Override
          public void onCreate() {

             Log.i(tag, "ServerProxy service created");
             callInfoToast("ServerProxy service created");
             SharedConstants.MY_ID = SharedPrefUtils.getString(getApplicationContext(),SharedPrefUtils.GENERAL, SharedPrefUtils.MY_NUMBER);
             SharedConstants.specialCallPath = Constants.specialCallPath;

             if(!SharedConstants.MY_ID.equals(""))
              connect();

          }

          @Override
          public void onDestroy() {

              Log.e(tag, "ServerProxy service is being destroyed");
              callErrToast("ServerProxy service is being destroyed");
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
                          Log.e(tag, infoMsg);
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
              Log.i(tag, "Initiating login sequence...");

              new Thread() {
                  @Override
                  public void run() {
                      try {
                          MessageLogin msgLogin = new MessageLogin(SharedConstants.MY_ID);
                          Log.i(tag, "Sending login message to server...");
                          connectionToServer.sendMessage(msgLogin);
                      } catch (Exception e1) {
                          Log.e(tag, "Login failed:" + e1.getMessage());
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
          public void uploadFileToServer(final byte[] fileData, final String extension, final String destNumber) {
              new Thread() {
                  @Override
                  public void run() {

                      TransferDetails td = new TransferDetails(SharedConstants.MY_ID, destNumber, fileData.length, extension);

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
           * @param phoneNumber - The number of whom to check is logged-in
           */
          public void isLogin(final String phoneNumber) {
              new Thread() {
                  @Override
                  public void run() {
                      MessageIsLogin msgIsLogin = new MessageIsLogin(SharedConstants.MY_ID, phoneNumber);

                      try {
                          if (connectionToServer != null)
                              connectionToServer.sendMessage(msgIsLogin);
                      } catch (IOException e) {
                          String errMsg = "ISLOGIN_ERROR. Exception:" + e.getMessage();
                          //sendEventReport(new EventReport(EventType.ISLOGIN_ERROR, errMsg, phoneNumber));
                          sendEventReportBroadcast(new EventReport(EventType.ISLOGIN_ERROR, errMsg, phoneNumber));
                          e.printStackTrace();

                          attemptToReconnect();
                      }
                  }

              }.start();
          }

          public ConnectionToServer getConnectionToServer() {
              return connectionToServer;
          }
	  	
	  	/* Internal server operations methods */

          private synchronized void setConnected() {
              connected = true;
          }

          private synchronized void setDisconnected() {
              connected = false;
          }

          private void openSocket() throws UnknownHostException, IOException {
              Log.i(tag, "Opening socket...");
              Socket socketToServer = new Socket(SharedConstants.HOST, SharedConstants.PORT);
              connectionToServer = new ConnectionToServer(socketToServer);
              Log.i(tag, "Socket is open");
              setConnected();
          }

          private void startClientActionListener() {
              Log.i(tag, "Starting client action listener...");

              new Thread() {
                  public void run() {

                      MessageToClient msgTC = null;

                      while (connected) {
                          try {
                              msgTC = connectionToServer.getMessage();
                              if (msgTC != null) {
                                  EventReport eventReport = msgTC
                                          .doClientAction(serverProxy);

                                  sendEventReportBroadcast(eventReport);

                                  //sendEventReport(eventReport);
                              }
                          } catch (ClassNotFoundException | IOException e) {
                              Log.e(tag, "Client action failure:" + e.getMessage());
                              //String errMsg = "CLIENT_ACTION_FAILURE. Exception:"+e.getMessage();
                              //sendEventReport(new EventReport(EventType.CLIENT_ACTION_FAILURE, errMsg,null));
                              e.printStackTrace();

                              attemptToReconnect();
                          }
                      }
                  }
              }.start();
          }

          public void startHeartBeatThread() {
              Log.i(tag, "Starting heartbeat thread...");

              new Thread() {
                  @Override
                  public void run() {
                      MessageHeartBeat msgHB = new MessageHeartBeat(SharedConstants.MY_ID);

                      while (connected)
                          sendHeartBeat(msgHB);
                  }

                  private void sendHeartBeat(MessageHeartBeat msgHB) {

                      try {
                          connectionToServer.sendMessage(msgHB);
                          Log.i(tag, "Heartbeat sent to server");

                          Thread.sleep(HEARTBEAT_INTERVAL);
                      } catch (IOException | InterruptedException e) {
                          Log.e(tag, "Heartbeat failure:" + e.getMessage());
                          String errMsg = "HEARTBEAT_FAILURE. Exception:" + e.getMessage();
                          sendEventReportBroadcast(new EventReport(EventType.DISPLAY_ERROR, errMsg, null));
                          //sendEventReport(new EventReport(EventType.DISPLAY_ERROR, errMsg, null));
                          e.printStackTrace();

                          attemptToReconnect();
                      }
                  }

              }.start();
          }

          private void attemptToReconnect() {
              new Thread() {
                  @Override
                  public void run() {
                      if (!_locked) {
                          _locked = true;
                          setDisconnected();

                          while (!connected) {
                              try {
                                  String infoMsg = "Reconnecting...";
                                  Log.i(tag, infoMsg);
                                  sendEventReportBroadcast(new EventReport(EventType.DISPLAY_ERROR, infoMsg, null));
                                  //sendEventReport(new EventReport(EventType.DISPLAY_ERROR, infoMsg, null));

                                  Thread.sleep(RECONNECT_INTERVAL);
                                  openSocket();
                                  startClientActionListener();
                                  startHeartBeatThread();
                                  login();
                              } catch (IOException | InterruptedException e) {
                                  Log.e(tag, e.getMessage());
                                  e.printStackTrace();
                              }

                          }
                          _locked = false;
                      }
                  }

              }.start();
          }

//          private void sendEventReport(EventReport report) {
//
//              Log.i(tag, "Firing event:" + report.status().toString());
//              _eventGenerator.fireEvent(report);
//          }

          private void sendEventReportBroadcast(EventReport report) {

              Log.i(tag, "Broadcasting event:" + report.status().toString());
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