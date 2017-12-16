package com_international.data.objects;

import com_international.enums.SpecialMediaType;

/**
 * Created by Mor on 10/03/2016.
 */
public class MediaCallData extends AbstractDataObject {

    public String FullphoneNumber;

    private String phoneNumber;

    private String visualMediaFilePath;

    private String audioMediaFilePath;

    private SpecialMediaType specialMediaType;

    private boolean doesAudioMediaExist;

    private boolean doesVisualMediaExist;

    private boolean shouldMuteRing;

    public MediaCallData() {}

    public boolean hasMedia() {
        return doesAudioMediaExist || doesVisualMediaExist;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public boolean doesAudioMediaExist() {
        return doesAudioMediaExist;
    }

    public void setDoesAudioMediaExist(boolean doesAudioFileExist) {
        this.doesAudioMediaExist = doesAudioFileExist;
    }

    public boolean doesVisualFileExist() {
        return doesVisualMediaExist;
    }

    public void setDoesVisualMediaExist(boolean doesVisualFileExist) {
        this.doesVisualMediaExist = doesVisualFileExist;
    }

    public boolean shouldMuteRing() {
        return shouldMuteRing;
    }

    public void setShouldMuteRing(boolean shouldMuteRing) {
        this.shouldMuteRing = shouldMuteRing;
    }

    public String getVisualMediaFilePath() {
        return visualMediaFilePath;
    }

    public void setVisualMediaFilePath(String visualMediaFilePath) {
        this.visualMediaFilePath = visualMediaFilePath;
    }

    public String getAudioMediaFilePath() {
        return audioMediaFilePath;
    }

    public void setAudioMediaFilePath(String audioMediaFilePath) {
        this.audioMediaFilePath = audioMediaFilePath;
    }

    public SpecialMediaType getSpecialMediaType() {
        return specialMediaType;
    }

    public void setSpecialMediaType(SpecialMediaType specialMediaType) {
        this.specialMediaType = specialMediaType;
    }

    @Override
    public String toString() {
        return "MediaCallData{" +
                "phoneNumber='" + phoneNumber + '\'' +
                ", visualMediaFilePath='" + visualMediaFilePath + '\'' +
                ", audioMediaFilePath='" + audioMediaFilePath + '\'' +
                ", specialMediaType=" + specialMediaType +
                ", doesAudioMediaExist=" + doesAudioMediaExist +
                ", doesVisualMediaExist=" + doesVisualMediaExist +
                ", shouldMuteRing=" + shouldMuteRing +
                '}';
    }
}
