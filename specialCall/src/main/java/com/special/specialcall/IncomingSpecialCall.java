package com.special.specialcall;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;

import android.media.MediaPlayer;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.ActionBar;
import android.support.v4.app.Fragment;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.PendingIntent;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.media.AudioManager;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.MediaController;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;
import android.os.Build;
import android.provider.MediaStore;
import android.telephony.TelephonyManager;

import com.android.internal.telephony.ITelephony;
import com.special.specialcall.R;
import com.special.specialcall.IncomingReceiver.CallStateListener;

import data_objects.Constants;
import data_objects.SharedPrefUtils;

public class IncomingSpecialCall extends ActionBarActivity implements OnClickListener {

	private ITelephony telephonyService;
	static boolean finishedIncomingCall = false;
	TelephonyManager tm;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		try {
			Intent intent = getIntent();
			String VideoOrPic = intent.getStringExtra("videoORpic");

			String tmp_arr[] = VideoOrPic.split("\\.");
			String downloadFileExtension = tmp_arr[1];
			downloadFileExtension = downloadFileExtension.toLowerCase();

			boolean imageValid = false;

			if(Arrays.asList(Constants.imageFormats).contains(downloadFileExtension))
				imageValid = true;

			boolean VideoValid = false;

			if(Arrays.asList(Constants.videoFormats).contains(downloadFileExtension))
				VideoValid = true;

			CallStateListener stateListener = new CallStateListener();
			tm = (TelephonyManager) getApplicationContext().getSystemService(Context.TELEPHONY_SERVICE);
			tm.listen(stateListener, PhoneStateListener.LISTEN_CALL_STATE);



			final String incomingNumber = SharedPrefUtils.getString(getApplicationContext(), SharedPrefUtils.GENERAL, "incomingNumber");




			//    Log.d("IncomingCallActivity: onCreate: ", "flag2");
			//  */ After this line, the code is not executed in Android 4.1 (Jelly Bean) only/*
			// TODO Auto-generated method stub
			getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
			getWindow().addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
			getWindow().addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			//getWindow().addFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
			getWindow().addFlags(
					WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL);
			//      Log.d("IncomingCallActivity: onCreate: ", "flagy");


			if (imageValid)
			{
				setContentView(R.layout.activity_incoming_special_call);

				String imageInSD = Constants.specialCallPath+incomingNumber+"/" + incomingNumber +"."+downloadFileExtension;
				Log.d("imageInSD ", imageInSD);
				BitmapFactory.decodeFile(imageInSD);
				ImageView myImageView = (ImageView)findViewById(R.id.CallerImage);
				myImageView.setImageBitmap(loadImage(imageInSD));

				//  Log.d("IncomingCallActivity: onCreate: ", "flagz");

				Button Answer = (Button)findViewById(R.id.Answer);
				Answer.setOnClickListener(this);

				Button Decline = (Button)findViewById(R.id.Decline);
				Decline.setOnClickListener(this);

			}
			if (VideoValid)
			{
				/////////////////   video view

				RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT);
				RelativeLayout rlayout  = new RelativeLayout(this);
				rlayout.setLayoutParams(params);
				rlayout.setBackgroundColor(Color.CYAN);
				rlayout.setGravity(Gravity.CENTER_VERTICAL);


				final File root = new File(Constants.specialCallPath+incomingNumber+"/" + incomingNumber +"."+downloadFileExtension);
				//root.mkdirs();

				RelativeLayout.LayoutParams videoParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT);
				videoParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
				videoParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
				videoParams.addRule(RelativeLayout.ALIGN_PARENT_TOP);
				videoParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
				Uri uri =Uri.fromFile(root);


				AudioManager audioManager =  (AudioManager) getSystemService(Context.AUDIO_SERVICE);
				int previousRingerState = SharedPrefUtils.getInt(getApplicationContext(), SharedPrefUtils.GENERAL, "ringerState", AudioManager.RINGER_MODE_NORMAL);

				// if previously ringer was on, put it back on for video
				if (previousRingerState == AudioManager.RINGER_MODE_NORMAL)
				{
					audioManager.setRingerMode(AudioManager.RINGER_MODE_NORMAL);
				}

				//Uri uri =Uri.parse("http://www.youtube.com/watch?v=RLZUKqpXYzU");

				final VideoView mVideoView  = new VideoView(getApplicationContext());
				MediaController mediaController = new MediaController(this);
				mediaController.setAnchorView(mVideoView);
				mediaController.setMediaPlayer(mVideoView);//////////
				mVideoView.setMediaController(mediaController);
				mVideoView.setOnPreparedListener(PreparedListener);
				mVideoView.setVideoURI(uri);
				mVideoView.requestFocus();
				mVideoView.setLayoutParams(videoParams);
				mVideoView.setId(3);
			//	mVideoView.start();


//				//Looping the video
//				mVideoView.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
//
//					@Override
//					public void onCompletion(MediaPlayer mp) {
//
//						mVideoView.start();
//
//					}
//				});

