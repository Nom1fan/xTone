package com.xtone.receiver;

        import android.content.BroadcastReceiver;
        import android.content.Context;
        import android.content.Intent;

        import com.xtone.logging.Logger;
        import com.xtone.logging.LoggerFactory;
        import com.xtone.service.IncomingCallService;

        import java.util.ArrayList;
        import java.util.List;

/**
 * Created by Mor on 12/09/2015.
 */
public class BootCompletedBroadcastReceiver extends BroadcastReceiver {

    private static final Logger log = LoggerFactory.getLogger();

    private static final String TAG = BootCompletedBroadcastReceiver.class.getSimpleName();

    private static final String BOOT_COMPLETED_ACTION = "android.intent.action.BOOT_COMPLETED";

    private static final String QUICKBOOT_POWERON_ACTION = "android.intent.action.QUICKBOOT_POWERON";

    private static final List<String> ACTIONS = new ArrayList<String>()  {{
        add(BOOT_COMPLETED_ACTION);
        add(QUICKBOOT_POWERON_ACTION);
    }};

    @Override
    public void onReceive(Context context, Intent intent) {
        if (ACTIONS.contains(intent.getAction())) {
            log.info(TAG, "Got BOOT_COMPLETED action. Starting IncomingCallService...");
            Intent incomingReceiverIntent = new Intent(context, IncomingCallService.class);
            context.startService(incomingReceiverIntent);
        }
    }

}
