package com.ui.activities;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.MediaController;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.VideoView;
import com.interfaces.ITelephony;
import com.services.IncomingService;
import com.special.app.R;
import com.ui.components.AutoSizeButton;


import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import FilesManager.FileManager;


public class IncomingSpecialCall extends ActionBarActivity implements OnClickListener {

    private ITelephony telephonyService;
    private TelephonyManager tm;
    public static final String TAG = "IncomingSpecialCall";
    public static final String SPECIAL_CALL_FILEPATH = "SpecialCallFilePath";
    public static final String SPECIAL_CALL_CALLER = "SpecialCallCaller";
    private String callerNumber ;
    private boolean mIsBound = false;
    private boolean videoMedia = false;
    private IncomingService incomingReceiver;
    AudioManager audioManager = null ;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.i(TAG, "Entering " + TAG);

        try {


            ActionBar actionBar = getSupportActionBar();
            actionBar.hide();

            //Remove title bar
          //  this.requestWindowFeature(Window.FEATURE_NO_TITLE);
            //Remove notification bar
           // this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);


            Intent intent = getIntent();
            String mediaFilePath = intent.getStringExtra(SPECIAL_CALL_FILEPATH);
            callerNumber = intent.getStringExtra(SPECIAL_CALL_CALLER);




            CallStateListener stateListener = new CallStateListener();
            tm = (TelephonyManager) getApplicationContext().getSystemService(Context.TELEPHONY_SERVICE);
            tm.listen(stateListener, PhoneStateListener.LISTEN_CALL_STATE);

            Log.i(TAG, "Preparing to display:"+mediaFilePath);

            FileManager.FileType fileType = FileManager.getFileType(new File(mediaFilePath));

            this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN |
                            WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD |
                            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                            WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                            | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT /*| Intent.FLAG_ACTIVITY_CLEAR_TOP*/ | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL  // <<< flags added by RONY
                            | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED, // <<< flags added by RONY
                    WindowManager.LayoutParams.FLAG_FULLSCREEN |
                            WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD |
                            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                            WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);


            if (fileType == FileManager.FileType.IMAGE)
            {
                Log.i(TAG, "In IMAGE");
                setContentView(R.layout.activity_incoming_special_call);
                BitmapFactory.decodeFile(mediaFilePath);
                ImageView myImageView = (ImageView)findViewById(R.id.CallerImage);
                myImageView.setImageBitmap(loadImage(mediaFilePath));

                TextView myTextView = (TextView)findViewById(R.id.IncomingCallNumber);
                myTextView.setText(callerNumber);

                //  Log.d("IncomingCallActivity: onCreate: ", "flagz");
                videoMedia = false;

            }
            if (fileType == FileManager.FileType.VIDEO)
            {

                Log.i(TAG, "In VIDEO");
                videoMedia = true;
                // Special ringtone in video case is silent
                IncomingService.wasSpecialRingTone = true;

                RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT);
                RelativeLayout rlayout  = new RelativeLayout(this);
                rlayout.setLayoutParams(params);
                rlayout.setBackgroundColor(Color.CYAN);
                rlayout.setGravity(Gravity.CENTER_VERTICAL);

                final File root = new File(mediaFilePath);

