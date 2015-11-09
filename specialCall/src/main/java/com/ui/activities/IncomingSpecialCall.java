package com.ui.activities;

import android.app.Activity;
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
import android.support.v7.internal.widget.ActionBarOverlayLayout;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.transition.TransitionManager;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
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


public class IncomingSpecialCall extends Activity implements OnClickListener {

    private ITelephony telephonyService;
    private TelephonyManager tm;
    public static final String TAG = "IncomingSpecialCall";
    public static final String SPECIAL_CALL_FILEPATH = "SpecialCallFilePath";
    public static final String SPECIAL_CALL_CALLER = "SpecialCallCaller";
    private String callerNumber ;
    private boolean mIsBound = false;
    private boolean videoMedia = false;

    AudioManager audioManager = null ;
    RelativeLayout rlayout;
    VideoViewCustom mVideoView;
    MediaController mediaController;
    ImageView myImageView;
    String mediaFilePath;
    boolean FullScreen = true;
    int videoTransitionID ;
    int imageTransitionID ;
    public static Context ctx;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.i(TAG, "Entering " + TAG);

        try {


            Intent intent = getIntent();
            mediaFilePath = intent.getStringExtra(SPECIAL_CALL_FILEPATH);
            callerNumber = intent.getStringExtra(SPECIAL_CALL_CALLER);

            CallStateListener stateListener = new CallStateListener();
            tm = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);
            tm.listen(stateListener, PhoneStateListener.LISTEN_CALL_STATE);

            Log.i(TAG, "Preparing to display:" + mediaFilePath);

            FileManager.FileType fileType = FileManager.getFileType(new File(mediaFilePath));

