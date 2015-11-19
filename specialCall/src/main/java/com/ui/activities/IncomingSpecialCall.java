package com.ui.activities;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.AttributeSet;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.MediaController;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.VideoView;

import com.interfaces.ITelephony;
import com.services.IncomingService;
import com.special.app.R;
import com.utils.BroadcastUtils;
import com.utils.SharedPrefUtils;

import java.io.File;

import EventObjects.EventReport;
import EventObjects.EventType;
import FilesManager.FileManager;


public class IncomingSpecialCall extends Activity implements OnClickListener {

    private ITelephony telephonyService;
    private TelephonyManager tm;
    public static final String TAG = "IncomingSpecialCall";
    public static final String SPECIAL_CALL_FILEPATH = "SpecialCallFilePath";
    public static final String SPECIAL_CALL_CALLER = "SpecialCallCaller";
    public static final String SPECIAL_CALL_PREVIEW = "SpecialCallPreview";

    private String callerNumber ;
    private boolean isPreview = false;
    private boolean videoMedia = false;
    private boolean isCallFinished = false;
    private AudioManager audioManager = null ;
    private MediaController mediaController;
    private ImageView myImageView;
    private String mediaFilePath;
    private Bitmap spCallBitmap;

    // WindowManager.LayoutParams windowParams;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.i(TAG, "Entering " + TAG);

        try {

            Intent intent = getIntent();
            mediaFilePath = intent.getStringExtra(SPECIAL_CALL_FILEPATH);
            callerNumber = intent.getStringExtra(SPECIAL_CALL_CALLER);
            isPreview = intent.getBooleanExtra(SPECIAL_CALL_PREVIEW, false);

            if (!isPreview) {
                CallStateListener stateListener = new CallStateListener();
                tm = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);
                tm.listen(stateListener, PhoneStateListener.LISTEN_CALL_STATE);
            }


            Log.i(TAG, "Preparing to display:" + mediaFilePath);

            FileManager.FileType fileType = FileManager.getFileType(new File(mediaFilePath));



            this.getWindow().setFlags(
                    WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON |
                    WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD |
                    WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON |
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                    WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY,
                    WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON |
                    WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD |
                    WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON  |
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                    WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY
            );



            WindowManager.LayoutParams lp = this.getWindow().getAttributes();
            lp.dimAmount=0.0f;
            this.getWindow().setAttributes(lp);

