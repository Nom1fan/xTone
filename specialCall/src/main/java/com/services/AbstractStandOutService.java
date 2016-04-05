package com.services;

import android.app.AlarmManager;
import android.app.PendingIntent;
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
import android.util.AttributeSet;
import android.util.Log;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Toast;
import android.widget.VideoView;

import com.mediacallz.app.R;
import com.receivers.StartStandOutServicesFallBackReceiver;
import com.utils.BitmapUtils;
import com.utils.ContactsUtils;
import com.utils.MCBlockListUtils;
import com.utils.SharedPrefUtils;
import com.utils.UI_Utils;

import java.io.File;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import Exceptions.FileDoesNotExistException;
import Exceptions.FileExceedsMaxSizeException;
import Exceptions.FileInvalidFormatException;
import Exceptions.FileMissingExtensionException;
import FilesManager.FileManager;
import utils.PhoneNumberUtils;
import wei.mark.standout.StandOutWindow;
import wei.mark.standout.constants.StandOutFlags;
import wei.mark.standout.ui.Window;

public abstract class AbstractStandOutService extends StandOutWindow {

    public static final String ACTION_STOP_RING = "com.services.AbstractStandOutService.ACTION_STOP_RING";
    public static final String ACTION_START = "com.services.AbstractStandOutService.ACTION_START";
    public static final String ACTION_PREVIEW = "com.services.AbstractStandOutService.ACTION_PREVIEW";

    public static final String PREVIEW_VISUAL_MEDIA = "com.services.AbstractStandOutService.VisualMediaPreview";
    public static final String PREVIEW_AUDIO = "com.services.AbstractStandOutService.AudioPreview";
    protected boolean isHidden = false;
    protected String TAG;
    protected int mWidth;
    protected int mHeight;
    private int statusBarHeighet;
    protected ImageView mSpecialCallMutUnMuteBtn;
    protected ImageView mSpecialCallVolumeUpBtn;
    protected ImageView mSpecialCallVolumeDownBtn;
    protected ImageView mSpecialCallBlockBtn;
    protected RelativeLayout mRelativeLayout;
    protected View mcButtonsOverlay;
    protected boolean isMuted = false;
    protected MediaPlayer mMediaPlayer;
    protected boolean windowCloseActionWasMade = true;
    protected View mSpecialCallView;
    protected AudioManager mAudioManager;
    protected CallStateListener mPhoneListener;
    protected OnVideoPreparedListener mVideoPreparedListener;
    protected boolean volumeChangeByMCButtons = false;
    protected int mVolumeBeforeMute = 0;
    protected String mIncomingOutgoingNumber = "";
    protected String mContactTitleOnWindow = "";
    private static final long REPEAT_TIME = 60000;
    private static final String alarmActionIntent = "com.android.mediacallz.ALARM_ACTION";
    protected boolean mPreviewStart = false;
    protected boolean showFirstTime = false;
    public AbstractStandOutService(String TAG) {
        this.TAG = TAG;
    }

