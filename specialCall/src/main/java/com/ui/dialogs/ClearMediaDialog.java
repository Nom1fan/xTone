package com.ui.dialogs;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.widget.TextView;

import com.mediacallz.app.R;
import com.services.StorageServerProxyService;
import com.utils.SharedPrefUtils;
import com.utils.UI_Utils;

import DataObjects.SpecialMediaType;


public class ClearMediaDialog extends android.app.DialogFragment {

    private static final String TAG  = ClearMediaDialog.class.getSimpleName();
    private String _destPhoneNumber  = "";
    private SpecialMediaType spMediaType;
    private Context mcontext;

    public ClearMediaDialog(SpecialMediaType MediaType , String destphoneNumber ,Context context ){

        _destPhoneNumber = destphoneNumber;
        spMediaType = MediaType;
        mcontext = context;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // Use the Builder class for convenient dialog construction
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        final TextView content = new TextView(getActivity());
        content.setText(R.string.clear_dialog_summary);

        builder.setTitle(R.string.clear_dialog_title)
                .setView(content)
                .setPositiveButton(R.string.clear_btn, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {

                        Intent i = new Intent(getActivity().getApplicationContext(), StorageServerProxyService.class);
                        i.setAction(StorageServerProxyService.ACTION_CLEAR_MEDIA);
                        i.putExtra(StorageServerProxyService.DESTINATION_ID, _destPhoneNumber);
                        i.putExtra(StorageServerProxyService.SPECIAL_MEDIA_TYPE, spMediaType);
                        getActivity().getApplicationContext().startService(i);

                        if (!SharedPrefUtils.getBoolean(mcontext, SharedPrefUtils.GENERAL, SharedPrefUtils.DONT_SHOW_AGAIN_CLEAR_DIALOG)) {
                            UI_Utils.showWaitingForTranferSuccussDialog(mcontext, "ClearMediaDialog", getResources().getString(R.string.sending_clear_contact)
                                    , getResources().getString(R.string.waiting_for_clear_transfer_sucess_dialog_msg));
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