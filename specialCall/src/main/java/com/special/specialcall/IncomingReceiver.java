package com.special.specialcall;

import android.app.ActivityManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.IBinder;
import android.provider.MediaStore;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.util.List;
import DataObjects.TransferDetails;
import EventObjects.Event;
import EventObjects.EventReport;
import EventObjects.EventType;
import Exceptions.FileDoesNotExistException;
import Exceptions.FileExceedsMaxSizeException;
import Exceptions.FileInvalidFormatException;
import Exceptions.FileMissingExtensionException;
import FilesManager.FileManager;
import data_objects.Constants;
import data_objects.SharedPrefUtils;


public class IncomingReceiver extends Service {

    public static boolean wasSpecialRingTone = false;
    public static boolean isInFront = false;
    private static CallStateListener phoneListener;
    private String incomingCallNumber;
	private Context gcontext;
	private static volatile boolean InRingingSession = false;
	private Intent specialCallIntent = new Intent();
	private ActivityManager mActivityManager;
    private final int TOP_ACTIVITY_RETRIES = 5;
    private IntentFilter intentFilter = new IntentFilter(Event.EVENT_ACTION);
	private static final String TAG = "IncomingReceiver";


    /* Service operations methods */

	@Override
	public void onCreate() {
		Log.i(TAG, "Service onCreate");
        callInfoToast("IncomingSpecialCall Service created", Color.CYAN);
        registerReceiver(downloadReceiver,intentFilter);

		try
		{
			gcontext = getApplicationContext();

			if (phoneListener==null){
				phoneListener = new CallStateListener();
				TelephonyManager tm = (TelephonyManager) gcontext.getSystemService(Context.TELEPHONY_SERVICE);
				tm.listen(phoneListener, PhoneStateListener.LISTEN_CALL_STATE);

			}
		}
		catch (Exception e) {
            e.printStackTrace();
			Log.e(TAG, e.getMessage());
		}

	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {


		Log.i(TAG, "onStartCommand START_STICKY");
		return START_STICKY;

	}

	@Override
	public void onDestroy() {
        Log.e(TAG, "Service onDestroy");
        if(downloadReceiver!=null)
            unregisterReceiver(downloadReceiver);

	}

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

    /* Assisting methods and listeners */

    /**
     * Listener for downloads
     * Responsible for setting/deleting files and preparing for later media display after a new successful download event is received
     */
    private BroadcastReceiver downloadReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            EventReport eventReport = (EventReport) intent.getSerializableExtra(Event.EVENT_REPORT);

            if (eventReport.status() == EventType.DOWNLOAD_SUCCESS)
            {
                Log.i(TAG, "In: DOWNLOAD_SUCCESS");
                TransferDetails td = (TransferDetails) eventReport.data();
                FileManager.FileType fType = td.getFileType();
                String extension = td.getExtension();
                String fFullName = td.getSourceWithExtension();
                String source = td.getSourceId();

                switch (fType) {
                    case RINGTONE:
                        setNewRingTone(fFullName, source, extension);
                        deleteFilesIfNecessary(fFullName, fType, source);
                        break;

                    case VIDEO:
                    case IMAGE:
                        SharedPrefUtils.setString(getApplicationContext(),
                                SharedPrefUtils.MEDIA_EXTENSION, source,
                                extension);
                        deleteFilesIfNecessary(fFullName, fType, source);
                        break;
                }


            }
        }