    //region Service methods

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);

        android.os.Process.setThreadPriority(-20); // TODO Is this good or not ?
        prepareCallStateListener();
        prepareStandOutWindowDisplay();

        return START_STICKY;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        statusBarHeighet = getStatusBarHeight();

        Log.i(TAG, "Service onCreate");
    }

    @Override
    public void onDestroy() {
        Log.e(TAG, "Service onDestroy");
        super.onDestroy();

    }

    @Override
    public void onLowMemory() {
        Log.e(TAG, "Service onLowMemory");
    }

    @Override
    public void onTrimMemory(int level) {
        Log.e(TAG, "Service onTrimMemory Level: " + String.valueOf(level));
    }

    @Override
    public IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException("Not yet implemented");
    }
    //endregion

    //region Standout Window methods
    @Override
    public String getAppName() {
        Log.i(TAG, "getAppName");
        return mContactTitleOnWindow;
    }

    @Override
    public int getAppIcon() {
        return R.drawable.color_mc;
    }

    @Override
    public void createAndAttachView(int id, FrameLayout frame) {

        Log.i(TAG, "In createAndAttachView()");

        if (mRelativeLayout != null)
            frame.addView(mRelativeLayout);

        LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
        mcButtonsOverlay = inflater.inflate(R.layout.mc_buttons_overlay, null);
        prepareMCButtonsOnRelativeLayoutOverlay();

        frame.addView(mcButtonsOverlay);

        frame.setBackgroundColor(Color.BLACK);
        windowCloseActionWasMade = false;
    }

    private void prepareMCButtonsOnRelativeLayoutOverlay() {

        if (!SharedPrefUtils.getBoolean(getApplicationContext(), SharedPrefUtils.SERVICES, SharedPrefUtils.DISABLE_VOLUME_BUTTONS)) {
            prepareMuteBtn();
            prepareVolumeBtn();
        }
        else
            disableVolumeButtons();

        prepareBlockButton();
    }

    @Override
    public StandOutLayoutParams getParams(int id, Window window) {
        Log.i(TAG, "In StandOutLayoutParams()");
        return new StandOutLayoutParams(id, mWidth, mHeight, 0, 0);
    }

    @Override
    public boolean onShow(int id, Window window) {
        Log.i(TAG, "onShow");
        if (isHidden) {
            verifyAudioManager();
            mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, mVolumeBeforeMute, 0);
            isHidden = false;
        }

    if (showFirstTime) {
        window.edit().setPosition(SharedPrefUtils.getInt(getApplicationContext(), SharedPrefUtils.SERVICES, SharedPrefUtils.MC_WINDOW_X_LOCATION_BY_USER),
                SharedPrefUtils.getInt(getApplicationContext(), SharedPrefUtils.SERVICES, SharedPrefUtils.MC_WINDOW_Y_LOCATION_BY_USER)).commit();

        showFirstTime = false;
    }

        return false;
    }

    @Override
    public boolean onHide(int id, Window window) {
        Log.i(TAG, "onHide");
        verifyAudioManager();
        mVolumeBeforeMute = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, 0, 0);
        isHidden = true;
        return false;
    }

    @Override
    public int getFlags(int id) {
        Log.i(TAG, "getFlags");

        // These flags enable:
        // The system window decorations, to drag the window,
        // the ability to hide windows, and to tap the window to bring to front
        return StandOutFlags.FLAG_DECORATION_SYSTEM
                | StandOutFlags.FLAG_BODY_MOVE_ENABLE
                | StandOutFlags.FLAG_WINDOW_HIDE_ENABLE
                // | StandOutFlags.FLAG_WINDOW_BRING_TO_FRONT_ON_TAP
                | StandOutFlags.FLAG_WINDOW_EDGE_LIMITS_ENABLE
                | StandOutFlags.FLAG_WINDOW_PINCH_RESIZE_ENABLE;
    }

    @Override
    public Intent getHiddenNotificationIntent(int id) {
        Log.i(TAG, "getHiddenNotificationIntent");
        // Return an Intent that restores the MultiWindow
        return StandOutWindow.getShowIntent(this, getClass(), id);
    }

    @Override
    public boolean onUpdate(int id, Window window, StandOutLayoutParams params) {

        Log.i(TAG, "onUpdate");
        try {
            ((VideoViewCustom) mSpecialCallView).setDimensions(window.getHeight(), window.getWidth());
            ((VideoViewCustom) mSpecialCallView).getHolder().setFixedSize(window.getHeight(), window.getWidth());
            ((VideoViewCustom) mSpecialCallView).postInvalidate(); // TODO Rony maybe not needed invalidate
        } catch (Exception e) {
            Log.i(TAG, "can't onUpdate mSpecialCallView video, i guess it's not a video");
        }
        return false;
    }

    @Override
    public boolean onClose(int id, Window window) {
        Log.i(TAG, "onClose");
        showFirstTime = false;
        int coordinates[] = new int[2];
        window.getLocationOnScreen(coordinates);

        // get size of window set last by user
        SharedPrefUtils.setInt(getApplicationContext(), SharedPrefUtils.SERVICES, SharedPrefUtils.MC_WINDOW_HEIGHET_BY_USER, window.getHeight());
        SharedPrefUtils.setInt(getApplicationContext(), SharedPrefUtils.SERVICES, SharedPrefUtils.MC_WINDOW_WIDTH_BY_USER, window.getWidth());

        // get location of window on screen set last by user
        SharedPrefUtils.setInt(getApplicationContext(), SharedPrefUtils.SERVICES, SharedPrefUtils.MC_WINDOW_X_LOCATION_BY_USER, coordinates[0]);
        SharedPrefUtils.setInt(getApplicationContext(), SharedPrefUtils.SERVICES, SharedPrefUtils.MC_WINDOW_Y_LOCATION_BY_USER, coordinates[1] - statusBarHeighet); // remove the Heighet of the status bar

        Log.i(TAG, "Heighet: " + String.valueOf(window.getHeight()) + "Width: " + String.valueOf(window.getWidth()) + "X: " + String.valueOf(coordinates[0]) + "Y: " + String.valueOf(coordinates[1] - statusBarHeighet));

        stopSound();

        return false;
    }
    //endregion

    //region Methods for UI init
    private void prepareStandOutWindowDisplay() {
        Display display = mWindowManager.getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        mWidth = size.x;
        mHeight = size.y * 63 / 100;

        showFirstTime = true;

        if (SharedPrefUtils.getInt(getApplicationContext(), SharedPrefUtils.SERVICES, SharedPrefUtils.MC_WINDOW_WIDTH_BY_USER) > 0)
                mWidth = SharedPrefUtils.getInt(getApplicationContext(),SharedPrefUtils.SERVICES, SharedPrefUtils.MC_WINDOW_WIDTH_BY_USER);

        if (SharedPrefUtils.getInt(getApplicationContext(), SharedPrefUtils.SERVICES, SharedPrefUtils.MC_WINDOW_HEIGHET_BY_USER) > 0)
              mHeight = SharedPrefUtils.getInt(getApplicationContext(),SharedPrefUtils.SERVICES, SharedPrefUtils.MC_WINDOW_HEIGHET_BY_USER);


    }

    private void prepareRelativeLayout() {
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

    private void prepareImageView(String mediaFilePath) {
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

    private void prepareVideoView(String mediaFilePath) {
        Log.i(TAG, "Preparing VideoView");
        // VideoView on Relative Layout
        final File root = new File(mediaFilePath);
        SharedPrefUtils.setBoolean(getApplicationContext(),SharedPrefUtils.SERVICES,SharedPrefUtils.DISABLE_VOLUME_BUTTONS,false);
        Uri uri = Uri.fromFile(root);
        Log.i(TAG, "Video uri=" + uri);

        mSpecialCallView = new VideoViewCustom(this);
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
        ((VideoViewCustom) mSpecialCallView).setVideoURI(uri);
        ((VideoViewCustom) mSpecialCallView).requestFocus();

        // Once the VideoView is prepared, the prepared listener will activate
        ((VideoViewCustom) mSpecialCallView).setOnPreparedListener(mVideoPreparedListener);
    }

    // Default view for outgoing call when there is no image or video
    protected void prepareDefaultViewForSpecialCall(String callNumber) {
        Log.i(TAG, "Preparing SpecialCall view");

        // Attempting to induce garbage collection
        System.gc();

        prepareRelativeLayout();

        mSpecialCallView = new ImageView(this);
        mSpecialCallView.setLayoutParams(new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT));
        //     ((ImageView)mSpecialCallView).setScaleType(ImageView.ScaleType.FIT_XY); STRECTH IMAGE ON FULL SCREEN <<< NOT SURE IT's GOOD !!!!!
        ((ImageView) mSpecialCallView).setScaleType(ImageView.ScaleType.FIT_CENTER); // <<  just place the image Center of Window and fit it with ratio


        ((ImageView) mSpecialCallView).setImageResource(R.drawable.color_mc);

        mRelativeLayout.addView(mSpecialCallView);
    }

    protected void prepareViewForSpecialCall(FileManager.FileType fileType, String mediaFilePath) {
        Log.i(TAG, "Preparing SpecialCall view");

        // Attempting to induce garbage collection
        System.gc();

        prepareRelativeLayout();

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
    }
    private void disableVolumeButtons() {
        Log.i(TAG, "Preparing disableVolumeButtons");

        mSpecialCallMutUnMuteBtn = (ImageView) mcButtonsOverlay.findViewById(R.id.mc_mute_unmute);
        mSpecialCallVolumeDownBtn = (ImageView) mcButtonsOverlay.findViewById(R.id.volume_down);
        mSpecialCallVolumeUpBtn = (ImageView) mcButtonsOverlay.findViewById(R.id.volume_up);

        mSpecialCallMutUnMuteBtn.setVisibility(View.INVISIBLE);
        mSpecialCallVolumeDownBtn.setVisibility(View.INVISIBLE);
        mSpecialCallVolumeUpBtn.setVisibility(View.INVISIBLE);

    }
    private void prepareMuteBtn() {
        Log.i(TAG, "Preparing Mute Button");

        //TODO check if Togglebutton for imageView good also?
        mSpecialCallMutUnMuteBtn = (ImageView) mcButtonsOverlay.findViewById(R.id.mc_mute_unmute);
        verifyAudioManager();
        if (mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC) == 0) {
            mSpecialCallMutUnMuteBtn.setImageResource(R.drawable.mute);//TODO : setImageResource need to be replaced ? memory issue ?
            mSpecialCallMutUnMuteBtn.bringToFront();
        } else {
            mSpecialCallMutUnMuteBtn.setImageResource(R.drawable.unmute);//TODO : setImageResource need to be replaced ? memory issue ?
            mSpecialCallMutUnMuteBtn.bringToFront();
        }


        mSpecialCallMutUnMuteBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Log.i(TAG, "mute and Umute by click");
                if (isMuted) {  // in versions of KITKAT and lower , we start in muted mode on the music stream , because we don't know when answering happens and we should stop it.
                    Log.i(TAG, "UNMUTE by button");
                    volumeChangeByMCButtons = true;

                    verifyAudioManager();

                    mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, mVolumeBeforeMute, 0);
                    isMuted = false;

                    mSpecialCallMutUnMuteBtn.setImageResource(R.drawable.unmute);//TODO : setImageResource need to be replaced ? memory issue ?
                    mSpecialCallMutUnMuteBtn.bringToFront();

                } else {
                    Log.i(TAG, "MUTE by button");
                    volumeChangeByMCButtons = true;
                    verifyAudioManager();
                    mVolumeBeforeMute = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
                    mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, 0, 0);
                    isMuted = true;

                    mSpecialCallMutUnMuteBtn.setImageResource(R.drawable.mute);//TODO : setImageResource need to be replaced ? memory issue ?
                    mSpecialCallMutUnMuteBtn.bringToFront();

                }
            }
        });


    }

    private void prepareVolumeBtn() {
        Log.i(TAG, "Preparing Volume Button");

        mSpecialCallVolumeDownBtn = (ImageView) mcButtonsOverlay.findViewById(R.id.volume_down);
        mSpecialCallVolumeUpBtn = (ImageView) mcButtonsOverlay.findViewById(R.id.volume_up);

        //ImageView for volume down Special Incoming Call
        mSpecialCallVolumeDownBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

                volumeChangeByMCButtons = true;
                verifyAudioManager();
                mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC) - 1, 0); // decrease volume

            }
        });
        mSpecialCallVolumeDownBtn.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {

                volumeChangeByMCButtons = true;

                verifyAudioManager();
                mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, 0, 0); // decrease volume

                return true;
            }
        });

        //ImageView for volume up Special Incoming Call
        mSpecialCallVolumeUpBtn.bringToFront();
        mSpecialCallVolumeUpBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                volumeChangeByMCButtons = true;

                verifyAudioManager();
                mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC) + 1, 0); // increase volume

            }
        });

        mSpecialCallVolumeUpBtn.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {

                volumeChangeByMCButtons = true;

                verifyAudioManager();
                mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, mAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC), 0); // increase volume

                return true;
            }
        });


    }

    private void prepareBlockButton() {
        Log.i(TAG, "Preparing Block Button");
        mSpecialCallBlockBtn = (ImageView) mcButtonsOverlay.findViewById(R.id.block_mc);
        //ImageView for Closing Special Incoming Call
        mSpecialCallBlockBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

                Set<String> blockedNumbers = new HashSet<String>();
                blockedNumbers = MCBlockListUtils.getBlockListFromShared(getApplicationContext());
                blockedNumbers.add(PhoneNumberUtils.toValidLocalPhoneNumber(mIncomingOutgoingNumber));

                MCBlockListUtils.setBlockListFromShared(getApplicationContext(), blockedNumbers);
                UI_Utils.callToast(mIncomingOutgoingNumber + " Is Now MC BLOCKED !!! ", Color.RED, Toast.LENGTH_SHORT, getApplicationContext());

                closeSpecialCallWindowWithoutRingtone();
            }
        });
    }
    //endregion

    //region Hooks for subclasses
    /**
     * Subclasses should implement this method and decide what to do according to call state
     *
     * @param state          The call state
     * @param incomingNumber The incoming number in case of an incoming call. Otherwise (outgoing call), null.
     */
    protected abstract void syncOnCallStateChange(int state, String incomingNumber);

    /**
     * Subclasses must implement the way they play sound during ringing
     *
     * @param context
     * @param uri     The URI of the sound file
     */
    protected abstract void playSound(Context context, Uri uri);
    //endregion

    //region Telephony methods
    private void prepareCallStateListener() {
        if (mPhoneListener == null) {
            mPhoneListener = new CallStateListener();
            TelephonyManager tm = (TelephonyManager) getApplicationContext().getSystemService(Context.TELEPHONY_SERVICE);
            tm.listen(mPhoneListener, PhoneStateListener.LISTEN_CALL_STATE);
        }
    }

    protected boolean isRingingSession(String directionOfSession) {

        return SharedPrefUtils.getBoolean(getApplicationContext(), SharedPrefUtils.SERVICES, directionOfSession);

    }

    protected void setRingingSession(String directionOfSession, boolean inSession) {

        SharedPrefUtils.setBoolean(getApplicationContext(), SharedPrefUtils.SERVICES, directionOfSession, inSession);

    }

    protected void backupMusicVolume() {

        verifyAudioManager();
        Log.i(TAG, "mRingVolume Original" + SharedPrefUtils.getInt(getApplicationContext(), SharedPrefUtils.SERVICES, SharedPrefUtils.RING_VOLUME));
        SharedPrefUtils.setInt(getApplicationContext(), SharedPrefUtils.SERVICES, SharedPrefUtils.MUSIC_VOLUME, mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC));
    }

    protected void backupRingVolume() {

        verifyAudioManager();
        int ringVolume = mAudioManager.getStreamVolume(AudioManager.STREAM_RING);
        SharedPrefUtils.setInt(getApplicationContext(), SharedPrefUtils.SERVICES, SharedPrefUtils.RING_VOLUME, ringVolume);
        Log.i(TAG, "mRingVolume Original=" + ringVolume);
    }

    protected void verifyAudioManager() {

        if (mAudioManager == null)
        {
            Log.i(TAG , "Audio manager was null , re-instantiated");
            mAudioManager = (AudioManager) getApplicationContext().getSystemService(Context.AUDIO_SERVICE);
        }
    }
    //endregion

    //region MC actions methods
    /**
     * Starts standout window for MC, showing visual media (video/image) or default in case of audio only
     *
     * @param visualMediaFilePath The file path to the media
     * @param callNumber          The incoming/outgoing number the MC is triggered for
     * @return true if visual MC was started, false otherwise
     */
    protected void startVisualMediaMC(String visualMediaFilePath, String callNumber, boolean attachDefaultView) {

        Log.i(TAG, "startVisualMediaMC SharedPrefUtils visualMediaFilePath:" + visualMediaFilePath);
    if (attachDefaultView)
        SharedPrefUtils.setBoolean(getApplicationContext(),SharedPrefUtils.SERVICES,SharedPrefUtils.DISABLE_VOLUME_BUTTONS,false);
    else
        SharedPrefUtils.setBoolean(getApplicationContext(),SharedPrefUtils.SERVICES,SharedPrefUtils.DISABLE_VOLUME_BUTTONS,true);

        String contactName = ContactsUtils.getContactName(getApplicationContext(), callNumber);
        mContactTitleOnWindow = (!contactName.equals("") ? contactName + " " + callNumber : callNumber);
        Random r = new Random();
        int randomWindowId = r.nextInt(Integer.MAX_VALUE);  // fixing a bug: when the same ID the window isn't released good enough so we need to make a different window in the mean time

        if (new File(visualMediaFilePath).exists()) {
            try {
                FileManager fm = new FileManager(visualMediaFilePath);
                prepareViewForSpecialCall(fm.getFileType(), fm.getFileFullPath());
                Intent i = new Intent(this, this.getClass());
                i.putExtra("id", randomWindowId);
                i.setAction(StandOutWindow.ACTION_SHOW);

                startService(i);
            } catch (FileInvalidFormatException |
                    FileExceedsMaxSizeException |
                    FileDoesNotExistException |
                    FileMissingExtensionException e) {
                e.printStackTrace();
            }
            // Only audio MC - Using default mc view
        } else if (attachDefaultView) { // // TODO: 19/02/2016  Rony Remove Default View for the first few months :)

            prepareDefaultViewForSpecialCall(callNumber);

            Intent i = new Intent(this, this.getClass());
            i.putExtra("id", randomWindowId);
            i.setAction(StandOutWindow.ACTION_SHOW);
            startService(i);

        } else {
            Log.e(TAG, "Empty media file path! Cannot start special call media");
        }
    }

    protected void closeSpecialCallWindowWithoutRingtone() {

        Log.i(TAG, "closeSpecialCallWindowWithoutRingtone():");
        if (isRingingSession(SharedPrefUtils.OUTGOING_RINGING_SESSION) || isRingingSession(SharedPrefUtils.INCOMING_RINGING_SESSION)) {
            stopSound();

            Intent i = new Intent(this, this.getClass());
            i.setAction(StandOutWindow.ACTION_CLOSE_ALL);
            Log.i(TAG, "StandOutWindow.ACTION_CLOSE_ALL");
            startService(i);
        }
    }

    protected void startAudioSpecialCall(String audioFilePath) {

        try {

            FileManager audioFile = new FileManager(audioFilePath);

            if(audioFile.doesFileExist()) {
                SharedPrefUtils.setBoolean(getApplicationContext(),SharedPrefUtils.SERVICES,SharedPrefUtils.DISABLE_VOLUME_BUTTONS,false);
                Log.i(TAG, "Ringtone before playing sound. audioFilePath: " + audioFilePath + " URI: " + Uri.parse(audioFilePath).toString());
                playSound(getApplicationContext(), Uri.parse(audioFilePath));
            }else
            {
                SharedPrefUtils.setBoolean(getApplicationContext(),SharedPrefUtils.SERVICES,SharedPrefUtils.DISABLE_VOLUME_BUTTONS,true);
            }

        } catch (FileMissingExtensionException |
                FileDoesNotExistException |
                FileExceedsMaxSizeException |
                FileInvalidFormatException e) {
            e.printStackTrace();
        }
    }

    protected void stopSound() {

        Log.i(TAG, "Stop ringtone sound");

        try {
            if (mMediaPlayer != null) {
                Log.i(TAG, "mMediaPlayer=" + mMediaPlayer);
                mMediaPlayer.stop();
                mMediaPlayer.release();
            } else
                Log.i(TAG, "mMediaPlayer Fucking Null WTF !!!!");
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, "Failed to Stop sound. Exception:" + (e.getMessage() != null ? e.getMessage() : e));
        }
    }
    //endregion

    //region Helper methods
    public int getStatusBarHeight() {
        int result = 0;
        int resourceId = getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            result = getResources().getDimensionPixelSize(resourceId);
        }
        return result;
    }

    protected void removeTempMd5ForCallRecord() {

        SharedPrefUtils.remove(getApplicationContext(), SharedPrefUtils.SERVICES, SharedPrefUtils.TEMP_VISUALMD5);
        SharedPrefUtils.remove(getApplicationContext(), SharedPrefUtils.SERVICES, SharedPrefUtils.TEMP_AUDIOMD5);
    }

    protected void setTempMd5ForCallRecord(String visualFilePath, String audioFilePath) {

        if(new File(audioFilePath).exists())
            SharedPrefUtils.setString(getApplicationContext(), SharedPrefUtils.SERVICES ,SharedPrefUtils.TEMP_AUDIOMD5, FileManager.getMD5(audioFilePath));

        if(new File(visualFilePath).exists())
            SharedPrefUtils.setString(getApplicationContext(), SharedPrefUtils.SERVICES, SharedPrefUtils.TEMP_VISUALMD5, FileManager.getMD5(visualFilePath));

    }


    public void setAlarm(Context context)
    {
        AlarmManager service = (AlarmManager) context
                .getSystemService(Context.ALARM_SERVICE);
        Intent i = new Intent(context, StartStandOutServicesFallBackReceiver.class);
        i.setAction(alarmActionIntent);
        PendingIntent pending = PendingIntent.getBroadcast(context, 0, i,
                PendingIntent.FLAG_CANCEL_CURRENT);


        Calendar cal = Calendar.getInstance();
        // Start 30 seconds after boot completed
        cal.add(Calendar.SECOND, 30);
        //
        // Fetch every 30 seconds
        // InexactRepeating allows Android to optimize the energy consumption
        service.setInexactRepeating(AlarmManager.RTC_WAKEUP,
                cal.getTimeInMillis(), REPEAT_TIME, pending);
    }

