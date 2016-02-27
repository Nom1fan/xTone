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
import android.provider.ContactsContract;
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
import android.widget.Toast;
import android.widget.VideoView;

import com.special.app.R;
import com.utils.BitmapUtils;
import com.utils.SharedPrefUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
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
    protected ImageView mSpecialCallMuteBtn;
    protected ImageView mSpecialCallVolumeUpBtn;
    protected ImageView mSpecialCallVolumeDownBtn;
    protected ImageView mSpecialCallBlockBtn;
    protected RelativeLayout mRelativeLayout;
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

    /* Service methods */

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

        if (mAudioManager!=null)
        {mAudioManager.setStreamMute(AudioManager.STREAM_MUSIC,false);  // TODO Rony : Replace Deprecated !! Check All places
        mAudioManager.setStreamMute(AudioManager.STREAM_RING,false);}  // TODO Rony : Replace Deprecated !! Check All places
    }

    /* Standout Window methods */

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

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
        // TODO Rony make another RelativeLayout with transpernt and buttons already on on the old relative layout. and no need for stupid margins
        frame.setBackgroundColor(Color.BLACK);
        windowCloseActionWasMade = false;
    }

    @Override
    public StandOutLayoutParams getParams(int id, Window window) {
        Log.i(TAG, "In StandOutLayoutParams()");
        return new StandOutLayoutParams(id, mWidth, mHeight, 0, 0);
    }

    /* Private classes and listeners */

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

    //TODO Is there a way to do this without reflection?
    private String getContactName(final String phoneNumber)
    {
        Uri uri;
        String[] projection;
        Uri mBaseUri = Contacts.Phones.CONTENT_FILTER_URL;
        projection = new String[] { android.provider.Contacts.People.NAME };
        try {
            Class<?> c = Class.forName("android.provider.ContactsContract$PhoneLookup");
            mBaseUri = (Uri) c.getField("CONTENT_FILTER_URI").get(mBaseUri);
            projection = new String[] { "display_name" };
        }
        catch (Exception e) { } // Why are we obsorbing the exception?

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
        String contactName = getContactName(callNumber);
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
        prepareMuteBtn();
        prepareVolumeBtn();
        prepareBlockButton();

        mSpecialCallView = new ImageView(this);
        mSpecialCallView.setLayoutParams(new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT));
        //     ((ImageView)mSpecialCallView).setScaleType(ImageView.ScaleType.FIT_XY); STRECTH IMAGE ON FULL SCREEN <<< NOT SURE IT's GOOD !!!!!
        ((ImageView) mSpecialCallView).setScaleType(ImageView.ScaleType.FIT_CENTER); // <<  just place the image Center of Window and fit it with ratio


        ((ImageView) mSpecialCallView).setImageResource(R.drawable.color_mc);



        mRelativeLayout.addView(mSpecialCallView);
        mRelativeLayout.addView(mSpecialCallTextView);
        mRelativeLayout.addView(mSpecialCallCloseBtn);
        mRelativeLayout.addView(mSpecialCallMuteBtn);
        mRelativeLayout.addView(mSpecialCallVolumeDownBtn);
        mRelativeLayout.addView(mSpecialCallVolumeUpBtn);
        mRelativeLayout.addView(mSpecialCallBlockBtn);
    }

    protected void prepareViewForSpecialCall(FileManager.FileType fileType , String mediaFilePath, String callNumber) {
        Log.i(TAG, "Preparing SpecialCall view");

        // Attempting to induce garbage collection
        System.gc();

        prepareRelativeLayout();
        prepareCallNumberTextView(callNumber);
        prepareCloseBtn();
        prepareMuteBtn();
        prepareVolumeBtn();
        prepareBlockButton();

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
        mRelativeLayout.addView(mSpecialCallMuteBtn);
        mRelativeLayout.addView(mSpecialCallVolumeDownBtn);
        mRelativeLayout.addView(mSpecialCallVolumeUpBtn);
        mRelativeLayout.addView(mSpecialCallBlockBtn);

    }

    private void prepareMuteBtn()
    {
        Log.i(TAG, "Preparing Mute Button");

        //ImageView for Closing Special Incoming Call
        mSpecialCallMuteBtn = new ImageView(this);
        RelativeLayout.LayoutParams lp1 = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.WRAP_CONTENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT);
        lp1.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
        lp1.addRule(RelativeLayout.ALIGN_BOTTOM);
        lp1.addRule(RelativeLayout.ALIGN_RIGHT);
        lp1.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
        mSpecialCallMuteBtn.setImageResource(R.drawable.unmute);  //TODO : setImageResource need to be replaced ? memory issue ?
        mSpecialCallMuteBtn.setBackgroundColor(Color.WHITE);
        mSpecialCallMuteBtn.setLayoutParams(lp1); // TODO Rony make another RelativeLayout with transpernt and buttons already on on the old relative layout. and no need for stupid margins
        mSpecialCallMuteBtn.setClickable(true);
        mSpecialCallMuteBtn.bringToFront();
        mSpecialCallMuteBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (isMuted) {  // in versions of KITKAT and lower , we start in muted mode on the music stream , because we don't know when answering happens and we should stop it.
                    volumeChangeByMCButtons = true;

                    mAudioManager.setStreamMute(AudioManager.STREAM_MUSIC, false); // TODO Rony : Replace Deprecated !! Check All places
                    mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, mVolumeBeforeMute, 0);
                    isMuted = false;
                    mSpecialCallMuteBtn.setImageResource(R.drawable.unmute);//TODO : setImageResource need to be replaced ? memory issue ?
                    mSpecialCallMuteBtn.bringToFront();
                    Log.i(TAG, "UNMUTE by button");
                } else {

                    volumeChangeByMCButtons = true;
                    mVolumeBeforeMute = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
                    mAudioManager.setStreamMute(AudioManager.STREAM_MUSIC, true); // TODO Rony : Replace Deprecated !! Check All places
                    isMuted = true;
                    mSpecialCallMuteBtn.setImageResource(R.drawable.mute);//TODO : setImageResource need to be replaced ? memory issue ?
                    mSpecialCallMuteBtn.bringToFront();
                    Log.i(TAG, "MUTE by button");
                }
            }
        });
    }

    private void prepareVolumeBtn()
    {
        Log.i(TAG, "Preparing Volume Button");

        //ImageView for Closing Special Incoming Call
        mSpecialCallVolumeDownBtn = new ImageView(this);
        RelativeLayout.LayoutParams lp1 = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.WRAP_CONTENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT);
        lp1.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
        lp1.addRule(RelativeLayout.ALIGN_BOTTOM);
        lp1.addRule(RelativeLayout.ALIGN_RIGHT);
        lp1.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
        lp1.addRule(RelativeLayout.ALIGN_TOP,mSpecialCallMuteBtn.getId());
       // Log.i(TAG, " mSpecialCallMuteBtn.getHeight() : " + String.valueOf(mSpecialCallMuteBtn.getHeight()));
        lp1.setMargins(0,0,0,200); // TODO Rony make another RelativeLayout with transpernt and buttons already on on the old relative layout. and no need for stupid margins
        mSpecialCallVolumeDownBtn.setImageResource(R.drawable.minusvol);  //TODO : setImageResource need to be replaced ? memory issue ?
        mSpecialCallVolumeDownBtn.setBackgroundColor(Color.WHITE);
        mSpecialCallVolumeDownBtn.setLayoutParams(lp1);
        mSpecialCallVolumeDownBtn.setClickable(true);
        mSpecialCallVolumeDownBtn.bringToFront();
        mSpecialCallVolumeDownBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

                volumeChangeByMCButtons = true;
                mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC) - 1 ,0); // decrease volume

            }
        });


        //ImageView for Closing Special Incoming Call
        mSpecialCallVolumeUpBtn = new ImageView(this);
        RelativeLayout.LayoutParams lp2 = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.WRAP_CONTENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT);
        lp2.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
        lp2.addRule(RelativeLayout.ALIGN_BOTTOM);
        lp2.addRule(RelativeLayout.ALIGN_RIGHT);
        lp2.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
       // Log.i(TAG, " mSpecialCallVolumeDownBtn.getHeight() : " + String.valueOf(mSpecialCallVolumeDownBtn.getMaxHeight()));
        lp2.setMargins(0,0,0,400); // TODO Rony make another RelativeLayout with transpernt and buttons already on on the old relative layout. and no need for stupid margins
        lp2.addRule(RelativeLayout.ALIGN_TOP,mSpecialCallVolumeDownBtn.getId());
        mSpecialCallVolumeUpBtn.setImageResource(R.drawable.plus);//TODO : setImageResource need to be replaced ? memory issue ?
        mSpecialCallVolumeUpBtn.setBackgroundColor(Color.WHITE);
        mSpecialCallVolumeUpBtn.setLayoutParams(lp2);
        mSpecialCallVolumeUpBtn.setClickable(true);
        mSpecialCallVolumeUpBtn.bringToFront();
        mSpecialCallVolumeUpBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                volumeChangeByMCButtons = true;
                mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC) + 1 ,0); // increase volume

            }
        });



    }

    private void prepareBlockButton()
    {
        Log.i(TAG, "Preparing Mute Button");

        //ImageView for Closing Special Incoming Call
        mSpecialCallBlockBtn = new ImageView(this);
        RelativeLayout.LayoutParams lp2 = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.WRAP_CONTENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT);
        lp2.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
        lp2.addRule(RelativeLayout.ALIGN_BOTTOM);
        lp2.addRule(RelativeLayout.ALIGN_RIGHT);
        lp2.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
        // Log.i(TAG, " mSpecialCallVolumeDownBtn.getHeight() : " + String.valueOf(mSpecialCallVolumeDownBtn.getMaxHeight()));
        lp2.setMargins(0,0,0,600);  // TODO Rony make another RelativeLayout with transpernt and buttons already on on the old relative layout. and no need for stupid margins
        lp2.addRule(RelativeLayout.ALIGN_TOP,mSpecialCallVolumeUpBtn.getId());
        mSpecialCallBlockBtn.setImageResource(R.drawable.blocked_mc);//TODO : setImageResource need to be replaced ? memory issue ?
        mSpecialCallBlockBtn.setBackgroundColor(Color.WHITE);
        mSpecialCallBlockBtn.setLayoutParams(lp2);
        mSpecialCallBlockBtn.setClickable(true);
        mSpecialCallBlockBtn.bringToFront();
        mSpecialCallBlockBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

                Set<String> blockedNumbers = new HashSet<String>();
                // TODO Rony Add method to GET the block_list from MCblockutils
                blockedNumbers = SharedPrefUtils.getStringSet(getApplicationContext(),SharedPrefUtils.SETTINGS,SharedPrefUtils.BLOCK_LIST);
                blockedNumbers.add(toValidPhoneNumber(mIncomingOutgoingNumber)); // TODO Rony use Phone Number Utils

                // TODO Rony Add method to SET the block_list from MCblockutils
                SharedPrefUtils.setStringSet(getApplicationContext(), SharedPrefUtils.SETTINGS, SharedPrefUtils.BLOCK_LIST, blockedNumbers);

                // TODO Rony Show Toast from UIUtils
                Toast.makeText(AbstractStandOutService.this,mIncomingOutgoingNumber +" Is Now MC BLOCKED !!! ", Toast.LENGTH_SHORT).show();

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

    private String toValidPhoneNumber(String str) { // TODO Rony use from PhoneNumbers Utils

        str = str.replaceAll("[^0-9]","");

        if (str.startsWith("9720")){
            str= str.replaceFirst("9720","0");
        }
        if (str.startsWith("972")){
            str= str.replaceFirst("972","0");
        }


        return str;
    }

    protected boolean checkIfNumberIsMCBlocked(String incomingNumber) { // TODO Rony move it to MCBlockListUtils or whatever
        Log.i(TAG, "check if number blocked: " + incomingNumber);
        //MC Permissions: ALL , Only contacts , Specific Black List Contacts
        String permissionLevel = SharedPrefUtils.getString(getApplicationContext(), SharedPrefUtils.RADIO_BUTTON_SETTINGS, SharedPrefUtils.WHO_CAN_MC_ME);

        if (permissionLevel.isEmpty())
        {
            SharedPrefUtils.setString(getApplicationContext(), SharedPrefUtils.RADIO_BUTTON_SETTINGS, SharedPrefUtils.WHO_CAN_MC_ME, "ALL");
        }
        else
        {
            switch (permissionLevel) {

                case "ALL":
                    return false;

                case "CONTACTS":

                    // GET ALL CONTACTS
                    List<String> contactPhonenumbers = new ArrayList<String>(); // TODO Rony use the contactsUtils Method
                    Cursor curPhones = this.getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null, null, null, null);
                    assert curPhones != null;
                    while (curPhones.moveToNext())
                    {
                        String phoneNumber = curPhones.getString(curPhones.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
                        contactPhonenumbers.add(phoneNumber.replaceAll("\\D+", "")); // TODO Rony Use PhoneNumberUtils to ValidPhoneNumber
                    }
                    curPhones.close();

                    if(contactPhonenumbers.contains(incomingNumber.replaceAll("\\D+", ""))) // TODO Rony Use PhoneNumberUtils to ValidPhoneNumber
                        return false;
                    else
                        return true;


                case "black_list":

                    Set<String> blockedSet = SharedPrefUtils.getStringSet(getApplicationContext(), SharedPrefUtils.SETTINGS, SharedPrefUtils.BLOCK_LIST);
                    if (!blockedSet.isEmpty()) {
                        incomingNumber = incomingNumber.replaceAll("\\D+", ""); // TODO Rony Use PhoneNumberUtils to ValidPhoneNumber

                        if (blockedSet.contains(incomingNumber)) {
                            Log.i(TAG, "NUMBER MC BLOCKED: " + incomingNumber);
                            return true;
                        }
                    }
                    else {
                        Log.w(TAG, "BlackList empty allowing phone number: " + incomingNumber);
                        return false;
                    }
            }
        }
        return false;
    }



}