            this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN |
                            WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD |
                            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                            WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON |   WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |   WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                            | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT /*| Intent.FLAG_ACTIVITY_CLEAR_TOP*/   // <<< flags added by RONY
                            | /*Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP |*/ Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED // <<< flags added by RONY
                    , WindowManager.LayoutParams.FLAG_FULLSCREEN |
                            WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD |
                            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                            WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON |  WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |   WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
            );


            WindowManager.LayoutParams lp = this.getWindow().getAttributes();
            lp.dimAmount=0.0f;
            this.getWindow().setAttributes(lp);

           // getWindow().addFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);

            if (fileType == FileManager.FileType.IMAGE)
            {
                this.getWindow().addFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
                Log.i(TAG, "In IMAGE");
                setContentView(R.layout.activity_incoming_special_call);
                BitmapFactory.decodeFile(mediaFilePath);

                myImageView = (ImageView)findViewById(R.id.CallerImage);

                myImageView.setImageBitmap(loadImage(mediaFilePath));

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

                // setting Relative Layout Paramaters
                RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT);
                /*RelativeLayout*/ rlayout  = new RelativeLayout(this);
                rlayout.setLayoutParams(params);
               //   rlayout.setBackgroundColor(Color.GREEN);
                rlayout.setGravity(Gravity.CENTER_VERTICAL);


                // VideoView on Relative Layout
                final File root = new File(mediaFilePath);
                RelativeLayout.LayoutParams videoParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT);
                videoParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
                videoParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
                videoParams.addRule(RelativeLayout.ALIGN_PARENT_TOP);
                videoParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
                Uri uri = Uri.fromFile(root);
                // final VideoView mVideoView  = new VideoView(IncomingSpecialCall.this);
                mVideoView  = new VideoViewCustom(IncomingSpecialCall.this);
                //  mVideoView.setBackgroundColor(Color.TRANSPARENT);
                // MediaController mediaController = new MediaController(this);
                mediaController = new MediaController(this);
                mediaController.setAnchorView(mVideoView);
                mediaController.setMediaPlayer(mVideoView);
                mediaController.setBackgroundColor(Color.WHITE);


                mVideoView.setMediaController(mediaController);
                mVideoView.setOnPreparedListener(PreparedListener);
                mVideoView.setVideoURI(uri);
                mVideoView.requestFocus();
                mVideoView.setLayoutParams(videoParams);
               // mVideoView.setBackgroundColor(Color.YELLOW);

                // TextView for Showing Incoming Call Number and contact name
                RelativeLayout.LayoutParams textViewParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
                textViewParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
                TextView incomingCallNumber = new TextView(IncomingSpecialCall.this);
                incomingCallNumber.setBackgroundColor(getResources().getColor(R.color.black));
                incomingCallNumber.setText(callerNumber);
                incomingCallNumber.setPadding(10, 10, 10, 10);
                incomingCallNumber.setTextColor(getResources().getColor(R.color.white));
                incomingCallNumber.setLayoutParams(textViewParams);
                incomingCallNumber.bringToFront();

                //Transition Button
                RelativeLayout.LayoutParams b2Params = new RelativeLayout.LayoutParams(150, 150);
                b2Params.addRule(RelativeLayout.ALIGN_RIGHT,  incomingCallNumber.getId());
                b2Params.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
                b2Params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
                Button videoTransition = new AutoSizeButton(this);
                videoTransition.setText("Minimize");
                videoTransition.setBackgroundColor(Color.GRAY);
                videoTransition.setLayoutParams(b2Params);


                // adding view to the RelativeLayout
                rlayout.addView(mVideoView);
                rlayout.addView(incomingCallNumber);
                rlayout.addView(videoTransition);


                setContentView(rlayout);


                videoTransitionID= videoTransition.getId();
                videoTransition.setOnClickListener(this);



            }
        }
        catch (Exception e) {
            Log.e(TAG, e.toString());
            e.printStackTrace();
        }
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




    private void finishSpecialCall(){

        try {
            this.finish();
            mIsBound = true;
            videoMedia = false;
        } catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    @Override
    public void onClick(View v) {

        int id = v.getId();
        if (id == videoTransitionID) {
            Log.i(TAG, "INSIDE Transition Button");

            ctx = getApplicationContext();
            mIsBound = false;
         //   this.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE);
            this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN |
                            WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD |
                            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                            WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON |  WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |   WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL | WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH  |WindowManager.LayoutParams.FLAG_SPLIT_TOUCH |   WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY  //| WindowManager.LayoutParams.TYPE_SYSTEM_ALERT
                            | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT /*| Intent.FLAG_ACTIVITY_CLEAR_TOP*/   // <<< flags added by RONY
                            | /*Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP |*/ Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED // <<< flags added by RONY
                    , WindowManager.LayoutParams.FLAG_FULLSCREEN |
                            WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD |
                            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                            WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON |  WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |   WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL | WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH  |WindowManager.LayoutParams.FLAG_SPLIT_TOUCH |   WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY  //| WindowManager.LayoutParams.TYPE_SYSTEM_ALERT
            );

            WindowManager.LayoutParams lp = this.getWindow().getAttributes();
            lp.dimAmount=0.0f;
            this.getWindow().setAttributes(lp);


            int currentPosition = mVideoView.getCurrentPosition();
            mVideoView.stopPlayback();


            rlayout.removeAllViews();

            if (FullScreen) {
                final float scale = this.getResources().getDisplayMetrics().density;
                int pixels = (int) (400 * scale + 0.5f);

                FrameLayout.LayoutParams rel_btn = new FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT, pixels);
                rel_btn.bottomMargin = pixels;
                rlayout.setBottom(pixels);

                rlayout.setLayoutParams(rel_btn);
            } else {
                FrameLayout.LayoutParams rel_btn = new FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT);

                rlayout.setLayoutParams(rel_btn);


            }


            // VideoView on Relative Layout
            final File root = new File(mediaFilePath);

            RelativeLayout.LayoutParams videoParams;

            if (FullScreen) {
                videoParams = new RelativeLayout.LayoutParams(300, 300);
                FullScreen = false;
            } else {
                videoParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT);
                FullScreen = true;
            }

            videoParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
            videoParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
            videoParams.addRule(RelativeLayout.ALIGN_PARENT_TOP);
            videoParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
            Uri uri = Uri.fromFile(root);
            // final VideoView mVideoView  = new VideoView(IncomingSpecialCall.this);
            mVideoView = new VideoViewCustom(IncomingSpecialCall.this);
            //  mVideoView.setBackgroundColor(Color.TRANSPARENT);
            // MediaController mediaController = new MediaController(this);
            mediaController = new MediaController(this);
            mediaController.setAnchorView(mVideoView);
            mediaController.setMediaPlayer(mVideoView);
            mediaController.setBackgroundColor(Color.BLUE);


            mVideoView.setMediaController(mediaController);
            mVideoView.setOnPreparedListener(PreparedListener);
            mVideoView.setVideoURI(uri);
            mVideoView.requestFocus();
            mVideoView.seekTo(currentPosition);
          //  mVideoView.setBackgroundColor(Color.RED);

            mVideoView.setLayoutParams(videoParams);


            // TextView for Showing Incoming Call Number and contact name
            RelativeLayout.LayoutParams textViewParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
            textViewParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
            TextView incomingCallNumber = new TextView(IncomingSpecialCall.this);
            incomingCallNumber.setBackgroundColor(getResources().getColor(R.color.black));
            incomingCallNumber.setText(callerNumber);
            incomingCallNumber.setPadding(10, 10, 10, 10);
            incomingCallNumber.setTextColor(getResources().getColor(R.color.white));
            incomingCallNumber.setLayoutParams(textViewParams);
            incomingCallNumber.bringToFront();

            //Transition Button
            RelativeLayout.LayoutParams b2Params = new RelativeLayout.LayoutParams(150, 150);
            b2Params.addRule(RelativeLayout.ALIGN_RIGHT, incomingCallNumber.getId());
            b2Params.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
            b2Params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
            Button videoTransition = new AutoSizeButton(this);
            videoTransition.setText("Transition");
            videoTransition.setBackgroundColor(Color.GRAY);
            videoTransition.setLayoutParams(b2Params);


            rlayout.addView(mVideoView);
            rlayout.addView(incomingCallNumber);
           // rlayout.addView(videoTransition);




          //  videoTransitionID= videoTransition.getId();
          //  videoTransition.setOnClickListener(this);


        }
