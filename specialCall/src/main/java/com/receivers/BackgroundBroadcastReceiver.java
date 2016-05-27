package com.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.support.design.widget.Snackbar;
import android.util.Log;

import com.app.AppStateManager;
import com.data_objects.Constants;
import com.mediacallz.app.R;
import com.ui.activities.LoginActivity;
import com.utils.CacheUtils;
import com.utils.ContactsUtils;
import com.utils.LUT_Utils;
import com.utils.SharedPrefUtils;
import com.utils.UI_Utils;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import DataObjects.DataKeys;
import DataObjects.SpecialMediaType;
import EventObjects.Event;
import EventObjects.EventReport;
import EventObjects.EventType;
import FilesManager.FileManager;

/**
 * Created by Mor on 01/10/2015.
 */
public class BackgroundBroadcastReceiver extends BroadcastReceiver {

    private static final String TAG = BackgroundBroadcastReceiver.class.getSimpleName();

    @Override
    public void onReceive(Context context, Intent intent) {

        EventReport report = (EventReport) intent.getSerializableExtra(Event.EVENT_REPORT);

        Log.i(TAG, "Receiving event:" + report.status());

        //Ignore refresh UI event sent from here on the same events channel
        if (report.status().equals(EventType.REFRESH_UI) || AppStateManager.getAppState(context).equals(AppStateManager.STATE_LOGGED_OUT))
            return;

        boolean shouldShowSnackBar = true;
        String msg = "";
        int sBarDuration = 0;
        int color = 0;
        boolean isLoading = false;

        switch (report.status()) {

            //region Events in loading states
            case RECONNECT_ATTEMPT: {
                shouldShowSnackBar = false;
                //TODO commented out by Mor - need to check if this is necessary or just bothers the user
//                // Setting loading state
//                String timeOutMsg = "Oops! Please check your internet connection.";
//                AppStateManager.setAppState(context, TAG + " RECONNECT ATTEMPT",
//                        AppStateManager.createLoadingState(new EventReport(EventType.DISCONNECTED, timeOutMsg, null), 0),
//                        report.desc());
//
//                // Setting parameters for snackbar message
//                color = Color.RED;
//                sBarDuration = Snackbar.SnackbarDuration.LENGTH_INDEFINITE;
            }
            break;

            case FETCHING_USER_DATA: {
                // Setting loading state
                String timeoutMsg = context.getResources().getString(R.string.oops_try_again);
                String loadingMsg = context.getResources().getString(R.string.fetching_user_data);
                AppStateManager.setAppState(context, TAG + " FETCHING_USER_DATA",
                        AppStateManager.createLoadingState(new EventReport(EventType.ISREGISTERED_ERROR, timeoutMsg, null), 10 * 1000),
                        loadingMsg);

                // Setting parameters for snackbar message
                msg = loadingMsg;
                color = Color.GREEN;
                sBarDuration = Snackbar.LENGTH_LONG;
                isLoading = true;
            }
            break;

            case CONNECTING: {
                //TODO commented out by Mor - need to check if this is necessary or just bothers the user
//                // Setting loading state
//                String timeoutMsg = "Huh? No internet?";
//                AppStateManager.setAppState(context, TAG + " CONNECTING",
//                        AppStateManager.createLoadingState(new EventReport(EventType.DISCONNECTED, timeoutMsg, null), 0),
//                        report.desc());
//                // Setting parameters for snackbar message
//                color = Color.GREEN;
//                sBarDuration = Snackbar.SnackbarDuration.LENGTH_INDEFINITE;
            }
            break;

            case COMPRESSING: {
                // Setting loading state
                String timeoutMsg = context.getResources().getString(R.string.compression_took_too_long);
                String loadingMsg = context.getResources().getString(R.string.compressing_file);
                AppStateManager.setAppState(context, TAG,
                        AppStateManager.createLoadingState(new EventReport(EventType.REFRESH_UI, timeoutMsg, null),
                                AppStateManager.MAXIMUM_TIMEOUT_IN_MILLISECONDS), loadingMsg);

                // Setting parameters for snackbar message
                msg = loadingMsg;
                color = Color.GREEN;
                sBarDuration = Snackbar.LENGTH_INDEFINITE;
                isLoading = true;
            }
            break;

            case UPLOADING: {
                // Setting loading state
                String timeoutMsg = context.getResources().getString(R.string.upload_took_too_long);
                String loadingMsg = context.getResources().getString(R.string.uploading);
                AppStateManager.setAppState(context, TAG,
                        AppStateManager.createLoadingState(new EventReport(EventType.REFRESH_UI, timeoutMsg, null),
                                AppStateManager.MAXIMUM_TIMEOUT_IN_MILLISECONDS), loadingMsg);

                // Setting parameters for snackbar message
                msg = loadingMsg;
                color = Color.GREEN;
                sBarDuration = Snackbar.LENGTH_INDEFINITE;
                isLoading = true;
            }
            break;
            //endregion

            case LOADING_CANCEL:
                AppStateManager.setAppPrevState(context, TAG + " " + EventType.LOADING_CANCEL);

                // Setting parameters for snackbar message
                msg = context.getResources().getString(R.string.action_cancelled);
                color = Color.YELLOW;
                sBarDuration = Snackbar.LENGTH_LONG;
                break;

            case UPDATE_USER_RECORD_SUCCESS:
                shouldShowSnackBar = false;
                Constants.MY_ANDROID_VERSION(context, Build.VERSION.RELEASE);
                break;

            //region Events in Idle, ready and disabled states
            case DISPLAY_MESSAGE:
                msg = report.desc();
                color = Color.GREEN;
                sBarDuration = Snackbar.LENGTH_INDEFINITE;
                break;

            case DISPLAY_ERROR:
                msg = report.desc();
                color = Color.RED;
                sBarDuration = Snackbar.LENGTH_INDEFINITE;
                break;

            case DISCONNECTED:
                AppStateManager.setAppState(context, TAG + " DISCONNECTED", AppStateManager.STATE_DISABLED);

                // Setting parameters for snackbar message
                msg = context.getResources().getString(R.string.disconnected);
                color = Color.RED;
                sBarDuration = Snackbar.LENGTH_INDEFINITE;
                break;

            case DESTINATION_DOWNLOAD_COMPLETE: {
                Log.i(TAG, "In: DESTINATION_DOWNLOAD_COMPLETE");
                // Preparing _data for uploaded media thumbnail display
                Map data = (Map) report.data();
                String destId = data.get(DataKeys.DESTINATION_ID).toString();
                LUT_Utils lut_utils = new LUT_Utils(SpecialMediaType.valueOf(data.get(DataKeys.SPECIAL_MEDIA_TYPE).toString()));
                lut_utils.saveUploadedPerNumber(
                        context,
                        destId,
                        FileManager.FileType.valueOf(data.get(DataKeys.FILE_TYPE).toString()),
                        data.get(DataKeys.FILE_PATH_ON_SRC_SD).toString());

                // Setting parameters for snackbar message
                msg = String.format(context.getResources().getString(R.string.destination_download_complete),
                        ContactsUtils.getContactNameHtml(context, destId));
                color = Color.GREEN;
                sBarDuration = Snackbar.LENGTH_LONG;
            }
                break;

            case CLEAR_SUCCESS: {
                // Preparing _data for uploaded media thumbnail removal
                Map data = (Map) report.data();
                String destId = data.get(DataKeys.DESTINATION_ID).toString();
                LUT_Utils lut_utils = new LUT_Utils(SpecialMediaType.valueOf(data.get(DataKeys.SPECIAL_MEDIA_TYPE).toString()));
                lut_utils.removeUploadedMediaPerNumber(context, destId);
                lut_utils.removeUploadedTonePerNumber(context, destId);

                // Setting parameters for snackbar message
                msg = String.format(context.getResources().getString(R.string.destination_media_cleared),
                        ContactsUtils.getContactNameHtml(context, destId));
                color = Color.GREEN;
                sBarDuration = Snackbar.LENGTH_LONG;
            }
                break;

            case USER_REGISTERED_TRUE: {
                String destNumber = (String) report.data();
                if(destNumber!=null) {
                    msg = String.format(context.getResources().getString(R.string.user_is_registered),
                            ContactsUtils.getContactNameHtml(context, destNumber));
                    CacheUtils.setPhone(context, (String) report.data());

                    // Setting parameters for snackbar message
                    color = Color.GREEN;
                    sBarDuration = Snackbar.LENGTH_LONG;
                }
                // Setting state
                AppStateManager.setAppState(context, TAG, AppStateManager.STATE_READY);
            }
                break;
//            case UPLOAD_SUCCESS:
//                msg = context.getResources().getString(R.string.upload_success);
//            //case COMPRESSION_COMPLETE: //TODO commented out by Mor - need to check if this is necessary or just bothers the user
//                // Setting state
//                AppStateManager.setAppState(context, TAG, AppStateManager.STATE_READY);
//
//                // Setting parameters for snackbar message
//                color = Color.GREEN;
//                sBarDuration = Snackbar.LENGTH_LONG;
//                break;

            case CONNECTED:
                // Setting state based on previous state
                String prevState = AppStateManager.getAppPrevState(context);
                if (prevState.equals(AppStateManager.STATE_LOGGED_IN))
                    AppStateManager.setAppState(context, TAG, AppStateManager.STATE_IDLE);
                else
                    AppStateManager.setAppState(context, TAG, AppStateManager.getAppPrevState(context));

                // Setting parameters for snackbar message
                msg = context.getResources().getString(R.string.connected);
                color = Color.GREEN;
                sBarDuration = Snackbar.LENGTH_LONG;
                break;

            case REGISTER_SUCCESS:
                // Handled by LoginActivity
                break;

            case USER_REGISTERED_FALSE: {
                String destNumber = (String) report.data();
                msg =  String.format(context.getResources().getString(R.string.user_is_unregistered),
                        ContactsUtils.getContactNameHtml(context, destNumber));
            }
            case ISREGISTERED_ERROR:
            case STORAGE_ACTION_FAILURE:
            case UNREGISTER_FAILURE:
                if(msg.equals(""))
                    msg = context.getResources().getString(R.string.oops_try_again);
                // Setting app state
                AppStateManager.setAppState(context, TAG, AppStateManager.STATE_IDLE);

                // Setting parameters for snackbar message
                color = Color.RED;
                sBarDuration = Snackbar.LENGTH_LONG;
                break;

            case UNREGISTER_SUCCESS:
                try {

                    shouldShowSnackBar = false;
                    //TODO Decide if we should delete MEDIA_CALLZ_HISTORY folder contents too or not
                    FileManager.deleteDirectoryContents(new File(Constants.INCOMING_FOLDER));
                    FileManager.deleteDirectoryContents(new File(Constants.OUTGOING_FOLDER));
                    FileManager.deleteDirectoryContents(new File(Constants.TEMP_RECORDING_FOLDER));

                    //TODO Make sure this doesn't create issues since it delete all app states and such
                    SharedPrefUtils.removeAll(context);

                    AppStateManager.setAppState(context, TAG, AppStateManager.STATE_LOGGED_OUT);
                    Intent i = new Intent(context, LoginActivity.class);
                    i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    context.startActivity(i);

                } catch (IOException e) {
                    e.printStackTrace();
                    Log.e(TAG, "Failed during unregister procedure. [Exception]:"
                            + (e.getMessage() != null ? e.getMessage() : e));
                }
                break;
            //endregion

            default: // Event not meant for background receiver
                    return;

        }

        if(shouldShowSnackBar)
            UI_Utils.showSnackBar(msg, color, sBarDuration, isLoading, context);
    }

}
