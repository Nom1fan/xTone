package com.data.objects;

import com.files.media.MediaFile;

import java.io.Serializable;

/**
 * Created by Mor on 10/03/2016.
 */
public class CallRecord implements Serializable {

    private static final long serialVersionUID = 7408472793374531808L;
    private static final String TAG = CallRecord.class.getSimpleName();

    private String sourceId;
    private String destinationId;

    private MediaFile visualMediaFile;
    private MediaFile audioMediaFile;

    private String visualMd5;
    private String audioMd5;

    private SpecialMediaType specialMediaType;

    public CallRecord(String source,
                      String destination,
                      MediaFile visualMediaFile,
                      String visualM5,
                      MediaFile audioMediaFile,
                      String audioMd5,
                      SpecialMediaType specialMediaType) {

        sourceId = source;
        destinationId = destination;

        this.visualMediaFile = visualMediaFile;
        this.audioMediaFile = audioMediaFile;

        if(visualM5!=null)
            visualMd5 = visualM5;

        if(audioMd5!=null)
            this.audioMd5 = audioMd5;

        this.specialMediaType = specialMediaType;

        System.out.println("I/" + TAG + ": CallRecord:" + this.toString());
    }

    @Override
    public String toString() {

        StringBuilder builder = new StringBuilder();
        builder.
                append(", [Source]:").append(sourceId).
                append(", [Destination]:").append(destinationId).
                append(", [Special Media Type]:").append(specialMediaType.toString());

                if(visualMediaFile !=null) {
                    builder.append(", [Visual Media File]:").append(visualMediaFile);
                    builder.append(", [visual_md5]:").append(visualMd5);
                }
                if (audioMediaFile !=null) {
                    builder.append(", [Audio Media File]:").append(audioMediaFile);
                    builder.append(", [audio_md5]:").append(audioMd5);
                }

        return builder.toString();
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

    public String getVisualMd5() {
        return visualMd5;
    }

    public String getAudioMd5() {
        return audioMd5;
    }
}
