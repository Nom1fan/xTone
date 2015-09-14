package com.special.specialcall;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.android.services.IncomingReceiver;
import com.android.services.ServerProxy;

/**
 * Created by Mor on 12/09/2015.
 */
public class BootCompletedBroadcastReceiver extends BroadcastReceiver {

        private static final String ACTION = "android.intent.action.BOOT_COMPLETED";

        @Override
        public void onReceive(Context context, Intent intent) {
            if (ACTION.equals(intent.getAction())) {
                Intent serverProxyIntent = new Intent(context, ServerProxy.class);
                context.startService(serverProxyIntent);
                Intent incomingReceiverIntent = new Intent(context, IncomingReceiver.class);
                context.startService(incomingReceiverIntent);
            }
        }

}
