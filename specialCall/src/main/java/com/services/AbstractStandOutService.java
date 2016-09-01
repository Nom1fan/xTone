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
import android.os.Vibrator;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.MediaController;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import com.crashlytics.android.Crashlytics;
import com.mediacallz.app.R;
import com.receivers.StartStandOutServicesFallBackReceiver;
import com.utils.BitmapUtils;
import com.utils.ContactsUtils;
import com.utils.MCBlockListUtils;
import com.utils.SharedPrefUtils;
import com.utils.UI_Utils;

import java.io.File;
import java.io.IOException;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import Exceptions.FileDoesNotExistException;
import Exceptions.FileExceedsMaxSizeException;
import Exceptions.FileInvalidFormatException;
import Exceptions.FileMissingExtensionException;
import FilesManager.FileManager;
import pl.droidsonroids.gif.GifDrawable;
import pl.droidsonroids.gif.GifImageView;
import utils.PhoneNumberUtils;
import wei.mark.standout.StandOutWindow;
import wei.mark.standout.constants.StandOutFlags;
import wei.mark.standout.ui.Window;

import static com.crashlytics.android.Crashlytics.*;

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
    protected TextView mSpecialCallVolumeValueTextView;
    protected ImageView mSpecialCallBlockBtn;
    protected RelativeLayout mRelativeLayout;
    protected View mcButtonsOverlay;
    protected boolean isMuted = false;
    protected MediaPlayer mMediaPlayer;
    protected boolean windowCloseActionWasMade = true;
    protected View mSpecialCallView;
    protected GifDrawable mGifDrawable;
    protected AudioManager mAudioManager;
    protected AudioManager mPreviewAudioManager;
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
    protected boolean mOutgoingCall = false;
    protected Vibrator vibrator;
    protected String _contactName = "";
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

        log(Log.INFO,TAG, "Service onCreate");
    }

    @Override
    public void onDestroy() {
        Crashlytics.log(Log.ERROR,TAG, "Service onDestroy");
        super.onDestroy();

    }

    @Override
    public void onLowMemory() {
        Crashlytics.log(Log.ERROR,TAG, "Service onLowMemory");
    }

    @Override
    public void onTrimMemory(int level) {
        Crashlytics.log(Log.ERROR,TAG, "Service onTrimMemory Level: " + String.valueOf(level));
    }

    @Override
    public IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException("Not yet implemented");
    }
    //endregion

    //region Standout Window methods
    @Override
    public String getAppName() {
        log(Log.INFO,TAG, "getAppName");
        return mContactTitleOnWindow;
    }

    @Override
    public int getAppIcon() {
        return R.drawable.color_mc;
    }

    @Override
    public void createAndAttachView(int id, FrameLayout frame) {

        log(Log.INFO,TAG, "In createAndAttachView()");

        if (mRelativeLayout != null)
            frame.addView(mRelativeLayout);

        LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
        mcButtonsOverlay = inflater.inflate(R.layout.mc_buttons_overlay, null);

        prepareMCButtonsOnRelativeLayoutOverlay();

        if (SharedPrefUtils.getBoolean(getApplicationContext(), SharedPrefUtils.SERVICES, SharedPrefUtils.ASK_BEFORE_MEDIA_SHOW_FOR_STANDOUT)) {
            ImageView title_bkg = (ImageView) mcButtonsOverlay.findViewById(R.id.name_title_background);
            ImageView volume_bkg = (ImageView) mcButtonsOverlay.findViewById(R.id.volume_background);
            title_bkg.setBackgroundColor(Color.TRANSPARENT);
            volume_bkg.setBackgroundColor(Color.TRANSPARENT);
        }

            frame.addView(mcButtonsOverlay);

        mRelativeLayout.setBackgroundColor(Color.TRANSPARENT);
        windowCloseActionWasMade = false;
    }

    private void prepareMCButtonsOnRelativeLayoutOverlay() {

        if (!SharedPrefUtils.getBoolean(getApplicationContext(), SharedPrefUtils.SERVICES, SharedPrefUtils.DISABLE_VOLUME_BUTTONS)) {

            if (!mPreviewStart)
            {   log(Log.INFO,TAG, "set Volume Buttons ");
                prepareMuteBtn();
                prepareVolumeBtn();}
            else
            {
                log(Log.INFO,TAG, "set Preview Volume Buttons ");
                previewPrepareMuteBtn();
                previewPrepareVolumeBtn();
            }

        }
        else
            disableVolumeButtons();

        if (!mPreviewStart)
            prepareBlockButton();
    }

    @Override
    public StandOutLayoutParams getParams(int id, Window window) {
        log(Log.INFO,TAG, "In StandOutLayoutParams()");

        if (SharedPrefUtils.getInt(getApplicationContext(), SharedPrefUtils.SERVICES, SharedPrefUtils.PREVIEW_MC_WINDOW_WIDTH_BY_USER) > 0 && mPreviewStart)
            mWidth = SharedPrefUtils.getInt(getApplicationContext(),SharedPrefUtils.SERVICES, SharedPrefUtils.PREVIEW_MC_WINDOW_WIDTH_BY_USER);
        if (SharedPrefUtils.getInt(getApplicationContext(), SharedPrefUtils.SERVICES, SharedPrefUtils.PREVIEW_MC_WINDOW_HEIGHET_BY_USER) > 0 && mPreviewStart)
            mHeight = SharedPrefUtils.getInt(getApplicationContext(),SharedPrefUtils.SERVICES, SharedPrefUtils.PREVIEW_MC_WINDOW_HEIGHET_BY_USER);



        if (TAG.contains("OutgoingService")) { // we can't use the shared pref boolean because it's been used and is false becuase of the syncWithBuggyIdleState

            if (SharedPrefUtils.getInt(getApplicationContext(), SharedPrefUtils.SERVICES, SharedPrefUtils.OUTGOING_MC_WINDOW_WIDTH_BY_USER) > 0)
                mWidth = SharedPrefUtils.getInt(getApplicationContext(), SharedPrefUtils.SERVICES, SharedPrefUtils.OUTGOING_MC_WINDOW_WIDTH_BY_USER);

            if (SharedPrefUtils.getInt(getApplicationContext(), SharedPrefUtils.SERVICES, SharedPrefUtils.OUTGOING_MC_WINDOW_HEIGHET_BY_USER) > 0)
                mHeight = SharedPrefUtils.getInt(getApplicationContext(), SharedPrefUtils.SERVICES, SharedPrefUtils.OUTGOING_MC_WINDOW_HEIGHET_BY_USER);
            log(Log.INFO, TAG, "Outgoing Call getParams Heighet: " + mHeight + "Width: " + mWidth);
        }

        if (SharedPrefUtils.getBoolean(getApplicationContext(), SharedPrefUtils.SERVICES, SharedPrefUtils.INCOMING_RINGING_SESSION)) {

            if (SharedPrefUtils.getInt(getApplicationContext(), SharedPrefUtils.SERVICES, SharedPrefUtils.INCOMING_MC_WINDOW_WIDTH_BY_USER) > 0)
                mWidth = SharedPrefUtils.getInt(getApplicationContext(), SharedPrefUtils.SERVICES, SharedPrefUtils.INCOMING_MC_WINDOW_WIDTH_BY_USER);

            if (SharedPrefUtils.getInt(getApplicationContext(), SharedPrefUtils.SERVICES, SharedPrefUtils.INCOMING_MC_WINDOW_HEIGHET_BY_USER) > 0)
                mHeight = SharedPrefUtils.getInt(getApplicationContext(), SharedPrefUtils.SERVICES, SharedPrefUtils.INCOMING_MC_WINDOW_HEIGHET_BY_USER);

            log(Log.INFO, TAG, "Incoming Call getParams Heighet: " + mHeight + "Width: " + mWidth);
        }

        return new StandOutLayoutParams(id, mWidth, mHeight, 0, 0);
    }

    @Override
    public boolean onShow(int id, Window window) {
        log(Log.INFO,TAG, "abstract : onShow");
        try {

            if (!mPreviewStart)
                log(Log.INFO,TAG, "abstract MUSIC_VOLUME Original" + String.valueOf(mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC)));
            else
                log(Log.INFO,TAG, "abstract Preview MUSIC_VOLUME Original" + String.valueOf(mPreviewAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC)));

        } catch (Exception e) {
            log(Log.INFO,TAG, "audiomanager confusion " + e.getMessage());
        }

        if (isHidden) {

            try {
            if (!mPreviewStart) {
                verifyAudioManager();
                mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, mVolumeBeforeMute, 0);
            }else {
                verifyPreviewAudioManager();
                mPreviewAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, mVolumeBeforeMute, 0);
            }
            } catch (Exception e) {
                log(Log.INFO,TAG, "audiomanager confusion " + e.getMessage());
            }

            log(Log.INFO,TAG, "OnHide Mute Volume Now : " + String.valueOf(mVolumeBeforeMute));
            isHidden = false;
        }

        if (showFirstTime) {

          if (mPreviewStart) {
              window.edit().setPosition(SharedPrefUtils.getInt(getApplicationContext(), SharedPrefUtils.SERVICES, SharedPrefUtils.PREVIEW_MC_WINDOW_X_LOCATION_BY_USER),
                      SharedPrefUtils.getInt(getApplicationContext(), SharedPrefUtils.SERVICES, SharedPrefUtils.PREVIEW_MC_WINDOW_Y_LOCATION_BY_USER)).commit();
          }

          if ((SharedPrefUtils.getBoolean(getApplicationContext(), SharedPrefUtils.SERVICES, SharedPrefUtils.OUTGOING_WINDOW_SESSION))) {
              window.edit().setPosition(SharedPrefUtils.getInt(getApplicationContext(), SharedPrefUtils.SERVICES, SharedPrefUtils.OUTGOING_MC_WINDOW_X_LOCATION_BY_USER),
                      SharedPrefUtils.getInt(getApplicationContext(), SharedPrefUtils.SERVICES, SharedPrefUtils.OUTGOING_MC_WINDOW_Y_LOCATION_BY_USER)).commit();
          }

          if ((SharedPrefUtils.getBoolean(getApplicationContext(), SharedPrefUtils.SERVICES, SharedPrefUtils.INCOMING_WINDOW_SESSION))) {
              window.edit().setPosition(SharedPrefUtils.getInt(getApplicationContext(), SharedPrefUtils.SERVICES, SharedPrefUtils.INCOMING_MC_WINDOW_X_LOCATION_BY_USER),
                      SharedPrefUtils.getInt(getApplicationContext(), SharedPrefUtils.SERVICES, SharedPrefUtils.INCOMING_MC_WINDOW_Y_LOCATION_BY_USER)).commit();
          }

            verifyAudioManager();
            mSpecialCallVolumeValueTextView = (TextView) mcButtonsOverlay.findViewById(R.id.volume_value);
            mSpecialCallVolumeValueTextView.setText(Integer.toString(mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC)));


            log(Log.INFO,TAG, "showFirstTime");
            showFirstTime = false;
        }

        return false;
    }

    @Override
    public boolean onHide(int id, Window window) {
        log(Log.INFO,TAG, "onHide");
        stopVibrator();
        verifyAudioManager();
        mVolumeBeforeMute = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, 0, 0);
        isHidden = true;
        return false;
    }

    @Override
    public int getFlags(int id) {
        log(Log.INFO,TAG, "getFlags");


        if (SharedPrefUtils.getBoolean(getApplicationContext(), SharedPrefUtils.SERVICES, SharedPrefUtils.ASK_BEFORE_MEDIA_SHOW_FOR_STANDOUT)){
                return StandOutFlags.FLAG_DECORATION_SYSTEM
                        | StandOutFlags.FLAG_BODY_MOVE_ENABLE
//                        | StandOutFlags.FLAG_DECORATION_RESIZE_DISABLE
                        | StandOutFlags.FLAG_WINDOW_PINCH_RESIZE_ENABLE
                        | StandOutFlags.FLAG_DECORATION_MAXIMIZE_DISABLE;

            }

        // These flags enable:
        // The system window decorations, to drag the window,
        // the ability to hide windows, and to tap the window to bring to front
        return StandOutFlags.FLAG_DECORATION_SYSTEM
                | StandOutFlags.FLAG_BODY_MOVE_ENABLE
                //| StandOutFlags.FLAG_WINDOW_HIDE_ENABLE
                // | StandOutFlags.FLAG_WINDOW_BRING_TO_FRONT_ON_TAP
               //| StandOutFlags.FLAG_WINDOW_EDGE_LIMITS_ENABLE // fixing issue when orientation somehow becomes landscape when incoming call and moving back to portrait limits the window from landscape params and can't be moved because of the edge limits...
                | StandOutFlags.FLAG_WINDOW_PINCH_RESIZE_ENABLE;
    }

    @Override
    public Intent getHiddenNotificationIntent(int id) {
        log(Log.INFO,TAG, "getHiddenNotificationIntent");
        // Return an Intent that restores the MultiWindow
        return StandOutWindow.getShowIntent(this, getClass(), id);
    }

    @Override
    public boolean onUpdate(int id, Window window, StandOutLayoutParams params) {

        //Crashlytics.log(Log.INFO,TAG, "onUpdate");
        try {



            ((VideoViewCustom) mSpecialCallView).setDimensions(window.getHeight(), window.getWidth());
            ((VideoViewCustom) mSpecialCallView).getHolder().setFixedSize(window.getHeight(), window.getWidth());
            ((VideoViewCustom) mSpecialCallView).postInvalidate(); // TODO Rony maybe not needed invalidate
        } catch (Exception e) {
        //    Crashlytics.log(Log.INFO,TAG, "can't onUpdate mSpecialCallView video, i guess it's not a video");
        }
        return false;
    }

    @Override
    public boolean onCloseAll() {
        log(Log.INFO,TAG, "onCloseAll");
       /* if (mPreviewStart) {
            Crashlytics.log(Log.INFO,TAG,"Incase outgoing call is made return volume music to 0");
            verifyAudioManager();
            mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, 0, 0); // setting max volume for music -5 as it's to high volume
        }*/
        stopSound();
        stopVibrator();
      //  releaseResources();
        return false;
    }

    @Override
    public boolean onClose(int id, Window window) {
        log(Log.INFO, TAG, "onClose");
        showFirstTime = false;
        int coordinates[] = new int[2];
        window.getLocationOnScreen(coordinates);

        if (SharedPrefUtils.getBoolean(getApplicationContext(),SharedPrefUtils.SERVICES, SharedPrefUtils.INCOMING_WINDOW_SESSION)
                && !SharedPrefUtils.getBoolean(getApplicationContext(),SharedPrefUtils.SERVICES,SharedPrefUtils.ASK_BEFORE_MEDIA_SHOW_FOR_STANDOUT)) // ignore when it's the AskBeforeShowView
            SharedPrefUtils.setBoolean(getApplicationContext(),SharedPrefUtils.SERVICES, SharedPrefUtils.DONT_BOTHER_INC_CALL_POPUP,true);

        int CurrentWindowHeighet = window.getHeight();
        int CurrentWindowWidth = window.getWidth();

        int CurrentXCoordinate = coordinates[0];
        int CurrentYCoordinate = coordinates[1] - statusBarHeighet;  // remove the Heighet of the status bar


        log(Log.INFO, TAG, "what we receive onClose: Heighet: " + String.valueOf(CurrentWindowHeighet) + "Width: " + String.valueOf(CurrentWindowWidth) + "X: " + String.valueOf(CurrentXCoordinate) + "Y: " + String.valueOf(CurrentYCoordinate));

        if (CurrentYCoordinate <0)
                CurrentYCoordinate = 0;
        if (CurrentXCoordinate < 0)
                CurrentXCoordinate = 0;


        //check that the window wasn't stretched larger than the screen params , because next time it will crash trying to fill the whole window
        if (SharedPrefUtils.getInt(getApplicationContext(), SharedPrefUtils.SERVICES, SharedPrefUtils.DEVICE_SCREEN_HEIGHET) < CurrentWindowHeighet) {

            CurrentWindowHeighet = SharedPrefUtils.getInt(getApplicationContext(), SharedPrefUtils.SERVICES, SharedPrefUtils.DEVICE_SCREEN_HEIGHET) - 1;
            CurrentYCoordinate = 0;
            CurrentXCoordinate = 0;
        }
        //check that the window wasn't stretched larger than the screen params , because next time it will crash trying to fill the whole window
        if (SharedPrefUtils.getInt(getApplicationContext(), SharedPrefUtils.SERVICES, SharedPrefUtils.DEVICE_SCREEN_WIDTH) < CurrentWindowWidth) {

            CurrentWindowWidth = SharedPrefUtils.getInt(getApplicationContext(), SharedPrefUtils.SERVICES, SharedPrefUtils.DEVICE_SCREEN_WIDTH) - 1;
            CurrentYCoordinate = 0;
            CurrentXCoordinate = 0;

        }


            Crashlytics.log(Log.INFO,TAG," Original Device heighet: " + SharedPrefUtils.getInt(getApplicationContext(), SharedPrefUtils.SERVICES, SharedPrefUtils.DEVICE_SCREEN_HEIGHET) + " new height: "+ CurrentWindowHeighet
                    + " Original Device width: " + SharedPrefUtils.getInt(getApplicationContext(), SharedPrefUtils.SERVICES, SharedPrefUtils.DEVICE_SCREEN_WIDTH) + " new width: "+ CurrentWindowWidth);


            if (mPreviewStart) {
                // get size of window set last by user
                SharedPrefUtils.setInt(getApplicationContext(), SharedPrefUtils.SERVICES, SharedPrefUtils.PREVIEW_MC_WINDOW_HEIGHET_BY_USER, CurrentWindowHeighet);
                SharedPrefUtils.setInt(getApplicationContext(), SharedPrefUtils.SERVICES, SharedPrefUtils.PREVIEW_MC_WINDOW_WIDTH_BY_USER, CurrentWindowWidth);

                // get location of window on screen set last by user
                SharedPrefUtils.setInt(getApplicationContext(), SharedPrefUtils.SERVICES, SharedPrefUtils.PREVIEW_MC_WINDOW_X_LOCATION_BY_USER, CurrentXCoordinate);
                SharedPrefUtils.setInt(getApplicationContext(), SharedPrefUtils.SERVICES, SharedPrefUtils.PREVIEW_MC_WINDOW_Y_LOCATION_BY_USER, CurrentYCoordinate);
            }

            if (SharedPrefUtils.getBoolean(getApplicationContext(), SharedPrefUtils.SERVICES, SharedPrefUtils.OUTGOING_WINDOW_SESSION)) {

                // get size of window set last by user
                SharedPrefUtils.setInt(getApplicationContext(), SharedPrefUtils.SERVICES, SharedPrefUtils.OUTGOING_MC_WINDOW_HEIGHET_BY_USER, CurrentWindowHeighet);
                SharedPrefUtils.setInt(getApplicationContext(), SharedPrefUtils.SERVICES, SharedPrefUtils.OUTGOING_MC_WINDOW_WIDTH_BY_USER, CurrentWindowWidth);

                // get location of window on screen set last by user
                SharedPrefUtils.setInt(getApplicationContext(), SharedPrefUtils.SERVICES, SharedPrefUtils.OUTGOING_MC_WINDOW_X_LOCATION_BY_USER, CurrentXCoordinate);
                SharedPrefUtils.setInt(getApplicationContext(), SharedPrefUtils.SERVICES, SharedPrefUtils.OUTGOING_MC_WINDOW_Y_LOCATION_BY_USER, CurrentYCoordinate);
                SharedPrefUtils.setBoolean(getApplicationContext(), SharedPrefUtils.SERVICES, SharedPrefUtils.INCOMING_WINDOW_SESSION, false);
                SharedPrefUtils.setBoolean(getApplicationContext(), SharedPrefUtils.SERVICES, SharedPrefUtils.OUTGOING_WINDOW_SESSION, false);

                log(Log.INFO, TAG, "Outgoing Call Heighet: " + String.valueOf(CurrentWindowHeighet) + "Width: " + String.valueOf(CurrentWindowWidth) + "X: " + String.valueOf(CurrentXCoordinate) + "Y: " + CurrentYCoordinate);
            }

            if (SharedPrefUtils.getBoolean(getApplicationContext(), SharedPrefUtils.SERVICES, SharedPrefUtils.INCOMING_WINDOW_SESSION) /*&&
               !SharedPrefUtils.getBoolean(getApplicationContext(),SharedPrefUtils.SERVICES,SharedPrefUtils.ASK_BEFORE_MEDIA_SHOW_FOR_STANDOUT)*/) { // don't save the size and location of AskBeforeShowView

                // get size of window set last by user
                SharedPrefUtils.setInt(getApplicationContext(), SharedPrefUtils.SERVICES, SharedPrefUtils.INCOMING_MC_WINDOW_HEIGHET_BY_USER, CurrentWindowHeighet);
                SharedPrefUtils.setInt(getApplicationContext(), SharedPrefUtils.SERVICES, SharedPrefUtils.INCOMING_MC_WINDOW_WIDTH_BY_USER, CurrentWindowWidth);

                // get location of window on screen set last by user
                SharedPrefUtils.setInt(getApplicationContext(), SharedPrefUtils.SERVICES, SharedPrefUtils.INCOMING_MC_WINDOW_X_LOCATION_BY_USER, CurrentXCoordinate);
                SharedPrefUtils.setInt(getApplicationContext(), SharedPrefUtils.SERVICES, SharedPrefUtils.INCOMING_MC_WINDOW_Y_LOCATION_BY_USER, CurrentYCoordinate);
                SharedPrefUtils.setBoolean(getApplicationContext(), SharedPrefUtils.SERVICES, SharedPrefUtils.INCOMING_WINDOW_SESSION, false);
                SharedPrefUtils.setBoolean(getApplicationContext(), SharedPrefUtils.SERVICES, SharedPrefUtils.OUTGOING_WINDOW_SESSION, false);

                log(Log.INFO, TAG, "Incoming Call Heighet: " + String.valueOf(CurrentWindowHeighet) + " Width: " + String.valueOf(CurrentWindowWidth) + " X: " + String.valueOf(CurrentXCoordinate) + " Y: " + String.valueOf(CurrentYCoordinate));
            }


        log(Log.INFO, TAG, "Heighet: " + String.valueOf(CurrentWindowHeighet) + "Width: " + String.valueOf(CurrentWindowWidth) + "X: " + String.valueOf(CurrentXCoordinate) + "Y: " + String.valueOf(CurrentYCoordinate));
        SharedPrefUtils.setBoolean(getApplicationContext(),SharedPrefUtils.SERVICES,SharedPrefUtils.ASK_BEFORE_MEDIA_SHOW_FOR_STANDOUT , false);
        stopSound();
        stopVibrator();

        return false;
    }
    //endregion

    //region Methods for UI init
    private void prepareStandOutWindowDisplay() {
        Display display = mWindowManager.getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        mWidth = size.x;
        mHeight = size.y * 55 / 100;

        showFirstTime = true;

    }

    protected void prepareRelativeLayout() {
        log(Log.INFO,TAG, "Preparing RelativeLayout");

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
        log(Log.INFO,TAG, "Preparing ImageView");

        boolean gifEnabled=false;
        String ext="";

        try {
            ext = FileManager.extractExtension(mediaFilePath);
        } catch (FileMissingExtensionException e) {
            e.printStackTrace();
        }

        if (ext.equals("gif"))
            gifEnabled =true;
        else
            gifEnabled=false;


        if (gifEnabled)
        {
            log(Log.INFO,TAG, "GIF Found");
            mSpecialCallView = new GifImageView(this);

            mSpecialCallView.setLayoutParams(new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT,
                    RelativeLayout.LayoutParams.MATCH_PARENT));

            RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) mSpecialCallView.getLayoutParams();

            params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
            params.addRule(RelativeLayout.ALIGN_PARENT_TOP);
            params.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
            params.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);

            try {
                mGifDrawable = new GifDrawable(mediaFilePath);
            } catch (IOException e) {
                e.printStackTrace();
            }

            ((GifImageView)mSpecialCallView).setImageURI(Uri.fromFile(new File(mediaFilePath)));
            final MediaController mc = new MediaController( this );
            mc.setMediaPlayer(mGifDrawable);
            mc.setAnchorView((mSpecialCallView));

            mGifDrawable.start();

            ((GifImageView)mSpecialCallView).setLayoutParams(params);

        }
            else {
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


    }

    private void prepareVideoView(String mediaFilePath) {
        log(Log.INFO,TAG, "Preparing VideoView");
        // VideoView on Relative Layout
        final File root = new File(mediaFilePath);
        SharedPrefUtils.setBoolean(getApplicationContext(),SharedPrefUtils.SERVICES,SharedPrefUtils.DISABLE_VOLUME_BUTTONS,false);
        Uri uri = Uri.fromFile(root);
        log(Log.INFO,TAG, "Video uri=" + uri);

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
        log(Log.INFO,TAG, "Preparing SpecialCall view");

        // Attempting to induce garbage collection
        System.gc();

        prepareRelativeLayout();

        mSpecialCallView = new ImageView(this);
        mSpecialCallView.setLayoutParams(new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT));
        //     ((ImageView)mSpecialCallView).setScaleType(ImageView.ScaleType.FIT_XY); STRECTH IMAGE ON FULL SCREEN <<< NOT SURE IT's GOOD !!!!!
        ((ImageView) mSpecialCallView).setScaleType(ImageView.ScaleType.FIT_CENTER); // <<  just place the image Center of Window and fit it with ratio

        ((ImageView) mSpecialCallView).setImageResource(R.drawable.ringtone_icon);

        mRelativeLayout.addView(mSpecialCallView);
    }

    protected void prepareViewForSpecialCall(FileManager.FileType fileType, String mediaFilePath) {
        log(Log.INFO,TAG, "Preparing SpecialCall view");

        // Attempting to induce garbage collection
        System.gc();

        prepareRelativeLayout();

        // Displaying image during call
        if (fileType == FileManager.FileType.IMAGE) {

            try {
                prepareImageView(mediaFilePath);
            } catch (NullPointerException | OutOfMemoryError e) {
                Crashlytics.log(Log.ERROR,TAG, "Failed decoding image:" + e.getMessage());
            }
        }
        // Displaying video during call
        else if (fileType == FileManager.FileType.VIDEO)
            prepareVideoView(mediaFilePath);

        mRelativeLayout.addView(mSpecialCallView);
    }
    private void disableVolumeButtons() {
        log(Log.INFO,TAG, "Preparing disableVolumeButtons");

        mSpecialCallMutUnMuteBtn = (ImageView) mcButtonsOverlay.findViewById(R.id.mc_mute_unmute);
        mSpecialCallVolumeDownBtn = (ImageView) mcButtonsOverlay.findViewById(R.id.volume_down);
        mSpecialCallVolumeUpBtn = (ImageView) mcButtonsOverlay.findViewById(R.id.volume_up);
        mSpecialCallVolumeValueTextView = (TextView) mcButtonsOverlay.findViewById(R.id.volume_value);

        mSpecialCallMutUnMuteBtn.setVisibility(View.INVISIBLE);
        mSpecialCallVolumeDownBtn.setVisibility(View.INVISIBLE);
        mSpecialCallVolumeUpBtn.setVisibility(View.INVISIBLE);
        mSpecialCallVolumeValueTextView.setVisibility(View.INVISIBLE);
    }

    private void prepareMuteBtn() {
        log(Log.INFO,TAG, "Preparing Mute Button");

        //TODO check if Togglebutton for imageView good also?
        mSpecialCallMutUnMuteBtn = (ImageView) mcButtonsOverlay.findViewById(R.id.mc_mute_unmute);
        verifyAudioManager();
        if (mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC) == 0) {
            mSpecialCallMutUnMuteBtn.setImageResource(R.drawable.mute_speaker_anim);//TODO : setImageResource need to be replaced ? memory issue ?
            mSpecialCallMutUnMuteBtn.bringToFront();
        } else {
            mSpecialCallMutUnMuteBtn.setImageResource(R.drawable.unmute_speaker_anim);//TODO : setImageResource need to be replaced ? memory issue ?
            mSpecialCallMutUnMuteBtn.bringToFront();
        }


        mSpecialCallMutUnMuteBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

                if (isMuted) {  // in versions of KITKAT and lower , we start in muted mode on the music stream , because we don't know when answering happens and we should stop it.
                    log(Log.INFO,TAG, "UNMUTE by button");
                    volumeChangeByMCButtons = true;

                    verifyAudioManager();
                    mAudioManager.setStreamMute(AudioManager.STREAM_MUSIC, false);

                   if (mVolumeBeforeMute!=0)
                    mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, mVolumeBeforeMute, 0);
                    mSpecialCallVolumeValueTextView.setText(Integer.toString(mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC)));
                    log(Log.INFO,TAG, "UNMUTE by button Volume Return to: " + String.valueOf(mVolumeBeforeMute));
                    isMuted = false;

                    mSpecialCallMutUnMuteBtn.setImageResource(R.drawable.unmute_speaker_anim);//TODO : setImageResource need to be replaced ? memory issue ?
                    mSpecialCallMutUnMuteBtn.bringToFront();
                    stopVibrator();

                } else {
                    log(Log.INFO,TAG, "MUTE by button");
                    volumeChangeByMCButtons = true;
                    verifyAudioManager();

                    mVolumeBeforeMute = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
                    log(Log.INFO,TAG, "MUTE by button , Previous volume: " + String.valueOf(mVolumeBeforeMute));
                    mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, 0, 0);
                    mAudioManager.setStreamMute(AudioManager.STREAM_MUSIC, true);
                    mSpecialCallVolumeValueTextView.setText(Integer.toString(mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC)));
                    isMuted = true;

                    mSpecialCallMutUnMuteBtn.setImageResource(R.drawable.mute_speaker_anim);//TODO : setImageResource need to be replaced ? memory issue ?
                    mSpecialCallMutUnMuteBtn.bringToFront();
                    stopVibrator();
                }
            }
        });


    }

    private void prepareVolumeBtn() {
        log(Log.INFO,TAG, "Preparing Volume Button");

        mSpecialCallVolumeDownBtn = (ImageView) mcButtonsOverlay.findViewById(R.id.volume_down);
        mSpecialCallVolumeUpBtn = (ImageView) mcButtonsOverlay.findViewById(R.id.volume_up);
        mSpecialCallVolumeValueTextView = (TextView) mcButtonsOverlay.findViewById(R.id.volume_value);

        //ImageView for volume down Special Incoming Call
        mSpecialCallVolumeDownBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

                volumeChangeByMCButtons = true;
                verifyAudioManager();
                mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC) - 1, 0); // decrease volume
                mSpecialCallVolumeValueTextView.setText(Integer.toString(mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC)));
                stopVibrator();

            }
        });
        mSpecialCallVolumeDownBtn.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {

                volumeChangeByMCButtons = true;

                verifyAudioManager();
                mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, 0, 0); // decrease volume
                mSpecialCallVolumeValueTextView.setText(Integer.toString(mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC)));
                stopVibrator();
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
                mSpecialCallVolumeValueTextView.setText(Integer.toString(mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC)));
                stopVibrator();
            }
        });

        mSpecialCallVolumeUpBtn.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {

                volumeChangeByMCButtons = true;

                verifyAudioManager();
                mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, mAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC), 0); // increase volume
                mSpecialCallVolumeValueTextView.setText(Integer.toString(mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC)));
                stopVibrator();
                return true;
            }
        });


    }


    private void previewPrepareMuteBtn() {
        log(Log.INFO,TAG, "Preparing Mute Button");

        //TODO check if Togglebutton for imageView good also?
        mSpecialCallMutUnMuteBtn = (ImageView) mcButtonsOverlay.findViewById(R.id.mc_mute_unmute);
        verifyPreviewAudioManager();
        if (mPreviewAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC) == 0) {
            mSpecialCallMutUnMuteBtn.setImageResource(R.drawable.mute_speaker_anim);//TODO : setImageResource need to be replaced ? memory issue ?
            mSpecialCallMutUnMuteBtn.bringToFront();
        } else {
            mSpecialCallMutUnMuteBtn.setImageResource(R.drawable.unmute_speaker_anim);//TODO : setImageResource need to be replaced ? memory issue ?
            mSpecialCallMutUnMuteBtn.bringToFront();
        }


        mSpecialCallMutUnMuteBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

                if (isMuted) {  // in versions of KITKAT and lower , we start in muted mode on the music stream , because we don't know when answering happens and we should stop it.
                    log(Log.INFO,TAG, "UNMUTE by button");
                    volumeChangeByMCButtons = true;

                    verifyPreviewAudioManager();
                    mPreviewAudioManager.setStreamMute(AudioManager.STREAM_MUSIC, false);
                    mPreviewAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, mVolumeBeforeMute, 0);
                    mSpecialCallVolumeValueTextView.setText(Integer.toString(mPreviewAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC)));
                    log(Log.INFO,TAG, "UNMUTE by button Volume Return to: " + String.valueOf(mVolumeBeforeMute));
                    isMuted = false;

                    mSpecialCallMutUnMuteBtn.setImageResource(R.drawable.unmute_speaker_anim);//TODO : setImageResource need to be replaced ? memory issue ?
                    mSpecialCallMutUnMuteBtn.bringToFront();
                    stopVibrator();
                } else {
                    log(Log.INFO,TAG, "MUTE by button");
                    volumeChangeByMCButtons = true;
                    verifyPreviewAudioManager();

                    mVolumeBeforeMute = mPreviewAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
                    log(Log.INFO,TAG, "MUTE by button , Previous volume: " + String.valueOf(mVolumeBeforeMute));
                    mPreviewAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, 0, 0);
                    mPreviewAudioManager.setStreamMute(AudioManager.STREAM_MUSIC, true);
                    mSpecialCallVolumeValueTextView.setText(Integer.toString(mPreviewAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC)));
                    isMuted = true;

                    mSpecialCallMutUnMuteBtn.setImageResource(R.drawable.mute_speaker_anim);//TODO : setImageResource need to be replaced ? memory issue ?
                    mSpecialCallMutUnMuteBtn.bringToFront();
                    stopVibrator();
                }
            }
        });


    }

    private void previewPrepareVolumeBtn() {
        log(Log.INFO,TAG, "Preparing Volume Button");

        mSpecialCallVolumeDownBtn = (ImageView) mcButtonsOverlay.findViewById(R.id.volume_down);
        mSpecialCallVolumeUpBtn = (ImageView) mcButtonsOverlay.findViewById(R.id.volume_up);
        mSpecialCallVolumeValueTextView = (TextView) mcButtonsOverlay.findViewById(R.id.volume_value);
        //ImageView for volume down Special Incoming Call
        mSpecialCallVolumeDownBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

                volumeChangeByMCButtons = true;
                verifyPreviewAudioManager();
                mPreviewAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, mPreviewAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC) - 1, 0); // decrease volume
                mSpecialCallVolumeValueTextView.setText(Integer.toString(mPreviewAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC)));

            }
        });
        mSpecialCallVolumeDownBtn.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {

                volumeChangeByMCButtons = true;

                verifyPreviewAudioManager();
                mPreviewAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, 0, 0); // decrease volume
                mSpecialCallVolumeValueTextView.setText(Integer.toString(mPreviewAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC)));
                return true;
            }
        });

        //ImageView for volume up Special Incoming Call
        mSpecialCallVolumeUpBtn.bringToFront();
        mSpecialCallVolumeUpBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                volumeChangeByMCButtons = true;

                verifyPreviewAudioManager();
                mPreviewAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, mPreviewAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC) + 1, 0); // increase volume
                mSpecialCallVolumeValueTextView.setText(Integer.toString(mPreviewAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC)));

            }
        });

        mSpecialCallVolumeUpBtn.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {

                volumeChangeByMCButtons = true;

                verifyPreviewAudioManager();
                mPreviewAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, mPreviewAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC), 0); // increase volume
                mSpecialCallVolumeValueTextView.setText(Integer.toString(mPreviewAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC)));

                return true;
            }
        });


    }




    private void prepareBlockButton() {
        log(Log.INFO,TAG, "Preparing Block Button");
        mSpecialCallBlockBtn = (ImageView) mcButtonsOverlay.findViewById(R.id.block_mc);
        //ImageView for Closing Special Incoming Call
        mSpecialCallBlockBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

                Set<String> blockedNumbers = new HashSet<String>();
                blockedNumbers = MCBlockListUtils.getBlockListFromShared(getApplicationContext());
                blockedNumbers.add(PhoneNumberUtils.toValidLocalPhoneNumber(mIncomingOutgoingNumber));

                MCBlockListUtils.setBlockListFromShared(getApplicationContext(), blockedNumbers);

                if(_contactName.isEmpty())
                    UI_Utils.callToast(mIncomingOutgoingNumber + " Is Now MC BLOCKED !!! ", Color.RED, Toast.LENGTH_SHORT, getApplicationContext());
                else
                    UI_Utils.callToast(_contactName + " Is Now MC BLOCKED !!! ", Color.RED, Toast.LENGTH_SHORT, getApplicationContext());

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
        SharedPrefUtils.setInt(getApplicationContext(), SharedPrefUtils.SERVICES, SharedPrefUtils.MUSIC_VOLUME, mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC));
        log(Log.INFO,TAG, "MUSIC_VOLUME Original: " + String.valueOf(mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC)));
    }

    protected void backupRingSettings() {

        verifyAudioManager();
        int ringerMode = mAudioManager.getRingerMode();

        log(Log.INFO,TAG, "RINGER_MODE Original: " + ringerMode);
        SharedPrefUtils.setInt(getApplicationContext(),SharedPrefUtils.SERVICES,SharedPrefUtils.RINGER_MODE , ringerMode);

        int ringVolume = mAudioManager.getStreamVolume(AudioManager.STREAM_RING);
        SharedPrefUtils.setInt(getApplicationContext(), SharedPrefUtils.SERVICES, SharedPrefUtils.RING_VOLUME, ringVolume);
        log(Log.INFO,TAG, "mRingVolume Original: " + ringVolume);
    }

    protected void verifyAudioManager() {

        if (mAudioManager == null)
        {
            log(Log.INFO,TAG , "Audio manager was null , re-instantiated");
            mAudioManager = (AudioManager) getApplicationContext().getSystemService(Context.AUDIO_SERVICE);
        }
    }

    protected void verifyPreviewAudioManager() {

        if (mPreviewAudioManager == null)
        {
            log(Log.INFO,TAG , "Audio manager was null , re-instantiated");
            mPreviewAudioManager = (AudioManager) getApplicationContext().getSystemService(Context.AUDIO_SERVICE);
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
    protected void startVisualMediaMC(String visualMediaFilePath, String callNumber, boolean attachDefaultView ,boolean VisualMediaExists) {

        log(Log.INFO,TAG, "startVisualMediaMC SharedPrefUtils visualMediaFilePath:" + visualMediaFilePath);
        if (attachDefaultView)
            SharedPrefUtils.setBoolean(getApplicationContext(),SharedPrefUtils.SERVICES, SharedPrefUtils.DISABLE_VOLUME_BUTTONS, false);
        else
            SharedPrefUtils.setBoolean(getApplicationContext(),SharedPrefUtils.SERVICES, SharedPrefUtils.DISABLE_VOLUME_BUTTONS, true);

        _contactName = ContactsUtils.getContactName(getApplicationContext(), callNumber);
        mContactTitleOnWindow = (!_contactName.equals("") ? _contactName + " " + callNumber : callNumber);
        Random r = new Random();
        int randomWindowId = r.nextInt(Integer.MAX_VALUE);  // fixing a bug: when the same ID the window isn't released good enough so we need to make a different window in the mean time

        if (VisualMediaExists) {
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
            Crashlytics.log(Log.ERROR,TAG, "Empty media file path! Cannot start special call media");
        }
    }

    protected void closeSpecialCallWindowWithoutRingtone() {

        log(Log.INFO,TAG, "closeSpecialCallWindowWithoutRingtone():");
        if (isRingingSession(SharedPrefUtils.OUTGOING_RINGING_SESSION) || isRingingSession(SharedPrefUtils.INCOMING_RINGING_SESSION)) {
            stopSound();
            stopVibrator();
            Intent i = new Intent(this, this.getClass());
            i.setAction(StandOutWindow.ACTION_CLOSE_ALL);
            log(Log.INFO,TAG, "StandOutWindow.ACTION_CLOSE_ALL");
            startService(i);
        }
    }

    protected void startAudioMediaMC(String audioFilePath) {

        if(new File(audioFilePath).exists()) {

            SharedPrefUtils.setBoolean(getApplicationContext(),SharedPrefUtils.SERVICES,SharedPrefUtils.DISABLE_VOLUME_BUTTONS,false);
            log(Log.INFO,TAG, "Ringtone before playing sound. audioFilePath: " + audioFilePath + " URI: " + Uri.parse(audioFilePath).toString());
            playSound(getApplicationContext(), Uri.parse(audioFilePath));
        } else
        {
            SharedPrefUtils.setBoolean(getApplicationContext(),SharedPrefUtils.SERVICES,SharedPrefUtils.DISABLE_VOLUME_BUTTONS,true);
        }
    }

    protected void stopSound() {

        log(Log.INFO,TAG, "Stop ringtone sound");

        try {
            if (mMediaPlayer != null && mMediaPlayer.isPlaying()) {
                log(Log.INFO,TAG, "mMediaPlayer=" + mMediaPlayer);
                mMediaPlayer.setVolume(0, 0);
                mMediaPlayer.stop();
                //   mMediaPlayer.reset();
                //   mMediaPlayer.release();
            } else
                log(Log.INFO,TAG, "mMediaPlayer is null or isn't playing");
        } catch (Exception e) {
            e.printStackTrace();
            Crashlytics.log(Log.ERROR,TAG, "Failed to Stop sound. Exception:" + (e.getMessage() != null ? e.getMessage() : e));
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

    public void stopVibrator()
    {
        try {
            if (vibrator!=null && mAudioManager.getRingerMode() != 0) {
                vibrator.cancel();
               vibrator = null;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
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


        verifyAudioManager();
        mAudioManager.setStreamMute(AudioManager.STREAM_MUSIC, false);

        try {
         if(!mOutgoingCall)
            mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, SharedPrefUtils.getInt(getApplicationContext(), SharedPrefUtils.SERVICES, SharedPrefUtils.MUSIC_VOLUME), 0);
        } catch (Exception e) {
            Crashlytics.log(Log.ERROR,TAG, "setStreamVolume  STREAM_MUSIC failed. Exception:" + (e.getMessage() != null ? e.getMessage() : e));
        }

        try {
            if (mMediaPlayer != null && mMediaPlayer.isPlaying()) {
                mMediaPlayer.setVolume(0, 0);
                //mMediaPlayer.release();
            } else
                log(Log.INFO,TAG, "mMediaPlayer is null or isn't playing");
        } catch (Exception e) {
            e.printStackTrace();
            Crashlytics.log(Log.ERROR,TAG, "Failed to Stop sound. Exception:" + (e.getMessage() != null ? e.getMessage() : e));
        }

        try {
        mGifDrawable.recycle();
        } catch (Exception e) {
            Crashlytics.log(Log.ERROR,TAG, "trying to recycle Gif Image. Exception:" + (e.getMessage() != null ? e.getMessage() : e));
        }
        mGifDrawable = null;
        vibrator=null;
        mMediaPlayer = null;
        mAudioManager = null;
        mPreviewAudioManager = null;
        isMuted = false;
        removeTempMd5ForCallRecord();
        mPreviewStart = false;
        windowCloseActionWasMade = true;
        volumeChangeByMCButtons = false;
        mVolumeBeforeMute = 0;
        mOutgoingCall=false;
                isHidden = false;
        _contactName="";
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
                Crashlytics.log(Log.ERROR,TAG, "Closing" + TAG + " MC StandooutWindow failed. Exception:" + (e.getMessage() != null ? e.getMessage() : e));
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
          //  Crashlytics.log(Log.INFO,TAG, "onMeasure");

            setMeasuredDimension(mForceWidth, mForceHeight);
        }
    }
    //endregion
}
