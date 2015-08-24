package com.special.specialcall;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import data_objects.Constants;
import data_objects.SharedPrefUtils;
import android.app.Activity;
import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.provider.MediaStore;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.Window;
import android.view.WindowManager;


import android.net.Uri;
import android.os.Handler;
import android.widget.Toast;

	 public class IncomingReceiver extends BroadcastReceiver {
		 static CallStateListener phoneListener;
		 		
		private String incomingCallNumber;
		private Context gcontext;
		private Activity activityRef;
		private Uri mOldUri;
		private boolean ringtoneIsLoad;
		private static volatile boolean InRingingSession = false;
		private MediaPlayer  mMediaPlayer = new MediaPlayer();
		private int absRingerState;
		private boolean isSpecialCall = false;
	
		private static final int MSG_ID_CHECK_TOP_ACTIVITY = 1;
		private Intent i = new Intent(); 
		private static boolean mDismissed = false;
		private ActivityManager mActivityManager;
		private AudioManager am ;    
		private static final long DELAY_INTERVAL = 100;		
		private Handler mHandler = new Handler() {
		
		public void handleMessage(android.os.Message msg) {
		                 if (msg.what == MSG_ID_CHECK_TOP_ACTIVITY && !mDismissed) {
		                    
		                	 List<ActivityManager.RunningTaskInfo> tasks = mActivityManager.getRunningTasks(1);
		                	 
		                     String topActivityName = tasks.get(0).topActivity.getClassName();
		                     if (!topActivityName.equals(activityRef.getComponentName().getClassName())) 
		                     {// Try to show on top until user dismiss this activity
		                                              
		                       i.setClass(gcontext, IncomingSpecialCall.class);
		                       i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		                       i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
		              
		                       i.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
		                    
		                       // video view
		                      i.putExtra("videoORpic",incomingCallNumber +"."+ SharedPrefUtils.getString(gcontext, SharedPrefUtils.MEDIA, incomingCallNumber)); 		                       
		                //       i.putExtra("videoORpic",incomingCallNumber +"."+ "jpg");
		                       gcontext.startActivity(i);
		                       activityRef.finish();
		                     }
		                     sendEmptyMessageDelayed(MSG_ID_CHECK_TOP_ACTIVITY,DELAY_INTERVAL);
		                 }
		                 
		             };
		         };	
			
		 public void onReceive(final Context context, Intent intent){
				
		   try 
		   {
				  
			 activityRef = MainActivity.getInstance();			  
			 gcontext = context;	
			 am =(AudioManager)gcontext.getSystemService(Context.AUDIO_SERVICE); 
	
			if (phoneListener==null){	
				Log.e("YAEL", "!!!phoneListener is been called !!!"); 
				phoneListener = new CallStateListener();
			 TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
	         tm.listen(phoneListener, PhoneStateListener.LISTEN_CALL_STATE);
			}
		  }
		  catch (Exception e) {
             Log.e("Phone Receive Error", " " + e);
		  }
		 }
				 		 
		 // Listener to detect incoming calls.
	     public class CallStateListener extends PhoneStateListener {
	          
	        	
	     @Override
	     public void onCallStateChanged(int state, String incomingNumber) {
	    	 syncOnCallStateChange(state,incomingNumber);
	       }
	     }
	          		 		 		 
	     public synchronized void syncOnCallStateChange(int state, String incomingNumber){
	    	 
	    	   	AudioManager audioManager = (AudioManager)gcontext.getSystemService(Context.AUDIO_SERVICE);  
	    	   	
	    	    
	    	       switch(state) 
	    	       {
	    	                                                 
	                  case TelephonyManager.CALL_STATE_RINGING:
	                  if (!InRingingSession)
	                  {    	 
	            
	                	InRingingSession = true;
	                	absRingerState = SharedPrefUtils.getInt(gcontext, SharedPrefUtils.GENERAL, "ringerState",2);
	                	int ringerMode = absRingerState;
	                	
	      
	                	// RINGTONE
	                //	Log.e("YAEL", "before ringtone"); 
	                	File k = new File(Constants.specialCallPath+incomingNumber+"/" ,incomingNumber +"." + SharedPrefUtils.getString(gcontext, SharedPrefUtils.RINGTONE, incomingNumber));
	                	if(k.exists() /*&& !VideoValid && RingerMode == 2*/ )//ringerState == 2) 
	                	{  
	                		  
	              	  Log.e("YAEL", "inside if ringtone");
	                	  
	               	  mOldUri = RingtoneManager.getActualDefaultRingtoneUri(gcontext, RingtoneManager.TYPE_RINGTONE);
	                	  // On call you replace the ringtone with your own mUri
	                      RingtoneManager.setActualDefaultRingtoneUri(
	                      gcontext,//MainActivity.this,
	                      RingtoneManager.TYPE_RINGTONE,
	                      Uri.parse( SharedPrefUtils.getString(gcontext, SharedPrefUtils.RINGTONE_URI, incomingNumber)));
	                      Log.e("YAEL", "3");
	                      ringtoneIsLoad = true;
	                      isSpecialCall = true;
	                	}

	                	String downloadFileExtension = SharedPrefUtils.getString(gcontext, SharedPrefUtils.MEDIA, incomingNumber);
	                	downloadFileExtension = downloadFileExtension.toLowerCase();
	                	
	                	if (Arrays.asList(Constants.videoFormats).contains((downloadFileExtension)))
	                		audioManager.setRingerMode(AudioManager.RINGER_MODE_SILENT);
	
	                	if (ringerMode ==2)
	                	{
	                		if (!ringtoneIsLoad)
	                		  mOldUri = RingtoneManager.getActualDefaultRingtoneUri(gcontext, RingtoneManager.TYPE_RINGTONE);
	                		 
	                		SharedPrefUtils.setString(gcontext, SharedPrefUtils.GENERAL, "mOldUri", mOldUri.toString());
	    		            SharedPrefUtils.setInt(gcontext, SharedPrefUtils.GENERAL, "ringerState", 2);
	    		       	}
	                	             	 
	                	incomingCallNumber = incomingNumber;
	                	//////// VIDEO OR IMAGE
	                	String filePath = incomingNumber + "."+ SharedPrefUtils.getString(gcontext, SharedPrefUtils.MEDIA, incomingNumber);
	                	File img = new File(Constants.specialCallPath+incomingNumber+"/" ,filePath);  /// 
	              
	                	if(img.exists()) 
	                	{              	  
	                		isSpecialCall = true;
	                		Log.e("YAEL", "if img exists");
	                    	SharedPrefUtils.setString(gcontext, SharedPrefUtils.GENERAL, "incomingNumber",incomingNumber);
	                    	SharedPrefUtils.setInt(gcontext, SharedPrefUtils.GENERAL, "ringerState", ringerMode);

	                    	ringtoneIsLoad = false;
	                    	IncomingSpecialCall.finishedIncomingCall = false;
	                    	Log.e("RONY", "Ringing!! RONY !! RINGING!!");
	                                	                  		 
	                    	new Thread(){
	                       
	                          @Override
	                          public void run() {
	                            
	                              Window window = activityRef.getWindow();
	                              window.addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
	                              mActivityManager = (ActivityManager) gcontext.getSystemService(Context.ACTIVITY_SERVICE);
	                              mHandler.sendEmptyMessageDelayed(MSG_ID_CHECK_TOP_ACTIVITY,DELAY_INTERVAL);
	                              mDismissed = false;  // returning the IncomingCall Handler to always check when the incoming call window pop up so we can override it                          
	                          }
	                    	}.run();  // must place run(); here and not start(); , priority higher for run();
	                	}  
	                  }
	                	break;                                          
	                                     
	                  case TelephonyManager.CALL_STATE_IDLE:
	                	  
	             
	                    	  if (isSpecialCall)
	                    		  {audioManager.setRingerMode(absRingerState);
	                    		  
	                    		  deleteDirectory( new File(Constants.specialCallPath+incomingCallNumber+"/"));
	                    		  Toast.makeText(gcontext, "Removed :" + Constants.specialCallPath+incomingCallNumber+"/", Toast.LENGTH_LONG).show();
	                    		  isSpecialCall = false;
	                    		  }
	                    	  
	                	  if (ringtoneIsLoad) 
	                	   {       
	                		  RingtoneManager.setActualDefaultRingtoneUri(
	                		  gcontext,//MainActivity.this,
	    	                  RingtoneManager.TYPE_RINGTONE,mOldUri);           
	                	   	  gcontext.getContentResolver().delete( Uri.parse( SharedPrefUtils.getString(gcontext, SharedPrefUtils.RINGTONE_URI, incomingCallNumber)),
	                		  MediaStore.MediaColumns.DATA + "=\"" + SharedPrefUtils.getString(gcontext, SharedPrefUtils.GENERAL, "mUriFilePath") + "\"",  null);            	  
	                	   	  deleteDirectory( new File(Constants.specialCallPath+incomingNumber+"/"));	             	 
	                	   	  ringtoneIsLoad = false;
	                	   	  
	                      		
	        	           }                  
	                	  if  (InRingingSession)
	                	  {
	            			                		 InRingingSession = false;
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
		 		 
	    public static void DismissIncomingCallActivity(Boolean dismiss) {
	        	 
	        	 mDismissed = dismiss;	        	 	        	 
	    }		 		 		 		 
}
