package com.special.specialcall;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import DataObjects.SharedConstants;
import EventObjects.Event;
import EventObjects.EventGenerator;
import EventObjects.EventListener;
import EventObjects.EventReport;
import Exceptions.EmptyDestinationNumberException;
import Exceptions.FileExceedsMaxSizeException;
import FilesManager.FileManager;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.media.AudioManager;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
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
import android.widget.TextView;
import android.widget.Toast;

import com.android.services.ServerProxy;

import data_objects.Constants;
import data_objects.SharedPrefUtils;

public class MainActivity extends Activity implements OnClickListener,
		EventListener {

	private int entries = 6;
	private String buttonLabels[];
	private HashMap<String, String> lastULThumbsPerUser = new HashMap<String, String>();
	private ServerProxy serverProxy;
	private EventGenerator eventGenerator;
	private String myPhoneNumber = "";
	private String destPhoneNumber = "";
	private String destName = "";	
	private BroadcastReceiver ringerModeReceiver;
	private String tag = "MAIN_ACTIVITY";
	private Uri outputFileUri;
	private boolean settingCallNumberComplete;
	private boolean LoggedIn = false;
	private AudioManager am;
	private int absRingerState = 2;
	private MyProgressBar pBar;
	boolean VideoValid = false;
	private boolean loading = false;
	private static MainActivity singleton;

	public static MainActivity getInstance() {
		return singleton;
	}

	private abstract class ActivityRequestCodes {

		public static final int PICK_SONG = 1;
		public static final int SELECT_CONTACT = 2;
		public static final int SELECT_PICTURE = 3;

	}


	@Override
	protected void onStart() {

		super.onStart();


		eventGenerator = new EventGenerator();
		eventGenerator.addEventListener(this);

		
	}

	@Override
	protected void onResume() {

		super.onResume();

		Context context = getApplicationContext();
		
		LoggedIn = SharedPrefUtils.getBoolean(context, SharedPrefUtils.GENERAL,
				"LoggedIn");
		
		if(LoggedIn)
		{
			if(serverProxy==null)
			{
				myPhoneNumber = SharedPrefUtils.getString(context,SharedPrefUtils.GENERAL,"myPhoneNumber");
				SharedConstants.MY_ID = myPhoneNumber;
				InitalizeConnection();				
			}
		}				

		IncomingSpecialCall.finishedIncomingCall = false;
		
		if (!LoggedIn) {
			setContentView(R.layout.loginuser);
			Button login = (Button) findViewById(R.id.login);
			login.setOnClickListener(this);
		} else {
			initializeUI();

			setDestPhoneNumber(SharedPrefUtils.getString(context,
					SharedPrefUtils.GENERAL, "DestPhoneNumber"));
			String destName = SharedPrefUtils.getString(context,
					SharedPrefUtils.GENERAL, "DestName");

			String mediaPath = lastULThumbsPerUser.get(destPhoneNumber);

			settingCallNumberComplete = false;
			EditText callNumberET = (EditText) findViewById(R.id.CallNumber);
			if (callNumberET != null)
				callNumberET.setText(destPhoneNumber,
						EditText.BufferType.EDITABLE);
			settingCallNumberComplete = true;

			((TextView) findViewById(R.id.destName)).setText(destName);

			if (mediaPath != null)
				setMediaSelectButtonThumbnail(mediaPath);

			if (loading)
				enableProgressBar();
			else
				disableProgressBar();
						
		}
	}

	 @Override
	 protected void onDestroy() {
	 super.onDestroy();
	// unbindService(mConnection);
	// unregisterReceiver(serviceReceiver);
//	 unregisterReceiver(ringerModeReceiver);
	 }

	@Override
	protected void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);
		singleton = this;

		IncomingSpecialCall.finishedIncomingCall = false;

		LoggedIn = SharedPrefUtils.getBoolean(getApplicationContext(),
				SharedPrefUtils.GENERAL, "LoggedIn");

