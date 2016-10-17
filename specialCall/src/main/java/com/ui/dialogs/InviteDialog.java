package com.ui.dialogs;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.view.ContextThemeWrapper;
import android.util.Log;
import android.widget.ImageButton;
import android.widget.TextView;

import com.mediacallz.app.R;
import com.utils.SharedPrefUtils;

import static com.crashlytics.android.Crashlytics.log;


public class InviteDialog extends android.app.DialogFragment {

    private static final String TAG  = InviteDialog.class.getSimpleName();

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // Use the Builder class for convenient dialog construction
        AlertDialog.Builder builder = new AlertDialog.Builder(new ContextThemeWrapper(getActivity(), R.style.AlertDialogCustom));

        final TextView content = new TextView(getActivity());
        content.setText(R.string.invite_dialog_summary);
        content.setTextColor(Color.WHITE);
        builder.setTitle(R.string.invite_dialog_title)
                .setView(content)
                .setPositiveButton(R.string.invite_btn, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {

                        try {

                            Intent intent = new Intent(Intent.ACTION_SENDTO,
                                    Uri.fromParts("sms", SharedPrefUtils.getString(getActivity().getApplicationContext(),SharedPrefUtils.GENERAL,SharedPrefUtils.DESTINATION_NUMBER), null));
                            intent.putExtra("sms_body", getResources().getString(R.string.invite));
                            startActivity(intent);

                            ImageButton clearButton = (ImageButton) getActivity().findViewById(R.id.clear);
                            clearButton.performClick();

                            SharedPrefUtils.setString(getActivity().getApplicationContext(), SharedPrefUtils.GENERAL, SharedPrefUtils.DESTINATION_NUMBER,"");
                            SharedPrefUtils.setString(getActivity().getApplicationContext(),SharedPrefUtils.GENERAL,SharedPrefUtils.DESTINATION_NAME,"");
                            getDialog().dismiss();

                        } catch (Exception ex) {
                            log(Log.ERROR,TAG, "Failed to open send SMS activity. [Exception]:" + (ex.getMessage() != null ? ex.getMessage() : ex));
                        }


                    }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {

                        ImageButton clearButton = (ImageButton) getActivity().findViewById(R.id.clear);
                        clearButton.performClick();

                        getDialog().dismiss();

                    }
                });

        return builder.create();
    }

}