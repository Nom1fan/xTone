package com.services;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Point;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.IBinder;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import com.special.app.R;
import com.utils.BitmapUtils;
import com.utils.ContactsUtils;
import com.utils.MCBlockListUtils;
import com.utils.PhoneNumberUtils;
import com.utils.UI_Utils;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

import Exceptions.FileDoesNotExistException;
import Exceptions.FileExceedsMaxSizeException;
import Exceptions.FileInvalidFormatException;
import Exceptions.FileMissingExtensionException;
import FilesManager.FileManager;
import wei.mark.standout.StandOutWindow;
import wei.mark.standout.ui.Window;

public abstract class AbstractStandOutService extends StandOutWindow {

    public static final String ACTION_STOP_RING = "com.services.AbstractStandOutService.ACTION_STOP_RING";
    public static final String ACTION_START = "com.services.AbstractStandOutService.ACTION_START";
    protected String TAG;
    protected int mWidth;
    protected int mHeight;
    protected TextView mSpecialCallTextView;
    protected ImageView mSpecialCallCloseBtn;
    protected ImageView mSpecialCallMutUnMuteBtn;
    protected ImageView mSpecialCallVolumeUpBtn;
    protected ImageView mSpecialCallVolumeDownBtn;
    protected ImageView mSpecialCallBlockBtn;
    protected RelativeLayout mRelativeLayout;
    protected View mcButtonsOverlay;
    protected boolean isMuted=false;
    protected boolean mInRingingSession = false;
    protected MediaPlayer mMediaPlayer;
    protected boolean windowCloseActionWasMade = true;
    protected boolean attachDefaultView = false;
    protected View mSpecialCallView;
    protected AudioManager mAudioManager;
    protected CallStateListener mPhoneListener;
    protected OnVideoPreparedListener mVideoPreparedListener;
    protected boolean volumeChangeByMCButtons = false;
    protected int  mVolumeBeforeMute = 0;
    protected String mIncomingOutgoingNumber="";

    public AbstractStandOutService(String TAG) {
        this.TAG = TAG;
    }

