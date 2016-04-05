package com.utils;

import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.data_objects.SnackbarData;
import com.github.amlcurran.showcaseview.ShowcaseView;
import com.github.amlcurran.showcaseview.targets.ViewTarget;
import com.nispok.snackbar.Snackbar;

import java.lang.reflect.Field;

import EventObjects.EventReport;
import EventObjects.EventType;

/**
 * Created by Mor on 12/02/2016.
 */
public abstract class UI_Utils {

    private static final String TAG = UI_Utils.class.getSimpleName();

    public static void callToast(final String text, final int color, final Context context) {
        new Handler(Looper.getMainLooper()).post(new Runnable() {

            @Override
            public void run() {
                Toast toast = Toast.makeText(context, text,
                        Toast.LENGTH_LONG);
                TextView v = (TextView) toast.getView().findViewById(
                        android.R.id.message);
                v.setTextColor(color);
                toast.show();
            }
        });

    }

    public static void callToast(final String text, final int color, final int length ,final Context context) {
        new Handler(Looper.getMainLooper()).post(new Runnable() {

            @Override
            public void run() {
                Toast toast = Toast.makeText(context, text,
                        length);
                TextView v = (TextView) toast.getView().findViewById(
                        android.R.id.message);
                v.setTextColor(color);
                toast.show();
            }
        });

    }

    public static void makeActionOverflowMenuShown(Context context) {
        //devices with hardware menu button (e.g. Samsung Note) don't show action overflow menu
        try {
            ViewConfiguration config = ViewConfiguration.get(context);
            Field menuKeyField = ViewConfiguration.class.getDeclaredField("sHasPermanentMenuKey");
            if (menuKeyField != null) {
                menuKeyField.setAccessible(true);
                menuKeyField.setBoolean(config, false);
            }
        } catch (Exception e) {
            Log.d(TAG, e.getLocalizedMessage());
        }
    }

    public static void unbindDrawables(View view) {
        if (view != null) {
            if (view.getBackground() != null) {
                view.getBackground().setCallback(null);
            }
            if (view instanceof ViewGroup) {
                for (int i = 0; i < ((ViewGroup) view).getChildCount(); i++) {
                    unbindDrawables(((ViewGroup) view).getChildAt(i));
                }
                ((ViewGroup) view).removeAllViews();
            }
        }
    }

    public static void showCaseView(Activity activity, ViewTarget target, String title, String details) {

        Log.i(TAG, "Title: "+ title + " details: " + details);
        new ShowcaseView.Builder(activity)
                .setTarget(target)
                .setContentTitle(title)
                .setContentText(details)
                .hideOnTouchOutside().withMaterialShowcase()
                .build();

    }

    public static void showSnackBar(String msg, int color, Snackbar.SnackbarDuration sBarDuration, Context context) {

        SnackbarData snackbarData = new SnackbarData(SnackbarData.SnackbarStatus.SHOW, color, sBarDuration, msg);
        BroadcastUtils.sendEventReportBroadcast(context , TAG, new EventReport(EventType.REFRESH_UI, null, snackbarData));
    }

}
