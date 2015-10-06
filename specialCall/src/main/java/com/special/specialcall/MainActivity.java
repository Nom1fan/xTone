package com.special.specialcall;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.os.Parcelable;
import android.provider.ContactsContract;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import com.android.services.IncomingReceiver;
import com.android.services.ServerProxy;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import DataObjects.SharedConstants;
import DataObjects.TransferDetails;
import EventObjects.Event;
import EventObjects.EventReport;
import Exceptions.FileDoesNotExistException;
import Exceptions.FileExceedsMaxSizeException;
import Exceptions.FileInvalidFormatException;
import Exceptions.FileMissingExtensionException;
import Exceptions.InvalidDestinationNumberException;
import FilesManager.FileManager;
import data_objects.Constants;
import utils.AppStateUtils;
import utils.LUT_Utils;
import data_objects.SharedPrefUtils;


public class MainActivity extends Activity implements OnClickListener {

	private int entries = 6;
	private String buttonLabels[];
	private ServerProxy serverProxy;
	private String myPhoneNumber = "";
	private String destPhoneNumber = "";
	private String destName = "";	
    private String tag = MainActivity.class.getSimpleName();
	private Uri outputFileUri;
	private ProgressBar pBar;
	private boolean mIsBound = false;
    private Context context;
    private LUT_Utils lut_utils;
	private BroadcastReceiver serviceReceiver;
	private IntentFilter serviceReceiverIntentFilter = new IntentFilter(Event.EVENT_ACTION);

	private abstract class ActivityRequestCodes {

		public static final int PICK_SONG = 1;
		public static final int SELECT_CONTACT = 2;
		public static final int SELECT_PICTURE = 3;

	}

	@Override
	protected void onStart() {

		super.onStart();

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
    protected void onPause()
    {
        super.onPause();

        doUnbindService();
        if(serviceReceiver!=null)
            unregisterReceiver(serviceReceiver);
        saveInstanceState();
    }

	@Override
    protected void onResume() {

		super.onResume();

        context = getApplicationContext();
        String appState = getState();
        Log.i(tag, "App State:" + appState);

        if(!appState.equals(SharedPrefUtils.STATE_LOGGED_OUT)) {

            startService(new Intent(this, IncomingReceiver.class));

            registerReceiver(serviceReceiver, serviceReceiverIntentFilter);

            if(!appState.equals(SharedPrefUtils.STATE_DISABLED)) {
                if (serverProxy == null) {
                    myPhoneNumber = SharedPrefUtils.getString(context, SharedPrefUtils.GENERAL, SharedPrefUtils.MY_NUMBER);
                    SharedConstants.MY_ID = myPhoneNumber;
                    initializeConnection();
                }
                else
                    doBindService();
            }

        }


        switch (appState)
        {
            case SharedPrefUtils.STATE_LOGGED_OUT:
                initializeLoginUI();
            break;

            case SharedPrefUtils.STATE_IDLE:
                stateIdle(tag + "::onResume() STATE_IDLE");
                restoreInstanceState();
            break;

            case SharedPrefUtils.STATE_READY:
                stateReady(tag + "::onResume() STATE_READY");
                restoreInstanceState();
            break;

            case SharedPrefUtils.STATE_LOADING:
                String loadingMsg = SharedPrefUtils.getString(context, SharedPrefUtils.GENERAL, SharedPrefUtils.LOADING_MESSAGE);
                stateLoading(tag + "::onResume() STATE_LOADING", loadingMsg, Color.YELLOW);
                restoreInstanceState();
            break;

            case SharedPrefUtils.STATE_DISABLED:
                writeErrStatBar("Disconnected. Check your internet connection.");
                stateDisabled(tag + "::onResume() STATE_DISABLED");
                restoreInstanceState();
            break;
        }
	}

	@Override
    protected void onDestroy() {
	    super.onDestroy();
        Log.i(tag, tag+" is being destroyed");
	 }

	@Override
	protected void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);

