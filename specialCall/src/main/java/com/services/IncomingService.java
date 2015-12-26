package com.services;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Point;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.IBinder;
import android.provider.Contacts;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.MediaController;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;
import com.special.app.R;
import com.utils.SharedPrefUtils;
import java.io.File;
import java.io.IOException;
import Exceptions.FileDoesNotExistException;
import Exceptions.FileExceedsMaxSizeException;
import Exceptions.FileInvalidFormatException;
import Exceptions.FileMissingExtensionException;
import FilesManager.FileManager;
import wei.mark.standout.StandOutWindow;
import wei.mark.standout.ui.Window;


public class IncomingService extends StandOutWindow {

    public static boolean wasSpecialRingTone = false;
    public static boolean isInFront = false;

    public static int ringVolume;
    public static int oldMediaVolume;


    public static AudioManager audioManager;
    private static CallStateListener phoneListener;
    private String incomingCallNumber;
    private static volatile boolean InRingingSession = false;
    private static final String TAG = IncomingService.class.getSimpleName();
    private MediaPlayer mMediaPlayer;
    private int mWidth;
    private int mHeight;
    private MediaController mediaController;
    private static int id = 0;
    private View specialCallView;
    private TextView specialCallTextView;
    private ImageView specialCallCloseBtn;
    private RelativeLayout relativeLayout;
    private Bitmap spCallBitmap;
    private boolean volumeChangeByService = false;
    private boolean windowCloseActionWasMade = true;
    private boolean alreadyMuted = false;
    public static final String ACTION_STOP_RING = "com.services.IncomingService.ACTION_STOP_RING";
    public static final String ACTION_START = "com.services.IncomingService.ACTION_START";
    private int mone=0;
    private MediaPlayer.OnPreparedListener PreparedListener = new MediaPlayer.OnPreparedListener(){

        @Override
        public void onPrepared(MediaPlayer m) {


            m.setLooping(true);
            m.setVolume(1.0f, 1.0f);
            m.start();
            Log.i(TAG, " Video registerVolumeReceiverMethod");
            registerVolumeReceiverMethod();
        }
    };

    /* Service operations methods */

    private final BroadcastReceiver VolumeButtonreceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            int volumeDuringRun = (Integer)intent.getExtras().get("android.media.EXTRA_VOLUME_STREAM_VALUE");

            Log.e(TAG, "BroadCastFlags: alreadyMuted: "+ alreadyMuted+ " InRingingSession: " + InRingingSession + " volumeChangeByService: "+ volumeChangeByService);
            if (!alreadyMuted && InRingingSession /*&& !volumeChangeByService*/)
            {
                try {
                    audioManager.setStreamMute(AudioManager.STREAM_MUSIC, true);
                    alreadyMuted =true;
                    Log.e(TAG, "MUTE STREAM_MUSIC ");
                } catch(Exception e) {
                e.printStackTrace();
                }



            }

            if(volumeChangeByService)
                volumeChangeByService=false;

