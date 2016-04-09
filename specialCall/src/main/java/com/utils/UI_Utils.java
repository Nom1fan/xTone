package com.utils;

import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.data_objects.SnackbarData;
import com.github.amlcurran.showcaseview.OnShowcaseEventListener;
import com.github.amlcurran.showcaseview.ShowcaseView;
import com.github.amlcurran.showcaseview.targets.ViewTarget;
import com.mediacallz.app.R;

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

    public static void showSnackBar(String msg, int color, int sBarDuration, boolean isLoading ,Context context) {

        SnackbarData snackbarData = new SnackbarData(SnackbarData.SnackbarStatus.SHOW, color, sBarDuration, msg, isLoading);
        BroadcastUtils.sendEventReportBroadcast(context, TAG, new EventReport(EventType.REFRESH_UI, null, snackbarData));
    }

    //region ShowCaseView methods
    public static void showCaseView(Activity activity, ViewTarget target, String title, String details) {

        Log.i(TAG, "Title: " + title + " details: " + details);
     try{
        new ShowcaseView.Builder(activity)
                .setTarget(target)
                .setContentTitle(title)
                .setContentText(details)
                .hideOnTouchOutside().withMaterialShowcase()
                .build();

    }
    catch (NullPointerException | OutOfMemoryError e) {
        e.printStackTrace();
    }


    }

    public static void showCaseViewSelectProfile(Context context,Activity activity) {

        ViewTarget targetProfileView = new ViewTarget(R.id.selectProfileMediaBtn, activity);
        UI_Utils.showCaseView(activity, targetProfileView, context.getResources().getString(R.string.profile_sv_title), context.getResources().getString(R.string.profile_sv_details));
    }

    public static void showCaseViewCall(Context context,Activity activity) {

        ViewTarget targetCallView = new ViewTarget(R.id.CallNow, activity);
        UI_Utils.showCaseView(activity, targetCallView, context.getResources().getString(R.string.call_sv_title), context.getResources().getString(R.string.call_sv_details));
    }

    public static void showCaseViewAfterUploadAndCall(final Context context, final Activity activity) {

        if (!(SharedPrefUtils.getBoolean(context, SharedPrefUtils.SHOWCASE, SharedPrefUtils.UPLOAD_BEFORE_CALL_VIEW)) && SharedPrefUtils.getBoolean(context, SharedPrefUtils.SHOWCASE, SharedPrefUtils.CALL_NUMBER_VIEW))
        {



          try {
              ViewTarget targetSelectMediaView = new ViewTarget(R.id.selectMediaBtn, activity);
              ShowcaseView sv = new ShowcaseView.Builder(activity)
                      .setTarget(targetSelectMediaView)
                      .setContentTitle(context.getResources().getString(R.string.callermedia_sv_title))
                      .setContentText(context.getResources().getString(R.string.callermedia_sv_details_image_ringtone))
                      .hideOnTouchOutside().
                              withMaterialShowcase().build();

              sv.setOnShowcaseEventListener(new OnShowcaseEventListener() {
                  @Override
                  public void onShowcaseViewHide(ShowcaseView showcaseView) {

                      showCaseViewCall(context, activity);
                  }

                  @Override
                  public void onShowcaseViewDidHide(ShowcaseView showcaseView) {

                  }

                  @Override
                  public void onShowcaseViewShow(ShowcaseView showcaseView) {

                  }

                  @Override
                  public void onShowcaseViewTouchBlocked(MotionEvent motionEvent) {

                  }
              });
          } catch (NullPointerException | OutOfMemoryError e) {
                  e.printStackTrace();
              }


              SharedPrefUtils.setBoolean(context, SharedPrefUtils.SHOWCASE, SharedPrefUtils.UPLOAD_BEFORE_CALL_VIEW, true);
        }


    }

    public static void showCaseViewSelectMedia(final Context context, final Activity activity){
        if (!SharedPrefUtils.getBoolean(context, SharedPrefUtils.SHOWCASE, SharedPrefUtils.SELECT_MEDIA_VIEW) && SharedPrefUtils.getBoolean(context, SharedPrefUtils.SHOWCASE, SharedPrefUtils.CALL_NUMBER_VIEW)) {
      try{
            ViewTarget targetSelectMediaView = new ViewTarget(R.id.selectMediaBtn, activity);
            ShowcaseView sv = new ShowcaseView.Builder(activity)
                    .setTarget(targetSelectMediaView)
                    .setContentTitle(context.getResources().getString(R.string.callermedia_sv_title))
                    .setContentText(context.getResources().getString(R.string.callermedia_sv_details))
                    .hideOnTouchOutside().
                            withMaterialShowcase().build();

            sv.setOnShowcaseEventListener(new OnShowcaseEventListener() {
                @Override
                public void onShowcaseViewHide(ShowcaseView showcaseView) {

                    showCaseViewSelectProfile(context, activity);
                }

                @Override
                public void onShowcaseViewDidHide(ShowcaseView showcaseView) {
                }

                @Override
                public void onShowcaseViewShow(ShowcaseView showcaseView) {
                }

                @Override
                public void onShowcaseViewTouchBlocked(MotionEvent motionEvent) {
                }
            });
        } catch (NullPointerException | OutOfMemoryError e) {
            e.printStackTrace();
        }
            SharedPrefUtils.setBoolean(context, SharedPrefUtils.SHOWCASE, SharedPrefUtils.SELECT_MEDIA_VIEW, true);
        }
    }

    public static void showCaseViewCallNumber(Context context,Activity activity) {
        if (!(SharedPrefUtils.getBoolean(context, SharedPrefUtils.SHOWCASE, SharedPrefUtils.CALL_NUMBER_VIEW))) {
            ViewTarget targetCallNumber = new ViewTarget(R.id.selectContactBtn, activity);
            UI_Utils.showCaseView(activity, targetCallNumber, context.getResources().getString(R.string.callnumber_sv_title), context.getResources().getString(R.string.callnumber_sv_details));
            SharedPrefUtils.setBoolean(context, SharedPrefUtils.SHOWCASE, SharedPrefUtils.CALL_NUMBER_VIEW, true);
        }
    }
    //endregion

}
