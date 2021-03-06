package com.utils;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.support.v7.app.AlertDialog;
import android.support.v7.view.ContextThemeWrapper;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.app.AppStateManager;
import com.daimajia.androidanimations.library.Techniques;
import com.data.objects.SnackbarData;
import com.event.EventReport;
import com.event.EventType;
import com.github.amlcurran.showcaseview.OnShowcaseEventListener;
import com.github.amlcurran.showcaseview.ShowcaseView;
import com.github.amlcurran.showcaseview.targets.ViewTarget;
import com.mediacallz.app.R;
import com.services.AbstractStandOutService;
import com.services.PreviewService;

import java.lang.reflect.Field;
import java.util.Random;

import static com.crashlytics.android.Crashlytics.log;
import static com.data.objects.SnackbarData.SnackbarStatus;

/**
 * Created by Mor on 12/02/2016.
 */
public abstract class UI_Utils {

    private static final String TAG = UI_Utils.class.getSimpleName();
    private static AlertDialog _waitingForTransferSuccessDialog;
    private static CountDownTimer ct;
    private static final Techniques[] inTechniques = {Techniques.Landing, Techniques.StandUp, Techniques.RollIn, Techniques.BounceIn, Techniques.FadeIn
            , Techniques.FlipInX, Techniques.RotateIn, Techniques.RotateInUpRight, Techniques.ZoomInDown};

    private static final Techniques[] outTechniques = {Techniques.Hinge, Techniques.TakingOff, Techniques.RollOut, Techniques.FadeOut
            , Techniques.FlipOutX, Techniques.RotateOut, Techniques.SlideOutRight, Techniques.ZoomOutUp};

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

    public static void callToast(final String text, final int color, final int length, final Context context) {
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

    public static void showSnackBar(String msg, int color, int sBarDuration, boolean isLoading, Context context) {

        SnackbarData snackbarData = new SnackbarData(SnackbarStatus.SHOW, color, sBarDuration, msg, isLoading);
        BroadcastUtils.sendEventReportBroadcast(context, TAG, new EventReport(EventType.REFRESH_UI, null, snackbarData));
    }

    public static void closeSnackBar(Context ctx) {
        BroadcastUtils.sendEventReportBroadcast(ctx, TAG, new EventReport(EventType.REFRESH_UI, null, new SnackbarData(SnackbarStatus.CLOSE)));
    }

    public static void refreshUI(Context ctx, SnackbarData snackbarData) {
        BroadcastUtils.sendEventReportBroadcast(ctx, TAG, new EventReport(EventType.REFRESH_UI, null, snackbarData));
    }

    public static Techniques getRandomInTechniques() {
        return inTechniques[new Random().nextInt(inTechniques.length)];
    }

    public static Techniques getRandomOutTechniques() {
        return outTechniques[new Random().nextInt(outTechniques.length)];
    }

    //region ShowCaseView methods
    public static void showCaseView(Activity activity, ViewTarget target, String title, String details) {
        log(Log.INFO, TAG, "Title: " + title + " details: " + details);
        try {

            new ShowcaseView.Builder(activity)
                    .setTarget(target)
                    .setContentTitle(title)
                    .setContentText(details)
                    .hideOnTouchOutside().withNewStyleShowcase()
                    .build().setButtonPosition(centerlizeParams());

        } catch (NullPointerException | OutOfMemoryError e) {
            e.printStackTrace();
        }
    }

    private static RelativeLayout.LayoutParams centerlizeParams() {

        RelativeLayout.LayoutParams rel_btn = new RelativeLayout.LayoutParams(
                0, 0);

        rel_btn.addRule(RelativeLayout.CENTER_HORIZONTAL, RelativeLayout.TRUE);
        rel_btn.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.TRUE);

        rel_btn.bottomMargin = 100;
        return rel_btn;
    }

    public static void showCaseViewSelectProfile(Context context, Activity activity) {

        ViewTarget targetProfileView = new ViewTarget(R.id.showcase_profilemedia, activity);
        UI_Utils.showCaseView(activity, targetProfileView, context.getResources().getString(R.string.profile_sv_title), context.getResources().getString(R.string.profile_sv_details));
    }

    public static void showCaseViewCall(Context context, Activity activity) {

        ViewTarget targetCallView = new ViewTarget(R.id.CallNow, activity);
        UI_Utils.showCaseView(activity, targetCallView, context.getResources().getString(R.string.call_sv_title), context.getResources().getString(R.string.call_sv_details));
    }

