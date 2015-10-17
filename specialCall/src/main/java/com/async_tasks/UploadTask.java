package com.async_tasks;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import com.ui.components.NotificationHelper;
import com.utils.BroadcastUtils;

import java.io.BufferedInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.sql.Connection;

import ClientObjects.ConnectionToServer;
import ClientObjects.IServerProxy;
import DataObjects.SharedConstants;
import DataObjects.TransferDetails;
import EventObjects.EventReport;
import EventObjects.EventType;
import FilesManager.FileManager;
import MessagesToClient.MessageToClient;
import MessagesToServer.MessageUploadFile;

public class UploadTask extends AsyncTask<TransferDetails,String,Void> {
    private NotificationHelper mNotificationHelper;
    private static final String TAG = UploadTask.class.getSimpleName();
    private ConnectionToServer _connectionToServer;
    private TransferDetails _td;
    private Context _context;

    public UploadTask(Context context, ConnectionToServer connectionToServer){
        _context = context;
        mNotificationHelper = new NotificationHelper(_context);
        _connectionToServer = connectionToServer;
    }

    protected void onPreExecute(){
        //Create the notification in the statusbar
        mNotificationHelper.createUploadNotification();
    }

    @Override
    protected Void doInBackground(TransferDetails... details) {

        _td = details[0];
        FileManager managedFile = _td.get_managedFile();
        MessageUploadFile msgUF = new MessageUploadFile(_td.getSourceId(),_td);

        try {
            _connectionToServer.sendToServer(msgUF);


            Log.i(TAG, "Initiating file data upload...");

            DataOutputStream dos = new DataOutputStream(_connectionToServer.getClientSocket().getOutputStream());

            FileInputStream fis = new FileInputStream(managedFile.getFile());
            BufferedInputStream bis = new BufferedInputStream(fis);

            byte[] buf = new byte[1024 * 8];
            long fileSize = managedFile.getFileSize();
            long bytesToRead = fileSize;
            int bytesRead;
            while (bytesToRead > 0 && (bytesRead = bis.read(buf, 0, (int) Math.min(buf.length, bytesToRead))) != -1) {
                dos.write(buf, 0, bytesRead);
                String msg = "File upload to:"+_td.getDestinationId()+" %.0f%% complete";
                float percent = (float) (fileSize - bytesToRead) / fileSize * 100;
                publishProgress(String.format(msg, percent));
                bytesToRead -= bytesRead;
            }
        }
         catch (IOException e) {
            e.printStackTrace();
            Log.e(TAG, "Failed:" + e.getMessage());
        }

        return null;
    }
    protected void onProgressUpdate(String... progress) {
        //This method runs on the UI thread, it receives progress updates
        //from the background thread and publishes them to the status bar
        mNotificationHelper.progressUpdate(progress[0]);
    }
    protected void onPostExecute(Void result)    {
        //The task is complete, tell the status bar about it
        mNotificationHelper.completed();
    }

}