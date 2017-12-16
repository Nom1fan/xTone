package com_international.files.media;

import com_international.utils.MediaFileUtils;
import com_international.utils.UtilityFactory;

import java.io.File;
import java.io.Serializable;


/**
 * Exposes various methods that manipulate and validate files and their properties.
 * When instantiated, it will hold a specific file which all non-static methods operations will relate to.
 * It also defines the maximum file size allowed in the system.
 *
 * @author Mor
 */
public class MediaFile implements Serializable {

    private static final String TAG = MediaFile.class.getSimpleName();

    private String md5;
    private File file;
    private String extension;
    private long size;
    private FileType fileType;
    private String uncompdFileFullPath;
    private boolean isCompressed = false;


    public enum FileType {IMAGE, VIDEO, AUDIO}

    public MediaFile() {

    }

    public MediaFile(File file) {
        this(file, false);
    }

    public MediaFile(File file, boolean shouldGenerateMd5) {
        MediaFileUtils mediaFileUtils = UtilityFactory.instance().getUtility(MediaFileUtils.class);

        this.file = file;
        extension = mediaFileUtils.extractExtension(this.file.getAbsolutePath());
        fileType = mediaFileUtils.getFileType(file);
        size = this.file.length();

        if(shouldGenerateMd5) {
            md5 = mediaFileUtils.getMD5(file.getAbsolutePath());
        }
    }

    public static String getTAG() {
        return TAG;
    }

    public String getMd5() {
        return md5;
    }

    public void setMd5(String md5) {
        this.md5 = md5;
    }

    public File getFile() {
        return file;
    }

    public void setFile(File file) {
        this.file = file;
    }

    public String getExtension() {
        return extension;
    }

    public void setExtension(String extension) {
        this.extension = extension;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public FileType getFileType() {
        return fileType;
    }

    public void setFileType(FileType fileType) {
        this.fileType = fileType;
    }

    public String getUncompdFileFullPath() {
        return uncompdFileFullPath;
    }

    public void setUncompdFileFullPath(String uncompdFileFullPath) {
        this.uncompdFileFullPath = uncompdFileFullPath;
    }

    public boolean isCompressed() {
        return isCompressed;
    }

    public void setCompressed(boolean compressed) {
        isCompressed = compressed;
    }

    @Override
    public String toString() {
        return "MediaFile{" +
                "md5='" + md5 + '\'' +
                ", file=" + file +
                ", extension='" + extension + '\'' +
                ", size=" + size +
                ", fileType=" + fileType +
                ", uncompdFileFullPath='" + uncompdFileFullPath + '\'' +
                ", isCompressed=" + isCompressed +
                '}';
    }
}
