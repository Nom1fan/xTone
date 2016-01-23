package com.data_objects;

import android.graphics.Color;

import com.nispok.snackbar.Snackbar;

import java.io.Serializable;

/**
 * Created by Mor on 23/01/2016.
 */
public class SnackbarData implements Serializable {

    private SnackbarStatus mStatus;
    private int mColor;
    private Snackbar.SnackbarDuration mDuration;
    private String mText;

    public SnackbarStatus getStatus() {
        return mStatus;
    }

    public int getColor() {
        return mColor;
    }

    public Snackbar.SnackbarDuration getmDuration() {
        return mDuration;
    }

    public String getText() {
        return mText;
    }

    public enum SnackbarStatus {

        SHOW,
        CLOSE
    }

    public SnackbarData(SnackbarStatus status, int color, Snackbar.SnackbarDuration duration, String text) {

        mStatus = status;
        mColor = color;
        mDuration = duration;
        mText = text;
    }

    public SnackbarData(SnackbarStatus status) {

        mStatus = status;
    }

}
