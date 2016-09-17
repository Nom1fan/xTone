package com.async_tasks;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;

import com.app.AppStateManager;
import com.interfaces.ICallbackListener;
import com.services.LogicServerProxyService;
import com.utils.BroadcastUtils;
import com.utils.CacheUtils;

import EventObjects.EventReport;
import EventObjects.EventType;

/**
 * Created by Mor on 26/02/2016.
 */
public class IsRegisteredTask extends AsyncTask<Context, Void, Void> {

    public static final String DRAW_SELECT_MEDIA_FALSE = "drawSelectMediaButton(false)";
    public static final String ENABLE_FETCH_PROGRESS_BAR = "enableFetchUserProgressBar()";
    private final String TAG = IsRegisteredTask.class.getSimpleName();
    private boolean _showProgressBar;
    private String _destPhone;
    private ICallbackListener _callbackListener;
    private Context _context;

    public IsRegisteredTask(String destPhone, ICallbackListener callbackListener) {

        this._destPhone = destPhone;
        this._callbackListener = callbackListener;
    }

    @Override
    protected Void doInBackground(Context... params) {

        _context = params[0];
        boolean isNonBlockingState = AppStateManager.isNonBlockingState(_context);
        boolean isPhoneInCache = CacheUtils.isPhoneInCache(_context, _destPhone); // Number was entered to cache after IS_REGISTERED_TRUE received in BackgroundBroadcastReceiver

        // Phone number is in registered cache - No need to check again
        if (isNonBlockingState && isPhoneInCache) {
            AppStateManager.setAppState(_context, TAG, AppStateManager.STATE_READY);
            BroadcastUtils.sendEventReportBroadcast(_context, TAG, new EventReport(EventType.REFRESH_UI));

            return null;
        }

        _callbackListener.doCallBackAction(DRAW_SELECT_MEDIA_FALSE);

        if (isNonBlockingState) {

            BroadcastUtils.sendEventReportBroadcast(_context, TAG + " onTextchanged()",
                    new EventReport(EventType.FETCHING_USER_DATA));

            // Sending action to find out if user is registered
            Intent i = new Intent(_context, LogicServerProxyService.class);
            i.setAction(LogicServerProxyService.ACTION_ISREGISTERED);
            i.putExtra(LogicServerProxyService.DESTINATION_ID, _destPhone);
            _context.startService(i);

            _showProgressBar = true;
        }

        return null;
    }

    @Override
    protected void onPostExecute(Void v) {

        if (_showProgressBar) {
            _callbackListener.doCallBackAction(ENABLE_FETCH_PROGRESS_BAR);
        }
    }
}
