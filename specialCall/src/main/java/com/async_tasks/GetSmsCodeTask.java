package com.async_tasks;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.ImageButton;
import android.widget.TextView;

import com.mediacallz.app.R;

import java.lang.ref.WeakReference;

/**
 * Created by Mor on 03/04/2016.
 */
public class GetSmsCodeTask extends AsyncTask<Void, String, String> {

    private static final String TAG = GetSmsCodeTask.class.getSimpleName();
    private Context _context;
    private final WeakReference<TextView> _textViewWeakReference;
    private final WeakReference<ImageButton> _getSmsButtonWeakReference;
    private static final int ONE_MINUTE = 60;
    private static final int ONE_HOUR = 60*ONE_MINUTE;
    private static final int MAX_RETRIES = 3;
    private static int _tryCount = 0;

    public GetSmsCodeTask(Context context, TextView textView, ImageButton getSmsButton) {

        _context = context;
        _textViewWeakReference = new WeakReference<>(textView);
        _getSmsButtonWeakReference = new WeakReference<>(getSmsButton);
    }

    @Override
    protected String doInBackground(Void... voids) {

        _tryCount++;

        if(_tryCount >= MAX_RETRIES) {
            return delayUntilNextRetry();
        }

        return countUntilSmsArrives();
    }

    @Override
    protected void onCancelled() {

        Log.i(TAG, "Got cancelled.");
        final TextView textView = _textViewWeakReference.get();
        final ImageButton getSmsCodeButton = _getSmsButtonWeakReference.get();
        if(textView!=null)
            textView.setText("");
        if(getSmsCodeButton!=null)
        {
            getSmsCodeButton.setEnabled(true);
            getSmsCodeButton.setImageResource(R.drawable.send_sms_icon);
        }
    }

    @Override
    protected void onProgressUpdate(String ... msg) {

        final TextView textView = _textViewWeakReference.get();
        final ImageButton getSmsButton = _getSmsButtonWeakReference.get();

        if(getSmsButton!=null && getSmsButton.isEnabled())
        {
            getSmsButton.setEnabled(false);
            getSmsButton.setImageResource(R.drawable.send_sms_icon_disabled);
        }
        if(textView!=null) {
            textView.setVisibility(TextView.VISIBLE);
            textView.setText(msg[0]);
        }
    }

    @Override
    protected void onPostExecute(String msg) {

        final TextView textView = _textViewWeakReference.get();
        if(textView!=null) {
            textView.setVisibility(TextView.VISIBLE);
            textView.setText(msg);
        }

        final ImageButton getSmsButton = _getSmsButtonWeakReference.get();
        if(getSmsButton!=null) {
            getSmsButton.setEnabled(true);
            getSmsButton.setImageResource(R.drawable.send_sms_icon);
        }
        _context = null;
    }

    private String countUntilSmsArrives() {

        int time = ONE_MINUTE;
        for(int j=time; j>-1; j--) {

            if(isCancelled())
                break;

            String msg = _context.getResources().getString(R.string.get_sms_wait_text);
            msg = String.format(msg, j);

            publishProgress(msg);
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        return _context.getResources().getString(R.string.get_sms_not_arrived);
    }

    private String delayUntilNextRetry() {

        int time = ONE_HOUR;

        for(int i=time; i>-1; i--) {

            if(isCancelled()) {
                break;
            }

            String msg = _context.getResources().getString(R.string.get_sms_time_till_retry);
            msg = String.format(msg, i);

            publishProgress(msg);
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        _tryCount = 0;

        return _context.getResources().getString(R.string.get_sms_you_may_retry);
    }


}
