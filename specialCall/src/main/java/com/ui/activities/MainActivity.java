package com.ui.activities;

import android.app.Activity;
import android.content.ActivityNotFoundException;
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
import android.provider.OpenableColumns;
import android.telephony.SmsManager;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Gravity;
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
import com.ipaulpro.afilechooser.utils.FileUtils;
import com.services.IncomingService;
import com.services.LogicServerProxyService;
import com.services.StorageServerProxyService;
import com.special.app.R;
import com.ui.components.AutoCompletePopulateListAsyncTask;
import com.ui.components.BitmapWorkerTask;
import com.utils.BroadcastUtils;
import com.utils.FilterWithSpaceAdapter;
import com.utils.LUT_Utils;
import com.utils.SharedPrefUtils;

import org.apache.commons.lang.math.NumberUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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

    private String buttonLabels[];
	private String myPhoneNumber = "";
	private String destPhoneNumber = "";
	private String destName = "";	
    private String tag = MainActivity.class.getSimpleName();
	private Uri outputFileUri;
	private ProgressBar pBar;
    private Context context;
    private LUT_Utils lut_utils;
	private BroadcastReceiver serviceReceiver;
	private IntentFilter serviceReceiverIntentFilter = new IntentFilter(Event.EVENT_ACTION);
    private abstract class ActivityRequestCodes {

        public static final int PICK_SONG = 1;
        public static final int SELECT_CONTACT = 2;
        public static final int SELECT_PICTURE = 3;

    }
    private AutoCompleteTextView mTxtPhoneNo;
    private int randomPIN=0;
    private static final int REQUEST_CHOOSER = 1234;
    private static boolean wasRegisteredChecked = false;
    private static boolean wasFileChooser=false;
    private Toast toast;

	@Override
	protected void onStart() {
		super.onStart();
        Log.i(tag, "onStart()");

		serviceReceiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {

					EventReport report = (EventReport) intent.getSerializableExtra(Event.EVENT_REPORT);
					eventReceived(new Event(this,report));
			}
		};
		registerReceiver(serviceReceiver, serviceReceiverIntentFilter);

	}

    @Override
    protected void onPause() {
        super.onPause();
        Log.i(tag, "onPause()");

        wasRegisteredChecked=false;
        if(serviceReceiver!=null)
        {  try
        {unregisterReceiver(serviceReceiver);}
        catch (Exception ex){
            Log.e(tag, ex.getMessage().toString());}
        }
        saveInstanceState();
    }

	@Override
    protected void onResume() {
        super.onResume();
        Log.i(tag, "onResume()");

        context = getApplicationContext();
        String appState = getState();
        Log.i(tag, "App State:" + appState);

        // startService(new Intent(this, IncomingService.class));

        Intent i = new Intent(this, IncomingService.class);
        i.setAction(IncomingService.ACTION_START);
        this.startService(i);

        Log.i(tag, "startService: IncomingService");

        if(!appState.equals(AppStateManager.STATE_LOGGED_OUT)) {

            // Taking Focus from AutoCompleteTextView in the end, so he won't pop up :) added also focus capabilities to the MainActivity Layout XML
            findViewById(R.id.mainActivity).requestFocus();

            registerReceiver(serviceReceiver, serviceReceiverIntentFilter);

            // ASYNC TASK To Populate all contacts , it can take long time and it delays the UI
            new AutoCompletePopulateListAsyncTask(this, mTxtPhoneNo).execute();

            if(appState.equals(AppStateManager.STATE_DISABLED)) {

                myPhoneNumber = Constants.MY_ID(context);
                initializeConnection();

            }

        }


        switch (appState)
        {
            case AppStateManager.STATE_LOGGED_OUT:
                initializeLoginUI();
            break;

            case AppStateManager.STATE_IDLE:
                stateIdle(tag + "::onResume() STATE_IDLE", "", Color.BLACK);
                restoreInstanceState();
                writeInfoStatBar("");
            break;

            case AppStateManager.STATE_READY:
                stateReady(tag + "::onResume() STATE_READY", "");
                restoreInstanceState();
            break;

            case AppStateManager.STATE_LOADING:
                String loadingMsg = SharedPrefUtils.getString(context, SharedPrefUtils.GENERAL, SharedPrefUtils.LOADING_MESSAGE);
                stateLoading(tag + "::onResume() STATE_LOADING", loadingMsg, Color.YELLOW);
                restoreInstanceState();
            break;

            case AppStateManager.STATE_DISABLED:
                String errMsg = "Disconnected. Check your internet connection.";
                stateDisabled(tag + "::onResume() STATE_DISABLED", errMsg);
                restoreInstanceState();
            break;
        }
	}

	@Override
    protected void onDestroy() {
	    super.onDestroy();
        Log.i(tag, "onDestroy()");
	 }

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
        Log.i(tag, "onCreate()");

        context = getApplicationContext();
        lut_utils = new LUT_Utils(context);

		if (getState().equals(AppStateManager.STATE_LOGGED_OUT)) {

            initializeLoginUI();

		} else {
			initializeUI();
            if (getState().equals(AppStateManager.STATE_LOGGED_IN)) {
                stateIdle(tag + "::onCreate()", "", Color.BLACK);
            }
		}
	}

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);


        if (resultCode == RESULT_OK) {

            wasRegisteredChecked = false;


                FileManager fm = null;

               if (requestCode == REQUEST_CHOOSER) {

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
                           uri = outputFileUri;
                       } else {
                           uri = data.getData();
                       }

                       // Get the File path from the Uri
                       String path = FileUtils.getPath(this, uri);
                       // Alternatively, use FileUtils.getFile(Context, Uri)
                       if (path != null) {
                            if (FileUtils.isLocal(path)) {

                                if (isCamera) {
                                    File file = new File(path);
                                    file.renameTo(new File(path += ".jpeg"));
                                }

                                fm = new FileManager(path);

                                if (fm != null) {
                                    wasRegisteredChecked = true;
                                    wasFileChooser=true;
                                    Log.e(tag,"onActivityResult RESULT_OK _ Rony");
                                    Intent i = new Intent(context, StorageServerProxyService.class);
                                    i.setAction(StorageServerProxyService.ACTION_UPLOAD);
                                    i.putExtra(StorageServerProxyService.DESTINATION_ID, destPhoneNumber);
                                    i.putExtra(StorageServerProxyService.FILE_TO_UPLOAD, fm);
                                    context.startService(i);

                                } else
                                    writeErrStatBar("An unknown error occured during file upload");
                            }
                       }


                   } catch (NullPointerException e) {
                       e.printStackTrace();
                       Log.e(tag, "It seems there was a problem with the file path.");
                       callErrToast("It seems there was a problem with the file path");
                   } catch (FileExceedsMaxSizeException e) {
                       callErrToast("Please select a file that weights less than:" +
                               FileManager.getFileSizeFormat(FileManager.MAX_FILE_SIZE));
                   } catch (FileInvalidFormatException e) {
                       e.printStackTrace();
                       callErrToast("Please select a valid format");
                   } catch (FileDoesNotExistException | FileMissingExtensionException e) {
                       e.printStackTrace();
                       callErrToast(e.getMessage());
                   }  catch (InvalidDestinationNumberException e)
                   {
                       //callErrToast("There was a problem with the destination number. Please try again");
                       writeErrStatBar("There was a problem with the destination number. Please try again");
                   }
               }

            restoreInstanceState();

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

                                    setDestPhoneNumber(number);
                                    destName = name;

                                    final AutoCompleteTextView ed_destinationNumber = ((AutoCompleteTextView) findViewById(R.id.CallNumber));
                                    if(ed_destinationNumber!=null) {
                                        ed_destinationNumber.setText(destPhoneNumber);
                                    }

                                    final TextView ed_destinationName = ((TextView) findViewById(R.id.destName));
                                    if(ed_destinationName!=null) {
                                        ed_destinationName.setText(destName);
                                    }

                                    saveInstanceState();

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

	private void populateArrays() {

		//TODO Move this to strings.xml as proper
		
		buttonLabels[0] = "CALL NOW";
		buttonLabels[1] = "How Will i look like";
		buttonLabels[2] = "Select Ringtone";
		buttonLabels[3] = "uploading video clip";
		buttonLabels[4] = "Download From Server";
		buttonLabels[5] = "Select Contact";
	}

	private void selectVisualMedia() {

		// Determine Uri of camera image to save.
		final File root = new File(Environment.getExternalStorageDirectory()
				+ File.separator + "MyDir" + File.separator);
		root.mkdirs();
		final String fname = "MyImage";
		final File sdImageMainDirectory = new File(root, fname);
		outputFileUri = Uri.fromFile(sdImageMainDirectory);

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
			intent.putExtra(MediaStore.EXTRA_OUTPUT, outputFileUri);
			cameraIntents.add(intent);
		}
        cameraIntents.add(videoIntent);

        // Create the ACTION_GET_CONTENT Intent
       final Intent intent = FileUtils.createGetContentIntent();

        Intent chooserIntent = Intent.createChooser(intent, "Select a file");

        // Add the camera options.
        chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS,
                cameraIntents.toArray(new Parcelable[cameraIntents.size()]));

        wasFileChooser=true;
        wasRegisteredChecked=true;
        startActivityForResult(chooserIntent, REQUEST_CHOOSER);



	}


	public void onClick(View v) {

        // Saving instance state
        saveInstanceState();

		int id = v.getId();
		if (id == R.id.CallNow) {

			launchDialer(destPhoneNumber);

		} else if (id == R.id.selectMediaBtn) {

         selectVisualMedia();

		}
        else if (id == R.id.selectProfileMediaBtn) {

           selectVisualMedia();

        }
        else if (id == R.id.selectRingtoneBtn) {

            selectVisualMedia();

		} else if (id == R.id.selectContactBtn) {

			Intent intent = new Intent(Intent.ACTION_PICK);
			intent.setType(ContactsContract.CommonDataKinds.Phone.CONTENT_TYPE);
			startActivityForResult(intent, ActivityRequestCodes.SELECT_CONTACT);
		}
        else if (id == R.id.clear) {

            AutoCompleteTextView textViewToClear = (AutoCompleteTextView)findViewById(R.id.CallNumber);
            textViewToClear.setText("");

        }

		else if (id == R.id.login_btn) {

           String myVerificationcode = ((EditText) findViewById(R.id.SMSCode)).getText().toString();
		//if (myVerificationcode.equals(String.valueOf(randomPIN))){    // NEED TO FIND A SMS GATEWAY FIRST
                myPhoneNumber = ((EditText) findViewById(R.id.LoginNumber))
                        .getText().toString();

                SharedPrefUtils.setString(context,
                        SharedPrefUtils.GENERAL, SharedPrefUtils.MY_NUMBER, myPhoneNumber);

                initializeConnection();

                File SpecialCallIncoming = new File(Constants.specialCallIncomingPath);
                SpecialCallIncoming.mkdirs();

                File SpecialCallOutgoing = new File(Constants.specialCallOutgoingPath);
                SpecialCallOutgoing.mkdirs();

                initializeUI();
                new AutoCompletePopulateListAsyncTask(this, mTxtPhoneNo).execute();
                stateIdle(tag + "::onClick() R.id.login", "", Color.BLACK);
          //  }// NEED TO FIND A SMS GATEWAY FIRST
          /*  else// NEED TO FIND A SMS GATEWAY FIRST
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
                y.setClass(context, Settings.class);

                startActivity(y);

                break;

            default:

                saveInstanceState();

                Intent o = new Intent();
                o.setClass(context, Settings.class);

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

                              if(!Constants.MY_TOKEN(context).equals("")) {
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

                                          String token = SharedPrefUtils.getString(context, SharedPrefUtils.GENERAL, SharedPrefUtils.MY_DEVICE_TOKEN);
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
                                          smsManager.sendTextMessage(loginNumber.getText().toString(), null, "SpecialCall SmsVerificationCode: "+String.valueOf(randomPIN), null, null);
                                          Toast.makeText(getApplicationContext(), "Message Sent To: " +loginNumber.getText().toString(),
                                                  Toast.LENGTH_LONG).show();
                                      } catch (Exception ex) {
                                          Toast.makeText(getApplicationContext(),
                                                  ex.getMessage().toString(),
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

        int entries = 6;
        buttonLabels = new String[entries];

		// Populate the data arrays
		populateArrays();

		// Set up buttons and attach click listeners

		// Solving bug that causes EditText to be unfocusable at startup

        mTxtPhoneNo  = (AutoCompleteTextView) findViewById(R.id.CallNumber);

        mTxtPhoneNo.setRawInputType(InputType.TYPE_CLASS_TEXT);

        mTxtPhoneNo.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> av, View arg1, int index, long arg3) {
                String[] nameAndPhone = ((String) av.getItemAtPosition(index)).split("\\\n");

                String name = nameAndPhone[0];
                String number = nameAndPhone[1];
                String NumericNumber = toNumeric(number);
                Log.e(tag, NumericNumber);
                if (NumericNumber.startsWith("972")){
                    NumericNumber= NumericNumber.replaceFirst("972","0");
               }
                if (NumericNumber.startsWith("9720")){
                    NumericNumber= NumericNumber.replaceFirst("9720","0");
                }

                mTxtPhoneNo.setText(NumericNumber);

                destName = name;
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
                        !wasRegisteredChecked) {

                    wasRegisteredChecked = true;
                    destPhoneNumber = destPhone;
                    drawSelectMediaButton(false);
                    drawSelectRingToneButton();

                    if (!getState().equals(AppStateManager.STATE_DISABLED) &&
                            !getState().equals(AppStateManager.STATE_LOADING)) {
                        String msg = "Fetching user data...";
                        stateLoading(tag + " onTextchanged()", msg, Color.GREEN);
                        BroadcastUtils.sendEventReportBroadcast(context, tag + " onTextchanged()", new EventReport(EventType.FETCHING_USER_DATA, msg, null));

                        Intent i = new Intent(context, LogicServerProxyService.class);
                        i.setAction(LogicServerProxyService.ACTION_ISREGISTERED);
                        i.putExtra(LogicServerProxyService.DESTINATION_ID, destPhone);
                        context.startService(i);
                    }
                } else {

                    wasRegisteredChecked = false;

                    if (wasFileChooser) {
                        wasRegisteredChecked = true;
                        wasFileChooser = false;
                    }
                    destPhoneNumber = "";
                    destName = "";

                    if (10 != s.length() ||
                            !NumberUtils.isNumber(destPhone))
                    {  runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                ImageView userStatus = (ImageView) findViewById(R.id.userStatus);
                                userStatus.setVisibility(View.INVISIBLE);
                            }
                        });
                      }


                    setDestNameTextView();
                    saveInstanceState();
                    if (getState().equals(AppStateManager.STATE_READY))
                        stateIdle(tag + "::onTextChanged()", "", Color.BLACK);
                    if (getState().equals(AppStateManager.STATE_READY))
                        stateIdle(tag + "::onTextChanged()", "", Color.BLACK);
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

		Button button3 = (Button) findViewById(R.id.selectRingtoneBtn);
		button3.setText(buttonLabels[2]);
		button3.getBackground().setColorFilter(0xFFFF0000,
                PorterDuff.Mode.MULTIPLY);
		button3.setOnClickListener(this);

		ImageButton button6 = (ImageButton) findViewById(R.id.selectContactBtn);
		// button6.setText(buttonLabels[5]);
		button6.setOnClickListener(this);

        ImageButton button7 = (ImageButton) findViewById(R.id.selectProfileMediaBtn);
        button7.setOnClickListener(this);

        ImageButton button8 = (ImageButton) findViewById(R.id.clear);
        button8.setOnClickListener(this);

	}

    public void eventReceived(Event event) {

		final EventReport report = event.report();

		switch (report.status()) {

		case UPLOAD_SUCCESS:
            if(isContactSelected())
                stateReady(tag + "EVENT: UPLOAD_SUCCESS", report.desc());
            else
                stateIdle(tag +" EVENT: UPLOAD_SUCCESS", report.desc(), Color.GREEN);
			break;

		case UPLOAD_FAILURE:
            stateIdle(tag +" EVENT: UPLOAD_FAILURE", "", Color.RED);
            writeErrStatBar(report.desc());
            break;

		case DOWNLOAD_SUCCESS:
            writeInfoStatBar(report.desc());
			break;

		case DOWNLOAD_FAILURE:
            writeErrStatBar(report.desc());
			break;

        case REGISTER_SUCCESS:
            stateIdle(tag + " EVENT: REGISTER_SUCCESS", report.desc(), Color.GREEN);
            break;

        case USER_REGISTERED_TRUE:
            destPhoneNumber = (String) report.data();
            stateReady(tag + " EVENT: USER_REGISTERED_TRUE", report.desc());

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    ImageView userStatus = (ImageView) findViewById(R.id.userStatus);
                    userStatus.setImageResource(R.drawable.positive);
                    userStatus.setVisibility(View.VISIBLE);
                    userStatus.bringToFront();
                }
            });


            break;

        case USER_REGISTERED_FALSE:
            destPhoneNumber = (String) report.data();
            stateIdle(tag+" EVENT: USER_REGISTERED_FALSE", report.desc(), Color.RED);

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    ImageView userStatus = (ImageView) findViewById(R.id.userStatus);
                    userStatus.setImageResource(R.drawable.negative);
                    userStatus.setVisibility(View.VISIBLE);
                    userStatus.bringToFront();
                }
            });

            break;

		case ISREGISTERED_ERROR:
            destPhoneNumber = (String) report.data();
			stateIdle(tag + "EVENT: ISREGISTERED_ERROR", "", Color.BLACK);
            writeErrStatBar(report.desc());

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    ImageView userStatus = (ImageView) findViewById(R.id.userStatus);
                    userStatus.setImageResource(R.drawable.negative);
                    userStatus.setVisibility(View.VISIBLE);
                    userStatus.bringToFront();
                }
            });

			break;

        case REFRESH_UI:
            String msg = report.desc();
            if(isContactSelected())
                stateReady(tag + "EVENT: REFRESH_UI", msg);
            else
                stateIdle(tag + "EVENT: REFRESH_UI", msg, Color.GREEN);
            break;

        case COMPRESSING:
            stateLoading(tag + " EVENT: COMPRESSING", report.desc(), Color.GREEN);
            break;

        case RECONNECT_ATTEMPT:
            stateLoading(tag + " EVENT: RECONNECT_ATTEMPT", report.desc(), Color.RED);
            break;

        case CONNECTING:
            stateLoading(tag + " EVENT: CONNECTING", report.desc(), Color.YELLOW);
            break;

        case CONNECTED:
            if(isContactSelected())
                stateReady(tag + "EVENT: REFRESH_UI", report.desc());
            else
                stateIdle(tag + "EVENT: REFRESH_UI", report.desc(), Color.GREEN);
            break;

        case DISCONNECTED:
            stateDisabled(tag+" EVENT: DISCONNECTED", report.desc());
            break;

//		case CLOSE_APP:
//			writeErrStatBar(report.desc());
//			finish();
//			break;

		case DISPLAY_ERROR:
			writeErrStatBar(report.desc());
			break;
			
		case DISPLAY_MESSAGE:
            writeInfoStatBar(report.desc());
			break;

        case LOADING_TIMEOUT:

            stateIdle(tag+" EVENT: LOADING_TIMEOUT", report.desc(), Color.YELLOW);
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
			Log.e(tag, "Undefined event status on EventReceived");
		}

	}


	/* -------------- Assisting methods -------------- */

    private void saveInstanceState() {

        // Saving destination number
        final AutoCompleteTextView ed_destinationNumber = ((AutoCompleteTextView) findViewById(R.id.CallNumber));
		if(ed_destinationNumber!=null) {
            destPhoneNumber = ed_destinationNumber.getText().toString();
            SharedPrefUtils.setString(context, SharedPrefUtils.GENERAL, SharedPrefUtils.DESTINATION_NUMBER, destPhoneNumber);
        }

        // Saving destination name
        final TextView ed_destinationName = ((TextView) findViewById(R.id.destName));
        if(ed_destinationName!=null) {
            destName = ed_destinationName.getText().toString();
            SharedPrefUtils.setString(context, SharedPrefUtils.GENERAL, SharedPrefUtils.DESTINATION_NAME, destName);
        }
    }

    private void restoreInstanceState() {


        // Restoring destination number
        final AutoCompleteTextView ed_destinationNumber =
                (AutoCompleteTextView) findViewById(R.id.CallNumber);
        String destNumber = SharedPrefUtils.getString(context, SharedPrefUtils.GENERAL, SharedPrefUtils.DESTINATION_NUMBER);
        if(ed_destinationNumber!=null && destNumber!=null)
            ed_destinationNumber.setText(destNumber);

        // Restoring destination name
        destName = SharedPrefUtils.getString(context, SharedPrefUtils.GENERAL, SharedPrefUtils.DESTINATION_NAME);
        setDestNameTextView();

        // Restoring my phone number
        myPhoneNumber = Constants.MY_ID(context);

    }

    private void checkDestinationNumber() throws InvalidDestinationNumberException {

        if(destPhoneNumber==null)
            throw new InvalidDestinationNumberException();
        if(destPhoneNumber.equals("") || destPhoneNumber.length() < 10)
            throw new InvalidDestinationNumberException();
    }

    private void setDestPhoneNumber(String destPhoneNumberAlphaNumeric) {

        destPhoneNumber = toNumeric(destPhoneNumberAlphaNumeric);
    }

    private void setDestNameTextView() {

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                final TextView tv_destName =
                        (TextView) findViewById(R.id.destName);
                if(tv_destName!=null && destName!=null)
                    tv_destName.setText(destName);
            }
        });

    }

    private String toNumeric(String str) {

        return str.replaceAll("[^0-9]","");
    }
    private void initializeConnection() {

        Intent i = new Intent();
        i.setClass(getBaseContext(), LogicServerProxyService.class);
        if(AppStateManager.getAppState(context).equals(AppStateManager.STATE_LOGGED_OUT))
            i.setAction(LogicServerProxyService.ACTION_REGISTER);
        else
           i.setAction(LogicServerProxyService.ACTION_RECONNECT);

        startService(i);

    }

    private boolean isContactSelected() {

        AutoCompleteTextView edCN = (AutoCompleteTextView) findViewById(R.id.CallNumber);
        String s = edCN.getText().toString();
        return 10 == s.length();
    }

    /* -------------- UI methods -------------- */


    /* --- UI States --- */

    public void stateReady(String tag, String msg) {

        setState(tag, AppStateManager.STATE_READY);

        if (!tag.contains("USER_REGISTERED_TRUE"))
        writeInfoStatBar(msg);

        runOnUiThread(new Runnable() {
            @Override
            public void run() {

                disableProgressBar();
                enableSelectMediaButton();
                enableContactEditText();
                enableSelectRingToneButton();
                enableSelectContactButton();
                enableCallButton();
            }
        });
    }

    public void stateIdle(String tag, String msg, int color) {

        if (!tag.contains("USER_REGISTERED_FALSE"))
        writeInfoStatBar(msg, color);
        setState(tag, AppStateManager.STATE_IDLE);

        runOnUiThread(new Runnable() {

            @Override
            public void run() {

                disableProgressBar();
                enableSelectContactButton();
                enableContactEditText();
                disableSelectMediaButton();
                disableSelectRingToneButton();
                disableCallButton();
            }
        });
    }

    public void stateDisabled(String tag, String msg) {

        setState(tag, AppStateManager.STATE_DISABLED);
        writeErrStatBar(msg);

        runOnUiThread(new Runnable() {

            @Override
            public void run() {
                disableSelectMediaButton();
                disableProgressBar();
                disableSelectRingToneButton();
                disableSelectContactButton();
                disableContactEditText();
                disableCallButton();
            }
        });
    }

    public void stateLoading(String tag, String msg, int color) {

        setState(tag, AppStateManager.STATE_LOADING);
        SharedPrefUtils.setString(context, SharedPrefUtils.GENERAL, SharedPrefUtils.LOADING_MESSAGE, msg);

        enableProgressBar();
        writeInfoStatBar(msg, color);
        runOnUiThread(new Runnable() {

            @Override
            public void run() {
                disableSelectMediaButton();
                disableSelectRingToneButton();
                disableSelectContactButton();
                disableContactEditText();
                disableCallButton();
            }
        });
    }

    private String getState() {

        return AppStateManager.getAppState(context);
    }

    private void setState(String tag, String state) {

        AppStateManager.setAppState(context, tag, state);
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

    private void disableSelectMediaButton() {

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                findViewById(R.id.selectMediaBtn).setClickable(false);
                drawSelectMediaButton(false);
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

    private void enableSelectRingToneButton() {

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                findViewById(R.id.selectRingtoneBtn).setEnabled(true);
                drawSelectRingToneButton();
            }
        });
    }

    private void disableSelectRingToneButton() {

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                findViewById(R.id.selectRingtoneBtn).setEnabled(false);
                drawSelectRingToneButton();
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
                pBar = (ProgressBar) findViewById(R.id.progressBar);
                pBar.setVisibility(ProgressBar.GONE);
            }
        });
	}

	private void enableProgressBar() {

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                pBar = (ProgressBar) findViewById(R.id.progressBar);
                pBar.setVisibility(ProgressBar.VISIBLE);
            }
        });
	}

    private void disableContactEditText() {

        findViewById(R.id.CallNumber).setEnabled(false);
    }

    private void enableContactEditText() {

       findViewById(R.id.CallNumber).setEnabled(true);
    }

	private void drawSelectMediaButton(boolean enabled)
    {
        try {
            FileManager.FileType fType;
            ImageButton selectMediaBtn = (ImageButton) findViewById(R.id.selectMediaBtn);

            String lastUploadedMediaPath = lut_utils.getUploadedMediaPerNumber(destPhoneNumber);
            if (!lastUploadedMediaPath.equals("")) {
                fType = FileManager.getFileType(lastUploadedMediaPath);

				BitmapWorkerTask task = new BitmapWorkerTask(selectMediaBtn);
				task.set_width(selectMediaBtn.getWidth());
				task.set_height(selectMediaBtn.getHeight());
				task.set_fileType(fType);
				task.execute(lastUploadedMediaPath);
            }
            else {
                if(enabled)
                    selectMediaBtn.setImageResource(R.drawable.defaultpic_enabled);
                else
                    selectMediaBtn.setImageResource(R.drawable.defaultpic_disabled);
            }


        } catch (FileInvalidFormatException | FileDoesNotExistException | FileMissingExtensionException e) {
            e.printStackTrace();
            SharedPrefUtils.remove(context, SharedPrefUtils.UPLOADED_MEDIA_THUMBNAIL, destPhoneNumber);
            }


    }

    private void drawSelectRingToneButton() {

        String wasRingToneUploaded = lut_utils.getUploadedRingTonePerNumber(destPhoneNumber);

        TextView ringtoneView = (TextView) findViewById(R.id.ringtoneName);

        Button ringButton = (Button) findViewById(R.id.selectRingtoneBtn);
        if(!wasRingToneUploaded.isEmpty())
        {
            ringButton.getBackground().setColorFilter(0xFF00FF00,
                    PorterDuff.Mode.MULTIPLY);


            ringtoneView.setText(FileManager.getFileNameWithExtension(wasRingToneUploaded));
            ringtoneView.setBackgroundColor(0xFF00FF00);


        }
        else
        {
            if(ringButton.isEnabled())
            {  ringButton.getBackground().setColorFilter(Color.LTGRAY,
                        PorterDuff.Mode.MULTIPLY);

                ringtoneView.setBackgroundColor(Color.WHITE);
                ringtoneView.setText("No Ringtone Ready Yet!");
            }
            else{
                ringButton.getBackground().setColorFilter(Color.DKGRAY,
                        PorterDuff.Mode.MULTIPLY);
                ringtoneView.setBackgroundColor(Color.DKGRAY);
                ringtoneView.setText("Can't Use Ringtone !");
            }
        }
    }

    private void drawSelectContactButton(boolean enabled) {

        ImageButton selectContactButton = (ImageButton) findViewById(R.id.selectContactBtn);
        if(enabled)
            selectContactButton.setImageResource(R.drawable.select_contact_enabled);
        else
            selectContactButton.setImageResource(R.drawable.select_contact_disabled);
    }

	private void callErrToast(final String text) {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				Toast toast = Toast.makeText(context, text,
						Toast.LENGTH_LONG);
				TextView v = (TextView) toast.getView().findViewById(
						android.R.id.message);
				v.setTextColor(Color.RED);
				toast.show();
			}
		});
	}


    private void writeErrStatBar(final String text) {

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if(toast != null) // if there is already a Toast , than cancel it because there is a new Toast
                {
                    toast.cancel();
                }

                toast = Toast.makeText(context, text,
                        Toast.LENGTH_LONG);
                TextView v = (TextView) toast.getView().findViewById(
                        android.R.id.message);
                v.setTextColor(Color.GREEN);
                toast.setGravity(Gravity.TOP, Gravity.CENTER_VERTICAL, Gravity.CENTER_VERTICAL);
                toast.show();
            }
        });
    }

    private void writeInfoStatBar(final String text) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if(toast != null) // if there is already a Toast , than cancel it because there is a new Toast
                {
                    toast.cancel();
                }
                toast = Toast.makeText(context, text,
                        Toast.LENGTH_LONG);
                TextView v = (TextView) toast.getView().findViewById(
                        android.R.id.message);
                v.setTextColor(Color.GREEN);
                toast.setGravity(Gravity.TOP, Gravity.CENTER_VERTICAL, Gravity.CENTER_VERTICAL);
                toast.show();
            }
        });
    }

    private void writeInfoStatBar(final String text, final int g) {

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if(toast != null) // if there is already a Toast , than cancel it because there is a new Toast
                    {
                        toast.cancel();
                    }
                    toast = Toast.makeText(context, text,
                            Toast.LENGTH_LONG);
                    TextView v = (TextView) toast.getView().findViewById(
                            android.R.id.message);
                    v.setTextColor(g);
                    toast.setGravity(Gravity.TOP, Gravity.CENTER_VERTICAL, Gravity.CENTER_VERTICAL);
                    toast.show();
                }
            });
    }
}