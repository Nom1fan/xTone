package com.model.response;

import com.data.objects.SpecialMediaType;
import com.files.media.MediaFile;

public class MediaCallDTO {

    private String sourceId;
    private String destinationId;
    private MediaFile visualMediaFile;
    private MediaFile audioMediaFile;
    private SpecialMediaType specialMediaType;

    public String getSourceId() {
        return sourceId;
    }

    public void setSourceId(String sourceId) {
        this.sourceId = sourceId;
    }

    public String getDestinationId() {
        return destinationId;
    }

    public void setDestinationId(String destinationId) {
        this.destinationId = destinationId;
    }

    public MediaFile getVisualMediaFile() {
        return visualMediaFile;
    }

    public void setVisualMediaFile(MediaFile visualMediaFile) {
        this.visualMediaFile = visualMediaFile;
    }

    public MediaFile getAudioMediaFile() {
        return audioMediaFile;
    }

    public void setAudioMediaFile(MediaFile audioMediaFile) {
        this.audioMediaFile = audioMediaFile;
    }

    public SpecialMediaType getSpecialMediaType() {
        return specialMediaType;
    }

    public void setSpecialMediaType(SpecialMediaType specialMediaType) {
        this.specialMediaType = specialMediaType;
    }
}