            Log.e(TAG, "Exited BroadCast oldMediaVolume: "+ oldMediaVolume+ " volumeDuringRun: " + volumeDuringRun);

        }
    };


    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(TAG, "Service onCreate");

        Display display = mWindowManager.getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        mWidth = size.x;
        mHeight = size.y*63/100;

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
        super.onStartCommand(intent, flags, startId);
        audioManager = (AudioManager) getApplicationContext().getSystemService(Context.AUDIO_SERVICE);
        Log.i(TAG, "onStartCommand START_STICKY");


        final Intent intentForThread = intent;

        new Thread() {

            @Override
            public void run() {
                if (intentForThread != null) {
                    String action = intentForThread.getAction();
                    Log.i(TAG, "Action:" + action);
                    switch (action) {

                        case ACTION_STOP_RING: {
                            stopSound();
                        }
                        break;
                        case ACTION_START: {
                        }break;
                    }
                }
            }

        }.start();


        return START_STICKY;

    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        Log.e(TAG, "Service onDestroy");

    }

    @Override
    public String getAppName() {
        return "SPECIAL CALL";
    }

    @Override
    public int getAppIcon() {
        return R.drawable.ic_launcher;
    }

    @Override
    public void createAndAttachView(int id, FrameLayout frame) {

        Log.i(TAG, "In createAndAttachView()");

        frame.addView(relativeLayout);

        frame.setBackgroundColor(Color.BLACK);
        windowCloseActionWasMade = false;
    }

    @Override
    public StandOutLayoutParams getParams(int id, Window window) {

        Log.i(TAG, "In StandOutLayoutParams()");
        return new StandOutLayoutParams(id, mWidth, mHeight, 0, 0);

    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
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

                        // Retrieving the ringtone volume
                        ringVolume = audioManager.getStreamVolume(AudioManager.STREAM_RING);
                        Log.e(TAG, "ringVolume Original"+ ringVolume);
                        incomingCallNumber = incomingNumber;

                        try
                        {

                            // Backing up the music
                            oldMediaVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);


                            // Setting music  to equal the ringtone volume
                            if (ringVolume==0) {
                                volumeChangeByService= true;
                                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, 0, 0); // ring volume max is 7(also System & Alarm max volume) , Music volume max is 15 (so we want to use full potential of the volume of the music stream)
                                Log.e(TAG, "STREAM_MUSIC Change : 0");
                            }else {
                                volumeChangeByService= true;
                                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, ringVolume * 2 + 1, 0); // ring volume max is 7(also System & Alarm max volume) , Music volume max is 15 (so we want to use full potential of the volume of the music stream)
                                Log.e(TAG, "STREAM_MUSIC Change : " + String.valueOf(ringVolume * 2 + 1));
                            }


                        }
                        catch(Exception e)
                        {
                            e.printStackTrace();
                            Log.e(TAG, "Failed to set stream volume:"+e.getMessage());
                        }

                        String ringtonePath = SharedPrefUtils.getString(getApplicationContext(), SharedPrefUtils.RINGTONE_FILEPATH, incomingCallNumber);
                        File ringtoneFile = new File(ringtonePath);

                        //Check if Mute Was Needed if not return to UnMute.
                        if (ringtoneFile.exists())
                        {

                            audioManager.setStreamMute(AudioManager.STREAM_RING, true);
                            Log.e(TAG, "MUTE STREAM_RING ");
                            Log.i(TAG, "YesRingtone YesMute !!!");


                            Runnable r = new Runnable() {
                                public void run() {
                                    Log.i(TAG, "startRingtoneSpecialCall Thread");
                                    try {
                                        startRingtoneSpecialCall();

                                    } catch(Exception e) {  e.printStackTrace();  }

                                }
                            };

                            new Thread(r).start();


                        }
                        else
                        {

                            try {
                                audioManager.setStreamMute(AudioManager.STREAM_RING, false);
                                Log.e(TAG, "UNMUTE STREAM_RING ");
                                Log.i(TAG, "NoRingtone NoMute");
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }

                        startMediaSpecialCall();

                        InRingingSession = true;
                    }
                    catch(Exception e)
                    {
                        e.printStackTrace();
                        Log.e(TAG, "CALL_STATE_RINGING failed:"+e.getMessage());
                    }

                }
                break;

            case TelephonyManager.CALL_STATE_IDLE:
            case TelephonyManager.CALL_STATE_OFFHOOK:
                Log.i(TAG, "TelephonyManager.CALL_STATE_IDLE");
                if (wasSpecialRingTone)
                {

                    wasSpecialRingTone = false;
                }
                closeSpecialCallWindowAndRingtone();


                mone=0;

                break;

        }

    }

    private void prepareViewForSpecialCall(FileManager.FileType fType, String mediaFilePath) {

        Log.i(TAG, "Preparing specialcall view");

        // Creating a new RelativeLayout
        relativeLayout = new RelativeLayout(this);

        // Defining the RelativeLayout layout parameters.
        // In this case I want to fill its parent
        RelativeLayout.LayoutParams rlp = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT,
                RelativeLayout.LayoutParams.MATCH_PARENT);

        relativeLayout.setLayoutParams(rlp);
        relativeLayout.setBackgroundColor(Color.BLACK);
        // Defining the layout parameters of the TextView
        RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.WRAP_CONTENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT);
        lp.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);

        //TextView for Showing Incoming Call Number and contact name
        specialCallTextView = new TextView(this);

        String contactName = getContactName(incomingCallNumber);
        specialCallTextView.setText(!contactName.equals("") ? contactName + " " + incomingCallNumber : incomingCallNumber);
        specialCallTextView.setBackgroundColor(Color.BLACK);
        specialCallTextView.setTextColor(Color.WHITE);
        specialCallTextView.setGravity(Gravity.BOTTOM | Gravity.LEFT);

        specialCallTextView.setLayoutParams(lp);



        //ImageView for Closing Special Incoming Call
        specialCallCloseBtn = new ImageView(this);
        RelativeLayout.LayoutParams lp1 = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.WRAP_CONTENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT);
        lp1.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
        lp1.addRule(RelativeLayout.ALIGN_BOTTOM);
        lp1.addRule(RelativeLayout.ALIGN_RIGHT);
        lp1.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
        specialCallCloseBtn.setImageResource(R.drawable.abc_ic_clear);
        specialCallCloseBtn.setBackgroundColor(Color.BLACK);
        specialCallCloseBtn.setLayoutParams(lp1);
        specialCallCloseBtn.setClickable(true);
        specialCallCloseBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

                closeSpecialCallWindowWithoutRingtone();

            }
        });


        System.gc();
        // Drawing image during call
        if (fType == FileManager.FileType.IMAGE) {

            try {

                Log.i(TAG, "In IMAGE");

                specialCallView = new ImageView(this);
                specialCallView.setLayoutParams(new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.MATCH_PARENT));
                //     ((ImageView)specialCallView).setScaleType(ImageView.ScaleType.FIT_XY); STRECTH IMAGE ON FULL SCREEN <<< NOT SURE IT's GOOD !!!!!
                ((ImageView)specialCallView).setScaleType(ImageView.ScaleType.FIT_CENTER); // <<  just place the image Center of Window and fit it with ratio
                final BitmapFactory.Options options = new BitmapFactory.Options();
                options.inJustDecodeBounds = true;
                BitmapFactory.decodeFile(mediaFilePath, options);
                options.inSampleSize = calculateInSampleSize(options, mWidth, mHeight);

                options.inJustDecodeBounds = false;
                spCallBitmap = BitmapFactory.decodeFile(mediaFilePath, options);

                if (spCallBitmap != null)
                    ((ImageView)specialCallView).setImageBitmap(spCallBitmap);
                else {
                    spCallBitmap = BitmapFactory.decodeFile(mediaFilePath);
                    ((ImageView)specialCallView).setImageBitmap(spCallBitmap);
                }

            }   catch (NullPointerException | OutOfMemoryError e) {
                Log.e(TAG, "Failed decoding image", e);
            }


            relativeLayout.addView(specialCallView);

        }
        if (fType == FileManager.FileType.VIDEO)
        {


            try {
                audioManager.setStreamMute(AudioManager.STREAM_RING, true);
                Log.e(TAG, "MUTE STREAM_RING ");
                Log.i(TAG, "VIDEO file detected MUTE Ring");

            } catch(Exception e) {  e.printStackTrace();  }

            Log.i(TAG, "In VIDEO");


            // Special ringtone in video case is silent
            IncomingService.wasSpecialRingTone = true;

            // VideoView on Relative Layout
            final File root = new File(mediaFilePath);

            Uri uri = Uri.fromFile(root);

            specialCallView = new VideoView(this);
            specialCallView.setLayoutParams(new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT,
                    RelativeLayout.LayoutParams.MATCH_PARENT));

            RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) specialCallView.getLayoutParams();

            params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
            params.addRule(RelativeLayout.ALIGN_PARENT_TOP);
            params.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
            params.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);

            specialCallView.setLayoutParams(params);