            // Drawing image during call
            if (fileType == FileManager.FileType.IMAGE) {

                try {
                    // this.getWindow().addFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
                    Log.i(TAG, "In IMAGE");
                    setContentView(R.layout.activity_incoming_special_call);

                    myImageView = (ImageView) findViewById(R.id.CallerImage);
                    int width = myImageView.getMinimumWidth();
                    int height = myImageView.getMinimumHeight();

                    final BitmapFactory.Options options = new BitmapFactory.Options();
                    options.inJustDecodeBounds = true;
                    BitmapFactory.decodeFile(mediaFilePath, options);
                    options.inSampleSize = calculateInSampleSize(options, width, height);

                    options.inJustDecodeBounds = false;
                    if(SharedPrefUtils.getBoolean(getApplicationContext(), SharedPrefUtils.GENERAL, SharedPrefUtils.WAS_SPIMAGE_DECODED)) {
                        spCallBitmap = BitmapFactory.decodeFile(mediaFilePath, options);
                        SharedPrefUtils.setBoolean(getApplicationContext(), SharedPrefUtils.GENERAL, SharedPrefUtils.WAS_SPIMAGE_DECODED, true);
                    }

                        if (spCallBitmap != null)
                            myImageView.setImageBitmap(spCallBitmap);
                        else {
                            spCallBitmap = BitmapFactory.decodeFile(mediaFilePath);
                            myImageView.setImageBitmap(spCallBitmap);
                        }


                }   catch (NullPointerException | OutOfMemoryError e) {
                    Log.e(TAG, "Failed decoding image", e);
                }

                // Drawing incoming number during call
                TextView myTextView = (TextView)findViewById(R.id.IncomingCallNumber);
                myTextView.setText(callerNumber);

                videoMedia = false;

            }
            if (fileType == FileManager.FileType.VIDEO)
            {

                Log.i(TAG, "In VIDEO");
                videoMedia = true;
                // Special ringtone in video case is silent
                IncomingService.wasSpecialRingTone = true;

                setContentView(R.layout.activity_incoming_special_call_video);


                // VideoView on Relative Layout
                final File root = new File(mediaFilePath);

                Uri uri = Uri.fromFile(root);
                VideoView mVideoView  = (VideoView)findViewById(R.id.CallerVideo);


                mediaController = new MediaController(this);
                mediaController.setAnchorView(mVideoView);
                mediaController.setMediaPlayer(mVideoView);
                mediaController.setBackgroundColor(Color.WHITE);


                mVideoView.setMediaController(mediaController);
                mVideoView.setOnPreparedListener(PreparedListener);
                mVideoView.setVideoURI(uri);
                mVideoView.setBackgroundColor(Color.TRANSPARENT);
                mVideoView.requestFocus();


                // TextView for Showing Incoming Call Number and contact name
                TextView myTextView = (TextView)findViewById(R.id.IncomingCallNumberVideo);
                myTextView.setText(callerNumber);

            }
        }
        catch (Exception e) {
            Log.e(TAG, e.toString());
            e.printStackTrace();
        }
    }

    @Override
    public void onClick(View v) {

        int id = v.getId();
    }


    public class VideoViewCustom extends VideoView {

        private int mForceHeight = 0;
        private int mForceWidth = 0;
        public VideoViewCustom(Context context) {
            super(context);
        }

        public VideoViewCustom(Context context, AttributeSet attrs) {
            this(context, attrs, 0);
        }

        public VideoViewCustom(Context context, AttributeSet attrs, int defStyle) {
            super(context, attrs, defStyle);
        }

        public void setDimensions(int w, int h) {
            this.mForceHeight = h;
            this.mForceWidth = w;

        }

        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            Log.i("@@@@", "onMeasure");

            setMeasuredDimension(mForceWidth, mForceHeight);
        }
    }




    @Override
    protected void onResume() {
        super.onResume();
        Log.i(TAG, "Entering OnResume");
        findViewById(R.id.container).requestFocus();
        IncomingService.isInFront = true;

    }

    private MediaPlayer.OnPreparedListener PreparedListener = new MediaPlayer.OnPreparedListener(){

        @Override
        public void onPrepared(MediaPlayer m) {

            Log.i(TAG, "Entering OnPreparedListener");
            m.setLooping(true);
            m.setVolume(1.0f, 1.0f);
            m.start();
            Log.i(TAG, "Finishing OnPreparedListener");
        }
    };

    private void finishSpecialCall(){

        try {
            isCallFinished = true;
            videoMedia = false;
            SharedPrefUtils.setBoolean(getApplicationContext(), SharedPrefUtils.GENERAL, SharedPrefUtils.WAS_SPIMAGE_DECODED, false);
            finish();

        } catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    /**
     * A placeholder fragment containing a simple view.
     */
    public static class PlaceholderFragment extends Fragment {

        public PlaceholderFragment() {
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View rootView = inflater.inflate(
                    R.layout.fragment_incoming_special_call, container, false);
            return rootView;
        }
    }

    public class CallStateListener extends PhoneStateListener {
        @Override
        public void onCallStateChanged(int state, String incomingNumber) {
            switch (state) {

                case TelephonyManager.CALL_STATE_IDLE:
                case TelephonyManager.CALL_STATE_OFFHOOK:
                {
                    Log.i(TAG, "TelephonyManager.CALL_STATE_IDLE");

                    if (audioManager != null){

                        new Thread() {
                            public void run() {
                                try {
                                    Thread.sleep(2000,0);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                                audioManager.setStreamMute(AudioManager.STREAM_MUSIC, false);
                            }
                        }.start();
                    }
                    finishSpecialCall();
                    break;
                }


            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.i(TAG, "Entering OnPause");
        if(!isCallFinished)
            BroadcastUtils.sendSpecialCallBroadcast(getApplicationContext(), TAG, new EventReport(EventType.SP_CALL_INC_MOVED_TO_BG, null, null));
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {  //// return button also should be filtered here >> Rony for now it just get into this method. maybe we need the return button to move the activity background
        super.onKeyDown(keyCode, event);
        Log.i(TAG, "Entering onKeyDown");
        if (keyCode == KeyEvent.KEYCODE_VOLUME_UP ||
                keyCode == KeyEvent.KEYCODE_VOLUME_DOWN ||
                keyCode == KeyEvent.KEYCODE_VOLUME_MUTE ) {


            Intent i = new Intent(this, IncomingService.class);
            i.setAction(IncomingService.ACTION_STOP_RING);
            this.startService(i);


            if (videoMedia)
            {
                audioManager = (AudioManager) this.getSystemService(Context.AUDIO_SERVICE);
                audioManager.setStreamMute(AudioManager.STREAM_MUSIC, true);
                videoMedia = false;
            }
            return true;
        }
        return false;
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


}
