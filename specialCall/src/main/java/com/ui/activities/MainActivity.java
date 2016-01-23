package com.ui.activities;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Parcelable;
import android.provider.ContactsContract;
import android.provider.MediaStore;
import android.telephony.SmsManager;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import com.app.AppStateManager;
import com.data_objects.Constants;
import com.data_objects.SnackbarData;
import com.ipaulpro.afilechooser.utils.FileUtils;
import com.nispok.snackbar.Snackbar;
import com.nispok.snackbar.SnackbarManager;
import com.nispok.snackbar.listeners.ActionClickListener;
import com.services.IncomingService;
import com.services.LogicServerProxyService;
import com.services.OutgoingService;
import com.services.StorageServerProxyService;
import com.special.app.R;
import com.ui.components.AutoCompletePopulateListAsyncTask;
import com.ui.components.BitmapWorkerTask;
import com.utils.BroadcastUtils;
import com.utils.LUT_Utils;
import com.utils.SharedPrefUtils;
import org.apache.commons.lang.math.NumberUtils;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import DataObjects.SpecialMediaType;
import EventObjects.Event;
import EventObjects.EventReport;
import EventObjects.EventType;
import Exceptions.FileDoesNotExistException;
import Exceptions.FileExceedsMaxSizeException;
import Exceptions.FileInvalidFormatException;
import Exceptions.FileMissingExtensionException;
import Exceptions.InvalidDestinationNumberException;
import FilesManager.FileManager;


public class MainActivity extends Activity implements OnClickListener {

    private String _myPhoneNumber = "";
    private String _destPhoneNumber = "";
    private String _destName = "";
    private static final String TAG = MainActivity.class.getSimpleName();
    private Uri _outputFileUri;
    private ProgressBar pFetchUserBar;
    private ProgressBar _pBar;
    private Context _context;
    private BroadcastReceiver _serviceReceiver;
    private IntentFilter serviceReceiverIntentFilter = new IntentFilter(Event.EVENT_ACTION);
    private abstract class ActivityRequestCodes {

        public static final int SELECT_CALLER_MEDIA = 1;
        public static final int SELECT_CONTACT = 2;
        public static final int SELECT_PROFILE_MEDIA = 3;

    }
    private AutoCompleteTextView mTxtPhoneNo;
    private int randomPIN=0;
    private boolean wasFileChooser=false;
    private final String shareBody = String.valueOf(R.string.invite);

    @Override
    protected void onStart() {
        super.onStart();
        Log.i(TAG, "onStart()");

        _serviceReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {

                EventReport report = (EventReport) intent.getSerializableExtra(Event.EVENT_REPORT);
                eventReceived(new Event(this,report));
            }
        };
        registerReceiver(_serviceReceiver, serviceReceiverIntentFilter);

    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.i(TAG, "onPause()");

        if(_serviceReceiver !=null)
        {  try
        {unregisterReceiver(_serviceReceiver);}
        catch (Exception ex){
            Log.e(TAG, ex.getMessage());}
        }
        saveInstanceState();
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.i(TAG, "onResume()");

        _context = getApplicationContext();
        String appState = getState();
        Log.i(TAG, "App State:" + appState);

        // Starting service responsible for incoming media callz
        Intent incomingServiceIntent = new Intent(this, IncomingService.class);
        incomingServiceIntent.setAction(IncomingService.ACTION_START);
        startService(incomingServiceIntent);

        // Starting service responsible for outgoing media callz
        Intent outgoingServiceIntent = new Intent(this, OutgoingService.class);
        outgoingServiceIntent.setAction(OutgoingService.ACTION_START);
        startService(outgoingServiceIntent);

        Log.i(TAG, "startService: IncomingService");

        syncUIwithAppState();

        if(!appState.equals(AppStateManager.STATE_LOGGED_OUT)) {

            // Taking Focus from AutoCompleteTextView in the end, so he won't pop up :) added also focus capabilities to the MainActivity Layout XML
            findViewById(R.id.mainActivity).requestFocus();

            registerReceiver(_serviceReceiver, serviceReceiverIntentFilter);

            // ASYNC TASK To Populate all contacts , it can take long time and it delays the UI
            new AutoCompletePopulateListAsyncTask(this, mTxtPhoneNo).execute();

            if(appState.equals(AppStateManager.STATE_DISABLED)) {

                _myPhoneNumber = Constants.MY_ID(_context);
                initializeConnection();

            }

            restoreInstanceState();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "onDestroy()");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i(TAG, "onCreate()");

        _context = getApplicationContext();