//            mediaController = new MediaController(this);
//            mediaController.setAnchorView(specialCallView);
//            mediaController.setMediaPlayer(((VideoView)specialCallView));
//            mediaController.setBackgroundColor(Color.WHITE);
//            ((VideoView)specialCallView).setMediaController(mediaController);
            ((VideoView)specialCallView).setOnPreparedListener(PreparedListener);
            ((VideoView)specialCallView).setVideoURI(uri);
            ((VideoView)specialCallView).requestFocus();

            relativeLayout.addView(specialCallView);
            //          relativeLayout.addView(mediaController);



        }
        relativeLayout.addView(specialCallTextView);
        relativeLayout.addView(specialCallCloseBtn);

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

    private void closeSpecialCallWindowWithoutRingtone(){

        if  (InRingingSession) {

            Runnable r = new Runnable() {
                public void run() {
                    Log.i(TAG, "mMediaPlayer.stop(); closeSpecialCallWindowWithoutRingtone");
                    try {
                        if(mMediaPlayer!=null)   //TODO Check in advance the file type and act accordingly
                            mMediaPlayer.stop();

                    } catch(Exception e) {  e.printStackTrace();  }

                }
            };

            new Thread(r).start();

            if (!windowCloseActionWasMade) {
                Intent i = new Intent(getApplicationContext(), IncomingService.class);
                i.setAction(StandOutWindow.ACTION_CLOSE);
                startService(i);
                windowCloseActionWasMade=true;
            }
        }



    }

    private void closeSpecialCallWindowAndRingtone(){

        if  (InRingingSession) {

            Runnable r = new Runnable() {
                public void run() {
                    InRingingSession = false;
                    try{     unregisterReceiver(VolumeButtonreceiver);}
                    catch(Exception exception)
                    {  Log.e(TAG,"UnregisterReceiver failed in IDLE");}

                    volumeChangeByService = false;
                    alreadyMuted = false;
                    Log.i(TAG, "mMediaPlayer.stop(); closeSpecialCallWindowAndRingtone");
                    try {
                        if(mMediaPlayer!=null)   //TODO Check in advance the file type and act accordingly
                            mMediaPlayer.stop();
                    } catch(Exception e) {  e.printStackTrace();  }



                        try {
                            Thread.sleep(1000, 0);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        try {
                        audioManager.setStreamMute(AudioManager.STREAM_MUSIC, false);
                            Log.e(TAG, "UNMUTE STREAM_MUSIC ");
                        } catch(Exception e) {  e.printStackTrace();  }

                        Log.e(TAG, " !!!!!!!!!! UNMUTED !!!!!!!!!!  "+" oldMediaVolume: "+ oldMediaVolume+ " OldringVolume: " + ringVolume);
                                try {
                        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, oldMediaVolume, 0);
                                    Log.e(TAG, "STREAM_MUSIC Change : " + String.valueOf(oldMediaVolume));

                                } catch(Exception e) {  e.printStackTrace();  }


                    try {
                        audioManager.setStreamMute(AudioManager.STREAM_RING, false);
                        Log.e(TAG, "UNMUTE STREAM_RING ");
                    } catch(Exception e) {  e.printStackTrace();  }
                /*
                    try {
                        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, ringVolume, 0);
                        Log.e(TAG, "STREAM_MUSIC Change : " + String.valueOf(ringVolume));
                    } catch(Exception e) {  e.printStackTrace();  }*/




                }
            };

            new Thread(r).start();

            if (!windowCloseActionWasMade) {
                Intent i = new Intent(getApplicationContext(), IncomingService.class);
                i.setAction(StandOutWindow.ACTION_CLOSE);
                startService(i);
                windowCloseActionWasMade=true;
            }





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
            if (audioManager.getStreamVolume(AudioManager.STREAM_MUSIC) != 0) {
                mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
                mMediaPlayer.prepare();
                mMediaPlayer.setLooping(true);
                mMediaPlayer.start();

                Log.i(TAG, " Ringtone registerVolumeReceiverMethod");
                registerVolumeReceiverMethod();

            }
        } catch (IOException e) {
            e.printStackTrace();
            Log.e(TAG, "Failed to play sound. Exception:" + e.getMessage());
        }
    }

    private void registerVolumeReceiverMethod() {


        Runnable r = new Runnable() {
            public void run() {

                try {
                    Thread.sleep(1500,0);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                IntentFilter filter = new IntentFilter();
                filter.addAction("android.media.VOLUME_CHANGED_ACTION");


                registerReceiver(VolumeButtonreceiver, filter);
                Log.e(TAG, "registerReceiver VolumeButtonReceiver Finished register");

            }
        };

        new Thread(r).start();



    }

    private void startMediaSpecialCall() {


        String mediaFilePath = SharedPrefUtils.getString(getApplicationContext(), SharedPrefUtils.MEDIA_FILEPATH, incomingCallNumber);

        Log.i(TAG, "startMediaSpecialCall SharedPrefUtils mediaFilePath:" + mediaFilePath);

        if(!mediaFilePath.equals("")) {
            try {
                FileManager fm = new FileManager(mediaFilePath);
                prepareViewForSpecialCall(fm.getFileType(), fm.getFileFullPath());
                Intent i = new Intent(this, IncomingService.class);
                i.setAction(StandOutWindow.ACTION_SHOW);
                startService(i);


            } catch (FileInvalidFormatException |
                    FileExceedsMaxSizeException |
                    FileDoesNotExistException   |
                    FileMissingExtensionException e) {
                e.printStackTrace();
            }
        }
    }

    public int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        // Raw height and width of image
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {

            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than the requested height and width.
            while ((halfHeight / inSampleSize) > reqHeight
                    && (halfWidth / inSampleSize) > reqWidth) {
                inSampleSize *= 2;
            }
        }

        return inSampleSize;
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

    public String getContactName(final String phoneNumber)
    {
        Uri uri;
        String[] projection;
        Uri mBaseUri = Contacts.Phones.CONTENT_FILTER_URL;
        projection = new String[] { android.provider.Contacts.People.NAME };
        try {
            Class<?> c =Class.forName("android.provider.ContactsContract$PhoneLookup");
            mBaseUri = (Uri) c.getField("CONTENT_FILTER_URI").get(mBaseUri);
            projection = new String[] { "display_name" };
        }
        catch (Exception e) {
        }


        uri = Uri.withAppendedPath(mBaseUri, Uri.encode(phoneNumber));
        Cursor cursor = this.getContentResolver().query(uri, projection, null, null, null);

        String contactName = "";

        if (cursor.moveToFirst())
        {
            contactName = cursor.getString(0);
        }

        cursor.close();
        cursor = null;

        return contactName;
    }



}
