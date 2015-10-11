package com.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.android.services.IncomingService;
import com.android.services.ServerProxyService;

/**
 * Created by Mor on 12/09/2015.
 */
public class BootCompletedBroadcastReceiver extends BroadcastReceiver {

        private static final String ACTION = "android.intent.action.BOOT_COMPLETED";

        @Override
        public void onReceive(Context context, Intent intent) {
            if (ACTION.equals(intent.getAction())) {
//                Intent serverProxyIntent = new Intent(context, ServerProxyService.class);
//                serverProxyIntent.setAction(ServerProxyService.ACTION_START);
//                context.startService(serverProxyIntent);
                Intent incomingReceiverIntent = new Intent(context, IncomingService.class);
                context.startService(incomingReceiverIntent);
            }
        }

}
