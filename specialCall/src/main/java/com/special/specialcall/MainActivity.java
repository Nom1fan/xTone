package com.special.specialcall;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import DataObjects.SharedConstants;
import DataObjects.TransferDetails;
import EventObjects.Event;
import EventObjects.EventReport;
import Exceptions.FileDoesNotExistException;
import Exceptions.FileMissingExtensionException;
import Exceptions.InvalidDestinationNumberException;
import Exceptions.FileExceedsMaxSizeException;
import Exceptions.FileInvalidFormatException;
import FilesManager.FileManager;
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
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.media.ThumbnailUtils;
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
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import com.android.services.IncomingReceiver;
import com.android.services.ServerProxy;

import data_objects.Constants;
import data_objects.LUT_Manager;
import data_objects.SharedPrefUtils;


public class MainActivity extends Activity implements OnClickListener {

	private int entries = 6;
	private String buttonLabels[];
	private ServerProxy serverProxy;
	private String myPhoneNumber = "";
	private String destPhoneNumber = "";
	private String destName = "";	
    private String tag = "MAIN_ACTIVITY";
	private Uri outputFileUri;
	private boolean LoggedIn = false;
	private ProgressBar pBar;
	private boolean loading = false;
	private boolean mIsBound = false;
    private Context context;
    private LUT_Manager lutManager;
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
		
		LoggedIn = SharedPrefUtils.getBoolean(context, SharedPrefUtils.GENERAL,
				"LoggedIn");
		
		if (LoggedIn)
		{

			if(serverProxy==null)
			{
				myPhoneNumber = SharedPrefUtils.getString(context,SharedPrefUtils.GENERAL,SharedPrefUtils.MY_NUMBER);
				SharedConstants.MY_ID = myPhoneNumber;
				InitializeConnection();
			}
            doBindService();
            registerReceiver(serviceReceiver,serviceReceiverIntentFilter);
			initializeUI();

			((TextView) findViewById(R.id.destName)).setText(destName);

		}
		else
		{
			setContentView(R.layout.loginuser);
			Button login = (Button) findViewById(R.id.login);
			login.setOnClickListener(this);
		}

