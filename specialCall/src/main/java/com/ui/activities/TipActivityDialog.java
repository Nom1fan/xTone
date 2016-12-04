package com.ui.activities;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;

import com.mediacallz.app.R;
import com.utils.SharedPrefUtils;

import java.util.Random;

/**
 * Created by rony on 12/10/2016.
 */
public class TipActivityDialog extends Activity{





    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.tip_dialog);
        this.setFinishOnTouchOutside(false);

        final String[] tipsCircularArray = new String[]{
                getApplicationContext().getResources().getString(R.string.tip1_windowresize),
                getApplicationContext().getResources().getString(R.string.tip2_profile),
                getApplicationContext().getResources().getString(R.string.tip3_block),
                getApplicationContext().getResources().getString(R.string.tip4_image_audio_gif),
                getApplicationContext().getResources().getString(R.string.tip5_you_can_preview),
                getApplicationContext().getResources().getString(R.string.tip6_ask_before_show)
        };

        Random r = new Random();
        final int[] tipsNum = {r.nextInt(6)};

        final int arrayLength = tipsCircularArray.length;

            // set the custom dialog components - text, image and button
            final TextView text = (TextView) findViewById(R.id.tip_msg);
            text.setText(tipsCircularArray[tipsNum[0] % arrayLength]);


            Button nextTipBtn = (Button) findViewById(R.id.next_tip);
            // if button is clicked, close the custom dialog
            nextTipBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    text.setText(tipsCircularArray[tipsNum[0]++ % arrayLength]);
                }
            });

            Button skipBtn = (Button) findViewById(R.id.skip);
            // if button is clicked, close the custom dialog
            skipBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    finish();
                }
            });

            CheckBox checkBox = (CheckBox) findViewById(R.id.dont_show_tips);
            checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

                    SharedPrefUtils.setBoolean(getApplicationContext(), SharedPrefUtils.GENERAL, SharedPrefUtils.DONT_SHOW_AGAIN_TIP, isChecked);

                }
            });
            checkBox.setText(this.getResources().getString(R.string.dont_show_again));


    }












}