    public static void showCaseViewAfterUploadAndCall(final Context context, final Activity activity) {

        if (!(SharedPrefUtils.getBoolean(context, SharedPrefUtils.SHOWCASE, SharedPrefUtils.UPLOAD_BEFORE_CALL_VIEW)) && SharedPrefUtils.getBoolean(context, SharedPrefUtils.SHOWCASE, SharedPrefUtils.CALL_NUMBER_VIEW)) {
            SharedPrefUtils.setBoolean(context, SharedPrefUtils.SHOWCASE, SharedPrefUtils.UPLOAD_BEFORE_CALL_VIEW, true);


            try {
                ViewTarget targetSelectMediaView = new ViewTarget(R.id.showcase_callermedia, activity);
                ShowcaseView sv = new ShowcaseView.Builder(activity)
                        .setTarget(targetSelectMediaView)
                        .setContentTitle(context.getResources().getString(R.string.callermedia_sv_title))
                        .setContentText(context.getResources().getString(R.string.callermedia_sv_details_image_ringtone))
                        .hideOnTouchOutside().
                                withNewStyleShowcase().build();

                sv.setButtonPosition(centerlizeParams());
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

        }

    }

    public static void showCaseViewSelectMedia(final Context context, final Activity activity) {
        if (!SharedPrefUtils.getBoolean(context, SharedPrefUtils.SHOWCASE, SharedPrefUtils.SELECT_MEDIA_VIEW) && SharedPrefUtils.getBoolean(context, SharedPrefUtils.SHOWCASE, SharedPrefUtils.CALL_NUMBER_VIEW)) {
            try {
                SharedPrefUtils.setBoolean(context, SharedPrefUtils.SHOWCASE, SharedPrefUtils.SELECT_MEDIA_VIEW, true);
                ViewTarget targetSelectMediaView = new ViewTarget(R.id.showcase_callermedia, activity);
                ShowcaseView sv = new ShowcaseView.Builder(activity)
                        .setTarget(targetSelectMediaView)
                        .setContentTitle(context.getResources().getString(R.string.callermedia_sv_title))
                        .setContentText(context.getResources().getString(R.string.callermedia_sv_details))
                        .hideOnTouchOutside().
                                withNewStyleShowcase().build();
                sv.setButtonPosition(centerlizeParams());

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

        }
    }

    public static void showCaseViewCallNumber(Context context, Activity activity) {
        if (!(SharedPrefUtils.getBoolean(context, SharedPrefUtils.SHOWCASE, SharedPrefUtils.CALL_NUMBER_VIEW)) && (AppStateManager.isLoggedIn(context))) {
            ViewTarget targetCallNumber = new ViewTarget(R.id.destName, activity);
            UI_Utils.showCaseView(activity, targetCallNumber, context.getResources().getString(R.string.callnumber_sv_title), context.getResources().getString(R.string.callnumber_sv_details));
            SharedPrefUtils.setBoolean(context, SharedPrefUtils.SHOWCASE, SharedPrefUtils.CALL_NUMBER_VIEW, true);
        }
    }

    //endregion
    public static void showWaitingForTranferSuccussDialog(final Context applicationContext, final String whoRequest, final String title, final String msg) {

        View checkBoxView = View.inflate(applicationContext, R.layout.checkbox_dont_show_again, null);
        CheckBox checkBox = (CheckBox) checkBoxView.findViewById(R.id.checkbox_dont_show_again);
        checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

                if (whoRequest.equals("ClearMediaDialog")) {
                    SharedPrefUtils.setBoolean(applicationContext, SharedPrefUtils.GENERAL, SharedPrefUtils.DONT_SHOW_AGAIN_CLEAR_DIALOG, isChecked);
                }

                if (whoRequest.equals("MainActivity")) {
                    SharedPrefUtils.setBoolean(applicationContext, SharedPrefUtils.GENERAL, SharedPrefUtils.DONT_SHOW_AGAIN_UPLOAD_DIALOG, isChecked);
                }
            }
        });
        checkBox.setText(applicationContext.getResources().getString(R.string.dont_show_again));

        final AlertDialog.Builder builder = new AlertDialog.Builder(new ContextThemeWrapper(applicationContext, R.style.AlertDialogCustom));
        builder.setTitle(title);
        builder.setMessage(String.format(msg, 20))

                .setView(checkBoxView)
                .setCancelable(false)
                .setPositiveButton(applicationContext.getResources().getString(R.string.just_notify), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {

                        dialog.cancel();
                        log(Log.INFO, TAG, "dialog.cancel();");
                    }
                });

        _waitingForTransferSuccessDialog = builder.create();
        _waitingForTransferSuccessDialog.show();

        ct = new CountDownTimer(20000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                _waitingForTransferSuccessDialog.setMessage(String.format(msg, millisUntilFinished / 1000));
            }

            @Override
            public void onFinish() {
                _waitingForTransferSuccessDialog.setMessage(applicationContext.getResources().getString(R.string.finished_waiting_msg));
            }
        }.start();

        log(Log.INFO, TAG, "waitingForTransferSuccessDialog.show();");
    }

    public static void dismissTransferSuccessDialog() {
        if (_waitingForTransferSuccessDialog != null) {
            ct.cancel();
            _waitingForTransferSuccessDialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick();
            log(Log.INFO, TAG, "waitingForTransferSuccessDialog.performClick();");
        }
    }

    public static void dismissAllStandOutWindows(Context context) {

        Intent closePrevious = new Intent(context, PreviewService.class); // PREVIEW_SERVICE
        closePrevious.setAction(AbstractStandOutService.ACTION_CLOSE_ALL);
        context.startService(closePrevious);
    }

}