//        if (id == imageTransitionID)
//        {
//
//
//            this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN |
//                    WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD |
//                    WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
//                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON |  WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |     WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY  //| WindowManager.LayoutParams.TYPE_SYSTEM_ALERT
//                    | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT /*| Intent.FLAG_ACTIVITY_CLEAR_TOP*/   // <<< flags added by RONY
//                    | /*Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP |*/ Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED // <<< flags added by RONY
//                    , WindowManager.LayoutParams.FLAG_FULLSCREEN |
//                    WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD |
//                    WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
//                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON |  WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |     WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY  //| WindowManager.LayoutParams.TYPE_SYSTEM_ALERT
//            );
//
//
//            ViewGroup.LayoutParams params = (ViewGroup.LayoutParams) myImageView.getLayoutParams();
//            if (FullScreen) {
//                final float scale = this.getResources().getDisplayMetrics().density;
//                int pixels = (int) (400 * scale + 0.5f);
//
//                params.height = pixels;
//                FullScreen = false;
//            } else {
//                params.height = RelativeLayout.LayoutParams.MATCH_PARENT;
//                FullScreen = true;
//            }
//
//            myImageView.setLayoutParams(params);
//
//
//
//          //  TextView myTextView = (TextView)findViewById(R.id.IncomingCallNumber);
//
//
//            //  Log.d("IncomingCallActivity: onCreate: ", "flagz");
//            videoMedia = false;
//
//
//
//            // TextView for Showing Incoming Call Number and contact name
//           // RelativeLayout.LayoutParams textViewParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
//           // textViewParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
//            TextView myTextView = (TextView)findViewById(R.id.IncomingCallNumber);
//            myTextView.setBackgroundColor(getResources().getColor(R.color.black));
//            myTextView.setText(callerNumber);
//            myTextView.setPadding(10, 10, 10, 10);
//            myTextView.setTextColor(getResources().getColor(R.color.white));
//         //   myTextView.setLayoutParams(textViewParams);
//            myTextView.bringToFront();
//
//            //Transition Button
//            RelativeLayout.LayoutParams b2Params = new RelativeLayout.LayoutParams(150, 150);
//            b2Params.addRule(RelativeLayout.ALIGN_RIGHT, myTextView.getId());
//            b2Params.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
//            b2Params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
//            Button ImageTransition = (Button)findViewById(R.id.Transition);
//            ImageTransition.setText("Transition");
//            ImageTransition.setBackgroundColor(Color.GRAY);
//            ImageTransition.setLayoutParams(b2Params);
//
//
//
//
//            ImageTransition.setOnClickListener(this);
//
//
//
//        }
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

        mIsBound = true;
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {  //// return button also should be filtered here >> Rony for now it just get into this method. maybe we need the return button to move the activity background
        super.onKeyDown(keyCode, event);
        Log.i(TAG, "Entering onKeyDown");
        if (keyCode == KeyEvent.KEYCODE_VOLUME_UP ||
                keyCode == KeyEvent.KEYCODE_VOLUME_DOWN ||
                keyCode == KeyEvent.KEYCODE_VOLUME_MUTE ) {


            Intent i = new Intent(this, IncomingService.class);
            i.setAction(IncomingService.STOP_RING);
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





}
