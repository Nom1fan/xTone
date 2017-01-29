package com.data.objects;

import android.util.Log;

import com.enums.SpecialMediaType;
import com.files.media.MediaFile;

import java.io.Serializable;

import static com.crashlytics.android.Crashlytics.log;

/**
 * Created by Mor on 10/03/2016.
 */
public class MediaCall implements Serializable {

    private static final long serialVersionUID = 7408472793374531808L;
    private static final String TAG = MediaCall.class.getSimpleName();

    private String sourceId;

    private String destinationId;

    private MediaFile visualMediaFile;

    private MediaFile audioMediaFile;

    private SpecialMediaType specialMediaType;

    public MediaCall(String sourceId,
                     String destinationId,
                     MediaFile visualMediaFile,
                     MediaFile audioMediaFile,
                     SpecialMediaType specialMediaType) {
        this.sourceId = sourceId;
        this.destinationId = destinationId;
        this.visualMediaFile = visualMediaFile;
        this.audioMediaFile = audioMediaFile;
        this.specialMediaType = specialMediaType;

        log(Log.INFO, TAG, "MediaCall:" + this.toString());
    }

    public SpecialMediaType getSpecialMediaType() {
        return specialMediaType;
    }

    public MediaFile getVisualMediaFile() {
        return visualMediaFile;
    }

    public String getDestinationId() {
        return destinationId;
    }

    public String getSourceId() {
        return sourceId;
    }

    public MediaFile getAudioMediaFile() {
        return audioMediaFile;
    }

    @Override
    public String toString() {
        return "MediaCall{" +
                "sourceId='" + sourceId + '\'' +
                ", destinationId='" + destinationId + '\'' +
                ", visualMediaFile=" + visualMediaFile +
                ", audioMediaFile=" + audioMediaFile +
                ", specialMediaType=" + specialMediaType +
                '}';
    }
}
