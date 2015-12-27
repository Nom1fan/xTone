package com.async_tasks;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import com.data_objects.Constants;
import com.interfaces.CallbackListener;
import com.ui.components.NotificationHelper;
import com.utils.BroadcastUtils;
import com.utils.NotificationUtils;
import java.io.BufferedInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import ClientObjects.ConnectionToServer;
import DataObjects.TransferDetails;
import EventObjects.EventReport;
import EventObjects.EventType;
import FilesManager.FileManager;
import MessagesToServer.MessageUploadFile;


public class UploadTask extends AsyncTask<Void,String,Void> {
    private NotificationHelper mNotificationHelper;
    private static final String TAG = UploadTask.class.getSimpleName();
    private ConnectionToServer _connectionToServer;
    private TransferDetails _td;
    private Context _context;
    private CallbackListener _callBackListener;

    public UploadTask(Context context, CallbackListener callBackListener , ConnectionToServer connectionToServer, TransferDetails td) {
        _context = context;
        _callBackListener = callBackListener;
        mNotificationHelper = NotificationUtils.getNextHelper();
        _connectionToServer = connectionToServer;
        _td = td;
    }

    protected void onPreExecute(){
        // Update initial notification message
        //mNotificationHelper.createUploadNotification();
    }

    @Override
    protected Void doInBackground(Void... voids) {

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

            //_connectionToServer.closeConnection();
        }
         catch (IOException e) {
            e.printStackTrace();
            Log.e(TAG, "Failed:" + e.getMessage());
             BroadcastUtils.sendEventReportBroadcast(_context, TAG,
                     new EventReport(EventType.UPLOAD_FAILURE, "Upload to "+_td.getDestinationId()+" failed:"+e.getMessage(),null));
        }

        Log.i(TAG, "Deleting "+_td.getDestinationId()+"'s outgoing folder after upload");
        File specialCallOutgoingDir = new File(Constants.specialCallOutgoingPath+_td.getDestinationId());

        String[] entries = specialCallOutgoingDir.list();
        for (String s : entries) {
            File currentFile = new File(specialCallOutgoingDir.getPath(), s);
            currentFile.delete();
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
        NotificationUtils.freeHelperSpace();
        //The task is complete, tell the call back listener about it
        _callBackListener.doCallbackAction();
    }

}