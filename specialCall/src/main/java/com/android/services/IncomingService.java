package com.android.services;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Binder;
import android.os.IBinder;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import com.special.specialcall.IncomingSpecialCall;

import java.io.File;
import java.io.IOException;

import Exceptions.FileDoesNotExistException;
import Exceptions.FileExceedsMaxSizeException;
import Exceptions.FileInvalidFormatException;
import Exceptions.FileMissingExtensionException;
import FilesManager.FileManager;
import data_objects.Constants;
import data_objects.SharedPrefUtils;


public class IncomingService extends Service {

    public static boolean wasSpecialRingTone = false;
    public static boolean isInFront = false;
    public static boolean videoStreamOn = false;
    public static int ringVolume;
    public static int oldMediaVolume;
    public static int oldAlarmVolume;

    public static AudioManager audioManager;
    private static CallStateListener phoneListener;
    private String incomingCallNumber;
    private static volatile boolean InRingingSession = false;
    private Intent specialCallIntent = new Intent();
    private final int TOP_ACTIVITY_RETRIES = 5;
    private static final String TAG = IncomingService.class.getSimpleName();
    private MediaPlayer mMediaPlayer;
    private final IBinder mBinder = new MyBinder();

    /* Service operations methods */

    @Override
    public void onCreate() {
        Log.i(TAG, "Service onCreate");
        callInfoToast("IncomingSpecialCall Service created", Color.CYAN);

        try
        {

            if (phoneListener==null){
                phoneListener = new CallStateListener();
                TelephonyManager tm = (TelephonyManager) getApplicationContext().getSystemService(Context.TELEPHONY_SERVICE);
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

        audioManager = (AudioManager) getApplicationContext().getSystemService(Context.AUDIO_SERVICE);
        Log.i(TAG, "onStartCommand START_STICKY");
        return START_STICKY;

    }

    @Override
    public void onDestroy() {
        Log.e(TAG, "Service onDestroy");

    }

    @Override
    public IBinder onBind(Intent arg0) {
        return mBinder;
    }


    public class MyBinder extends Binder {
        public IncomingService getService() {
            return IncomingService.this;
        }
    }

    /* Assisting methods and listeners */

    public void stopSound() {

        Log.i(TAG, "Stop ringtone sound");

        try
        {

            mMediaPlayer.stop();
        } catch(Exception e) {
            e.printStackTrace();
            Log.e(TAG, "Failed to Stop sound. Exception:" + e.getMessage());
        }
    }






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

    private synchronized void syncOnCallStateChange(int state, String incomingNumber){


        switch(state)
        {

            case TelephonyManager.CALL_STATE_RINGING:
                if (!InRingingSession)
                {
                    try
                    {

                        incomingCallNumber = incomingNumber;
                        String ringtonePath = SharedPrefUtils.getString(getApplicationContext(), SharedPrefUtils.RINGTONE_FILEPATH, incomingCallNumber);
                        File ringtoneFile = new File(ringtonePath);
                        Log.i(TAG, "InRingingSession SharedPrefUtils ringtonePath:" + ringtonePath);
                        try
                        {
                            // Retrieving the ringtone volume
                            ringVolume = audioManager.getStreamVolume(AudioManager.STREAM_RING);

                            // Backing up the music and alarm volume
                            oldMediaVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
                            oldAlarmVolume = audioManager.getStreamVolume(AudioManager.STREAM_ALARM);

                            // Setting music and alarm volume to equal the ringtone volume
                            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, ringVolume, 0);
                            audioManager.setStreamVolume(AudioManager.STREAM_ALARM, ringVolume, 0);
                        }
                        catch(Exception e)
                        {
                            e.printStackTrace();
                            Log.e(TAG, "Failed to set stream volume:"+e.getMessage());
                        }

                        if (ringtoneFile.exists())
                        {
                            Log.i(TAG, "AudioManager.STREAM_RING, true");
                            audioManager.setStreamMute(AudioManager.STREAM_RING, true);

                        }
                        else
                        {
                            Log.i(TAG, "AudioManager.STREAM_RING, false");
                            try {
                                audioManager.setStreamMute(AudioManager.STREAM_RING, false);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }

                        InRingingSession = true;

                        startRingtoneSpecialCall();
                        startMediaSpecialCall();
                    }
                    catch(Exception e)
                    {
                        e.printStackTrace();
                        Log.e(TAG, "CALL_STATE_RINGING failed:"+e.getMessage());
                    }

                }
                break;

            case TelephonyManager.CALL_STATE_IDLE:
                Log.i(TAG, "TelephonyManager.CALL_STATE_IDLE");
                if (wasSpecialRingTone)
                {

                    wasSpecialRingTone = false;
                }

                if  (InRingingSession) {
                    Log.i(TAG, "AudioManager.STREAM_RING, false");
                    try {
                        if(mMediaPlayer!=null)   //TODO Check in advance the file type and act accordingly
                            mMediaPlayer.stop();
                    } catch(Exception e) {  e.printStackTrace();  }

                    try {
                        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, oldMediaVolume, 0);
                        audioManager.setStreamVolume(AudioManager.STREAM_ALARM, oldAlarmVolume, 0);
                    } catch(Exception e) {  e.printStackTrace();  }

                    try {
                        audioManager.setStreamMute(AudioManager.STREAM_RING, false);
                    } catch(Exception e) {  e.printStackTrace();  }
                    InRingingSession = false;
                }
                break;
            case TelephonyManager.CALL_STATE_OFFHOOK:
                Log.i(TAG, "TelephonyManager.CALL_STATE_OFFHOOK");
                if (wasSpecialRingTone)
                {

                    wasSpecialRingTone = false;
                }

                if  (InRingingSession) {
                    Log.i(TAG, "AudioManager.STREAM_RING, false");
                    try {
                        if(mMediaPlayer!=null)
                            mMediaPlayer.stop();
                    } catch(Exception e) {  e.printStackTrace();  }

                    try {
                        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, oldMediaVolume, 0);
                        audioManager.setStreamVolume(AudioManager.STREAM_ALARM, oldAlarmVolume, 0);
                    } catch(Exception e) {  e.printStackTrace();  }

                    try {
                        audioManager.setStreamMute(AudioManager.STREAM_RING, false);
                    } catch(Exception e) {  e.printStackTrace();  }
                    InRingingSession = false;
                }
                break;
        }

    }