        /**
         * Deletes files in the source's designated directory by an algorithm based on the new downloaded file type:
         * This method does not delete the new downloaded file.
         * lets mark newDownloadedFileType as nDFT.
         * nDFT = IMAGE --> deletes images and videos
         * nDFT = RINGTONE --> deletes ringtones and videos
         * nDFT = VIDEO --> deletes all
         *
         * @param newDownloadedFileType The type of the files just downloaded and should be created in the source designated folder
         * @param source The source number of the sender of the file
         */
        private void deleteFilesIfNecessary(String addedFileName, FileManager.FileType newDownloadedFileType, String source) {

            File spDir = new File(Constants.specialCallPath+source);
            File[] files = spDir.listFiles();
            try
            {
                switch (newDownloadedFileType)
                {
                    case RINGTONE:

                        for (int i = 0; i < files.length; ++i)
                        {
                            File file = files[i];
                            String fileName = file.getName(); // This includes extension
                            FileManager.FileType fileType = FileManager.getFileType(file);

                            if (!fileName.equals(addedFileName) &&
                                    (fileType == FileManager.FileType.VIDEO ||
                                            fileType == FileManager.FileType.RINGTONE)) {
                                FileManager.delete(file);
                            }
                        }
                        break;
                    case IMAGE:

                        for (int i = 0; i < files.length; ++i)
                        {
                            File file = files[i];
                            String fileName = file.getName(); // This includes extension
                            FileManager.FileType fileType = FileManager.getFileType(file);

                            if (!fileName.equals(addedFileName) &&
                                    (fileType == FileManager.FileType.VIDEO ||
                                            fileType == FileManager.FileType.IMAGE)) {
                                FileManager.delete(file);
                            }
                        }
                        break;

                    case VIDEO:

                        for (int i = 0; i < files.length; ++i)
                        {
                            File file = files[i];
                            String fileName = file.getName(); // This includes extension
                            if(!fileName.equals(addedFileName))
                                FileManager.delete(file);
                        }
                        break;
                }

            }
            catch (FileInvalidFormatException e) {
                e.printStackTrace();
                Log.e(TAG, "Invalid file type:"+e.getMessage()+" in SpecialCall directory of source:"+source);
            } catch (FileDoesNotExistException e) {
                e.printStackTrace();
                Log.e(TAG, e.getMessage());
            } catch (FileMissingExtensionException e) {
                e.printStackTrace();
                Log.e(TAG, e.getMessage());
            }

        }

    };

    /**
     * Listener for call states
     * Responsible for setting and starting special data on call and restoring previous data once call is terminated
     */
	private class CallStateListener extends PhoneStateListener {


		@Override
		public void onCallStateChanged(int state, String incomingNumber) {
			syncOnCallStateChange(state,incomingNumber);
		}
	}

	public synchronized void syncOnCallStateChange(int state, String incomingNumber){

		switch(state)
		{

			case TelephonyManager.CALL_STATE_RINGING:
				if (!InRingingSession)
				{
					Log.i(TAG, "TelephonyManager.CALL_STATE_RINGING");

                    InRingingSession = true;
                    incomingCallNumber = incomingNumber;

                    saveOldRingToneUri();
	                startRingToneSpecialCall();
                    startMediaSpecialCall();
                }
            break;

			case TelephonyManager.CALL_STATE_IDLE:

				Log.i(TAG, "TelephonyManager.CALL_STATE_IDLE");
				if (wasSpecialRingTone)
				{
                    String oldUri = SharedPrefUtils.getString(gcontext, SharedPrefUtils.GENERAL, SharedPrefUtils.OLD_RINGTONE_URI);
					RingtoneManager.setActualDefaultRingtoneUri(
                            gcontext,
                            RingtoneManager.TYPE_RINGTONE, Uri.parse(oldUri));

					wasSpecialRingTone = false;
				}

				if  (InRingingSession)
				{
					InRingingSession = false;
				}
            break;

		}

	}



    private void displaySpecialCallActivity(FileManager.FileType fType, String mediaFilePath) {

        if (fType == FileManager.FileType.VIDEO) {

            // On call you replace the ringtone with a silent URI , defined as null
            RingtoneManager.setActualDefaultRingtoneUri(gcontext,RingtoneManager.TYPE_RINGTONE,null);

        }

        specialCallIntent.setClass(gcontext, IncomingSpecialCall.class);
        specialCallIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        specialCallIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        specialCallIntent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        specialCallIntent.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
        specialCallIntent.addFlags(Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT);
        specialCallIntent.addFlags(Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
     //  specialCallIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);



        specialCallIntent.putExtra(IncomingSpecialCall.SPECIAL_CALL_FILEPATH, mediaFilePath);

        Log.i(TAG, "START ACTIVITY before For");
        gcontext.startActivity(specialCallIntent);

        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }


        int count;
        int numFailures = 0;
        for(count=0;count<10;++count)
        {
            count++;
            mActivityManager = (ActivityManager) gcontext.getSystemService(Context.ACTIVITY_SERVICE);
            List<ActivityManager.RunningTaskInfo> tasks = mActivityManager.getRunningTasks(1);
            String topActivityName = tasks.get(0).topActivity.getClassName();

            Log.i(TAG, "Into the For isInFront: " + isInFront);
           // if (!topActivityName.equals(IncomingSpecialCall.class.getName())) {// Try to show on top TOP_ACTIVITY_RETRIES times user dismiss this activity
            if(!isInFront)
            {   numFailures++;

                Log.i(TAG, "START ACTIVITY");
                gcontext.startActivity(specialCallIntent);
               // isInFront = true;

                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

            }

        }
        Log.i(TAG, "Exited after For ");

        if(numFailures==count)
        {
            Log.e(TAG, "Failed to set IncomingSpecialCall activity to top after:"+TOP_ACTIVITY_RETRIES+" retries");
        }

    }

    /**
     * Saves previous ringtone URI to restore later
     */
    private void saveOldRingToneUri() {

        Uri oldOri = RingtoneManager.getActualDefaultRingtoneUri(gcontext, RingtoneManager.TYPE_RINGTONE);
        if(oldOri!=null)
            SharedPrefUtils.setString(gcontext, SharedPrefUtils.GENERAL, SharedPrefUtils.OLD_RINGTONE_URI, oldOri.toString());
    }

    private void setNewRingTone(String fFullName, String source, String extension) {

        Log.i(TAG,"In: setNewRingTone");
        File newSoundFile = new File(Constants.specialCallPath + source
                + "/", fFullName);

        if (newSoundFile.exists()) {
            // Setting up new ringtone in user's ringtones list
            ContentValues values = new ContentValues();
            values.put(MediaStore.MediaColumns.DATA,
                    newSoundFile.getAbsolutePath());
            values.put(MediaStore.MediaColumns.TITLE, source);
            values.put(MediaStore.MediaColumns.MIME_TYPE, "audio/"
                    + extension);
            values.put(MediaStore.Audio.Media.ARTIST, "SpecialCallUI");
            values.put(MediaStore.MediaColumns.SIZE, 215454); // ///
            // what
            // to do
            // here
            // !!!!!!!!!!!!
            // ->
            // WTF
            // Rony?
            // LOL
            values.put(MediaStore.Audio.Media.IS_RINGTONE, true);

            Uri uri = MediaStore.Audio.Media
                    .getContentUriForPath(newSoundFile
                            .getAbsolutePath());
            getContentResolver().delete(
                    uri,
                    MediaStore.MediaColumns.DATA + "=\""
                            + newSoundFile.getAbsolutePath() + "\"",
                    null);
            Uri newUri = getContentResolver().insert(uri, values);

            // Backing up uri, extension and filepath in shared prefs
            SharedPrefUtils.setString(getApplicationContext(),
                    SharedPrefUtils.RINGTONE_URI, source,
                    newUri.toString());
            SharedPrefUtils.setString(getApplicationContext(),
                    SharedPrefUtils.RINGTONE_EXTENSION, source,
                    extension);
            SharedPrefUtils.setString(getApplicationContext(),
                    SharedPrefUtils.RINGTONE_FILEPATH, source,
                    newSoundFile.getAbsolutePath());

        }
        else
            Log.e(TAG,"File not found:" + newSoundFile.getAbsolutePath());
    }

    private void startRingToneSpecialCall() {

        String ringToneFileExtension = SharedPrefUtils.getString(gcontext, SharedPrefUtils.RINGTONE_EXTENSION, incomingCallNumber);
        String ringToneFileName = incomingCallNumber+"."+ringToneFileExtension;

        File ringtoneFile = new File(Constants.specialCallPath+incomingCallNumber+"/" ,ringToneFileName);
        if(ringtoneFile.exists())
        {
            // Saving previous ringtone uri
            Uri oldOri = RingtoneManager.getActualDefaultRingtoneUri(gcontext, RingtoneManager.TYPE_RINGTONE);
            SharedPrefUtils.setString(gcontext, SharedPrefUtils.GENERAL, SharedPrefUtils.OLD_RINGTONE_URI, oldOri.toString());

            // The new special ringtone uri
            String newUri = SharedPrefUtils.getString(gcontext, SharedPrefUtils.RINGTONE_URI, incomingCallNumber);

            // On call ringtone uri is replaced with the special uri
            RingtoneManager.setActualDefaultRingtoneUri(
                    gcontext,//MainActivity.this,
                    RingtoneManager.TYPE_RINGTONE,
                    Uri.parse(newUri));

            wasSpecialRingTone = true;
        }
    }

    private void startMediaSpecialCall() {

        String mediaFileExtension = SharedPrefUtils.getString(gcontext, SharedPrefUtils.MEDIA_EXTENSION, incomingCallNumber);
        String mediaFileName = incomingCallNumber+"."+mediaFileExtension;
        String mediaFilePath = Constants.specialCallPath + incomingCallNumber + "/" + mediaFileName;
        try {
            FileManager fm = new FileManager(mediaFilePath);
            displaySpecialCallActivity(fm.getFileType(), fm.getFileFullPath());

        } catch (FileInvalidFormatException e) {
            e.printStackTrace();
        } catch (FileExceedsMaxSizeException e) {
            e.printStackTrace();
        } catch (FileDoesNotExistException e) {
            e.printStackTrace();
        } catch (FileMissingExtensionException e) {
            e.printStackTrace();
        }
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
