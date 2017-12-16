package com_international.ui.dialogs;

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

import com_international.app.AppStateManager;
import com_international.mediacallz.app.R;
import com_international.services.ServerProxyService;
import com_international.ui.activities.MainActivity;


public class DeleteAccountDialog extends android.app.DialogFragment {

    private static final String TAG  = DeleteAccountDialog.class.getSimpleName();

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // Use the Builder class for convenient dialog construction
        AlertDialog.Builder builder = new AlertDialog.Builder(new ContextThemeWrapper(getActivity(), R.style.AlertDialogCustom));

        final Context context = getActivity().getApplicationContext();

        final TextView content = new TextView(context);
        content.setText(R.string.delete_account_are_you_sure_text);
        content.setTextColor(Color.WHITE);
        builder.setTitle(R.string.delete_account_title)
                .setView(content)
                .setPositiveButton(R.string.confirm, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {

                        Intent i = new Intent(context, ServerProxyService.class);
                        i.setAction(ServerProxyService.ACTION_UNREGISTER);
                        context.startService(i);

                        // Preparing loading state with timeout in case unregister fails
                        String unregisterFailedMsg = getActivity().getResources().getString(R.string.delete_account_failed);
                        String unregisteringMsg = getResources().getString(R.string.deleting_account_msg);
                        AppStateManager.setLoadingState(context, TAG, unregisteringMsg, unregisterFailedMsg);

                        // Returning from settings to MainActivity
                        Intent ii = new Intent(context, MainActivity.class);
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