        if (getState().equals(AppStateManager.STATE_LOGGED_OUT)) {

            initializeLoginUI();

        } else {
            initializeUI();
            if (getState().equals(AppStateManager.STATE_LOGGED_IN)) {
                stateIdle();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {

            if (requestCode == ActivityRequestCodes.SELECT_CALLER_MEDIA) {

                SpecialMediaType specialMediaType = SpecialMediaType.CALLER_MEDIA;
                uploadFile(data, specialMediaType);
            }
            else if(requestCode == ActivityRequestCodes.SELECT_PROFILE_MEDIA) {

                SpecialMediaType specialMediaType = SpecialMediaType.PROFILE_MEDIA;
                uploadFile(data, specialMediaType);
            }

            if (requestCode == ActivityRequestCodes.SELECT_CONTACT) {
                try {
                    if (data != null) {
                        Uri uri = data.getData();

                        if (uri != null) {
                            Cursor c = null;
                            try {
                                c = getContentResolver()
                                        .query(uri,
                                                new String[] {
                                                        ContactsContract.CommonDataKinds.Phone.NUMBER,
                                                        ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME, },
                                                null, null, null);

                                if (c != null && c.moveToFirst()) {
                                    String number = c.getString(0);
                                    String name = c.getString(1);

                                    saveInstanceState(name, toValidPhoneNumber(number));

                                }
                            } finally {
                                if (c != null) {
                                    c.close();
                                }
                            }
                        }
                    } else
                        throw new Exception("SELECT_CONTACT: data is null");
                } catch (Exception e) {
                    e.printStackTrace();
                }

            }

        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    private void selectMedia(int code) {

        // Determine Uri of camera image to save.
        final File root = new File(Environment.getExternalStorageDirectory()
                + File.separator + "MyDir" + File.separator);
        root.mkdirs();
        final String fname = "MyImage";
        final File sdImageMainDirectory = new File(root, fname);
        _outputFileUri = Uri.fromFile(sdImageMainDirectory);

        // Camera.
        final List<Intent> cameraIntents = new ArrayList<>();
        final Intent captureIntent = new Intent(
                android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
        final PackageManager packageManager = getPackageManager();
        final List<ResolveInfo> listCam = packageManager.queryIntentActivities(
                captureIntent, 0);


        final Intent videoIntent = new Intent(
                MediaStore.ACTION_VIDEO_CAPTURE);

        for (ResolveInfo res : listCam) {
            final String packageName = res.activityInfo.packageName;
            final Intent intent = new Intent(captureIntent);
            intent.setComponent(new ComponentName(res.activityInfo.packageName,
                    res.activityInfo.name));
            intent.setPackage(packageName);
            intent.putExtra(MediaStore.EXTRA_OUTPUT, _outputFileUri);
            cameraIntents.add(intent);
        }
        cameraIntents.add(videoIntent);

        // Create the ACTION_GET_CONTENT Intent
        final Intent intent = FileUtils.createGetContentIntent();

        Intent chooserIntent = Intent.createChooser(intent, "Select Media File");

        // Add the camera options.
        chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS,
                cameraIntents.toArray(new Parcelable[cameraIntents.size()]));

        wasFileChooser=true;
        startActivityForResult(chooserIntent, code);
    }

    public void onClick(View v) {

        // Saving instance state
        saveInstanceState();

        int id = v.getId();
        if (id == R.id.CallNow) {

            launchDialer(_destPhoneNumber);

        } else if (id == R.id.selectMediaBtn) {

            selectMedia(ActivityRequestCodes.SELECT_CALLER_MEDIA);

        } else if (id == R.id.selectProfileMediaBtn) {

            selectMedia(ActivityRequestCodes.SELECT_PROFILE_MEDIA);

        }  else if (id == R.id.selectContactBtn) {

            Intent intent = new Intent(Intent.ACTION_PICK);
            intent.setType(ContactsContract.CommonDataKinds.Phone.CONTENT_TYPE);
            startActivityForResult(intent, ActivityRequestCodes.SELECT_CONTACT);

        } else if (id == R.id.clear) {

            AutoCompleteTextView textViewToClear = (AutoCompleteTextView)findViewById(R.id.CallNumber);
            textViewToClear.setText("");

        }
        else if(id == R.id.inviteButton){

            EditText callNumber = (EditText) findViewById(R.id.CallNumber);
            try {
                SmsManager smsManager = SmsManager.getDefault();
                smsManager.sendTextMessage(callNumber.getText().toString(), null, shareBody, null, null);
                writeInfoSnackBar("Invitation Sent To: " + callNumber.getText().toString());

            } catch (Exception ex) {
                writeErrStatBar(ex.getMessage());
            }
        }
        else if (id == R.id.login_btn) {

            String myVerificationcode = ((EditText) findViewById(R.id.SMSCode)).getText().toString();
            //if (myVerificationcode.equals(String.valueOf(randomPIN))){    // NEED TO FIND A SMS GATEWAY FIRST
            _myPhoneNumber = ((EditText) findViewById(R.id.LoginNumber))
                    .getText().toString();

            SharedPrefUtils.setString(_context,
                    SharedPrefUtils.GENERAL, SharedPrefUtils.MY_NUMBER, _myPhoneNumber);

            initializeConnection();

            File incomingFolder = new File(Constants.INCOMING_FOLDER);
            incomingFolder.mkdirs();

            File outgoingFolder = new File(Constants.OUTGOING_FOLDER);
            outgoingFolder.mkdirs();

            File tempCompressedFolder = new File(Constants.TEMP_COMPRESSED_FOLDER);
            tempCompressedFolder.mkdirs();

            initializeUI();
            new AutoCompletePopulateListAsyncTask(this, mTxtPhoneNo).execute();
            stateIdle();
            //  }//TODO NEED TO FIND A SMS GATEWAY FIRST
          /*  else
            {
                Toast.makeText(getApplicationContext(), "Code Wan't Correct Please Try Again !",
                        Toast.LENGTH_SHORT).show();

            }*/
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_settings:

                saveInstanceState();
                Intent y = new Intent();
                y.setClass(_context, Settings.class);
                startActivity(y);
                break;

            case R.id.action_share:

                saveInstanceState();

                Intent sharingIntent = new Intent(android.content.Intent.ACTION_SEND);
                sharingIntent.setType("text/plain");
                sharingIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, "MediaCallz (Open it in Google Play Store to Download the Application)");

                sharingIntent.putExtra(android.content.Intent.EXTRA_TEXT, shareBody);
                startActivity(Intent.createChooser(sharingIntent, "Share via"));


                break;


            default:
                saveInstanceState();
                Intent o = new Intent();
                o.setClass(_context, Settings.class);
                startActivity(o);
                break;
        }
        return true;
    }

    private void initializeLoginUI() {

        setContentView(R.layout.loginuser);
        findViewById(R.id.login_btn).setOnClickListener(this);

        runOnUiThread(new Runnable() {

                          @Override
                          public void run() {

                              if(!Constants.MY_TOKEN(_context).equals("")) {
                                  findViewById(R.id.initProgressBar).setVisibility(ProgressBar.INVISIBLE);
                                  findViewById(R.id.initTextView).setVisibility(TextView.INVISIBLE);
                              }

                              Button loginBtn = (Button) findViewById(R.id.login_btn);
                              loginBtn.setEnabled(false);
                              loginBtn.setText("Login");

                              Button GetSMSCode = (Button) findViewById(R.id.GetSMSCode);
                              GetSMSCode.setEnabled(false);
                              EditText SmsCodeVerification = (EditText) findViewById(R.id.SMSCode);
                              SmsCodeVerification.setEnabled(false);

                              EditText loginNumberET = (EditText) findViewById(R.id.LoginNumber);
                              loginNumberET.addTextChangedListener(new TextWatcher() {
                                  @Override
                                  public void beforeTextChanged(CharSequence s, int start, int count, int after) {

                                  }

                                  @Override
                                  public void onTextChanged(CharSequence s, int start, int before, int count) {

                                      if (10 == s.length()) {

                                          String token = SharedPrefUtils.getString(_context, SharedPrefUtils.GENERAL, SharedPrefUtils.MY_DEVICE_TOKEN);
                                          if (token != null && !token.equals("")) {
                                              findViewById(R.id.GetSMSCode).setEnabled(true);
                                              findViewById(R.id.SMSCode).setEnabled(true);
                                              findViewById(R.id.login_btn).setEnabled(true);  // REMOVE // NEED TO FIND A SMS GATEWAY FIRST
                                          }
                                      } else {
                                          findViewById(R.id.GetSMSCode).setEnabled(false);
                                          findViewById(R.id.SMSCode).setEnabled(false);
                                          findViewById(R.id.login_btn).setEnabled(false);   // REMOVE // // NEED TO FIND A SMS GATEWAY FIRST
                                      }
                                  }

                                  @Override
                                  public void afterTextChanged(Editable s) {

                                  }
                              });




                              OnClickListener buttonListener = new View.OnClickListener() {

                                  @Override
                                  public void onClick(View v) {
                                      EditText loginNumber = (EditText) findViewById(R.id.LoginNumber);

                                      //generate a 4 digit integer 1000 <10000
                                      randomPIN = (int)(Math.random()*9000)+1000;
                                      try {
                                          SmsManager smsManager = SmsManager.getDefault();
                                          smsManager.sendTextMessage(loginNumber.getText().toString(), null, "MediaCallz SmsVerificationCode: "+String.valueOf(randomPIN), null, null);
                                          Toast.makeText(getApplicationContext(), "Message Sent To: " +loginNumber.getText().toString(),
                                                  Toast.LENGTH_LONG).show();
                                      } catch (Exception ex) {
                                          Toast.makeText(getApplicationContext(),
                                                  ex.getMessage(),
                                                  Toast.LENGTH_LONG).show();
                                          ex.printStackTrace();
                                      }

                                  }
                              };
                              GetSMSCode.setOnClickListener(buttonListener);


                              SmsCodeVerification.addTextChangedListener(new TextWatcher() {
                                  @Override
                                  public void beforeTextChanged(CharSequence s, int start, int count, int after) {

                                  }

                                  @Override
                                  public void onTextChanged(CharSequence s, int start, int before, int count) {

                                      if (4 == s.length()) {

                                          findViewById(R.id.login_btn).setEnabled(true);

                                      }
                                      else
                                          findViewById(R.id.login_btn).setEnabled(false);
                                  }

                                  @Override
                                  public void afterTextChanged(Editable s) {

                                  }
                              });
                          }
                      }
        );
    }

    private void initializeUI() {

        setContentView(R.layout.activity_main);

        //prepareMsgBarAnimation();

        // Setting up buttons and attaching click listeners
        mTxtPhoneNo  = (AutoCompleteTextView) findViewById(R.id.CallNumber);
        mTxtPhoneNo.setRawInputType(InputType.TYPE_CLASS_TEXT);
        mTxtPhoneNo.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> av, View arg1, int index, long arg3) {

                String[] nameAndPhone = ((String) av.getItemAtPosition(index)).split("\\\n");
                String name = nameAndPhone[0];
                String number = nameAndPhone[1];
                String NumericNumber = toValidPhoneNumber(number);

                mTxtPhoneNo.setText(NumericNumber);
                _destName = name;
                setDestNameTextView();
            }
        });