        context = getApplicationContext();
        lut_utils = new LUT_Utils(context);

		if (getState().equals(SharedPrefUtils.STATE_LOGGED_OUT)) {

            initializeLoginUI();

		} else {
			initializeUI();
            if (getState().equals(SharedPrefUtils.STATE_LOGGED_IN)) {
                stateIdle(tag+"::onCreate()");
            }
		}
	}

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {

            try {

                restoreInstanceState();

                FileManager fm = null;

                if (requestCode == ActivityRequestCodes.SELECT_PICTURE) {
                    final boolean isCamera;

                    checkDestinationNumber();

                    if (data == null) {
                        isCamera = true;
                    } else {
                        final String action = data.getAction();
                        if (action == null) {
                            isCamera = false;
                        } else {
                            isCamera = action
                                    .equals(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
                        }
                    }

                    Uri selectedImageOrVideoUri;
                    if (isCamera) {
                        selectedImageOrVideoUri = outputFileUri;
                    } else {
                        selectedImageOrVideoUri = data.getData();
                    }

                    String imgOrVidPath = getRealPathFromURI(selectedImageOrVideoUri);

                    if(isCamera)
                    {
                        File file = new File(imgOrVidPath);
                        file.renameTo(new File(imgOrVidPath+=".jpeg"));

                    }

                    fm = new FileManager(imgOrVidPath);
                }

                if (requestCode == ActivityRequestCodes.PICK_SONG) {

                    checkDestinationNumber();

                    Uri uriRingtone = data.getData();
                    String ringTonePath = getRealPathFromURI(uriRingtone);
                    fm = new FileManager(ringTonePath);
                }

                if(fm!=null) {
                    serverProxy.uploadFileToServer(destPhoneNumber, fm);
                    setState(tag + "::onActivityResult upload file", SharedPrefUtils.STATE_LOADING);
                            SharedPrefUtils.setString(context, SharedPrefUtils.GENERAL, SharedPrefUtils.LOADING_MESSAGE, "Uploading file to server...");
                }
            }
            catch(NullPointerException e)
            {
                e.printStackTrace();
                Log.e(tag,"It seems there was a problem with the file path.");
                callErrToast("It seems there was a problem with the file path");
                //writeErrStatBar("It seems there was a problem with the file path");
            }
            catch (FileExceedsMaxSizeException e)
            {
                callErrToast("Please select a file that weights less than:"+
                        FileManager.getFileSizeFormat(FileManager.MAX_FILE_SIZE));
//                writeErrStatBar("Please select a file that weights less than:"+
//                        FileManager.getFileSizeFormat(FileManager.MAX_FILE_SIZE));
            }
            catch (InvalidDestinationNumberException e)
            {
                //callErrToast("There was a problem with the destination number. Please try again");
                writeErrStatBar("There was a problem with the destination number. Please try again");
            }
            catch (FileInvalidFormatException e)
            {
                e.printStackTrace();
                callErrToast("Please select a valid format");
                //writeErrStatBar("Please select a valid format");
            } catch (FileDoesNotExistException | FileMissingExtensionException e) {
                e.printStackTrace();
                callErrToast(e.getMessage());
                //writeErrStatBar(e.getMessage());
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

                                    setDestPhoneNumber(number);
                                    destName = name;

                                    final EditText ed_destinationNumber = ((EditText) findViewById(R.id.CallNumber));
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

	// Launch the phone dialer
	public void launchDialer(String number) {
		String numberToDial = "tel:" + number;
		startActivity(new Intent(Intent.ACTION_DIAL, Uri.parse(numberToDial)));
	}

	// Method to populate the data arrays
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
		final List<Intent> cameraIntents = new ArrayList<Intent>();
		final Intent captureIntent = new Intent(
				android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
		final PackageManager packageManager = getPackageManager();
		final List<ResolveInfo> listCam = packageManager.queryIntentActivities(
				captureIntent, 0);

		for (ResolveInfo res : listCam) {
			final String packageName = res.activityInfo.packageName;
			final Intent intent = new Intent(captureIntent);
			intent.setComponent(new ComponentName(res.activityInfo.packageName,
					res.activityInfo.name));
			intent.setPackage(packageName);
			intent.putExtra(MediaStore.EXTRA_OUTPUT, outputFileUri);
			cameraIntents.add(intent);
		}

		// Filesystem.
		final Intent galleryIntent = new Intent();
		galleryIntent.setType("*/*");
		galleryIntent.setAction(Intent.ACTION_GET_CONTENT);

		// Chooser of filesystem options.
		final Intent chooserIntent = Intent.createChooser(galleryIntent,
				"Select Source");

        // Add the camera options.
        chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS,
				cameraIntents.toArray(new Parcelable[] {}));

		startActivityForResult(chooserIntent,
				ActivityRequestCodes.SELECT_PICTURE);
	}

	private void selectRingtone() {


		Intent intent_selectRingTone = new Intent();
		intent_selectRingTone.setType("audio/*");
		intent_selectRingTone.setAction(Intent.ACTION_GET_CONTENT);

		try
        {
			startActivityForResult(intent_selectRingTone, ActivityRequestCodes.PICK_SONG);
		}
        catch(ActivityNotFoundException e)
        {

            intent_selectRingTone.setType("*/*");

            try
            {
                startActivityForResult(intent_selectRingTone, ActivityRequestCodes.PICK_SONG);
            }
            catch (ActivityNotFoundException e1)
            {
                e.printStackTrace();
                String errMsg = "Failed to start activity to select a ringtone:"+e.getMessage();
			    Log.e(tag,errMsg);
                //callErrToast(errMsg);
                writeErrStatBar(errMsg);
            }
		}
	}

	public void onClick(View v) {

        // Saving instance state
        saveInstanceState();

		int id = v.getId();
		if (id == R.id.CallNow) {

			launchDialer(destPhoneNumber);

		} else if (id == R.id.selectMediaBtn) {

			selectVisualMedia();

		} else if (id == R.id.selectRingtoneBtn) {

			selectRingtone();

		} else if (id == R.id.selectContactBtn) {

			Intent intent = new Intent(Intent.ACTION_PICK);
			intent.setType(ContactsContract.CommonDataKinds.Phone.CONTENT_TYPE);
			startActivityForResult(intent, ActivityRequestCodes.SELECT_CONTACT);
		}

		else if (id == R.id.login) {

			myPhoneNumber = ((EditText) findViewById(R.id.LoginNumber))
					.getText().toString();
			
			SharedConstants.MY_ID = myPhoneNumber;

			SharedPrefUtils.setString(context,
					SharedPrefUtils.GENERAL, SharedPrefUtils.MY_NUMBER, myPhoneNumber);

			initializeConnection();

			File SpecialCallIncoming = new File(Constants.specialCallPath);
			SpecialCallIncoming.mkdirs();

			initializeUI();
			stateIdle(tag+"::onClick() R.id.login");
		}

		else if (id == R.id.settingsBtn) {

            // Saving instance state
            saveInstanceState();

			Intent i = new Intent();
			i.setClass(context, Settings.class);

			startActivity(i);

		}

	}

    private void initializeLoginUI() {

        setContentView(R.layout.loginuser);
        findViewById(R.id.login).setOnClickListener(this);


        runOnUiThread(new Runnable() {

                          @Override
                          public void run() {

                              Button loginBtn = (Button) findViewById(R.id.login);
                              loginBtn.setEnabled(false);
                              loginBtn.setText("Login");

                              EditText loginNumberET = (EditText) findViewById(R.id.LoginNumber);

                              loginNumberET.addTextChangedListener(new TextWatcher() {
                                  @Override
                                  public void beforeTextChanged(CharSequence s, int start, int count, int after) {

                                  }

                                  @Override
                                  public void onTextChanged(CharSequence s, int start, int before, int count) {

                                      if (10 == s.length())
                                          findViewById(R.id.login).setEnabled(true);
                                      else

                                          findViewById(R.id.login).setEnabled(false);
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
		
		buttonLabels = new String[entries];

		// Populate the data arrays
		populateArrays();

		// Set up buttons and attach click listeners

		// Solving bug that causes EditText to be unfocusable at startup
		EditText edCN = (EditText) findViewById(R.id.CallNumber);
		edCN.setOnTouchListener(new View.OnTouchListener() {

			@Override
			public boolean onTouch(View v, MotionEvent event) {

				v.setFocusable(true);
				v.setFocusableInTouchMode(true);
				v.performClick();
				return false;
			}
		});

		edCN.addTextChangedListener(new TextWatcher() {

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count,
                                          int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before,
                                      int count) {

                String destPhone = s.toString();

                if (10 == s.length()) {

                    destPhoneNumber = destPhone;
                    drawSelectMediaButton(false);
                    drawSelectRingToneButton();

                    if ((serverProxy != null) &&
                            !getState().equals(SharedPrefUtils.STATE_DISABLED) &&
                            !getState().equals(SharedPrefUtils.STATE_LOADING))
                        serverProxy.isLogin(destPhone);

                } else {
                    destPhoneNumber="";
                    destName="";
                    setDestNameTextView();
                    saveInstanceState();
                    if(getState().equals(SharedPrefUtils.STATE_READY))
                        stateIdle(tag + "::onTextChanged()");
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

		ImageButton settingsBtn = (ImageButton) findViewById(R.id.settingsBtn);
		settingsBtn.setOnClickListener(this);
	}


    public void eventReceived(Event event) {

		final EventReport report = event.report();

		switch (report.status()) {

        case SERVER_PROXY_CREATED:
            if(serverProxy==null)
                doBindService();
            break;

		case UPLOAD_SUCCESS:
			writeInfoStatBar(report.desc(), Color.YELLOW);
            if(isContactSelected())
                stateReady(tag + "EVENT: UPLOAD_SUCCESS");
            else
                stateIdle(tag +" EVENT: UPLOAD_SUCCESS");
			break;

		case UPLOAD_FAILURE:
            writeErrStatBar(report.desc());
            stateIdle(tag +" EVENT: UPLOAD_FAILURE");
            break;

		case DOWNLOAD_SUCCESS:
            writeInfoStatBar(report.desc());
			break;

		case DOWNLOAD_FAILURE:
            writeErrStatBar(report.desc());
			break;

        case LOGIN_SUCCESS:
            writeInfoStatBar(report.desc());
            stateIdle(tag + " EVENT: LOGIN_SUCCESS");
            break;

		case ISLOGIN_ONLINE:
            destPhoneNumber = (String) report.data();

            if(!getState().equals(SharedPrefUtils.STATE_LOADING) &&
               !getState().equals(SharedPrefUtils.STATE_DISABLED)) {
                writeInfoStatBar(report.desc());
                stateReady(tag + " EVENT: ISLOGIN_ONLINE");
            }
			break;

		case ISLOGIN_ERROR:
            destPhoneNumber = (String) report.data();
            writeErrStatBar(report.desc());
			stateIdle(tag + "EVENT: ISLOGIN_ERROR");
			break;

		case ISLOGIN_OFFLINE:
            destPhoneNumber = (String) report.data();
            writeErrStatBar(report.desc());
			stateIdle(tag+" EVENT: ISLOGIN_OFFLINE");
			break;

		case ISLOGIN_UNREGISTERED:
            destPhoneNumber = (String) report.data();
			writeErrStatBar(report.desc());
			stateIdle(tag+" EVENT: ISLOGIN_UNREGISTERED");
			break;

		case DESTINATION_DOWNLOAD_COMPLETE:
			writeInfoStatBar(report.desc());
            TransferDetails td = (TransferDetails) report.data();
            lut_utils.saveUploadedPerNumber(td.getDestinationId(), td.getFileType(), td.get_fullFilePathSrcSD());
            if(isContactSelected())
                serverProxy.isLogin(destPhoneNumber);
			break;

        case RECONNECT_ATTEMPT:
            stateLoading(tag + " EVENT: RECONNECT_ATTEMPT", report.desc(), Color.RED);
            break;

        case CONNECTING:
            stateLoading(tag +" EVENT: CONNECTING", report.desc(), Color.YELLOW);
            break;

        case DISCONNECTED:
            writeErrStatBar(report.desc());
            stateDisabled(tag+" EVENT: DISCONNECTED");
            break;

		case CLOSE_APP:
			writeErrStatBar(report.desc());
			finish();
			break;

		case DISPLAY_ERROR:
			writeErrStatBar(report.desc());
			break;
			
		case DISPLAY_MESSAGE:
            writeInfoStatBar(report.desc());
			break;
		


		default:
			Log.e(tag, "Undefined event status on EventReceived");
		}

	}


	/* -------------- ServerProxy service methods -------------- */

	private ServiceConnection mConnection = new ServiceConnection() {
		public void onServiceConnected(ComponentName className, IBinder service) {
			// This is called when the connection with the service has been
			// established, giving us the service object we can use to
			// interact with the service.  Because we have bound to a explicit
			// service that we know is running in our own process, we can
			// cast its IBinder to a concrete class and directly access it.
			serverProxy = ((ServerProxy.MyBinder)service).getService();

		}

		public void onServiceDisconnected(ComponentName className) {
			// This is called when the connection with the service has been
			// unexpectedly disconnected -- that is, its process crashed.
			// Because it is running in our same process, we should never
			// see this happen.
			serverProxy = null;
		}
	};

	void doBindService() {
		// Establish a connection with the service.  We use an explicit
		// class name because we want a specific service implementation that
		// we know will be running in our own process (and thus won't be
		// supporting component replacement by other applications).
		bindService(new Intent(this,
                ServerProxy.class), mConnection, 0);
		mIsBound = true;
	}

	void doUnbindService() {
		if (mIsBound) {
			// Detach our existing connection.
			unbindService(mConnection);
			mIsBound = false;
		}
	}

	/* -------------- Assisting methods -------------- */

    private void saveInstanceState() {

        // Saving destination number
        final EditText ed_destinationNumber = ((EditText) findViewById(R.id.CallNumber));
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

        // Saving my phone number
        if(myPhoneNumber!=null)
            SharedPrefUtils.setString(context, SharedPrefUtils.GENERAL, SharedPrefUtils.MY_NUMBER, myPhoneNumber);
    }

    private void restoreInstanceState() {

        // Restoring destination number
        final EditText ed_destinationNumber =
                (EditText) findViewById(R.id.CallNumber);
        String destNumber = SharedPrefUtils.getString(context, SharedPrefUtils.GENERAL, SharedPrefUtils.DESTINATION_NUMBER);
        if(ed_destinationNumber!=null && destNumber!=null)
            ed_destinationNumber.setText(destNumber);

        // Restoring destination name
        destName = SharedPrefUtils.getString(context, SharedPrefUtils.GENERAL, SharedPrefUtils.DESTINATION_NAME);
        setDestNameTextView();

        // Restoring my phone number
        myPhoneNumber = SharedPrefUtils.getString(context, SharedPrefUtils.GENERAL, SharedPrefUtils.MY_NUMBER);
        if(myPhoneNumber!=null)
            SharedConstants.MY_ID = myPhoneNumber;

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

    private String getRealPathFromURI(Uri contentURI) {

        String result = null;
        try {
            Cursor cursor = getContentResolver().query(contentURI, null, null,
                    null, null);
            if (cursor == null) { // Source is Dropbox or other similar local
                // file path
                result = contentURI.getPath();
            } else {
                cursor.moveToFirst();
                int idx = cursor
                        .getColumnIndex(MediaStore.Images.ImageColumns.DATA);
                result = cursor.getString(idx);
                cursor.close();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return result;

    }

    private void initializeConnection() {

        // Starting service
        // Intent serverProxyIntent = new Intent(this, ServerProxy.class);
        // serverProxyIntent.putExtra("myphonenumber", myPhoneNumber);
        // serverProxyIntent.putExtra("workingdir", workingDir);

        Intent i = new Intent();
        i.setClass(getBaseContext(), ServerProxy.class);
        i.setAction(ServerProxy.ACTION_START);
        startService(i);
        doBindService();

//		serverProxy = new ServerProxy(eventGenerator);
//        serverProxy.connect();
    }

    private boolean isContactSelected() {

        EditText edCN = (EditText) findViewById(R.id.CallNumber);
        String s = edCN.getText().toString();
        if(10 == s.length())
            return true;
        return false;
    }

    /* -------------- UI methods -------------- */

    /* --- UI States --- */

    private void stateReady(String tag) {

        setState(tag, SharedPrefUtils.STATE_READY);

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

    private void stateIdle(String tag) {

        setState(tag, SharedPrefUtils.STATE_IDLE);

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

    private void stateDisabled(String tag) {

        setState(tag, SharedPrefUtils.STATE_DISABLED);

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

    private void stateLoading(String tag, String msg, int color) {

        setState(tag, SharedPrefUtils.STATE_LOADING);
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

        return AppStateUtils.getAppState(context);
    }

    private void setState(String tag, String state) {

        AppStateUtils.setAppState(context, tag, state);
    }


    /* --- UI elements controls --- */

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
            SharedPrefUtils.remove(context, SharedPrefUtils.UPLOADED_MEDIA_THUMBNAIL, destPhoneNumber);
            }


    }

    private void drawSelectRingToneButton() {

        boolean wasRingToneUploaded = lut_utils.getUploadedRingTonePerNumber(destPhoneNumber);
        Button ringButton = (Button) findViewById(R.id.selectRingtoneBtn);
        if(wasRingToneUploaded)
        {
            ringButton.getBackground().setColorFilter(0xFF00FF00,
                    PorterDuff.Mode.MULTIPLY);
        }
        else
        {
            if(ringButton.isEnabled())
                ringButton.getBackground().setColorFilter(Color.LTGRAY,
                        PorterDuff.Mode.MULTIPLY);
            else
                ringButton.getBackground().setColorFilter(Color.DKGRAY,
                        PorterDuff.Mode.MULTIPLY);
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

	private void callInfoToast(final String text) {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				Toast toast = Toast.makeText(context, text,
						Toast.LENGTH_LONG);
				TextView v = (TextView) toast.getView().findViewById(
						android.R.id.message);
				v.setTextColor(Color.GREEN);
				toast.show();
			}
		});
	}
	
	private void callInfoToast(final String text, final int g) {
		runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast toast = Toast.makeText(context, text,
                        Toast.LENGTH_LONG);
                TextView v = (TextView) toast.getView().findViewById(
                        android.R.id.message);
                v.setTextColor(g);
                toast.show();
            }
        });
	}

    private void writeErrStatBar(final String text) {

        TextView statusBar = (TextView)findViewById(R.id.statusBar);
        statusBar.setTextColor(Color.RED);
        statusBar.setText(text);
    }

    private void writeInfoStatBar(final String text) {

        TextView statusBar = (TextView)findViewById(R.id.statusBar);
        statusBar.setTextColor(Color.GREEN);
        statusBar.setText(text);
    }

    private void writeInfoStatBar(final String text, final int g) {

        TextView statusBar = (TextView)findViewById(R.id.statusBar);
        statusBar.setTextColor(g);
        statusBar.setText(text);
    }
}