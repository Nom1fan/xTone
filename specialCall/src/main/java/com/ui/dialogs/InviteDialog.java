package com.ui.dialogs;

import android.Manifest;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.view.ContextThemeWrapper;
import android.telephony.SmsManager;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import com.mediacallz.app.R;

import static com.crashlytics.android.Crashlytics.log;

public class InviteDialog extends DialogFragment {

    private static final String TAG  = InviteDialog.class.getSimpleName();
    private String name = "";
    private String number = "";

    public InviteDialog(String name,String number){
        this.name = name;
        this.number = number;

    }


    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState ) {
        // Use the Builder class for convenient dialog construction
        AlertDialog.Builder builder = new AlertDialog.Builder(new ContextThemeWrapper(getActivity(), R.style.AlertDialogCustom));

        final Context context = getActivity().getApplicationContext();

        String msg =  String.format(context.getResources().getString(R.string.user_is_unregistered), name);

        final TextView content = new TextView(context);
        content.setText(R.string.invite_dialog_summary);
        content.setTextColor(Color.WHITE);
        builder.setTitle(msg)
                .setView(content)
                .setPositiveButton(R.string.invite_btn, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {

                        try {

                            sendSMS(context,number,getResources().getString(R.string.invite));

                            getDialog().dismiss();

                        } catch (Exception ex) {
                            log(Log.ERROR,TAG, "Failed to open send SMS activity. [Exception]:" + (ex.getMessage() != null ? ex.getMessage() : ex));
                        }


                    }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {

                        getDialog().dismiss();

                    }
                });

        return builder.create();
    }


    public void sendSMS(Context context,String phoneNo, String msg) {


        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED)
        {
            try {
                SmsManager smsManager = SmsManager.getDefault();
                smsManager.sendTextMessage(phoneNo, null, msg, null, null);
                Toast.makeText(context, "Message Sent",
                        Toast.LENGTH_LONG).show();
            } catch (Exception ex) {
                Toast.makeText(context,ex.getMessage().toString(),
                        Toast.LENGTH_LONG).show();
                ex.printStackTrace();
            }
        }
        else
        {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            {
                requestPermissions(new String[]{Manifest.permission.SEND_SMS}, 10);
            }
        }


    }

}