        mTxtPhoneNo.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {

                v.setFocusable(true);
                v.setFocusableInTouchMode(true);
                v.performClick();
                return false;
            }
        });

        mTxtPhoneNo.addTextChangedListener(new TextWatcher() {

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count,
                                          int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before,
                                      int count) {

                String destPhone = s.toString();

                if (10 == s.length() &&
                        NumberUtils.isNumber(destPhone) &&
                        !wasFileChooser) {

                    _destPhoneNumber = destPhone;
                    drawSelectMediaButton(false);
                    drawSelectRingToneName();

                    if (!getState().equals(AppStateManager.STATE_DISABLED) &&
                            !getState().equals(AppStateManager.STATE_LOADING)) {
                        String msg = "Fetching user data...";
                        BroadcastUtils.sendEventReportBroadcast(_context, TAG + " onTextchanged()",
                                new EventReport(EventType.FETCHING_USER_DATA, msg, null));

                        Intent i = new Intent(_context, LogicServerProxyService.class);
                        i.setAction(LogicServerProxyService.ACTION_ISREGISTERED);
                        i.putExtra(LogicServerProxyService.DESTINATION_ID, destPhone);
                        _context.startService(i);

                        enableUserFetchProgressBar();
                    }
                } else {

                    // Resetting the flag
                    wasFileChooser = false;

                    _destPhoneNumber = "";
                    _destName = "";

                    if (10 != s.length() || !NumberUtils.isNumber(destPhone))
                    {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                disableUserStatusIcon();
                                vanishInviteButton();

                                if (getState().equals(AppStateManager.STATE_READY)) {
                                    AppStateManager.setAppState(_context, TAG + " onTextChanged()", AppStateManager.STATE_IDLE);
                                    BroadcastUtils.sendEventReportBroadcast(_context, TAG + "onTextChanged()", new EventReport(EventType.REFRESH_UI, "", null));
                                }
                            }
                        });
                    }

                    setDestNameTextView();
                    saveInstanceState();
                }
            }

            @Override
            public void afterTextChanged(Editable s) {


            }
        });

        Button button1 = (Button) findViewById(R.id.CallNow);
        button1.setOnClickListener(this);
        ImageButton button2 = (ImageButton) findViewById(R.id.selectMediaBtn);
        button2.setOnClickListener(this);
        ImageButton button6 = (ImageButton) findViewById(R.id.selectContactBtn);
        button6.setOnClickListener(this);
        ImageButton button7 = (ImageButton) findViewById(R.id.selectProfileMediaBtn);
        button7.setOnClickListener(this);
        ImageButton button8 = (ImageButton) findViewById(R.id.clear);
        button8.setOnClickListener(this);

        ImageButton invite = (ImageButton) findViewById(R.id.inviteButton);
        invite.setOnClickListener(this);

    }

    public void eventReceived(Event event) {

        final EventReport report = event.report();

        switch (report.status()) {

            case USER_REGISTERED_FALSE:
                userStatusUnregistered();
                break;

            case REFRESH_UI:
                SnackbarData data = (SnackbarData) report.data();
                syncUIwithAppState();

                if(data!=null)
                   handleSnackBar((SnackbarData) report.data());
                break;

            case DISPLAY_ERROR:
                writeErrStatBar(report.desc());
                break;

            case DISPLAY_MESSAGE:
                writeInfoSnackBar(report.desc());
                break;

            case TOKEN_RETRIEVED:
                findViewById(R.id.initProgressBar).setVisibility(ProgressBar.INVISIBLE);
                findViewById(R.id.initTextView).setVisibility(TextView.INVISIBLE);
                EditText loginET = (EditText)findViewById(R.id.LoginNumber);
                CharSequence loginNumber = loginET.getText();
                if(10 == loginNumber.length())
                    findViewById(R.id.login_btn).setEnabled(true);
                break;

            default:
                Log.e(TAG, "Undefined event status on EventReceived");
        }
    }


	/* -------------- Assisting methods -------------- */

    /**
     * Saving the instance state - to be used from onPause()
     */
    private void saveInstanceState() {

        // Saving destination number
        final AutoCompleteTextView ed_destinationNumber = ((AutoCompleteTextView) findViewById(R.id.CallNumber));
		if(ed_destinationNumber!=null) {
            _destPhoneNumber = ed_destinationNumber.getText().toString();
            SharedPrefUtils.setString(_context, SharedPrefUtils.GENERAL, SharedPrefUtils.DESTINATION_NUMBER, _destPhoneNumber);
        }

        // Saving destination name
        final TextView ed_destinationName = ((TextView) findViewById(R.id.destName));
        if(ed_destinationName!=null) {
            _destName = ed_destinationName.getText().toString();
            SharedPrefUtils.setString(_context, SharedPrefUtils.GENERAL, SharedPrefUtils.DESTINATION_NAME, _destName);
        }
    }

    /**
     * Saving the instance state - Should be used from onActivityResult.SELECT_CONTACT
     * @param destName The destination name to be saved
     * @param destNumber The destination number to be saved
     */
    private void saveInstanceState(String destName, String destNumber) {

        // Saving destination number
        _destPhoneNumber = destNumber;
        SharedPrefUtils.setString(_context, SharedPrefUtils.GENERAL, SharedPrefUtils.DESTINATION_NUMBER, _destPhoneNumber);


        // Saving destination name
        _destName = destName;
        SharedPrefUtils.setString(_context, SharedPrefUtils.GENERAL, SharedPrefUtils.DESTINATION_NAME, _destName);
    }

    private void restoreInstanceState() {

        Log.i(TAG, "Restoring instance state");

        // Restoring destination number
        final AutoCompleteTextView ed_destinationNumber =
                (AutoCompleteTextView) findViewById(R.id.CallNumber);
        String destNumber = SharedPrefUtils.getString(_context, SharedPrefUtils.GENERAL, SharedPrefUtils.DESTINATION_NUMBER);
        if(ed_destinationNumber!=null && destNumber!=null)
            ed_destinationNumber.setText(destNumber);

        // Restoring destination name
        _destName = SharedPrefUtils.getString(_context, SharedPrefUtils.GENERAL, SharedPrefUtils.DESTINATION_NAME);
        setDestNameTextView();

        // Restoring my phone number
        _myPhoneNumber = Constants.MY_ID(_context);

    }

    private void checkDestinationNumber() throws InvalidDestinationNumberException {

        if(_destPhoneNumber ==null)
            throw new InvalidDestinationNumberException();
        if(_destPhoneNumber.equals("") || _destPhoneNumber.length() < 10)
            throw new InvalidDestinationNumberException();
    }

    private void setDestNameTextView() {

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                final TextView tv_destName =
                        (TextView) findViewById(R.id.destName);
                if (tv_destName != null && _destName != null)
                    tv_destName.setText(_destName);
            }
        });

    }

    private String toValidPhoneNumber(String str) {

        str = str.replaceAll("[^0-9]","");

        if (str.startsWith("972")){
            str= str.replaceFirst("972","0");
        }
        if (str.startsWith("9720")){
            str= str.replaceFirst("9720","0");
        }

        return str;
    }

    private void initializeConnection() {

        Intent i = new Intent();
        i.setClass(getBaseContext(), LogicServerProxyService.class);
        if(AppStateManager.getAppState(getApplicationContext()).equals(AppStateManager.STATE_LOGGED_OUT))
            i.setAction(LogicServerProxyService.ACTION_REGISTER);
        else
            i.setAction(LogicServerProxyService.ACTION_RECONNECT);

        startService(i);

    }

    private void uploadFile(Intent data, SpecialMediaType specialMediaType) {

        FileManager fm = null;

        final boolean isCamera;
        try {
            checkDestinationNumber();
            Uri uri;

            if (data == null) {
                isCamera = true;
            } else {
                final String action = data.getAction();
                isCamera = action != null && action.equals(MediaStore.ACTION_IMAGE_CAPTURE);
            }
            if (isCamera) {
                uri = _outputFileUri;
            } else {
                uri = data.getData();
            }

            // Get the File path from the Uri
            String path = FileUtils.getPath(this, uri);
            // Alternatively, use FileUtils.getFile(Context, Uri)
            if (path != null) if (FileUtils.isLocal(path)) {

                if (isCamera) {
                    File file = new File(path);
                    file.renameTo(new File(path += ".jpeg"));
                }

                fm = new FileManager(path);
                wasFileChooser = true;
                Log.i(TAG, "onActivityResult RESULT_OK _ Rony");
                Intent i = new Intent(_context, StorageServerProxyService.class);
                i.setAction(StorageServerProxyService.ACTION_UPLOAD);
                i.putExtra(StorageServerProxyService.DESTINATION_ID, _destPhoneNumber);
                i.putExtra(StorageServerProxyService.SPECIAL_MEDIA_TYPE, specialMediaType);
                i.putExtra(StorageServerProxyService.FILE_TO_UPLOAD, fm);
                _context.startService(i);

            }

        } catch (NullPointerException e) {
            e.printStackTrace();
            Log.e(TAG, "It seems there was a problem with the file path.");
            writeInfoSnackBar("Oops! problem with file");
        } catch (FileExceedsMaxSizeException e) {
            writeInfoSnackBar("Oops! Select a file that weights less than:" +
                    FileManager.getFileSizeFormat(FileManager.MAX_FILE_SIZE));
        } catch (FileInvalidFormatException | FileDoesNotExistException | FileMissingExtensionException e) {
            e.printStackTrace();
            writeInfoSnackBar("Oops! Invalid file");
        } catch (InvalidDestinationNumberException e) {
            writeInfoSnackBar("Oops! Invalid destination number");
        }
    }

    private boolean isContactSelected() {

        AutoCompleteTextView edCN = (AutoCompleteTextView) findViewById(R.id.CallNumber);
        String s = edCN.getText().toString();
        return 10 == s.length();
    }

    /* -------------- UI methods -------------- */


    /* --- UI States --- */

    public void stateReady() {

        runOnUiThread(new Runnable() {
            @Override
            public void run() {

                disableProgressBar();
                enableSelectMediaButton();
                disableUserFetchProgressBar();
                enableSelectProfileMediaButton();
                enableContactEditText();
                enableSelectContactButton();
                enableCallButton();
                userStatusRegistered();
            }
        });
    }

    public void stateIdle() {

        runOnUiThread(new Runnable() {

            @Override
            public void run() {

                disableProgressBar();
                enableSelectContactButton();
                enableContactEditText();
                disableUserFetchProgressBar();
                disableSelectProfileMediaButton();
                disableSelectCallerMediaButton();
                disableCallButton();
            }
        });
    }

    public void stateDisabled() {

        runOnUiThread(new Runnable() {

            @Override
            public void run() {
                disableSelectCallerMediaButton();
                disableSelectProfileMediaButton();
                disableUserFetchProgressBar();
                disableProgressBar();
                disableSelectContactButton();
                disableContactEditText();
                disableCallButton();
                disableInviteButton();
            }
        });
    }

    public void stateLoading() {

        runOnUiThread(new Runnable() {

            @Override
            public void run() {

                enableProgressBar();
                disableSelectCallerMediaButton();
                disableSelectProfileMediaButton();
                disableSelectContactButton();
                disableContactEditText();
                disableCallButton();
            }
        });
    }

    private String getState() {

        return AppStateManager.getAppState(_context);
    }

    private void syncUIwithAppState() {

        switch (AppStateManager.getAppState(_context))
        {
            case AppStateManager.STATE_LOGGED_OUT:
                initializeLoginUI();
                break;

            case AppStateManager.STATE_IDLE:
                stateIdle();
                break;

            case AppStateManager.STATE_READY:
                stateReady();
                break;

            case AppStateManager.STATE_LOADING:
                stateLoading();
                break;

            case AppStateManager.STATE_DISABLED:
                stateDisabled();
                break;
        }
    }

    /* --- UI elements controls --- */

    public void launchDialer(String number) {
        String numberToDial = "tel:" + number;
        startActivity(new Intent(Intent.ACTION_DIAL, Uri.parse(numberToDial)));
    }

    private void disableCallButton() {

        runOnUiThread(new Runnable() {

            public void run() {
                findViewById(R.id.CallNow).setEnabled(false);
            }

        });
    }

    private void enableCallButton() {

        runOnUiThread(new Runnable() {

            @Override
            public void run() {
                findViewById(R.id.CallNow).setEnabled(true);
            }
        });
    }

    private void disableSelectCallerMediaButton() {

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                findViewById(R.id.selectMediaBtn).setClickable(false);
                drawSelectMediaButton(false);
            }
        });
    }

    private void disableSelectProfileMediaButton() {

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                findViewById(R.id.selectProfileMediaBtn).setClickable(false);
                drawSelectProfileMediaButton(false);
            }
        });
    }

    private void enableSelectMediaButton() {

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                findViewById(R.id.selectMediaBtn).setClickable(true);
                drawSelectMediaButton(true);
            }
        });
    }

    private void enableSelectProfileMediaButton() {

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                findViewById(R.id.selectProfileMediaBtn).setClickable(true);
                drawSelectProfileMediaButton(true);
            }
        });
    }

    private void disableSelectContactButton() {

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                findViewById(R.id.selectContactBtn).setEnabled(false);
                drawSelectContactButton(false);
            }
        });
    }

    private void enableSelectContactButton() {

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                findViewById(R.id.selectContactBtn).setEnabled(true);
                drawSelectContactButton(true);
            }
        });
    }

    private void disableProgressBar() {

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                _pBar = (ProgressBar) findViewById(R.id.progressBar);
                _pBar.setVisibility(ProgressBar.GONE);
            }
        });
    }

    private void enableProgressBar() {

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                _pBar = (ProgressBar) findViewById(R.id.progressBar);
                _pBar.setVisibility(ProgressBar.VISIBLE);
            }
        });
    }

    private void disableContactEditText() {

        findViewById(R.id.CallNumber).setEnabled(false);
    }

    private void enableContactEditText() {

        findViewById(R.id.CallNumber).setEnabled(true);
    }

    private void disableUserFetchProgressBar() {

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                pFetchUserBar = (ProgressBar) findViewById(R.id.fetchuserprogress);
                pFetchUserBar.setVisibility(ProgressBar.GONE);
            }
        });
    }

    private void enableUserFetchProgressBar() {

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                pFetchUserBar = (ProgressBar) findViewById(R.id.fetchuserprogress);
                pFetchUserBar.setVisibility(ProgressBar.VISIBLE);

                disableUserStatusIcon();
                vanishInviteButton();
            }
        });
    }

    private void userStatusRegistered() {

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                enableUserStatusPositiveIcon();
                vanishInviteButton();
            }
        });
    }

    private void vanishInviteButton() {

        ImageButton invite = (ImageButton) findViewById(R.id.inviteButton);
        invite.setVisibility(View.INVISIBLE);
        invite.setClickable(false);
    }

    private void disableInviteButton() {

        ImageButton invite = (ImageButton) findViewById(R.id.inviteButton);
        invite.setEnabled(false);
    }

    private void enableInviteButton() {

        ImageButton invite = (ImageButton) findViewById(R.id.inviteButton);
        invite.setVisibility(View.VISIBLE);
        invite.setClickable(true);
        invite.bringToFront();
    }

    private void userStatusUnregistered() {

        runOnUiThread(new Runnable() {
            @Override
            public void run() {

            enableUserStatusNegativeIcon();
            enableInviteButton();


            }
        });
    }

    private void disableUserStatusIcon() {

        ImageView userStatus = (ImageView) findViewById(R.id.userStatus);
        userStatus.setVisibility(View.INVISIBLE);
    }

    private void enableUserStatusPositiveIcon() {

        ImageView userStatus = (ImageView) findViewById(R.id.userStatus);
        userStatus.setImageResource(R.drawable.positive);
        userStatus.setVisibility(View.VISIBLE);
        userStatus.bringToFront();
    }

    private void enableUserStatusNegativeIcon() {

        ImageView userStatus = (ImageView) findViewById(R.id.userStatus);
        userStatus.setImageResource(R.drawable.negative);
        userStatus.setVisibility(View.VISIBLE);
        userStatus.bringToFront();
    }

    private void drawSelectMediaButton(boolean enabled) {

        LUT_Utils lut_utils = new LUT_Utils(_context, SpecialMediaType.CALLER_MEDIA);
        try {
            FileManager.FileType fType;
            ImageButton selectCallerMediaBtn = (ImageButton) findViewById(R.id.selectMediaBtn);

            if(!enabled)
                selectCallerMediaBtn.setImageResource(R.drawable.defaultpic_disabled);
            else {

                String lastUploadedMediaPath = lut_utils.getUploadedMediaPerNumber(_destPhoneNumber);
                if (!lastUploadedMediaPath.equals("")) {
                    fType = FileManager.getFileType(lastUploadedMediaPath);

                    BitmapWorkerTask task = new BitmapWorkerTask(selectCallerMediaBtn);
                    task.set_width(selectCallerMediaBtn.getWidth());
                    task.set_height(selectCallerMediaBtn.getHeight());
                    task.set_fileType(fType);
                    task.execute(lastUploadedMediaPath);

                    ImageView mediaStatus = (ImageView) findViewById(R.id.mediaStatus);
                    mediaStatus.setVisibility(View.VISIBLE);
                    mediaStatus.setImageResource(R.drawable.doublepositive);
                    mediaStatus.bringToFront();

                } else {// enabled but no uploaded media
                    if(enabled)
                        selectCallerMediaBtn.setImageResource(R.drawable.defaultpic_enabled);
                    else
                        selectCallerMediaBtn.setImageResource(R.drawable.defaultpic_disabled);

                ImageView mediaStatus = (ImageView) findViewById(R.id.mediaStatus);
                mediaStatus.setVisibility(View.INVISIBLE);}
            }

        } catch (FileInvalidFormatException |
                FileDoesNotExistException   |
                FileMissingExtensionException e) {
            e.printStackTrace();
            lut_utils.removeUploadedMediaPerNumber(_destPhoneNumber);
        }
    }

    private void drawSelectProfileMediaButton(boolean enabled) {

        LUT_Utils lut_utils = new LUT_Utils(_context, SpecialMediaType.PROFILE_MEDIA);
        try {
            FileManager.FileType fType;
            ImageButton selectProfileMediaBtn = (ImageButton) findViewById(R.id.selectProfileMediaBtn);

            if(!enabled)
                selectProfileMediaBtn.setImageResource(R.drawable.defaultpic_disabled);
            else {

                String lastUploadedMediaPath = lut_utils.getUploadedMediaPerNumber(_destPhoneNumber);
                if (!lastUploadedMediaPath.equals("")) {
                    fType = FileManager.getFileType(lastUploadedMediaPath);

                    BitmapWorkerTask task = new BitmapWorkerTask(selectProfileMediaBtn);
                    task.set_width(selectProfileMediaBtn.getWidth());
                    task.set_height(selectProfileMediaBtn.getHeight());
                    task.set_fileType(fType);
                    task.execute(lastUploadedMediaPath);
                } else // enabled but no uploaded media
                    selectProfileMediaBtn.setImageResource(R.drawable.defaultpic_enabled);
            }

        } catch (FileInvalidFormatException |
                FileDoesNotExistException   |
                FileMissingExtensionException e) {
            e.printStackTrace();
            lut_utils.removeUploadedMediaPerNumber(_destPhoneNumber);
        }
    }

    private void drawSelectRingToneName() {

        LUT_Utils lut_utils = new LUT_Utils(_context, SpecialMediaType.CALLER_MEDIA);
        String ringToneFilePath = lut_utils.getUploadedTonePerNumber(_destPhoneNumber);
        TextView ringtoneView = (TextView) findViewById(R.id.ringtoneName);

        if(!ringToneFilePath.isEmpty())
        {
            ringtoneView.setText(FileManager.getFileNameWithExtension(ringToneFilePath));
            ringtoneView.setBackgroundColor(0xFF00FF00);

            ImageView ringtoneStatus = (ImageView) findViewById(R.id.ringtoneStatus);
            ringtoneStatus.setVisibility(View.VISIBLE);
            ringtoneStatus.setImageResource(R.drawable.doublepositive);
            ringtoneStatus.bringToFront();

        }
        else
        {
            ImageView ringtoneStatus = (ImageView) findViewById(R.id.ringtoneStatus);
            ringtoneStatus.setVisibility(View.INVISIBLE);
        }
    }

    private void drawSelectContactButton(boolean enabled) {

        ImageButton selectContactButton = (ImageButton) findViewById(R.id.selectContactBtn);
        if(enabled)
            selectContactButton.setImageResource(R.drawable.select_contact_enabled);
        else
            selectContactButton.setImageResource(R.drawable.select_contact_disabled);
    }

    private void writeErrStatBar(final String text) {

    }

    private void writeInfoSnackBar(final String text) {

        Log.i(TAG, "Snackbar showing:"+text);

        SnackbarManager.show(
                Snackbar.with(getApplicationContext()) // context
                        .text(text) // text to display
                        .actionLabel("Close") // action button label
                        .duration(Snackbar.SnackbarDuration.LENGTH_LONG)
                        .actionListener(new ActionClickListener() {
                            @Override
                            public void onActionClicked(Snackbar snackbar) {
                                SnackbarManager.dismiss();
                            }
                        }) // action button's ActionClickListener
                , this); // activity where it is displayed

    }

    private void writeInfoSnackBar(final String text, final int color, Snackbar.SnackbarDuration duration) {

        Log.i(TAG, "Snackbar showing:"+text);

        SnackbarManager.show(
                Snackbar.with(getApplicationContext()) // context
                        .text(text) // text to display
                        .textColor(color)
                        .actionLabel("Close") // action button label
                        .duration(duration)
                        .actionListener(new ActionClickListener() {
                            @Override
                            public void onActionClicked(Snackbar snackbar) {
                                SnackbarManager.dismiss();
                            }
                        }) // action button's ActionClickListener
                , this); // activity where it is displayed
    }

    private void handleSnackBar(SnackbarData snackbarData) {

        switch(snackbarData.getStatus())
        {
            case CLOSE:
                SnackbarManager.dismiss();
            break;

            case SHOW:
                writeInfoSnackBar(snackbarData.getText(), snackbarData.getColor(), snackbarData.getmDuration());
            break;
        }
    }
}