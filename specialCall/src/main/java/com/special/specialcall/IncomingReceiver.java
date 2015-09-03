package com.special.specialcall;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import data_objects.Constants;
import data_objects.SharedPrefUtils;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.os.IBinder;
import android.provider.MediaStore;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.Window;
import android.view.WindowManager;
import android.net.Uri;
import android.os.Handler;
import android.widget.Toast;

public class IncomingReceiver extends Service {
	static CallStateListener phoneListener;

	private String incomingCallNumber;
	private Context gcontext;
	private Activity activityRef;
	private Uri mOldUri;
	private boolean ringtoneIsLoad;
	private static volatile boolean InRingingSession = false;
	private boolean isSpecialCall = false;
	private static final int MSG_ID_CHECK_TOP_ACTIVITY = 1;
	private Intent i = new Intent();
	private static boolean mDismissed = false;
	private ActivityManager mActivityManager;
    private final int MAX_TOP_ACTIVITY_RETRIES = 5;
	private static final long DELAY_INTERVAL = 100;
	private Handler mHandler = new Handler() {

		public void handleMessage(android.os.Message msg) {
			Log.e("RONY", "before If in Handler");
			if (msg.what == MSG_ID_CHECK_TOP_ACTIVITY && !mDismissed) {

				Log.e("RONY", "before Getrunningtasks");
				List<ActivityManager.RunningTaskInfo> tasks = mActivityManager.getRunningTasks(1);

				Log.e("RONY", "before getclassname");
				String topActivityName = tasks.get(0).topActivity.getClassName();
				Log.e("RONY", "topActivityName: " + topActivityName);
				Log.e("RONY", "MyClass: " + IncomingSpecialCall.class.getSimpleName());

				if (!topActivityName.equals(IncomingSpecialCall.class.getSimpleName()))
				{// Try to show on top until user dismiss this activity

					i.setClass(gcontext, IncomingSpecialCall.class);
					i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
					i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);

					i.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);

					// video view
					i.putExtra("videoORpic", incomingCallNumber + "." + SharedPrefUtils.getString(gcontext, SharedPrefUtils.MEDIA, incomingCallNumber));
					//       i.putExtra("videoORpic",incomingCallNumber +"."+ "jpg");
					gcontext.startActivity(i);
					//activityRef.finish();
				}
				sendEmptyMessageDelayed(MSG_ID_CHECK_TOP_ACTIVITY,DELAY_INTERVAL);
			}

		};
	};



	@Override
	public void onCreate() {
		Log.e("RONY", "Service onCreate");

		try
		{
			activityRef = MainActivity.getInstance();
			gcontext = getApplicationContext();

			if (phoneListener==null){
				Log.e("YAEL", "!!!phoneListener is been called !!!");
				phoneListener = new CallStateListener();
				TelephonyManager tm = (TelephonyManager) gcontext.getSystemService(Context.TELEPHONY_SERVICE);
				tm.listen(phoneListener, PhoneStateListener.LISTEN_CALL_STATE);
			}
		}
		catch (Exception e) {
			Log.e("Phone Receive Error", " " + e);
		}

	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {


		Log.e("RONY", "onStartCommand Start_sticky");
		return START_STICKY;

	}


	@Override
	public void onDestroy() {
		Log.e("RONY", "Service onDestroy");

	}


	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	// Listener to detect incoming calls.
	public class CallStateListener extends PhoneStateListener {


		@Override
		public void onCallStateChanged(int state, String incomingNumber) {
			syncOnCallStateChange(state,incomingNumber);
		}
	}

	public synchronized void syncOnCallStateChange(int state, String incomingNumber){



		switch(state)
		{

			case TelephonyManager.CALL_STATE_RINGING:
				if (!InRingingSession)
				{
					Log.e("RONY", "TelephonyManager.CALL_STATE_RINGING");
					InRingingSession = true;
					mOldUri = RingtoneManager.getActualDefaultRingtoneUri(gcontext, RingtoneManager.TYPE_RINGTONE);

					// RINGTONE
					File ringtoneFile = new File(Constants.specialCallPath+incomingNumber+"/" ,incomingNumber +"." + SharedPrefUtils.getString(gcontext, SharedPrefUtils.RINGTONE, incomingNumber));
					if(ringtoneFile.exists())
					{

						// On call you replace the ringtone with your own mUri
						RingtoneManager.setActualDefaultRingtoneUri(
								gcontext,//MainActivity.this,
								RingtoneManager.TYPE_RINGTONE,
								Uri.parse(SharedPrefUtils.getString(gcontext, SharedPrefUtils.RINGTONE_URI, incomingNumber)));

						ringtoneIsLoad = true;
						isSpecialCall = true;
					}

					// Saving previous ringtone uri
					SharedPrefUtils.setString(gcontext, SharedPrefUtils.GENERAL, "mOldUri", mOldUri.toString());


					String downloadFileExtension = SharedPrefUtils.getString(gcontext, SharedPrefUtils.MEDIA, incomingNumber);
					downloadFileExtension = downloadFileExtension.toLowerCase();
					incomingCallNumber = incomingNumber;

					// VIDEO OR IMAGE
					String filePath = incomingNumber + "."+ SharedPrefUtils.getString(gcontext, SharedPrefUtils.MEDIA, incomingNumber);
					File mediaFile = new File(Constants.specialCallPath+incomingNumber+"/" ,filePath);

					if(mediaFile.exists())
					{

                        if (Arrays.asList(Constants.videoFormats).contains((downloadFileExtension))) {

									// On call you replace the ringtone with a silent URI , defined as null
								RingtoneManager.setActualDefaultRingtoneUri(gcontext,RingtoneManager.TYPE_RINGTONE,null);

                        }

						isSpecialCall = true;

						SharedPrefUtils.setString(gcontext, SharedPrefUtils.GENERAL, "incomingNumber", incomingNumber);


						ringtoneIsLoad = false;
						IncomingSpecialCall.finishedIncomingCall = false;
						Log.e("RONY", "Ringing!! RONY !! RINGING!!");


						i.setClass(gcontext, IncomingSpecialCall.class);
						i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
						i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
						i.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
						// video view
						i.putExtra("videoORpic", incomingCallNumber + "." + SharedPrefUtils.getString(gcontext, SharedPrefUtils.MEDIA, incomingCallNumber));
						//       i.putExtra("videoORpic",incomingCallNumber +"."+ "jpg");


						Log.e("RONY", "START ACTIVITY before while");
						mDismissed = true;
						gcontext.startActivity(i);


						//activityRef.finish();
						try {
							Thread.sleep(500);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}


					//	mDismissed = false;

                        int count = 0;

						while (count<MAX_TOP_ACTIVITY_RETRIES) {
                            count++;
							mActivityManager = (ActivityManager) gcontext.getSystemService(Context.ACTIVITY_SERVICE);
							List<ActivityManager.RunningTaskInfo> tasks = mActivityManager.getRunningTasks(1);
							String topActivityName = tasks.get(0).topActivity.getClassName();

							Log.e("RONY", "Into the While");
							if (!topActivityName.equals(IncomingSpecialCall.class.getName())) {// Try to show on top until user dismiss this activity

                                i.setClass(gcontext, IncomingSpecialCall.class);
								i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
								i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                                i.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
								// video view
                                i.putExtra("videoORpic", incomingCallNumber + "." + SharedPrefUtils.getString(gcontext, SharedPrefUtils.MEDIA, incomingCallNumber));
								//       i.putExtra("videoORpic",incomingCallNumber +"."+ "jpg");


                                Log.e("RONY", "START ACTIVITY");
								gcontext.startActivity(i);


								//activityRef.finish();
								try {
									Thread.sleep(1000);
								} catch (InterruptedException e) {
									e.printStackTrace();
								}

							}

						}


/*

						new Thread(){

							@Override
							public void run() {
							//	Log.e("RONY", "activityRef.getWindow BEFORE");
							//	Window window = activityRef.getWindow();
							//	Log.e("RONY", "activityRef.getWindow AFTER");
							//	window.addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
								mActivityManager = (ActivityManager) gcontext.getSystemService(Context.ACTIVITY_SERVICE);
							//	boolean isSuccess = mHandler.sendEmptyMessageDelayed(MSG_ID_CHECK_TOP_ACTIVITY, DELAY_INTERVAL);
							//	Log.e("RONY", "mHandler.sendEmptyMessageDelayed Result:"+isSuccess);
								mDismissed = false;  // returning the IncomingCall Handler to always check when the incoming call window pop up so we can override it

								while (!mDismissed){

									List<ActivityManager.RunningTaskInfo> tasks = mActivityManager.getRunningTasks(1);
									String topActivityName = tasks.get(0).topActivity.getClassName();


									if (!topActivityName.equals(IncomingSpecialCall.class.getName()))
									{// Try to show on top until user dismiss this activity

										i.setClass(gcontext, IncomingSpecialCall.class);
										i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
										i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
										i.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
										// video view
										i.putExtra("videoORpic", incomingCallNumber + "." + SharedPrefUtils.getString(gcontext, SharedPrefUtils.MEDIA, incomingCallNumber));
										//       i.putExtra("videoORpic",incomingCallNumber +"."+ "jpg");

										new Thread() {

											public void run() {
												Log.e("RONY", "START ACTIVITY");
												gcontext.startActivity(i);
												mDismissed = true;
												Log.e("RONY", "After Dismissed");
											}
										}.run();
										//activityRef.finish();
										try {
											Thread.sleep(2000);
										} catch (InterruptedException e) {
											e.printStackTrace();
										}
										Log.e("RONY", "After Sleep");
									}

								}
							}
						}.run();  // must place run(); here and not start(); , priority higher for run();
*/
						Log.e("RONY", "Exited after Thread ");

					}
				}
				break;

			case TelephonyManager.CALL_STATE_IDLE:

				Log.e("RONY", "CALL_STATE_IDLE ");
				if (isSpecialCall)
				{

					RingtoneManager.setActualDefaultRingtoneUri(
							gcontext,
							RingtoneManager.TYPE_RINGTONE, mOldUri);


					deleteDirectory( new File(Constants.specialCallPath+incomingCallNumber+"/"));
					Toast.makeText(gcontext, "Removed :" + Constants.specialCallPath+incomingCallNumber+"/", Toast.LENGTH_LONG).show();
					isSpecialCall = false;
				}

				if (ringtoneIsLoad)
				{

					gcontext.getContentResolver().delete( Uri.parse( SharedPrefUtils.getString(gcontext, SharedPrefUtils.RINGTONE_URI, incomingCallNumber)),
							MediaStore.MediaColumns.DATA + "=\"" + SharedPrefUtils.getString(gcontext, SharedPrefUtils.GENERAL, "mUriFilePath") + "\"",  null);
					deleteDirectory( new File(Constants.specialCallPath+incomingNumber+"/"));
					ringtoneIsLoad = false;


				}
				if  (InRingingSession)
				{
					InRingingSession = false;
					DismissIncomingCallActivity(true);
					Log.e("RONY", "Dismiss is TRUE !! ");
				}
				break;



		}

	}

	public static boolean deleteDirectory(File path) {
		if(path.exists()) {
			File[] files = path.listFiles();
			if (files == null) {
				return true;
			}
			for(int i=0; i<files.length; i++) {
				if(files[i].isDirectory()) {
					deleteDirectory(files[i]);
				}
				else {
					files[i].delete();
				}
			}
		}
		return(path.delete());
	}

	public static synchronized void DismissIncomingCallActivity(Boolean dismiss) {

		mDismissed = dismiss;
	}
}
