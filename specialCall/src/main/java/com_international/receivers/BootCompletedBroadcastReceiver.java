package com_international.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com_international.services.IncomingService;
import com_international.services.OutgoingService;

/**
 * Created by Mor on 12/09/2015.
 */
public class BootCompletedBroadcastReceiver extends BroadcastReceiver {

        private static final String ACTION = "android.intent.action.BOOT_COMPLETED";

        @Override
        public void onReceive(Context context, Intent intent) {
            if (ACTION.equals(intent.getAction())) {

                // Re-starting IncomingService
                Intent incomingReceiverIntent = new Intent(context, IncomingService.class);
                incomingReceiverIntent.setAction(IncomingService.ACTION_START);
                context.startService(incomingReceiverIntent);

                // Re-starting OutgoingService
                Intent OutgoingReceiverIntent = new Intent(context, OutgoingService.class);
                OutgoingReceiverIntent.setAction(OutgoingService.ACTION_START);
                context.startService(OutgoingReceiverIntent);
            }
        }

}