    private void displaySpecialCallActivity(FileManager.FileType fType, String mediaFilePath) {


        if (fType == FileManager.FileType.VIDEO) {

            Log.i(TAG, "AudioManager.STREAM_RING, true");
            try {
                audioManager.setStreamMute(AudioManager.STREAM_RING, true);
            } catch(Exception e) {  e.printStackTrace();  }
            videoStreamOn = true;
        }

        specialCallIntent.setClass(getApplicationContext(), IncomingSpecialCall.class);
        /*specialCallIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);*/
        specialCallIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        specialCallIntent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        specialCallIntent.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
        specialCallIntent.addFlags(Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT);
        specialCallIntent.addFlags(Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
        //  specialCallIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);

        specialCallIntent.putExtra(IncomingSpecialCall.SPECIAL_CALL_FILEPATH, mediaFilePath);
        specialCallIntent.putExtra("Ringervolume", ringVolume);

        Log.i(TAG, "START ACTIVITY before For");
        getApplicationContext().startActivity(specialCallIntent);

        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }


        int count;
        int numFailures = 0;
        for(count=0;count<20;++count)
        {
            count++;
            Log.i(TAG, "Into the For isInFront: " + isInFront);
            if(!isInFront)
            {   numFailures++;

                Log.i(TAG, "START ACTIVITY");

                Intent i=new Intent(getApplicationContext(),IncomingSpecialCall.class);
                specialCallIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                specialCallIntent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                getApplicationContext().startActivity(specialCallIntent);

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

    private void startRingtoneSpecialCall() {

        String ringtonePath = SharedPrefUtils.getString(getApplicationContext(), SharedPrefUtils.RINGTONE_FILEPATH, incomingCallNumber);

        if (!ringtonePath.equals("")) try {
            FileManager ringtoneFile = new FileManager(ringtonePath);

            // The new special ringtone uri
            Log.i(TAG, "ringotne before playsound ringtonePath: " + ringtonePath + " URI: " + Uri.parse(ringtonePath).toString());
            playSound(getApplicationContext(), Uri.parse(ringtonePath));

        } catch (FileMissingExtensionException |
                FileDoesNotExistException      |
                FileExceedsMaxSizeException    |
                FileInvalidFormatException e) {
            e.printStackTrace();
        }
    }

    private void playSound(Context context, Uri alert) {

        Log.i(TAG, "Playing ringtone sound");
        mMediaPlayer = new MediaPlayer();
        try
        {
            try {
                mMediaPlayer.setDataSource(context, alert);
            } catch (IOException e) {
                e.printStackTrace();
            }
            final AudioManager audioManager = (AudioManager) context
                    .getSystemService(Context.AUDIO_SERVICE);
            if (audioManager.getStreamVolume(AudioManager.STREAM_ALARM) != 0) {
                mMediaPlayer.setAudioStreamType(AudioManager.STREAM_ALARM);
                mMediaPlayer.prepare();
                mMediaPlayer.setLooping(true);
                mMediaPlayer.start();
            }
        } catch (IOException e) {
            e.printStackTrace();
            Log.e(TAG, "Failed to play sound. Exception:" + e.getMessage());
        }
    }

    private void startMediaSpecialCall() {

        String mediaFilePath = SharedPrefUtils.getString(getApplicationContext(), SharedPrefUtils.MEDIA_FILEPATH, incomingCallNumber);

        Log.i(TAG, "startMediaSpecialCall SharedPrefUtils mediaFilePath:" + mediaFilePath);

        if(!mediaFilePath.equals("")) {
            try {
                FileManager fm = new FileManager(mediaFilePath);
                displaySpecialCallActivity(fm.getFileType(), fm.getFileFullPath());

            } catch (FileInvalidFormatException |
                    FileExceedsMaxSizeException |
                    FileDoesNotExistException   |
                    FileMissingExtensionException e) {
                e.printStackTrace();
            }
        }
    }



    /* UI methods */

    private void callInfoToast(final String text, final int g) {

        Toast toast = Toast.makeText(getApplicationContext(), text,
                Toast.LENGTH_LONG);
        TextView v = (TextView) toast.getView().findViewById(
                android.R.id.message);
        v.setTextColor(g);
        toast.show();
    }





}
