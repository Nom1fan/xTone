package com.services;

import android.content.Context;
import android.content.Intent;
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
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.VideoView;

import com.special.app.R;
import com.utils.BitmapUtils;
import com.utils.ContactsUtils;

import java.io.File;

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
    protected RelativeLayout mRelativeLayout;
    protected boolean mInRingingSession = false;
    protected MediaPlayer mMediaPlayer;
    protected boolean windowCloseActionWasMade = true;
    protected boolean attachDefaultView = false;
    protected View mSpecialCallView;
    protected AudioManager mAudioManager;
    protected CallStateListener mPhoneListener;
    protected OnVideoPreparedListener mVideoPreparedListener;


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

        prepareCallStateListener();
        prepareStandOutWindowDisplay();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.e(TAG, "Service onDestroy");
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
        frame.setBackgroundColor(Color.BLACK);
        windowCloseActionWasMade = false;
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
        Log.i(TAG, "Preparing close Button");

        //ImageView for Closing Special Incoming Call
        mSpecialCallCloseBtn = new ImageView(this);
        RelativeLayout.LayoutParams lp1 = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.WRAP_CONTENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT);
        lp1.addRule(RelativeLayout.ALIGN_PARENT_TOP);
        lp1.addRule(RelativeLayout.ALIGN_TOP);
        lp1.addRule(RelativeLayout.ALIGN_RIGHT);
        lp1.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
        mSpecialCallCloseBtn.setImageResource(R.drawable.close);
        mSpecialCallCloseBtn.setBackgroundColor(Color.BLACK);
        mSpecialCallCloseBtn.setLayoutParams(lp1);
        mSpecialCallCloseBtn.setClickable(true);
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
        Log.i(TAG, "Video uri="+uri);

        mSpecialCallView = new VideoView(this);
        mSpecialCallView.setLayoutParams(new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT,
                RelativeLayout.LayoutParams.MATCH_PARENT));

        RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) mSpecialCallView.getLayoutParams();

        params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
        params.addRule(RelativeLayout.ALIGN_PARENT_TOP);
        params.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
        params.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);

        mSpecialCallView.setLayoutParams(params);
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
        prepareCloseBtn();

        mSpecialCallView = new ImageView(this);
        mSpecialCallView.setLayoutParams(new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT));
        //     ((ImageView)mSpecialCallView).setScaleType(ImageView.ScaleType.FIT_XY); STRECTH IMAGE ON FULL SCREEN <<< NOT SURE IT's GOOD !!!!!
        ((ImageView) mSpecialCallView).setScaleType(ImageView.ScaleType.FIT_CENTER); // <<  just place the image Center of Window and fit it with ratio


         ((ImageView) mSpecialCallView).setImageResource(R.drawable.color_mc);


        mRelativeLayout.addView(mSpecialCallView);
        mRelativeLayout.addView(mSpecialCallTextView);
        mRelativeLayout.addView(mSpecialCallCloseBtn);
    }

    protected void prepareViewForSpecialCall(FileManager.FileType fileType , String mediaFilePath, String callNumber) {
        Log.i(TAG, "Preparing SpecialCall view");

        // Attempting to induce garbage collection
        System.gc();

        prepareRelativeLayout();
        prepareCallNumberTextView(callNumber);
        prepareCloseBtn();

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
        mRelativeLayout.addView(mSpecialCallCloseBtn);
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
        }else if (attachDefaultView){

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
