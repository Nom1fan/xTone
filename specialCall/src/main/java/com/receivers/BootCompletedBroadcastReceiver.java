package com.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.services.IncomingService;

/**
 * Created by Mor on 12/09/2015.
 */
public class BootCompletedBroadcastReceiver extends BroadcastReceiver {

        private static final String ACTION = "android.intent.action.BOOT_COMPLETED";

        @Override
        public void onReceive(Context context, Intent intent) {
            if (ACTION.equals(intent.getAction())) {

                Intent incomingReceiverIntent = new Intent(context, IncomingService.class);
                incomingReceiverIntent.setAction(IncomingService.ACTION_START);
                context.startService(incomingReceiverIntent);
            }
        }

}
