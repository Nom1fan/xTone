package com.ui.dialogs;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.text.Html;
import android.view.View;
import android.widget.TextView;

import com.data_objects.Constants;
import com.mediacallz.app.R;
import com.services.LogicServerProxyService;


public class TermsAndConditionsDialog extends android.app.DialogFragment {

    private static final String TAG  = TermsAndConditionsDialog.class.getSimpleName();
    private String _loginNumber  = "";
    private String _smsVerificationCode  = "";

    public TermsAndConditionsDialog(String loginNumber, String smsCode){

         _loginNumber = loginNumber;
         _smsVerificationCode = smsCode;

    }

    private View.OnClickListener TermsAndConditions = new View.OnClickListener() {
        public void onClick(View v) {

            String url = Constants.TERMS_AND_PRIVACY_URL;
            Intent i = new Intent(Intent.ACTION_VIEW);
            i.setData(Uri.parse(url));
            startActivity(i);
        }
    };

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // Use the Builder class for convenient dialog construction
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        final TextView content = new TextView(getActivity());
        String tac =getResources().getString(R.string.terms_and_conditions);
        content.setText(Html.fromHtml(tac));
        content.setClickable(true);
        content.setOnClickListener(TermsAndConditions);

        builder.setTitle(R.string.terms_and_conditions_title)
                .setView(content)
                .setPositiveButton(R.string.terms_and_conditions_button, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {

                        Constants.MY_ID(getActivity().getApplicationContext(), _loginNumber);

                        Intent i = new Intent(getActivity().getApplicationContext(), LogicServerProxyService.class);
                        i.setAction(LogicServerProxyService.ACTION_REGISTER);
                        i.putExtra(LogicServerProxyService.SMS_CODE, Integer.parseInt(_smsVerificationCode));
                        getActivity().getApplicationContext().startService(i);


                    }
                })
                .setNegativeButton(R.string.back, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {

                        getDialog().dismiss();
                    }
                });

        return builder.create();
    }


}