    //region Service methods
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);

        String action = null;
        if(intent!=null)
            action = intent.getAction();
        if(action!=null)
            Log.i(TAG, "Action:" + action);
        return START_STICKY;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(TAG, "Service onCreate");
        android.os.Process.setThreadPriority(-20);
        prepareCallStateListener();
        prepareStandOutWindowDisplay();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.e(TAG, "Service onDestroy");

        if (mAudioManager!=null)
        {mAudioManager.setStreamMute(AudioManager.STREAM_MUSIC, false);  // TODO Rony : Replace Deprecated !! Check All places
        mAudioManager.setStreamMute(AudioManager.STREAM_RING,false);}  // TODO Rony : Replace Deprecated !! Check All places
    }

    @Override
    public IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException("Not yet implemented");
    }
    //endregion

    //region Standout Window methods
    @Override
    public String getAppName() {
        return "SPECIAL CALL";
    }

    @Override
    public int getAppIcon() {
        return R.drawable.color_mc;
    }

    @Override
    public void createAndAttachView(int id, FrameLayout frame) {

        Log.i(TAG, "In createAndAttachView()");

        frame.addView(mRelativeLayout);

        LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
        mcButtonsOverlay = inflater.inflate(R.layout.mc_buttons_overlay, null);
        prepareMCButtonsOnRelativeLayoutOverlay();

        frame.addView(mcButtonsOverlay);

        frame.setBackgroundColor(Color.BLACK);
        windowCloseActionWasMade = false;
    }

    private void prepareMCButtonsOnRelativeLayoutOverlay() {

        prepareCloseBtn();
        prepareMuteBtn();
        prepareVolumeBtn();
        prepareBlockButton();
    }

    @Override
    public StandOutLayoutParams getParams(int id, Window window) {
        Log.i(TAG, "In StandOutLayoutParams()");
        return new StandOutLayoutParams(id, mWidth, mHeight, 0, 0);
    }
    //endregion

    //region Private classes and listeners
    /**
     * Listener for call states
     * Listens for different call states
     */
    private class CallStateListener extends PhoneStateListener {

        @Override
        public void onCallStateChanged(int state, String incomingNumber) {

            syncOnCallStateChange(state, incomingNumber);
        }
    }

    /**
     * Listener for video views to be prepared
     * Subclasses should override
     */
    protected class OnVideoPreparedListener implements MediaPlayer.OnPreparedListener {

        @Override
        public void onPrepared(MediaPlayer mp) {
            throw new UnsupportedOperationException("Not yet implemented");
        }
    }

    /* Assisting methods */

    /**
     * Subclasses should implement this method and decide what to do according to call state
     * @param state The call state
     * @param incomingNumber The incoming number in case of an incoming call. Otherwise (outgoing call), null.
     */
    protected abstract void syncOnCallStateChange(int state, String incomingNumber);

    private void prepareCallStateListener()
    {
        if (mPhoneListener == null) {
            mPhoneListener = new CallStateListener();
            TelephonyManager tm = (TelephonyManager) getApplicationContext().getSystemService(Context.TELEPHONY_SERVICE);
            tm.listen(mPhoneListener, PhoneStateListener.LISTEN_CALL_STATE);
        }
    }

    private void prepareStandOutWindowDisplay()
    {
        Display display = mWindowManager.getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        mWidth = size.x;
        mHeight = size.y*63/100;
    }

    private void prepareRelativeLayout()
    {
        Log.i(TAG, "Preparing RelativeLayout");

        // Creating a new RelativeLayout
        mRelativeLayout = new RelativeLayout(this);

        // Defining the RelativeLayout layout parameters.
        // In this case I want to fill its parent
        RelativeLayout.LayoutParams rlp = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT,
                RelativeLayout.LayoutParams.MATCH_PARENT);

        mRelativeLayout.setLayoutParams(rlp);
        mRelativeLayout.setBackgroundColor(Color.BLACK);
    }

    private void prepareCallNumberTextView(String callNumber)
    {
        // Defining the layout parameters of the TextView
        RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.WRAP_CONTENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT);
        lp.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);

        //TextView for Showing Incoming Call Number and contact name
        mSpecialCallTextView = new TextView(this);
        String contactName = ContactsUtils.getContactName(getApplicationContext(), callNumber);
        mSpecialCallTextView.setText(!contactName.equals("") ? contactName + " " + callNumber : callNumber);
        mSpecialCallTextView.setBackgroundColor(Color.BLACK);
        mSpecialCallTextView.setTextColor(Color.WHITE);
        mSpecialCallTextView.setGravity(Gravity.BOTTOM | Gravity.LEFT);
        mSpecialCallTextView.setLayoutParams(lp);
    }

    private void prepareCloseBtn()
    {
        mSpecialCallCloseBtn = (ImageView) mcButtonsOverlay.findViewById(R.id.close_mc);
        Log.i(TAG, "Preparing close Button");
        //ImageView for Closing Special Incoming Call
        mSpecialCallCloseBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                closeSpecialCallWindowWithoutRingtone();
            }
        });

    }

    private void prepareImageView(String mediaFilePath)
    {
        Log.i(TAG, "Preparing ImageView");

        mSpecialCallView = new ImageView(this);
        mSpecialCallView.setLayoutParams(new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT));
        //     ((ImageView)mSpecialCallView).setScaleType(ImageView.ScaleType.FIT_XY); STRECTH IMAGE ON FULL SCREEN <<< NOT SURE IT's GOOD !!!!!
        ((ImageView) mSpecialCallView).setScaleType(ImageView.ScaleType.FIT_CENTER); // <<  just place the image Center of Window and fit it with ratio
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(mediaFilePath, options);
        options.inSampleSize = BitmapUtils.calculateInSampleSize(options, mWidth, mHeight);

        options.inJustDecodeBounds = false;
        Bitmap spCallBitmap = BitmapFactory.decodeFile(mediaFilePath, options);

        if (spCallBitmap != null)
            ((ImageView) mSpecialCallView).setImageBitmap(spCallBitmap);
        else {
            spCallBitmap = BitmapFactory.decodeFile(mediaFilePath);
            ((ImageView) mSpecialCallView).setImageBitmap(spCallBitmap);
        }
    }

    private void prepareVideoView(String mediaFilePath)
    {
        Log.i(TAG, "Preparing VideoView");
        // VideoView on Relative Layout
        final File root = new File(mediaFilePath);

        Uri uri = Uri.fromFile(root);
        Log.i(TAG, "Video uri=" + uri);

        mSpecialCallView = new VideoView(this);
        mSpecialCallView.setLayoutParams(new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT,
                RelativeLayout.LayoutParams.MATCH_PARENT));

        RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) mSpecialCallView.getLayoutParams();

        params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
        params.addRule(RelativeLayout.ALIGN_PARENT_TOP);
        params.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
        params.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);

        mSpecialCallView.setLayoutParams(params);
        //TODO should we use MediaController controls to control video ??? TBD
        // mediaController = new MediaController(this);
        // mediaController.setAnchorView(mSpecialCallView);
        // mediaController.setMediaPlayer(((VideoView)mSpecialCallView));
        // mediaController.setBackgroundColor(Color.WHITE);
        // ((VideoView)mSpecialCallView).setMediaController(mediaController);
        // mRelativeLayout.addView(mediaController);
        ((VideoView) mSpecialCallView).setVideoURI(uri);
        ((VideoView) mSpecialCallView).requestFocus();

        // Once the VideoView is prepared, the prepared listener will activate
        ((VideoView) mSpecialCallView).setOnPreparedListener(mVideoPreparedListener);
    }

    // Default view for outgoing call when there is no image or video
    protected void prepareDefaultViewForSpecialCall(String callNumber) {
        Log.i(TAG, "Preparing SpecialCall view");

        // Attempting to induce garbage collection
        System.gc();

        prepareRelativeLayout();
        prepareCallNumberTextView(callNumber);

        mSpecialCallView = new ImageView(this);
        mSpecialCallView.setLayoutParams(new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT));
        //     ((ImageView)mSpecialCallView).setScaleType(ImageView.ScaleType.FIT_XY); STRECTH IMAGE ON FULL SCREEN <<< NOT SURE IT's GOOD !!!!!
        ((ImageView) mSpecialCallView).setScaleType(ImageView.ScaleType.FIT_CENTER); // <<  just place the image Center of Window and fit it with ratio


        ((ImageView) mSpecialCallView).setImageResource(R.drawable.color_mc);


        mRelativeLayout.addView(mSpecialCallView);
        mRelativeLayout.addView(mSpecialCallTextView);
    }

    protected void prepareViewForSpecialCall(FileManager.FileType fileType , String mediaFilePath, String callNumber) {
        Log.i(TAG, "Preparing SpecialCall view");

        // Attempting to induce garbage collection
        System.gc();

        prepareRelativeLayout();
        prepareCallNumberTextView(callNumber);

        // Displaying image during call
        if (fileType == FileManager.FileType.IMAGE) {

            try {
                    prepareImageView(mediaFilePath);
            } catch (NullPointerException | OutOfMemoryError e) {
                Log.e(TAG, "Failed decoding image", e);
            }
        }
        // Displaying video during call
        else if (fileType == FileManager.FileType.VIDEO)
            prepareVideoView(mediaFilePath);

        mRelativeLayout.addView(mSpecialCallView);
        mRelativeLayout.addView(mSpecialCallTextView);
    }

    private void prepareMuteBtn() {
        Log.i(TAG, "Preparing Mute Button");

        //TODO check if Togglebutton for imageView good also?
        mSpecialCallMutUnMuteBtn = (ImageView) mcButtonsOverlay.findViewById(R.id.mc_mute_unmute);
        if (mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC) ==  0)
        {
            mSpecialCallMutUnMuteBtn.setImageResource(R.drawable.mute);//TODO : setImageResource need to be replaced ? memory issue ?
            mSpecialCallMutUnMuteBtn.bringToFront();
        }
        else
        {
            mSpecialCallMutUnMuteBtn.setImageResource(R.drawable.unmute);//TODO : setImageResource need to be replaced ? memory issue ?
            mSpecialCallMutUnMuteBtn.bringToFront();
        }

        //ImageView for Closing Special Incoming Call

        mSpecialCallMutUnMuteBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Log.i(TAG, "mute and Umute by click");
                if (isMuted) {  // in versions of KITKAT and lower , we start in muted mode on the music stream , because we don't know when answering happens and we should stop it.
                    Log.i(TAG, "UNMUTE by button");
                    volumeChangeByMCButtons = true;
                    mAudioManager.setStreamMute(AudioManager.STREAM_MUSIC, false); // TODO Rony : Replace Deprecated !! Check All places
                    mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, mVolumeBeforeMute, 0);
                    isMuted = false;

                    mSpecialCallMutUnMuteBtn.setImageResource(R.drawable.unmute);//TODO : setImageResource need to be replaced ? memory issue ?
                    mSpecialCallMutUnMuteBtn.bringToFront();

                } else {
                    Log.i(TAG, "MUTE by button");
                    volumeChangeByMCButtons = true;
                    mVolumeBeforeMute = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
                    mAudioManager.setStreamMute(AudioManager.STREAM_MUSIC, true); // TODO Rony : Replace Deprecated !! Check All places
                    isMuted = true;

                    mSpecialCallMutUnMuteBtn.setImageResource(R.drawable.mute);//TODO : setImageResource need to be replaced ? memory issue ?
                    mSpecialCallMutUnMuteBtn.bringToFront();

                }
            }
        });



    }

    private void prepareVolumeBtn()
    {
        Log.i(TAG, "Preparing Volume Button");

        mSpecialCallVolumeDownBtn = (ImageView) mcButtonsOverlay.findViewById(R.id.volume_down);
        mSpecialCallVolumeUpBtn = (ImageView) mcButtonsOverlay.findViewById(R.id.volume_up);

        //ImageView for volume down Special Incoming Call
          mSpecialCallVolumeDownBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

                volumeChangeByMCButtons = true;
                mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC) - 1, 0); // decrease volume

            }
        });
        mSpecialCallVolumeDownBtn.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {

                volumeChangeByMCButtons = true;

                mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, 0, 0); // decrease volume

                return true;
            }
        });

        //ImageView for volume up Special Incoming Call
        mSpecialCallVolumeUpBtn.bringToFront();
        mSpecialCallVolumeUpBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                volumeChangeByMCButtons = true;

                mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC) + 1, 0); // increase volume

            }
        });

        mSpecialCallVolumeUpBtn.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {

                volumeChangeByMCButtons = true;

                mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, mAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC), 0); // increase volume

                return true;
            }
        });


    }

    private void prepareBlockButton()
    {
        Log.i(TAG, "Preparing Block Button");
        mSpecialCallBlockBtn = (ImageView) mcButtonsOverlay.findViewById(R.id.block_mc);
        //ImageView for Closing Special Incoming Call
        mSpecialCallBlockBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

                Set<String> blockedNumbers = new HashSet<String>();
                blockedNumbers = MCBlockListUtils.getBlockListFromShared(getApplicationContext());
                blockedNumbers.add(PhoneNumberUtils.toValidPhoneNumber(mIncomingOutgoingNumber));

                MCBlockListUtils.setBlockListFromShared(getApplicationContext(), blockedNumbers);
                UI_Utils.callToast(mIncomingOutgoingNumber + " Is Now MC BLOCKED !!! ",Color.RED ,Toast.LENGTH_SHORT ,getApplicationContext());

                closeSpecialCallWindowWithoutRingtone();
            }
        });
    }

    protected void startMediaSpecialCall(String mediaFilePath, String callNumber) {

        Log.i(TAG, "startMediaSpecialCall SharedPrefUtils mediaFilePath:" + mediaFilePath);

        if(!mediaFilePath.equals("")) {
            try {
                FileManager fm = new FileManager(mediaFilePath);
                prepareViewForSpecialCall(fm.getFileType(), fm.getFileFullPath(), callNumber);
                Intent i = new Intent(this, this.getClass());
                //i.putExtra("id", mID);
                i.setAction(StandOutWindow.ACTION_SHOW);
                startService(i);
            } catch (FileInvalidFormatException |
                    FileExceedsMaxSizeException |
                    FileDoesNotExistException |
                    FileMissingExtensionException e) {
                e.printStackTrace();
            }
        }else if (attachDefaultView){ // // TODO: 19/02/2016  Rony Remove Default View for the first feew months :)

                prepareDefaultViewForSpecialCall(callNumber);

                Intent i = new Intent(this, this.getClass());

                i.setAction(StandOutWindow.ACTION_SHOW);
                startService(i);

        }
        else {
            Log.e(TAG, "Empty media file path! Cannot start special call media");
        }

    }

    protected void closeSpecialCallWindowWithoutRingtone() {

        Log.i(TAG, "closeSpecialCallWindowWithoutRingtone():");
        if  (mInRingingSession) {
            stopSound();

            if (!windowCloseActionWasMade) {
                Intent i = new Intent(this, this.getClass());
                i.setAction(StandOutWindow.ACTION_CLOSE);
                startService(i);
                windowCloseActionWasMade=true;
            }
        }
    }

    protected void startAudioSpecialCall(String audioFilePath) {

        try {
            new FileManager(audioFilePath);

            Log.i(TAG, "Ringtone before playing sound. audioFilePath: " + audioFilePath + " URI: " + Uri.parse(audioFilePath).toString());
            playSound(getApplicationContext(), Uri.parse(audioFilePath));

        } catch (FileMissingExtensionException |
                FileDoesNotExistException      |
                FileExceedsMaxSizeException    |
                FileInvalidFormatException e) {
            e.printStackTrace();
        }
    }

    /**
     * Subclasses must implement the way they play sound during ringing
     * @param context
     * @param uri The URI of the sound file
     */
    protected abstract void playSound(Context context, Uri uri);

    protected void stopSound() {

        Log.i(TAG, "Stop ringtone sound");

        try
        {
            if(mMediaPlayer!=null) {
                Log.i(TAG, "mMediaPlayer="+mMediaPlayer);
                mMediaPlayer.stop();
            }
        } catch(Exception e) {
            e.printStackTrace();
            Log.e(TAG, "Failed to Stop sound. Exception:" + e.getMessage());
        }
    }
    //endregion
}