        // Restoring instance state
        restoreInstanceState();
	}

	@Override
    protected void onDestroy() {
	 super.onDestroy();
	 }

	@Override
	protected void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);

        context = getApplicationContext();
        lutManager = new LUT_Manager(context);

		LoggedIn = SharedPrefUtils.getBoolean(context,
				SharedPrefUtils.GENERAL, "LoggedIn");


		if (!LoggedIn) {
			setContentView(R.layout.loginuser);

			Button login = (Button) findViewById(R.id.login);
			login.setOnClickListener(this);
		} else {
			initializeUI();

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
                    serverProxy.uploadFileToServer(fm.getFileData(), fm.getExtension(), fm.getFileType(), destPhoneNumber, fm.getFileFullPath());
                    loading = true;
                    disableCallButton();
                }
            }
            catch(NullPointerException e)
            {
                e.printStackTrace();
                Log.e(tag,"It seems there was a problem with the file path.");
                //callErrToast("It seems there was a problem with the file path");
                writeErrStatBar("It seems there was a problem with the file path");
            }
            catch (IOException e)
            {
                e.printStackTrace();
                Log.e(tag, "A problem occured while reading the file.");
                //callErrToast("A problem occured while reading the file");
                writeErrStatBar("A problem occured while reading the file");
            }
            catch (FileExceedsMaxSizeException e)
            {
//                callErrToast("Please select a file that weights less than:"+
//                        FileManager.getFileSizeFormat(FileManager.MAX_FILE_SIZE));
                writeErrStatBar("Please select a file that weights less than:"+
                        FileManager.getFileSizeFormat(FileManager.MAX_FILE_SIZE));
            }
            catch (InvalidDestinationNumberException e)
            {
                //callErrToast("There was a problem with the destination number. Please try again");
                writeErrStatBar("There was a problem with the destination number. Please try again");
            }
            catch (FileInvalidFormatException e)
            {
                e.printStackTrace();
                //callErrToast("Please select a valid format");
                writeErrStatBar("Please select a valid format");
            } catch (FileDoesNotExistException | FileMissingExtensionException e) {
                e.printStackTrace();
                //callErrToast(e.getMessage());
                writeErrStatBar(e.getMessage());
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

	private void openImageIntent() {

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

	private void uploadRingtoneIntent() {


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

		} else if (id == R.id.MyPic) {

			((Button) findViewById(R.id.CallNow)).setEnabled(false);
			openImageIntent();

		} else if (id == R.id.MyRing) {

			((Button) findViewById(R.id.CallNow)).setEnabled(false);
			uploadRingtoneIntent();
		} else if (id == R.id.Select_Contact) {

			Intent intent = new Intent(Intent.ACTION_PICK);
			intent.setType(ContactsContract.CommonDataKinds.Phone.CONTENT_TYPE);
			startActivityForResult(intent, ActivityRequestCodes.SELECT_CONTACT);
		}

		else if (id == R.id.login) {

			SharedPrefUtils.setBoolean(context,
					SharedPrefUtils.GENERAL, "LoggedIn", true);

			LoggedIn = true;


            startService(new Intent(this, IncomingReceiver.class));

			myPhoneNumber = ((EditText) findViewById(R.id.CallNumber))
					.getText().toString();
			
			SharedConstants.MY_ID = myPhoneNumber;

			SharedPrefUtils.setString(context,
					SharedPrefUtils.GENERAL, SharedPrefUtils.MY_NUMBER, myPhoneNumber);

			InitializeConnection();

			File SpecialCallIncoming = new File(Constants.specialCallPath);
			SpecialCallIncoming.mkdirs();

			initializeUI();
			disableGuiComponents();
		}

		else if (id == R.id.settingsBtn) {

            // Saving instance state
            saveInstanceState();

			Intent i = new Intent();
			i.setClass(context, Settings.class);

			startActivity(i);

		}

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
//                if (!settingCallNumberComplete)
//                    return;
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before,
                                      int count) {

                String destPhone = s.toString();

                if (10 == s.length()) {
                    if (serverProxy != null)
                        serverProxy.isLogin(destPhone);

                    destPhoneNumber = destPhone;

                } else if (0 == s.length()) {
                    disableGuiComponents();
                } else {
                    disableGuiComponents();
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

		Button button1 = (Button) findViewById(R.id.CallNow);
		button1.setOnClickListener(this);

		ImageButton button2 = (ImageButton) findViewById(R.id.MyPic);
		// button2.setText(buttonLabels[1]);
		// button2.getBackground().setColorFilter(0xFFFF0000,
		// PorterDuff.Mode.MULTIPLY);
		button2.setOnClickListener(this);

		Button button3 = (Button) findViewById(R.id.MyRing);
		button3.setText(buttonLabels[2]);
		button3.getBackground().setColorFilter(0xFFFF0000,
                PorterDuff.Mode.MULTIPLY);
		button3.setOnClickListener(this);

		ImageButton button6 = (ImageButton) findViewById(R.id.Select_Contact);
		// button6.setText(buttonLabels[5]);
		button6.setOnClickListener(this);

		ImageButton settingsBtn = (ImageButton) findViewById(R.id.settingsBtn);
		settingsBtn.setOnClickListener(this);

        if (loading)
            enableProgressBar();
        else
            disableProgressBar();

		disableGuiComponents();
	}


	public void eventReceived(Event event) {

		final EventReport report = event.report();

		switch (report.status()) {
		case UPLOAD_SUCCESS:
			writeInfoStatBar(report.desc(), Color.YELLOW);
			enableGuiComponents();
			disableProgressBar();
			loading = false;

			break;

		case UPLOAD_FAILURE:
			//callErrToast(report.desc());
            writeErrStatBar(report.desc());
			disableProgressBar();
			break;

		case DOWNLOAD_SUCCESS:
			//callInfoToast(report.desc());
            writeInfoStatBar(report.desc());
			disableProgressBar();
			break;

		case DOWNLOAD_FAILURE:
			disableProgressBar();
			//callErrToast(report.desc());
            writeErrStatBar(report.desc());
			break;

		case ISLOGIN_ONLINE:
            setMediaSelectButtonThumbnail((String) report.data());
			//callInfoToast(report.desc());
            writeInfoStatBar(report.desc());
			enableGuiComponents();
			break;

		case ISLOGIN_ERROR:
			//callErrToast(report.desc());
            writeErrStatBar(report.desc());
			disableGuiComponents();
			break;

		case ISLOGIN_OFFLINE:
            setMediaSelectButtonThumbnail((String) report.data());
			//callErrToast(report.desc());
            writeErrStatBar(report.desc());
			disableGuiComponents();
			break;

		case ISLOGIN_UNREGISTERED:
			writeErrStatBar(report.desc());
			disableGuiComponents();
			break;
			
		case RESPONSE_FAILURE:
			writeErrStatBar(report.desc());
			break;

		case DESTINATION_DOWNLOAD_COMPLETE:
			writeInfoStatBar(report.desc());
			disableProgressBar();
			enableCallButton();
            TransferDetails td = (TransferDetails) report.data();
            lutManager.saveUploadedPerNumber(td.getDestinationId(), td.getFileType(), td.get_fullFilePathSrcSD());
            drawUploadedContent(td.getDestinationId());
			break;

//		case CLIENT_ACTION_FAILURE:
//			writeErrStatBar(report.desc());
//			break;

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
        final TextView tv_destName =
                (TextView) findViewById(R.id.destName);
        destName = SharedPrefUtils.getString(context, SharedPrefUtils.GENERAL, SharedPrefUtils.DESTINATION_NAME);
        if(tv_destName!=null && destName!=null)
            tv_destName.setText(destName);

        // Restoring my phone number
        myPhoneNumber = SharedPrefUtils.getString(context, SharedPrefUtils.GENERAL, SharedPrefUtils.MY_NUMBER);
        if(myPhoneNumber!=null)
            SharedConstants.MY_ID = myPhoneNumber;

    }

    private void drawUploadedContent(final String destPhoneNumber) {

        runOnUiThread(new Runnable() {

            @Override
            public void run() {

                setMediaSelectButtonThumbnail(destPhoneNumber);
                setRingToneSelectButtonBg(destPhoneNumber);

                ViewGroup vg = (ViewGroup) findViewById(R.id.mainActivity);
                vg.postInvalidate();
            }
        });
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

	private void disableCallButton() {

		runOnUiThread(new Runnable() {

			public void run() {
				((Button) findViewById(R.id.CallNow)).setEnabled(false);
			}

		});
	}

	private void enableCallButton() {

		runOnUiThread(new Runnable() {

            public void run() {
                ((Button) findViewById(R.id.CallNow)).setEnabled(true);
            }

        });
	}

    private void disableSelectMediaButton() {

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    ImageButton myPic = ((ImageButton) findViewById(R.id.MyPic));
                    myPic.setClickable(false);
                    myPic.setImageResource(R.drawable.defaultpic_disabled);
                }
            });
    }

    private void enableSelectMediaButton() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ImageButton myPic = ((ImageButton) findViewById(R.id.MyPic));
                myPic.setClickable(true);
                myPic.setImageResource(R.drawable.defaultpic_enabled);
            }
        });
    }

    private void enableSelectRingToneButton() {

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Button ringButton = ((Button) findViewById(R.id.MyRing));
                ringButton.setClickable(true);
                ringButton.getBackground().setColorFilter(Color.LTGRAY,
                        PorterDuff.Mode.MULTIPLY);
                ringButton.refreshDrawableState();
            }
        });
    }

    private void disableSelectRingToneButton() {

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Button ringButton = ((Button) findViewById(R.id.MyRing));
                ringButton.setClickable(false);
                ringButton.getBackground().setColorFilter(Color.DKGRAY,
                        PorterDuff.Mode.MULTIPLY);
                ringButton.refreshDrawableState();
            }
        });
    }

	private void disableProgressBar() {

		loading = false;

		runOnUiThread(new Runnable() {
            @Override
            public void run() {
                pBar = (ProgressBar) findViewById(R.id.progressBar);
                pBar.setVisibility(ProgressBar.GONE);
            }
        });
	}

	private void enableProgressBar() {

		loading = true;

		runOnUiThread(new Runnable() {
            @Override
            public void run() {
                pBar = (ProgressBar) findViewById(R.id.progressBar);
                pBar.setVisibility(ProgressBar.VISIBLE);
            }
        });
	}

	private void enableGuiComponents() {

        enableSelectMediaButton();
        enableSelectRingToneButton();
        if(destPhoneNumber!=null)
            drawUploadedContent(destPhoneNumber);
        enableCallButton();
	}

	private void disableGuiComponents() {

		runOnUiThread(new Runnable() {

            @Override
            public void run() {

                disableSelectMediaButton();
                disableSelectRingToneButton();
                disableCallButton();

            }
        });
	}

	private void setMediaSelectButtonThumbnail(String destPhoneNumber)
    {
        try {
            Bitmap tmp_bitmap;
            Bitmap bitmap;
            FileManager.FileType fType = null;
            ImageButton selectMediaBtn = (ImageButton) findViewById(R.id.MyPic);

            String lastUploadedMediaPath = lutManager.getUploadedMediaPerNumber(destPhoneNumber);
            if (!lastUploadedMediaPath.equals("")) {
                fType = FileManager.getFileType(lastUploadedMediaPath);

                switch (fType) {
                    case IMAGE:
                        tmp_bitmap = BitmapFactory.decodeFile(lastUploadedMediaPath);
                        bitmap = Bitmap.createScaledBitmap(tmp_bitmap, selectMediaBtn.getWidth(), selectMediaBtn.getHeight(), false);
                        selectMediaBtn.setImageBitmap(bitmap);
                        break;

                    case VIDEO:
                        tmp_bitmap = ThumbnailUtils.createVideoThumbnail(lastUploadedMediaPath,
                                MediaStore.Images.Thumbnails.MINI_KIND);
                        bitmap = Bitmap.createScaledBitmap(tmp_bitmap, selectMediaBtn.getWidth(), selectMediaBtn.getWidth(),
                                false);
                        selectMediaBtn.setImageBitmap(bitmap);
                        break;
                }
            }
            else
                selectMediaBtn.setImageResource(R.drawable.defaultpic_enabled);

        } catch (FileInvalidFormatException | FileDoesNotExistException | FileMissingExtensionException e) {
            SharedPrefUtils.remove(context, SharedPrefUtils.UPLOADED_MEDIA_THUMBNAIL, destPhoneNumber);
            }


    }

    private void setRingToneSelectButtonBg(String destPhoneNumber) {

        boolean wasRingToneUploaded = lutManager.getUploadedRingTonePerNumber(destPhoneNumber);
        Button ringButton = (Button) findViewById(R.id.MyRing);
        if(wasRingToneUploaded)
        {
            ringButton.getBackground().setColorFilter(0xFF00FF00,
                    PorterDuff.Mode.MULTIPLY);
        }
        else
        {
            ringButton.getBackground().setColorFilter(Color.LTGRAY,
                    PorterDuff.Mode.MULTIPLY);
        }
        ringButton.refreshDrawableState();

    }

    private void InitializeConnection() {

		// Starting service
		// Intent serverProxyIntent = new Intent(this, ServerProxy.class);
		// serverProxyIntent.putExtra("myphonenumber", myPhoneNumber);
		// serverProxyIntent.putExtra("workingdir", workingDir);
		 startService(new Intent(getBaseContext(), ServerProxy.class));
         doBindService();

//		serverProxy = new ServerProxy(eventGenerator);
//        serverProxy.connect();
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