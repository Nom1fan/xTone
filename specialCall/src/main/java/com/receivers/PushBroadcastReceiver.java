package com.receivers;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;
import com.services.ServerProxyService;
import com.google.gson.Gson;
import com.parse.ParsePushBroadcastReceiver;
import org.json.JSONException;
import org.json.JSONObject;

import DataObjects.PushEventKeys;
import DataObjects.TransferDetails;
import EventObjects.Event;
import EventObjects.EventReport;
import EventObjects.EventType;

/**
 * Created by mor on 10/09/2015.
 */
public class PushBroadcastReceiver extends ParsePushBroadcastReceiver {

    private static final String TAG = PushBroadcastReceiver.class.getSimpleName();
    private Context _context;
    private Gson gson = new Gson();

    @Override
    protected void onPushReceive(Context context, Intent intent) {

        _context = context;

        JSONObject pushData = getPushData(intent);
        String jsonData;
        TransferDetails td;


        try {
            String eventActionCode = pushData.getString(PushEventKeys.PUSH_EVENT_ACTION);
            Log.i(TAG, "PushEventActionCode:" + eventActionCode);
            switch(eventActionCode)
            {
                case PushEventKeys.PENDING_DOWNLOAD:
                    Log.i(TAG, "In:" + PushEventKeys.PENDING_DOWNLOAD);
                    jsonData = pushData.getString(PushEventKeys.PUSH_DATA);
                    td = gson.fromJson(jsonData, TransferDetails.class);

                    Intent i = new Intent(_context.getApplicationContext(), ServerProxyService.class);
                    i.setAction(ServerProxyService.ACTION_DOWNLOAD);
                    i.putExtra(PushEventKeys.PUSH_DATA, td);

                    _context.startService(i);
                break;

                case PushEventKeys.TRANSFER_SUCCESS:
                    Log.i(TAG, "In:" + PushEventKeys.TRANSFER_SUCCESS);
                    String msg = pushData.getString(PushEventKeys.PUSH_DATA);
                    jsonData = pushData.getString(PushEventKeys.PUSH_DATA_EXTRA);
                    td = gson.fromJson(jsonData, TransferDetails.class);

                    sendEventReportBroadcast(new EventReport(EventType.DESTINATION_DOWNLOAD_COMPLETE, msg , td));
                    super.onPushReceive(context, intent);
                    break;

                case PushEventKeys.SHOW_MESSAGE:
                    Log.i(TAG, "In:" + PushEventKeys.SHOW_MESSAGE);
                    super.onPushReceive(context, intent);
                break;
            }


        } catch (JSONException e) {
            e.printStackTrace();
        }


    }

    private void callInfoToast(Context context, final String text, final int g) {

        Toast toast = Toast.makeText(context, text,
                Toast.LENGTH_LONG);
        TextView v = (TextView) toast.getView().findViewById(
                android.R.id.message);
        v.setTextColor(g);
        toast.show();
    }

    private void sendEventReportBroadcast(EventReport report) {

        Log.i(TAG, "Broadcasting event:" + report.status().toString());
        Intent broadcastEvent = new Intent(Event.EVENT_ACTION);
        broadcastEvent.putExtra(Event.EVENT_REPORT, report);
        _context.sendBroadcast(broadcastEvent);
    }

    private JSONObject getPushData(Intent intent) {
        try {
            return new JSONObject(intent.getStringExtra("com.parse.Data"));
        } catch (JSONException var3) {
            Log.e(TAG, "Unexpected JSONException when receiving push data: ", var3);
            return null;
        }
    }
}