//		ringerModeReceiver = new BroadcastReceiver() {
//			@Override
//			public void onReceive(Context context, Intent intent) {
//				am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
//
//				switch (am.getRingerMode()) {
//				case AudioManager.RINGER_MODE_SILENT: {
//					absRingerState = AudioManager.RINGER_MODE_SILENT;
//					SharedPrefUtils.setInt(context, SharedPrefUtils.GENERAL, "ringerState", absRingerState);
//					Log.i("MyApp", "Silent mode");
//					break;
//				}
//				case AudioManager.RINGER_MODE_VIBRATE: {
//					absRingerState = AudioManager.RINGER_MODE_VIBRATE;
//					SharedPrefUtils.setInt(context, SharedPrefUtils.GENERAL, "ringerState", absRingerState);
//					Log.i("MyApp", "Vibrate mode");
//					break;
//				}
//				case AudioManager.RINGER_MODE_NORMAL: {
//					absRingerState = AudioManager.RINGER_MODE_NORMAL;
//					SharedPrefUtils.setInt(context, SharedPrefUtils.GENERAL, "ringerState", absRingerState);
//					Log.i("MyApp", "Normal mode");
//					break;
//				}
//				}
//			}
//		};
	//	IntentFilter filter = new IntentFilter(
	//			AudioManager.RINGER_MODE_CHANGED_ACTION);
	//	registerReceiver(ringerModeReceiver, filter);
		
		
		
		
		if (!LoggedIn) {
			setContentView(R.layout.loginuser);

			Button login = (Button) findViewById(R.id.login);
			login.setOnClickListener(this);
		} else {
			initializeUI();
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

		Intent intent_upload = new Intent();
		intent_upload.setType("audio/*");
		intent_upload.setAction(Intent.ACTION_GET_CONTENT);

		startActivityForResult(intent_upload, ActivityRequestCodes.PICK_SONG);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (resultCode == RESULT_OK) {
			try {
					setDestPhoneNumber(((EditText) findViewById(R.id.CallNumber))
							.getText().toString());
					destName = ((TextView) findViewById(R.id.destName)).getText()
							.toString();
				
					if(destPhoneNumber==null)
						throw new EmptyDestinationNumberException();
					if(destPhoneNumber.equals("") || destPhoneNumber.length() < 10)
						throw new EmptyDestinationNumberException();
					
					if (requestCode == ActivityRequestCodes.SELECT_PICTURE) {
						final boolean isCamera;
		
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
		

		
						myPhoneNumber = SharedPrefUtils.getString(
								getApplicationContext(), SharedPrefUtils.GENERAL,
								"myPhoneNumber");
						
						SharedConstants.MY_ID = myPhoneNumber;
		
						String imgOrVidPath = getRealPathFromURI(selectedImageOrVideoUri);	
						FileManager fm = new FileManager(imgOrVidPath);						
						fm.validateFileSize();
																	
						lastULThumbsPerUser.put(destPhoneNumber, imgOrVidPath);
						
						SharedPrefUtils.setString(getApplicationContext(),
								SharedPrefUtils.GENERAL, "DestPhoneNumber",
								destPhoneNumber);
						SharedPrefUtils.setString(getApplicationContext(),
								SharedPrefUtils.GENERAL, "DestName", destName);
						
						serverProxy.uploadFileToServer(fm.getFileData(),fm.getExtension(), destPhoneNumber);
						loading = true;
						disableCallButton();							
					}
		
					if (requestCode == ActivityRequestCodes.PICK_SONG) {
		
						Uri uriRingtone = data.getData();
						String ringTonePath = getRealPathFromURI(uriRingtone);	
						FileManager fm = new FileManager(ringTonePath);
						fm.validateFileSize();
						
						serverProxy.uploadFileToServer(fm.getFileData(),fm.getExtension(), destPhoneNumber);
						loading = true;
						disableCallButton();
					}
			}
			catch(NullPointerException e)
			{
				e.printStackTrace();
				Log.e(tag,"It seems there was a problem with the file path.");
				callErrToast("It seems there was a problem with the file path");
			} 
			catch (IOException e) 
			{					
				e.printStackTrace();
				Log.e(tag,"A problem occured while reading the file.");
				callErrToast("A problem occured while reading the file");										
			} 
			catch (FileExceedsMaxSizeException e) 
			{					
				callErrToast("Please select a file that weights less than:"+
						FileManager.getFileSizeFormat(FileManager.MAX_FILE_SIZE));
			} 
			catch (EmptyDestinationNumberException e) 
			{	
				callErrToast("There was a problem with the destination number. Please try again");
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

									SharedPrefUtils.setString(
											getApplicationContext(),
											SharedPrefUtils.GENERAL,
											"DestPhoneNumber", number);
									SharedPrefUtils.setString(
											getApplicationContext(),
											SharedPrefUtils.GENERAL,
											"DestName", name);

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

	private void setDestPhoneNumber(String destPhoneNumberAlphaNumeric) {
		
		destPhoneNumber = toNumeric(destPhoneNumberAlphaNumeric);		
	}
	
	private String toNumeric(String str) {
	
		return str.replaceAll("[^0-9]","");
	}

	public void onClick(View v) {

		int id = v.getId();
		if (id == R.id.CallNow) {

			launchDialer(((EditText) findViewById(R.id.CallNumber)).getText()
					.toString());

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

			SharedPrefUtils.setBoolean(getApplicationContext(),
					SharedPrefUtils.GENERAL, "LoggedIn", true);

			LoggedIn = true;

			myPhoneNumber = ((EditText) findViewById(R.id.CallNumber))
					.getText().toString();
			
			SharedConstants.MY_ID = myPhoneNumber;

			SharedPrefUtils.setString(getApplicationContext(),
					SharedPrefUtils.GENERAL, "myPhoneNumber", myPhoneNumber);

			InitalizeConnection();

			File SpecialCallIncoming = new File(Constants.specialCallPath);
			SpecialCallIncoming.mkdirs();

			initializeUI();
			disableGuiComponents();
		}

		else if (id == R.id.settingsBtn) {

			Intent i = new Intent();
			i.setClass(getApplicationContext(), Settings.class);

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
				if (!settingCallNumberComplete)
					return;
			}

			@Override
			public void onTextChanged(CharSequence s, int start, int before,
					int count) {

				String destPhone = s.toString();

				if (10 == s.length()) {				
					if (serverProxy!=null)	
						serverProxy.isLogin(destPhone);
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
		button1.setText(buttonLabels[0]);
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

		disableGuiComponents();
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

	public void eventReceived(Event event) {

		final EventReport report = event.report();

		switch (report.status()) {
		case UPLOAD_SUCCESS:
			callInfoToast(report.desc(), Color.YELLOW);
			enableGuiComponents();
			disableProgressBar();
			String fileExtension = (String) report.data();
			drawUploadedContent(fileExtension);
			loading = false;

			break;

		case UPLOAD_FAILURE:
			callErrToast(report.desc());
			disableProgressBar();
			break;

		case DOWNLOAD_SUCCESS:
			callInfoToast(report.desc());
			disableProgressBar();

			String tmp_arr[] = ((String) report.data()).split("\\.");
			String downloadFileName = tmp_arr[0];			
			String downloadFileExtension = tmp_arr[1];
			downloadFileExtension = downloadFileExtension.toLowerCase();

			boolean ringtoneValid = false;

			if(Arrays.asList(Constants.audioFormats).contains(downloadFileExtension))
				ringtoneValid = true;

			if (ringtoneValid) {
				File newSoundFile = new File(Constants.specialCallPath + downloadFileName
						+ "/", downloadFileName + "." + downloadFileExtension);
				if (newSoundFile.exists()) {
					ContentValues values = new ContentValues();
					values.put(MediaStore.MediaColumns.DATA,
							newSoundFile.getAbsolutePath());
					values.put(MediaStore.MediaColumns.TITLE, downloadFileName);
					values.put(MediaStore.MediaColumns.MIME_TYPE, "audio/"
							+ downloadFileExtension);
					values.put(MediaStore.Audio.Media.ARTIST, "SpecialCallUI");
					values.put(MediaStore.MediaColumns.SIZE, 215454); // ///
																		// what
																		// to do
																		// here
																		// !!!!!!!!!!!!
																		// ->
																		// WTF
																		// Rony?
																		// LOL
					values.put(MediaStore.Audio.Media.IS_RINGTONE, true);

					Uri uri = MediaStore.Audio.Media
							.getContentUriForPath(newSoundFile
									.getAbsolutePath());
					getContentResolver().delete(
							uri,
							MediaStore.MediaColumns.DATA + "=\""
									+ newSoundFile.getAbsolutePath() + "\"",
							null);
					Uri newUri = getContentResolver().insert(uri, values);

					SharedPrefUtils.setString(getApplicationContext(),
							SharedPrefUtils.RINGTONE_URI, downloadFileName,
							newUri.toString());
					SharedPrefUtils.setString(getApplicationContext(),
							SharedPrefUtils.RINGTONE, downloadFileName,
							downloadFileExtension);

					SharedPrefUtils.setString(getApplicationContext(),
							SharedPrefUtils.GENERAL, "mUri", newUri.toString());
					SharedPrefUtils.setString(getApplicationContext(),
							SharedPrefUtils.GENERAL, "mUriFilePath",
							newSoundFile.getAbsolutePath());

				} else
					callErrToast("Not Found:" + newSoundFile.getAbsolutePath());
			}
			boolean imageValid = false;

			if(Arrays.asList(Constants.imageFormats).contains(downloadFileExtension))
				imageValid = true;

			boolean VideoValid = false;
			
			if(Arrays.asList(Constants.videoFormats).contains(downloadFileExtension))
				VideoValid = true;

			if (imageValid || VideoValid)
				SharedPrefUtils.setString(getApplicationContext(),
						SharedPrefUtils.MEDIA, downloadFileName,
						downloadFileExtension);
			break;

		case DOWNLOAD_FAILURE:
			disableProgressBar();
			callErrToast(report.desc());
			break;

		case ISLOGIN_ONLINE:
			callInfoToast(report.desc());
			enableGuiComponents();
			break;

		case ISLOGIN_ERROR:
			callErrToast(report.desc());
			disableGuiComponents();
			break;

		case ISLOGIN_OFFLINE:
			callErrToast(report.desc());
			disableGuiComponents();
			break;

		case ISLOGIN_UNREGISTERED:
			callErrToast(report.desc());
			disableGuiComponents();
			break;
			
		case RESPONSE_FAILURE:
			callErrToast(report.desc());
			break;

		case RECEIVER_DOWNLOAD_COMPLETE:
			callInfoToast(report.desc());
			disableProgressBar();
			enableCallButton();
			break;

//		case PENDING_DOWNLOAD:
//			enableProgressBar();
//			callInfoToast(report.desc());
//			serverProxy.downloadFileFromServer((TransferDetails) report.data());
//			break;

//		case CLIENT_ACTION_FAILURE:
//			callErrToast(report.desc());
//			break;

		case CLOSE_APP:
			callErrToast(report.desc());
			cleanAndTerminate();
			break;

		case DISPLAY_ERROR:
			callErrToast(report.desc());
			break;
			
		case DISPLAY_MESSAGE:
			callInfoToast(report.desc());
			break;
		


		default:
			Log.e(tag, "Undefined event status on EventReceived");
		}

	}

	private void drawUploadedContent(final String fileExtension) {
		
		runOnUiThread(new Runnable() {
			
			@Override
			public void run() {
				
				if (Arrays.asList(Constants.imageFormats).contains(fileExtension)) {
					ImageButton picButton = (ImageButton) findViewById(R.id.MyPic);
					picButton.getBackground().setColorFilter(0xFF00FF00,
							PorterDuff.Mode.MULTIPLY);
					picButton.refreshDrawableState();
				}

				else if (Arrays.asList(Constants.audioFormats).contains(fileExtension)) {
					Button ringButton = (Button) findViewById(R.id.MyRing);
					ringButton.getBackground().setColorFilter(0xFF00FF00,
							PorterDuff.Mode.MULTIPLY);
					ringButton.refreshDrawableState();
				}	
				
				ViewGroup vg = (ViewGroup) findViewById(R.id.mainActivity);
				vg.postInvalidate();
			}
		});
	}

	private void cleanAndTerminate() {

		try {
			Thread.sleep(2000);
		} catch (InterruptedException e) {
		};

		this.finish();
	}

	/* -------------- Assisting methods -------------- */

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

	private void disableProgressBar() {

		loading = false;

		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				pBar = (MyProgressBar) findViewById(R.id.progressBar);
				pBar.dismiss();
			}
		});
	}

	private void enableProgressBar() {

		loading = true;

		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				pBar = (MyProgressBar) findViewById(R.id.progressBar);
				pBar.startAnimation();
			}
		});
	}

	private void enableGuiComponents() {

		runOnUiThread(new Runnable() {

			@Override
			public void run() {

				ImageButton myPic = ((ImageButton) findViewById(R.id.MyPic));
				myPic.setClickable(true);
				String thumbPath = lastULThumbsPerUser.get(destPhoneNumber);
				if (thumbPath != null)
					setMediaSelectButtonThumbnail(thumbPath);
				else
					myPic.setImageResource(R.drawable.defaultpic_enabled);
				((Button) findViewById(R.id.MyRing)).setEnabled(true);
				enableCallButton();

			}
		});
	}

	private void disableGuiComponents() {

		runOnUiThread(new Runnable() {

			@Override
			public void run() {

				ImageButton myPic = ((ImageButton) findViewById(R.id.MyPic));
				myPic.setClickable(false);
				myPic.setImageResource(R.drawable.defaultpic_disabled);
				((Button) findViewById(R.id.MyRing)).setEnabled(false);
				disableCallButton();

			}
		});
	}

	private void setMediaSelectButtonThumbnail(String mediaPath) {

		String extension = mediaPath.split("\\.")[1];
		if (Arrays.asList(Constants.imageFormats).contains((extension))) {
			Bitmap tmp_bitmap = BitmapFactory.decodeFile(mediaPath);
			Bitmap bitmap = Bitmap.createScaledBitmap(tmp_bitmap, 200, 200,
					false);
			((ImageButton) findViewById(R.id.MyPic)).setImageBitmap(bitmap);
		} else if (Arrays.asList(Constants.videoFormats).contains(extension)) {
			Bitmap tmp_bitmap = ThumbnailUtils.createVideoThumbnail(mediaPath,
					MediaStore.Images.Thumbnails.MINI_KIND);
			Bitmap bitmap = Bitmap.createScaledBitmap(tmp_bitmap, 200, 200,
					false);
			((ImageButton) findViewById(R.id.MyPic)).setImageBitmap(bitmap);
		}

	}

	private void InitalizeConnection() {

		// Starting service
		// Intent serverProxyIntent = new Intent(this, ServerProxy.class);
		// serverProxyIntent.putExtra("myphonenumber", myPhoneNumber);
		// serverProxyIntent.putExtra("workingdir", workingDir);
		// startService(serverProxyIntent);

		serverProxy = new ServerProxy(eventGenerator);
		serverProxy.connect();		
	}

	private void callErrToast(final String text) {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				Toast toast = Toast.makeText(getApplicationContext(), text,
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
				Toast toast = Toast.makeText(getApplicationContext(), text,
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
				Toast toast = Toast.makeText(getApplicationContext(), text,
						Toast.LENGTH_LONG);
				TextView v = (TextView) toast.getView().findViewById(
						android.R.id.message);
				v.setTextColor(g);
				toast.show();
			}
		});
	}

	public static boolean deleteDirectory(File path) {
		if (path.exists()) {
			File[] files = path.listFiles();
			if (files == null) {
				return true;
			}
			for (int i = 0; i < files.length; i++) {
				if (files[i].isDirectory()) {
					deleteDirectory(files[i]);
				} else {
					files[i].delete();
				}
			}
		}
		return (path.delete());
	}

}