				RelativeLayout.LayoutParams b1Params = new RelativeLayout.LayoutParams(150, 150);
				b1Params.addRule(RelativeLayout.ALIGN_LEFT,  mVideoView.getId());
				b1Params.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
				//	b1Params.setMargins(0, 50, 50, 0);

				Button videoAnswer = new Button(this);
				videoAnswer.setText("Answer");
				videoAnswer.setLayoutParams(b1Params);
				videoAnswer.setId(10000);




				//****************************************************************************

				RelativeLayout.LayoutParams b2Params = new RelativeLayout.LayoutParams(150, 150);
				b2Params.addRule(RelativeLayout.ALIGN_RIGHT,  videoAnswer.getId());
				b2Params.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
				//	b2Params.setMargins(50, 50, 0, 0);

				Button videoDecline = new Button(this);
				videoDecline.setText("Decline");
				videoDecline.setLayoutParams(b2Params);
				videoDecline.setId(10001);

				rlayout.addView(mVideoView);
				rlayout.addView(videoAnswer);
				rlayout.addView(videoDecline);

				setContentView(rlayout);

				videoAnswer.setOnClickListener(new OnClickListener() {
					@Override
					public void onClick(View arg0) {

						mVideoView.stopPlayback();
						Log.d("AnswerPlease", "InSecond Method Ans Call");
						// froyo and beyond trigger on buttonUp instead of buttonDown
						Intent buttonUp = new Intent(Intent.ACTION_MEDIA_BUTTON);
						buttonUp.putExtra(Intent.EXTRA_KEY_EVENT, new KeyEvent(
								KeyEvent.ACTION_UP, KeyEvent.KEYCODE_HEADSETHOOK));
						sendOrderedBroadcast(buttonUp, "android.permission.CALL_PRIVILEGED");
						Intent headSetUnPluggedintent = new Intent(Intent.ACTION_HEADSET_PLUG);
						headSetUnPluggedintent.addFlags(Intent.FLAG_RECEIVER_REGISTERED_ONLY);
						headSetUnPluggedintent.putExtra("state", 0);
						headSetUnPluggedintent.putExtra("name", "Headset");
						try {
							IncomingReceiver.DismissIncomingCallActivity(true);
							finishedIncomingCall = true;
							sendOrderedBroadcast(headSetUnPluggedintent, null);
							// Restore the default ringtone

							AudioManager am = (AudioManager) getBaseContext().getSystemService(Context.AUDIO_SERVICE);

							am.setRingerMode(SharedPrefUtils.getInt(getApplicationContext(), SharedPrefUtils.GENERAL, "ringerState",2));




							if (SharedPrefUtils.getInt(getApplicationContext(), SharedPrefUtils.GENERAL, "ringerState",2) == 2)
								RingtoneManager.setActualDefaultRingtoneUri(
										getApplicationContext(),//MainActivity.this,
										RingtoneManager.TYPE_RINGTONE,
										Uri.parse(SharedPrefUtils.getString(getApplicationContext(), SharedPrefUtils.GENERAL, "mOldUri")));

							deleteDirectory( new File(Environment.getExternalStorageDirectory()+"/SpecialCallIncoming/"+incomingNumber+"/"));
							Toast.makeText(getApplicationContext(), "Removed :" + Environment.getExternalStorageDirectory()+"/SpecialCallIncoming/"+incomingNumber+"/", Toast.LENGTH_LONG).show();
							finish();

						} catch (Exception e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						finally{
							finish();
						}
					}
				});
				videoDecline.setOnClickListener(new OnClickListener() {
					@Override
					public void onClick(View arg0) {

						try {
							mVideoView.stopPlayback();
							Log.d("REjectByRony", "OnRejectButton: " + "Reject OnClick");
							TelephonyManager tm = (TelephonyManager) getApplicationContext().getSystemService(getApplicationContext().TELEPHONY_SERVICE);
							Class c;

							c = Class.forName(tm.getClass().getName());

							Method m = null;
							try {
								m = c.getDeclaredMethod("getITelephony");
							} catch (NoSuchMethodException e1) {
								// TODO Auto-generated catch block
								e1.printStackTrace();
							}
							m.setAccessible(true);
							try {
								telephonyService = (ITelephony) m.invoke(tm);
							} catch (IllegalAccessException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							} catch (IllegalArgumentException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							} catch (InvocationTargetException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
							telephonyService.endCall();
							Log.d("REjectByRony", "FinishDecline");
							// dismissing our customized incoming call window

							IncomingReceiver.DismissIncomingCallActivity(true);
							finishedIncomingCall = true;
							// Restore the default ringtone
							AudioManager am = (AudioManager) getBaseContext().getSystemService(Context.AUDIO_SERVICE);
							am.setRingerMode(SharedPrefUtils.getInt(getApplicationContext(), SharedPrefUtils.GENERAL, "ringerState",2));


							if (SharedPrefUtils.getInt(getApplicationContext(), SharedPrefUtils.GENERAL, "ringerState",2) == 2)
								RingtoneManager.setActualDefaultRingtoneUri(
										getApplicationContext(),//MainActivity.this,
										RingtoneManager.TYPE_RINGTONE,
										Uri.parse(SharedPrefUtils.getString(getApplicationContext(), SharedPrefUtils.GENERAL, "mOldUri")));
							deleteDirectory( new File(Environment.getExternalStorageDirectory()+"/SpecialCallIncoming/"+incomingNumber+"/"));
							Toast.makeText(getApplicationContext(), "Removed :" + Environment.getExternalStorageDirectory()+"/SpecialCallIncoming/"+incomingNumber+"/", Toast.LENGTH_LONG).show();
							finish();


						}catch (ClassNotFoundException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						} finally{
							finish();
						}
					}

				});
			}
		}
		catch (Exception e) {
			Log.d("Exception", e.toString());
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	protected void onResume() {
		super.onResume();

		if (finishedIncomingCall)
		{


			final String incomingNumber = SharedPrefUtils.getString(getApplicationContext(), SharedPrefUtils.GENERAL, "incomingNumber");


			// deleteDirectory( new File(Environment.getExternalStorageDirectory()+"/SpecialCallIncoming/"+incomingNumber+"/"));
			Toast.makeText(getApplicationContext(), "Removed :" + Environment.getExternalStorageDirectory()+"/SpecialCallIncoming/"+incomingNumber+"/", Toast.LENGTH_LONG).show();

			Intent i = new Intent();

			SharedPrefUtils.setBoolean(getApplicationContext(), SharedPrefUtils.GENERAL, "LoggedIn",true);

			i.setClass(getApplicationContext(), MainActivity.class);
			i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			i.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
			startActivity(i);
			this.finish();
		}

	}

	private MediaPlayer.OnPreparedListener PreparedListener = new MediaPlayer.OnPreparedListener(){

		@Override
		public void onPrepared(MediaPlayer m) {

			Log.i("IncSpecialCall", "Starting OnPreparedListener");
			//m.setVolume(1, 1);
			m.setLooping(true);
			m.start();
			Log.i("IncSpecialCall", "Finishing OnPreparedListener");
		}
	};

	private Bitmap loadImage(String imgPath) {
		BitmapFactory.Options options;
		try {
			options = new BitmapFactory.Options();
			options.inSampleSize = 2;
			Bitmap bitmap = BitmapFactory.decodeFile(imgPath, options);
			return bitmap;
		} catch(Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	@Override
	public void onClick(View v) {

		int id = v.getId();
		if (id == R.id.Answer) {   /// ANSWER
			Log.d("AnswerPlease", "InSecond Method Ans Call");
			// froyo and beyond trigger on buttonUp instead of buttonDown
			Intent buttonUp = new Intent(Intent.ACTION_MEDIA_BUTTON);
			buttonUp.putExtra(Intent.EXTRA_KEY_EVENT, new KeyEvent(
					KeyEvent.ACTION_UP, KeyEvent.KEYCODE_HEADSETHOOK));
			sendOrderedBroadcast(buttonUp, "android.permission.CALL_PRIVILEGED");
			Intent headSetUnPluggedintent = new Intent(Intent.ACTION_HEADSET_PLUG);
			headSetUnPluggedintent.addFlags(Intent.FLAG_RECEIVER_REGISTERED_ONLY);
			headSetUnPluggedintent.putExtra("state", 0);
			headSetUnPluggedintent.putExtra("name", "Headset");
			try {
				IncomingReceiver.DismissIncomingCallActivity(true);
				finishedIncomingCall = true;
				sendOrderedBroadcast(headSetUnPluggedintent, null);

				Uri uri = Uri.parse(SharedPrefUtils.getString(getApplicationContext(), SharedPrefUtils.GENERAL, "mUri"));


				getContentResolver().delete(uri,MediaStore.MediaColumns.DATA + "=\"" +
						SharedPrefUtils.getString(getApplicationContext(), SharedPrefUtils.GENERAL, "mUriFilePath")  + "\"",  null);



				AudioManager am = (AudioManager) getBaseContext().getSystemService(Context.AUDIO_SERVICE);
				am.setRingerMode(SharedPrefUtils.getInt(getApplicationContext(), SharedPrefUtils.GENERAL, "ringerState",2));



				if (SharedPrefUtils.getInt(getApplicationContext(), SharedPrefUtils.GENERAL, "ringerState",2) == 2)
					RingtoneManager.setActualDefaultRingtoneUri(
							getApplicationContext(),//MainActivity.this,
							RingtoneManager.TYPE_RINGTONE,
							Uri.parse(SharedPrefUtils.getString(getApplicationContext(), SharedPrefUtils.GENERAL, "mOldUri")));

				deleteDirectory( new File(Environment.getExternalStorageDirectory()+"/SpecialCallIncoming/"+SharedPrefUtils.getString(getApplicationContext(), SharedPrefUtils.GENERAL, "incomingNumber")+"/"));
				Toast.makeText(getApplicationContext(), "Removed :" + Environment.getExternalStorageDirectory()+"/SpecialCallIncoming/"+SharedPrefUtils.getString(getApplicationContext(), SharedPrefUtils.GENERAL, "incomingNumber")+"/", Toast.LENGTH_LONG).show();



			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			finally
			{this.finish();}
		}

		else if (id == R.id.Decline ) {   /// DECLINE
			Log.d("REjectByRony", "OnRejectButton: " + "Reject OnClick");

			TelephonyManager tm = (TelephonyManager) getApplicationContext().getSystemService(getApplicationContext().TELEPHONY_SERVICE);
			try {
				Class c = Class.forName(tm.getClass().getName());
				Method m = c.getDeclaredMethod("getITelephony");
				m.setAccessible(true);
				telephonyService = (ITelephony) m.invoke(tm);
				telephonyService.endCall();
				Log.d("REjectByRony", "FinishDecline");
				// dismissing our customized incoming call window
				IncomingReceiver.DismissIncomingCallActivity(true);
				finishedIncomingCall = true;



				Uri uri = Uri.parse(SharedPrefUtils.getString(getApplicationContext(), SharedPrefUtils.GENERAL, "mUri"));
				getContentResolver().delete(uri,
						MediaStore.MediaColumns.DATA + "=\"" +  SharedPrefUtils.getString(getApplicationContext(), SharedPrefUtils.GENERAL, "mUriFilePath") + "\"",
						null);

				AudioManager am = (AudioManager) getBaseContext().getSystemService(Context.AUDIO_SERVICE);
				am.setRingerMode(SharedPrefUtils.getInt(getApplicationContext(), SharedPrefUtils.GENERAL, "ringerState",2));


				if (SharedPrefUtils.getInt(getApplicationContext(), SharedPrefUtils.GENERAL, "ringerState",2) == 2)
					RingtoneManager.setActualDefaultRingtoneUri(
							getApplicationContext(),//MainActivity.this,
							RingtoneManager.TYPE_RINGTONE,
							Uri.parse(SharedPrefUtils.getString(getApplicationContext(), SharedPrefUtils.GENERAL, "mOldUri")));

				deleteDirectory( new File(Environment.getExternalStorageDirectory()+"/SpecialCallIncoming/"+SharedPrefUtils.getString(getApplicationContext(), SharedPrefUtils.GENERAL, "incomingNumber")+"/"));
				Toast.makeText(getApplicationContext(), "Removed :" + Environment.getExternalStorageDirectory()+"/SpecialCallIncoming/"+SharedPrefUtils.getString(getApplicationContext(), SharedPrefUtils.GENERAL, "incomingNumber")+"/", Toast.LENGTH_LONG).show();

				this.finish();

			} catch (Exception e)
			{
				e.printStackTrace();
			}
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {

		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.incoming_special_call, menu);
		return true;
	}

	public static boolean deleteDirectory(File path) {
		if( path.exists() ) {
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
		return( path.delete() );
	}


	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	/**
	 * A placeholder fragment containing a simple view.
	 */
	public static class PlaceholderFragment extends Fragment {

		public PlaceholderFragment() {
		}

		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container,
								 Bundle savedInstanceState) {
			View rootView = inflater.inflate(
					R.layout.fragment_incoming_special_call, container, false);
			return rootView;
		}
	}

	public class CallStateListener extends PhoneStateListener {
		@Override
		public void onCallStateChanged(int state, String incomingNumber) {
			switch (state) {



				case TelephonyManager.DATA_DISCONNECTED:

				{  //Toast.makeText(getApplicationContext(), "CALL_STATE_IDLE BITCH: "+incomingNumber,Toast.LENGTH_LONG).show();
					IncomingReceiver.DismissIncomingCallActivity(true);
					finishedIncomingCall = true;
					finish();

					break;}


			}
		}
	}



}
