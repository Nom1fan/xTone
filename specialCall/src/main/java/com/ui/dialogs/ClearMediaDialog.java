package com.ui.dialogs;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.widget.TextView;

import com.app.AppStateManager;
import com.mediacallz.app.R;
import com.services.ServerProxyService;

import com.data.objects.SpecialMediaType;


public class ClearMediaDialog extends android.app.DialogFragment {

    private static final String TAG  = ClearMediaDialog.class.getSimpleName();
    private String _destPhoneNumber  = "";
    private SpecialMediaType spMediaType;

    public ClearMediaDialog(SpecialMediaType MediaType , String destphoneNumber){

        _destPhoneNumber = destphoneNumber;
        spMediaType = MediaType;
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

                        Intent i = new Intent(getActivity().getApplicationContext(), ServerProxyService.class);
                        i.setAction(ServerProxyService.ACTION_CLEAR_MEDIA);
                        i.putExtra(ServerProxyService.DESTINATION_ID, _destPhoneNumber);
                        i.putExtra(ServerProxyService.SPECIAL_MEDIA_TYPE, spMediaType);
                        getActivity().getApplicationContext().startService(i);

                        String timeoutMsg = getActivity().getResources().getString(R.string.oops_try_again);
                        String loadingMsg = getActivity().getResources().getString(R.string.please_wait);
                        AppStateManager.setLoadingState(getActivity(), TAG, loadingMsg, timeoutMsg);

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