                RelativeLayout.LayoutParams videoParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT);
                videoParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
                videoParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
                videoParams.addRule(RelativeLayout.ALIGN_PARENT_TOP);
                videoParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
                Uri uri = Uri.fromFile(root);

                final VideoView mVideoView  = new VideoView(IncomingSpecialCall.this);
                MediaController mediaController = new MediaController(this);
                mediaController.setAnchorView(mVideoView);
                mediaController.setMediaPlayer(mVideoView);
                mVideoView.setMediaController(mediaController);
                mVideoView.setOnPreparedListener(PreparedListener);
                mVideoView.setVideoURI(uri);
                mVideoView.requestFocus();
                mVideoView.setLayoutParams(videoParams);


                RelativeLayout.LayoutParams textViewParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
                textViewParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);

                TextView incomingCallNumber = new TextView(IncomingSpecialCall.this);
                incomingCallNumber.setBackgroundColor(getResources().getColor(R.color.black));
                incomingCallNumber.setText(callerNumber);
                incomingCallNumber.setPadding(10, 10, 10, 10);
                incomingCallNumber.setTextColor(getResources().getColor(R.color.white));
                incomingCallNumber.setLayoutParams(textViewParams);
                incomingCallNumber.bringToFront();

                rlayout.addView(mVideoView);
                rlayout.addView(incomingCallNumber);

                setContentView(rlayout);

                //setVisible(true);


            }
        }
        catch (Exception e) {
            Log.e(TAG, e.toString());
            e.printStackTrace();
        }
    }



    @Override
    protected void onResume() {
        super.onResume();
        Log.i(TAG, "Entering OnResume");
        IncomingService.isInFront = true;
        doBindService();

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


    private Bitmap loadImage(String imgPath) {
        BitmapFactory.Options options;
        try {
            options = new BitmapFactory.Options();
            options.inSampleSize = 2;
            Bitmap bitmap = BitmapFactory.decodeFile(imgPath, options);
            return bitmap;
        } catch(Exception e) {
            e.printStackTrace();
        }
        return null;
    }



    @Override
    public void onClick(View v) {

        int id = v.getId();
        /*if (id == R.id.Answer) {   /// ANSWER
            Log.i(TAG, "InSecond Method Ans Call");
            answerSpecialCall();
        }*/


    }

    private void finishSpecialCall(){

        try {
          this.finish();
            videoMedia = false;
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

                case TelephonyManager.DATA_DISCONNECTED: {
                    Log.i(TAG, "TelephonyManager.DATA_DISCONNECTED");

                    finishSpecialCall();
                    if (audioManager != null){

                        Runnable r = new Runnable() {
                            public void run() {
                                try {
                                    Thread.sleep(2000,0);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                                audioManager.setStreamMute(AudioManager.STREAM_MUSIC, false);
                            }
                        };

                        new Thread(r).start();



                }
                    break;
                }


            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.i(TAG, "Entering OnPause");
        IncomingService.isInFront = false;
        doUnbindService();

    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        super.onKeyDown(keyCode, event);
        Log.i(TAG, "Entering onKeyDown");
        if (keyCode == KeyEvent.KEYCODE_VOLUME_UP ||
            keyCode == KeyEvent.KEYCODE_VOLUME_DOWN ||
            keyCode == KeyEvent.KEYCODE_VOLUME_MUTE ) {
            incomingReceiver.stopSound();

            if (videoMedia)
            {
                audioManager = (AudioManager) getApplicationContext().getSystemService(Context.AUDIO_SERVICE);
                audioManager.setStreamMute(AudioManager.STREAM_MUSIC, true);
                videoMedia = false;
            }
            return true;
        }
        return false;
    }

    private ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            // This is called when the connection with the service has been
            // established, giving us the service object we can use to
            // interact with the service.  Because we have bound to a explicit
            // service that we know is running in our own process, we can
            // cast its IBinder to a concrete class and directly access it.
            incomingReceiver = ((IncomingService.MyBinder)service).getService();

        }

        public void onServiceDisconnected(ComponentName className) {
            // This is called when the connection with the service has been
            // unexpectedly disconnected -- that is, its process crashed.
            // Because it is running in our same process, we should never
            // see this happen.
            incomingReceiver = null;
        }
    };

    void doBindService() {
        // Establish a connection with the service.  We use an explicit
        // class name because we want a specific service implementation that
        // we know will be running in our own process (and thus won't be
        // supporting component replacement by other applications).
        Log.i(TAG, "Entering doBindService");
        bindService(new Intent(this,
                IncomingService.class), mConnection, 0);
        mIsBound = true;
    }

    void doUnbindService() {

        if (mIsBound) {
            // Detach our existing connection.
            Log.i(TAG, "Entering doBindService");
            unbindService(mConnection);
            mIsBound = false;
        }
    }


}
