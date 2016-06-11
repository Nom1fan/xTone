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
import com.services.LogicServerProxyService;
import com.ui.activities.MainActivity;

import EventObjects.EventReport;
import EventObjects.EventType;


public class DeleteAccountDialog extends android.app.DialogFragment {

    private static final String TAG  = DeleteAccountDialog.class.getSimpleName();

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // Use the Builder class for convenient dialog construction
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        final TextView content = new TextView(getActivity());
        content.setText(R.string.delete_account_are_you_sure_text);

        builder.setTitle(R.string.delete_account_title)
                .setView(content)
                .setPositiveButton(R.string.confirm, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {

                        Intent i = new Intent(getActivity(), LogicServerProxyService.class);
                        i.setAction(LogicServerProxyService.ACTION_UNREGISTER);
                        getActivity().startService(i);

                        // Preparing loading state with timeout in case unregister fails
                        String unregisterFailedMsg = getActivity().getResources().getString(R.string.delete_account_failed);
                        String unregisteringMsg = getResources().getString(R.string.deleting_account_msg);
                        EventReport unregisterFailed = new EventReport(
                                EventType.UNREGISTER_FAILURE,
                                unregisterFailedMsg,
                                null);
                        AppStateManager.setLoadingState(getActivity(), TAG, unregisteringMsg, unregisterFailedMsg);

                        // Returning from settings to MainActivity
                        Intent ii = new Intent(getActivity(), MainActivity.class);
                        startActivity(ii);

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