package com.data_objects;

import java.io.Serializable;

/**
 * Created by Mor on 23/01/2016.
 */
public class SnackbarData implements Serializable {

    private SnackbarStatus mStatus;
    private int mColor;
    private int mDuration;
    private String mText;
    private boolean mIsLoading = false;

    public SnackbarData(SnackbarStatus status, int color, int duration, String text) {

        mStatus = status;
        mColor = color;
        mDuration = duration;
        mText = text;
    }

    public SnackbarData(SnackbarStatus status, int color, int duration, String text, boolean isLoading) {

        mStatus = status;
        mColor = color;
        mDuration = duration;
        mText = text;
        mIsLoading = isLoading;
    }

    public SnackbarStatus getStatus() {
        return mStatus;
    }

    public int getColor() {
        return mColor;
    }

    public int getDuration() {
        return mDuration;
    }

    public String getText() {
        return mText;
    }

    public boolean isLoading() {
        return mIsLoading;
    }

    public enum SnackbarStatus {

        SHOW,
        CLOSE
    }

}