/*    public void CancelAlarm(Context context)
    {
        Intent intent = new Intent(context, Alarm.class);
        PendingIntent sender = PendingIntent.getBroadcast(context, 0, intent, 0);
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        alarmManager.cancel(sender);
    }*/
    //endregion

    //region Private classes and listeners
    protected void releaseResources() {

        mMediaPlayer.release();
        mMediaPlayer = null;
        mAudioManager = null;
        isMuted = false;
        removeTempMd5ForCallRecord();
        mPreviewStart = false;
        windowCloseActionWasMade = true;
        volumeChangeByMCButtons = false;
        mVolumeBeforeMute = 0;
        isHidden = false;
        SharedPrefUtils.setBoolean(getApplicationContext(),SharedPrefUtils.SERVICES,SharedPrefUtils.DISABLE_VOLUME_BUTTONS,false);
        //mPhoneListener = null;
        // TODO Release more Resources

    }

    /**
     * Listener for call states
     * Listens for different call states
     */
    private class CallStateListener extends PhoneStateListener {

        @Override
        public void onCallStateChanged(int state, String incomingNumber) {

            try {
                syncOnCallStateChange(state, incomingNumber);
            } catch (Exception e) {
                Log.e(TAG, "Closing" + TAG + " MC StandooutWindow failed. Exception:" + (e.getMessage() != null ? e.getMessage() : e));
            }
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

    private class VideoViewCustom extends VideoView {

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
            Log.i(TAG, "onMeasure");

            setMeasuredDimension(mForceWidth, mForceHeight);
        }
    }
    //endregion
}
