package com.async_tasks;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.util.Log;

import com.data_objects.Constants;
import com.mediacallz.app.R;
import com.utils.BroadcastUtils;

import java.io.BufferedInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.Serializable;

import ClientObjects.ConnectionToServer;
import DataObjects.TransferDetails;
import EventObjects.EventReport;
import EventObjects.EventType;
import FilesManager.FileManager;
import MessagesToServer.MessageUploadFile;


public class UploadTask extends AsyncTask<Void,Integer,Void> implements Serializable {
    //private NotificationHelper mNotificationHelper;
    private static final String TAG = UploadTask.class.getSimpleName();
    private ConnectionToServer _connectionToServer;
    private TransferDetails _td;
    private Activity _context;
    private ProgressDialog _progDialog;
    private UploadTask _taskInstance;

    public UploadTask(ConnectionToServer connectionToServer, TransferDetails td) {

        _connectionToServer = connectionToServer;
        _td = td;
        _taskInstance = this;
    }

    @Override
    protected void onPreExecute() {

        String cancel = _context.getResources().getString(R.string.cancel);

        _progDialog.setCancelable(false);
        _progDialog.setTitle(_context.getResources().getString(R.string.uploading));
        _progDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        _progDialog.setProgress(0);
        _progDialog.setMax(100);
        _progDialog.setButton(DialogInterface.BUTTON_NEGATIVE, cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

                _taskInstance.cancel(true);
            }
        });

        _progDialog.show();

    }

    @Override
    protected Void doInBackground(Void... voids) {

        FileManager managedFile = _td.get_managedFile();
        MessageUploadFile msgUF = new MessageUploadFile(_td.getSourceId(),_td);

        DataOutputStream dos;
        BufferedInputStream bis = null;
        try {
            _connectionToServer.sendToServer(msgUF);
            
            Log.i(TAG, "Initiating file data upload...");

            dos = new DataOutputStream(_connectionToServer.getClientSocket().getOutputStream());

            FileInputStream fis = new FileInputStream(managedFile.getFile());
            bis = new BufferedInputStream(fis);

            byte[] buf = new byte[1024 * 8];
            long fileSize = managedFile.getFileSize();
            long bytesToRead = fileSize;
            int bytesRead;
            while (bytesToRead > 0 && (bytesRead = bis.read(buf, 0, (int) Math.min(buf.length, bytesToRead))) != -1) {
                dos.write(buf, 0, bytesRead);
                float percent = (float) (fileSize - bytesToRead) / fileSize * 100;
                publishProgress((int)percent);
                bytesToRead -= bytesRead;
            }

        }
         catch (IOException e) {
            e.printStackTrace();
            Log.e(TAG, "Failed:" + e.getMessage());
             BroadcastUtils.sendEventReportBroadcast(_context, TAG,
                     new EventReport(EventType.STORAGE_ACTION_FAILURE, "Upload to "+_td.getDestinationId()+" failed:"+e.getMessage(),null));
        } finally {

            if(bis!=null) {
                try {
                    bis.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            clearMembers();

            Log.i(TAG, "Deleting "+_td.getDestinationId()+"'s temp compressed folder after upload");
            File tempCompressedDir = new File(Constants.TEMP_COMPRESSED_FOLDER +_td.getDestinationId());

            String[] entries = tempCompressedDir.list();
            for (String s : entries) {
                File currentFile = new File(tempCompressedDir.getPath(), s);
                currentFile.delete();
            }
        }

        return null;
    }

    @Override
    protected void onProgressUpdate(Integer... progress) {

        if(_progDialog!=null) {
            _progDialog.incrementProgressBy(progress[0]);
        }
    }

    @Override
    protected void onPostExecute(Void result)    {

        // The task is complete, tell the status bar about it
        // mNotificationHelper.completed();
        // NotificationUtils.freeHelperSpace();
        //The task is complete, tell the call back listener about it
        clearMembers();
    }

    private void clearMembers() {

        _context = null;
        if(_progDialog!=null) {
            _progDialog.dismiss();
            _progDialog = null;
        }
    }

    public void set_context(Activity _context) {
        this._context = _context;
    }

    public void set_progDialog(ProgressDialog _progDialog) {
        this._progDialog = _progDialog;
    }
}