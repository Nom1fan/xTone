package com.xtone.model;

import java.io.File;

public class MediaFile {

    private File file;

    private File thumbnailFile;

    private MediaFileType mediaFileType;

    public MediaFile() {
    }

    public MediaFile(File file, File thumbnailFile, MediaFileType mediaFileType) {
        this.file = file;
        this.thumbnailFile = thumbnailFile;
        this.mediaFileType = mediaFileType;
    }

    public File getFile() {
        return file;
    }

    public void setFile(File file) {
        this.file = file;
    }

    public File getThumbnailFile() {
        return thumbnailFile;
    }

    public void setThumbnailFile(File thumbnailFile) {
        this.thumbnailFile = thumbnailFile;
    }

    public MediaFileType getMediaFileType() {
        return mediaFileType;
    }

    public void setMediaFileType(MediaFileType mediaFileType) {
        this.mediaFileType = mediaFileType;
    }
}
