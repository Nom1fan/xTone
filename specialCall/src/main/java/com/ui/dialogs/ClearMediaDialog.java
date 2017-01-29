package com.ui.dialogs;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.view.ContextThemeWrapper;
import android.widget.TextView;

import com.mediacallz.app.R;
import com.services.ServerProxyService;

import com.enums.SpecialMediaType;
import com.utils.SharedPrefUtils;
import com.utils.UI_Utils;


public class ClearMediaDialog extends android.app.DialogFragment {

    private static final String TAG  = ClearMediaDialog.class.getSimpleName();
    private String destPhoneNumber = "";
    private SpecialMediaType spMediaType;

    public ClearMediaDialog(SpecialMediaType MediaType , String destphoneNumber){

        destPhoneNumber = destphoneNumber;
        spMediaType = MediaType;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // Use the Builder class for convenient dialog construction
        AlertDialog.Builder builder = new AlertDialog.Builder(new ContextThemeWrapper(getActivity(), R.style.AlertDialogCustom));

        final Context context = getActivity().getApplicationContext();

        final TextView content = new TextView(context);
        content.setText(R.string.clear_dialog_summary);
        content.setTextColor(Color.WHITE);
        builder.setTitle(R.string.clear_dialog_title)
                .setView(content)
                .setPositiveButton(R.string.clear_btn, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {

                        Intent i = new Intent(context, ServerProxyService.class);
                        i.setAction(ServerProxyService.ACTION_CLEAR_MEDIA);
                        i.putExtra(ServerProxyService.DESTINATION_ID, destPhoneNumber);
                        i.putExtra(ServerProxyService.SPECIAL_MEDIA_TYPE, spMediaType);
                        context.startService(i);


                        if (!SharedPrefUtils.getBoolean(context, SharedPrefUtils.GENERAL, SharedPrefUtils.DONT_SHOW_AGAIN_CLEAR_DIALOG)) {
                            UI_Utils.showWaitingForTranferSuccussDialog(context, "ClearMediaDialog", getResources().getString(R.string.sending_clear_contact)
                                    , getResources().getString(R.string.waiting_for_clear_transfer_success_dialog_msg));
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

}