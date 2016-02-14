package com.services;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.provider.ContactsContract;
import android.util.Log;

import com.batch.android.Batch;
import com.google.gson.Gson;
import com.receivers.PushReceiver;
import com.utils.BroadcastUtils;
import com.utils.SharedPrefUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import DataObjects.PushEventKeys;
import DataObjects.TransferDetails;
import EventObjects.EventReport;
import EventObjects.EventType;

/**
 * Created by Mor on 05/02/2016.
 */
public class PushService extends IntentService {

    private static final String TAG = PushService.class.getSimpleName();
    private Context _context;
    private Gson gson = new Gson();

    public PushService() {
        super(PushService.class.getSimpleName());
    }

    @Override
    protected void onHandleIntent(Intent intent) {

        _context = getApplicationContext();

        String jsonData;
        TransferDetails td;
        String eventActionCode;

        try {
            if( !Batch.Push.shouldDisplayPush(this, intent) ) // Check that the push is valid
            {
                String errMsg = "Invalid push data! Push data was null. Terminating push receive";
                Log.e(TAG, errMsg);
                throw new Exception(errMsg);
            }

            //String alert = intent.getStringExtra(Batch.Push.ALERT_KEY);
            //BatchPushData pushData = new BatchPushData(this, intent);
            eventActionCode = intent.getStringExtra(PushEventKeys.PUSH_EVENT_ACTION);
            Log.i(TAG, "PushEventActionCode:" + eventActionCode);

            switch(eventActionCode)
            {
                case PushEventKeys.PENDING_DOWNLOAD:
                    Log.i(TAG, "In:" + PushEventKeys.PENDING_DOWNLOAD);
                    jsonData = intent.getStringExtra(PushEventKeys.PUSH_EVENT_DATA);
                    td = gson.fromJson(jsonData, TransferDetails.class);

                  if(checkIfNumberIsMCBlocked(td.getSourceId())) //don't download if the number is blocked , just break and don't continue with the download flow
                        break;

                    Intent i = new Intent(_context.getApplicationContext(), StorageServerProxyService.class);
                    i.setAction(StorageServerProxyService.ACTION_DOWNLOAD);
                    i.putExtra(PushEventKeys.PUSH_DATA, td);
                    _context.startService(i);

                    break;

                case PushEventKeys.TRANSFER_SUCCESS:
                    String msg = intent.getStringExtra(Batch.Push.ALERT_KEY);
                    jsonData = intent.getStringExtra(PushEventKeys.PUSH_EVENT_DATA);
                    td = gson.fromJson(jsonData, TransferDetails.class);

                    BroadcastUtils.sendEventReportBroadcast(_context, TAG, new EventReport(EventType.DESTINATION_DOWNLOAD_COMPLETE, msg, td));
                    Batch.Push.displayNotification(this, intent);
                    break;

                case PushEventKeys.SHOW_MESSAGE:
                    Log.i(TAG, "In:" + PushEventKeys.SHOW_MESSAGE);
                    Batch.Push.displayNotification(this, intent);
                    break;
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {

            PushReceiver.completeWakefulIntent(intent);
        }
    }

    protected boolean checkIfNumberIsMCBlocked(String incomingNumber) {
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
                    List<String> phones = new ArrayList<String>();
                    Cursor curPhones = this.getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null, null, null, null);
                    while (curPhones.moveToNext())
                    {
                        String phoneNumber = curPhones.getString(curPhones.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
                        phones.add(phoneNumber.replaceAll("\\D+", ""));
                    }
                    curPhones.close();

                    if (phones.contains(incomingNumber.replaceAll("\\D+", "")))
                        return true;
                    else
                        return false;


                case "black_list":

                    Set<String> blockedSet = SharedPrefUtils.getStringSet(getApplicationContext(), SharedPrefUtils.SETTINGS, SharedPrefUtils.BLOCK_LIST);
                    Set<String> onlyNumbersBlockedSet = new HashSet<String>();
                    if (blockedSet!=null) {
                        incomingNumber = incomingNumber.replaceAll("\\D+", "");

                        for (String s : blockedSet) {
                            s = s.replaceAll("\\D+", "");
                            onlyNumbersBlockedSet.add(s);
                        }
                        if (onlyNumbersBlockedSet.contains(incomingNumber)) {
                            Log.i(TAG, "NUMBER MC BLOCKED: " + incomingNumber);
                            return true;
                        }
                    }
                    return false;

            }
        }
        return false;
    }

}

