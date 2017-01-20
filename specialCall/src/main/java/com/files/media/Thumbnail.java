package com.files.media;

/**
 * Created by Mor on 05/09/2015.
 */
public class Thumbnail {

    private MediaFile.FileType _fileType;
    private String _thumbPath;

    public Thumbnail(MediaFile.FileType fileType, String thumbPath) {

        _fileType = fileType;
        _thumbPath = thumbPath;
    }

    public MediaFile.FileType getFileType() {
        return _fileType;
    }

    public String getThumbPath() {
        return _thumbPath;
    }


}
