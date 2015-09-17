package com.special.specialcall;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.Fragment;
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
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.MediaController;
import android.widget.RelativeLayout;
import android.widget.VideoView;
import com.android.internal.telephony.ITelephony;
import com.android.services.IncomingReceiver;


import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import FilesManager.FileManager;


public class IncomingSpecialCall extends ActionBarActivity implements OnClickListener {

    private ITelephony telephonyService;
    private TelephonyManager tm;
    public static final String TAG = "IncomingSpecialCall";
    public static final String SPECIAL_CALL_FILEPATH = "SpecialCallFilePath";
    private boolean mIsBound = false;
    private IncomingReceiver incomingReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.i(TAG, "Entering " + TAG);

        try {

            Intent intent = getIntent();
            String mediaFilePath = intent.getStringExtra(SPECIAL_CALL_FILEPATH);

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
                //  Log.d("IncomingCallActivity: onCreate: ", "flagz");

                Button Answer = (Button)findViewById(R.id.Answer);
                Answer.setOnClickListener(this);

                Button Decline = (Button)findViewById(R.id.Decline);
                Decline.setOnClickListener(this);
            }
            if (fileType == FileManager.FileType.VIDEO)
            {
                Log.i(TAG, "In VIDEO");

                // Special ringtone in video case is silent
                IncomingReceiver.wasSpecialRingTone = true;

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

                final VideoView mVideoView  = new VideoView(getApplicationContext());
                MediaController mediaController = new MediaController(this);
                mediaController.setAnchorView(mVideoView);
                mediaController.setMediaPlayer(mVideoView);
                mVideoView.setMediaController(mediaController);
                mVideoView.setOnPreparedListener(PreparedListener);
                mVideoView.setVideoURI(uri);
                mVideoView.requestFocus();
                mVideoView.setLayoutParams(videoParams);

                RelativeLayout.LayoutParams b1Params = new RelativeLayout.LayoutParams(150, 150);
                b1Params.addRule(RelativeLayout.ALIGN_LEFT,  mVideoView.getId());
                b1Params.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
                b1Params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);

                Button videoAnswer = new AutoSizeButton(this);
                videoAnswer.setBackgroundColor(Color.GRAY);
                videoAnswer.setText("Answer");
                videoAnswer.setLayoutParams(b1Params);

                RelativeLayout.LayoutParams b2Params = new RelativeLayout.LayoutParams(150, 150);
                b2Params.addRule(RelativeLayout.ALIGN_RIGHT,  videoAnswer.getId());
                b2Params.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
                b2Params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);

                Button videoDecline = new AutoSizeButton(this);
                videoDecline.setText("Decline");
                videoDecline.setBackgroundColor(Color.GRAY);
                videoDecline.setLayoutParams(b2Params);

                rlayout.addView(mVideoView);
                rlayout.addView(videoAnswer);
                rlayout.addView(videoDecline);

                setContentView(rlayout);
                //setVisible(true);

                videoAnswer.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View arg0) {

                        mVideoView.stopPlayback();
                        Log.i(TAG, "InSecond Method Ans Call");
                        answerSpecialCall();

                    }
                });
                videoDecline.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View arg0) {

                        try {
                            mVideoView.stopPlayback();
                            Log.i(TAG, "OnRejectButton: " + "Reject OnClick");
                            TelephonyManager tm = (TelephonyManager) getApplicationContext().getSystemService(getApplicationContext().TELEPHONY_SERVICE);
                            Class c;

                            c = Class.forName(tm.getClass().getName());

                            Method m = null;
                            try {
                                m = c.getDeclaredMethod("getITelephony");
                            } catch (NoSuchMethodException e1) {
                                // TODO Auto-generated catch block
                                e1.printStackTrace();
                            }
                            m.setAccessible(true);
                            try {
                                telephonyService = (ITelephony) m.invoke(tm);
                            } catch (IllegalAccessException   |
                                     IllegalArgumentException |
                                     InvocationTargetException e) {
                                // TODO Auto-generated catch block
                                e.printStackTrace();
                            }
                            telephonyService.endCall();
                            Log.i(TAG, "FinishDecline");
                            finishSpecialCall();
                        }
                        catch (Exception e) {
                            Log.e(TAG, e.toString());
                            e.printStackTrace();
                        }

                    }

                });
            }
        }
        catch (Exception e) {
            Log.e(TAG, e.toString());
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }



    @Override
    protected void onResume() {
        super.onResume();
        Log.i(TAG, "Entering OnResume");
        IncomingReceiver.isInFront = true;
        doBindService();

    }

    private MediaPlayer.OnPreparedListener PreparedListener = new MediaPlayer.OnPreparedListener(){

        @Override
        public void onPrepared(MediaPlayer m) {

            Log.i(TAG, "Entering OnPreparedListener");
            m.setLooping(true);
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
        if (id == R.id.Answer) {   /// ANSWER
            Log.i(TAG, "InSecond Method Ans Call");
            answerSpecialCall();
        }

        else if (id == R.id.Decline ) {   /// DECLINE
            Log.i(TAG, "OnRejectButton: " + "Reject OnClick");

            TelephonyManager tm = (TelephonyManager) getApplicationContext().getSystemService(getApplicationContext().TELEPHONY_SERVICE);
            try {
                Class c = Class.forName(tm.getClass().getName());
                Method m = c.getDeclaredMethod("getITelephony");
                m.setAccessible(true);
                telephonyService = (ITelephony) m.invoke(tm);
                telephonyService.endCall();
                Log.i(TAG, "FinishDecline");
                finishSpecialCall();
            } catch (Exception e)
            {
                Log.e(TAG,"Decline error:"+e.getMessage());
                e.printStackTrace();
            }
        }
    }


    public void answerSpecialCall()
    {
        // froyo and beyond trigger on buttonUp instead of buttonDown
        Intent buttonUp = new Intent(Intent.ACTION_MEDIA_BUTTON);
        buttonUp.putExtra(Intent.EXTRA_KEY_EVENT, new KeyEvent(
                KeyEvent.ACTION_UP, KeyEvent.KEYCODE_HEADSETHOOK));
        try { sendOrderedBroadcast(buttonUp, "android.permission.CALL_PRIVILEGED"); }
        catch (Exception e) { e.printStackTrace();}
        Intent headSetUnPluggedintent = new Intent(Intent.ACTION_HEADSET_PLUG);
        headSetUnPluggedintent.addFlags(Intent.FLAG_RECEIVER_REGISTERED_ONLY);
        headSetUnPluggedintent.putExtra("state", 0);
        headSetUnPluggedintent.putExtra("name", "Headset");
        try {  sendOrderedBroadcast(headSetUnPluggedintent, null); }
        catch (Exception e) { e.printStackTrace();}
        finishSpecialCall();
    }

    private void finishSpecialCall(){

        try {
          this.finish();
        } catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.incoming_special_call, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
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

                    break;
                }


            }
        }
    }


    @Override
    protected void onPause() {
        super.onPause();
        Log.i(TAG, "Entering OnPause");
        IncomingReceiver.isInFront = false;
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
            incomingReceiver = ((IncomingReceiver.MyBinder)service).getService();

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
                IncomingReceiver.class), mConnection, 0);
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
