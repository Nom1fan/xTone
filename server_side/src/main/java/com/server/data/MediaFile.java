package com.server.data;

import com.server.utils.MediaFilesUtils;

import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.io.Serializable;

import lombok.Data;

@Data
public class MediaFile implements Serializable {

    private String md5;
    private File file;
    private String extension;
    private long size;
    private FileType fileType;
    private boolean isCompressed = false;

    public enum FileType { IMAGE, VIDEO, AUDIO }

    public MediaFile(File file) {
        md5 = MediaFilesUtils.getMD5(file.getAbsolutePath());
        this.file = file;
        extension = FilenameUtils.getExtension(file.getAbsolutePath());
        size = file.length();
        fileType = MediaFilesUtils.getFileType(extension);
    }

}