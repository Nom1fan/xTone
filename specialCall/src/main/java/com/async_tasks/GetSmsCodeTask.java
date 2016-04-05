package com.async_tasks;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.widget.Button;
import android.widget.TextView;

import com.mediacallz.app.R;
import com.services.LogicServerProxyService;

import java.lang.ref.WeakReference;

import utils.PhoneNumberUtils;

/**
 * Created by Mor on 03/04/2016.
 */
public class GetSmsCodeTask extends AsyncTask<String, String, String> {

    private Context _context;
    private final WeakReference<TextView> _textViewWeakReference;
    private final WeakReference<Button> _buttonWeakReference;
    private static final int ONE_MINUTE = 60;
    private static final int ONE_HOUR = 60*ONE_MINUTE;
    private static final int MAX_RETRIES = 3;
    private static int _tryCount = 0;

    public GetSmsCodeTask(Context context, TextView textView, Button button) {

        _context = context;
        _textViewWeakReference = new WeakReference<>(textView);
        _buttonWeakReference = new WeakReference<>(button);
    }

    @Override
    protected String doInBackground(String... params) {

        _tryCount++;

        if(_tryCount >= MAX_RETRIES) {
            return delayUntilNextRetry();
        }

        String phoneNumber = params[0];
        getSms(phoneNumber);

        return countUntilSmsArrives();
    }

    @Override
    protected void onCancelled(String str) {

        final TextView textView = _textViewWeakReference.get();
        if(textView!=null)
            textView.setText("");
    }

    @Override
    protected void onProgressUpdate(String ... msg) {

        final TextView textView = _textViewWeakReference.get();
        final Button button = _buttonWeakReference.get();

        if(button!=null && button.isEnabled())
            button.setEnabled(false);
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

        final Button button = _buttonWeakReference.get();
        if(button!=null)
            button.setEnabled(true);

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

    private void getSms(String phoneNumber) {

        String interPhoneNumber = PhoneNumberUtils.toValidInternationalPhoneNumber(
                phoneNumber,
                PhoneNumberUtils.Country.IL);

        Intent i = new Intent(_context, LogicServerProxyService.class);
        i.setAction(LogicServerProxyService.ACTION_GET_SMS_CODE);
        i.putExtra(LogicServerProxyService.INTER_PHONE, interPhoneNumber);
        _context.startService(i);
    }

    private String delayUntilNextRetry() {

        //TODO change to one hour after testing
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