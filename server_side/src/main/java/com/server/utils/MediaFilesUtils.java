package com.server.utils;

import com.server.data.MediaFile;

import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.List;

import static com.server.data.MediaFile.*;


/**
 * Created by Mor on 01/07/2016.
 */
public abstract class MediaFilesUtils {

    private static final String TAG = MediaFilesUtils.class.getSimpleName();

    private static final String[] imageFormats = {"jpg", "png", "jpeg", "bmp", "gif", "webp"};
    private static final List<String> imageFormatsList = Arrays.asList(imageFormats);
    private static final String[] audioFormats = {"mp3", "ogg", "flac", "mid", "xmf", "mxmf", "rtx", "ota", "imy", "wav", "m4a", "aac"};
    private static final List<String> audioFormatsList = Arrays.asList(audioFormats);
    private static final String[] videoFormats = {"avi", "mpeg", "mp4", "3gp", "wmv", "webm", "mkv"};
    private static final List<String> videoFormatsList = Arrays.asList(videoFormats);

    public static boolean isValidImageFormat(String pathOrUrl) {
        String extension = pathOrUrl.substring(pathOrUrl.lastIndexOf(".") + 1);
        return imageFormatsList.contains(extension.toLowerCase());
    }

    public static boolean isValidAudioFormat(String pathOrUrl) {
        String extension = pathOrUrl.substring(pathOrUrl.lastIndexOf(".") + 1);
        return audioFormatsList.contains(extension.toLowerCase());
    }

    public static boolean isValidVideoFormat(String pathOrUrl) {
        String extension = pathOrUrl.substring(pathOrUrl.lastIndexOf(".") + 1);
        return videoFormatsList.contains(extension.toLowerCase());
    }

    public static MediaFile createMediaFile(File file) {
        MediaFile result = null;
        try {
            result = new MediaFile(file);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    public static String getFileNameByUrl(String url) {
        return FilenameUtils.getName(url).replaceAll("%20", " ");
    }

    public static String getFileNameWithoutExtensionByUrl(String url) {
        return FilenameUtils.getBaseName(url).replaceAll("%20", " ");
    }

    public static String getFileSizeFormat(double _fileSize) {

        double MB = (int) Math.pow(2, 20);
        double KB = (int) Math.pow(2, 10);
        DecimalFormat df = new DecimalFormat("#.00"); // rounding to max 2 decimal places

        if (_fileSize >= MB) {
            double fileSizeInMB = _fileSize / MB; // File size in MBs
            return df.format(fileSizeInMB) + "MB";
        } else if (_fileSize >= KB) {
            double fileSizeInKB = _fileSize / KB; // File size in KBs
            return df.format(fileSizeInKB) + "KB";
        }

        // File size in Bytes
        return df.format(_fileSize) + "B";
    }

    public static String getMD5(String filepath) {

        try {

            InputStream input = new FileInputStream(filepath);
            byte[] buffer = new byte[1024];

            MessageDigest hashMsgDigest = null;
            hashMsgDigest = MessageDigest.getInstance("MD5");

            int read;
            do {
                read = input.read(buffer);
                if (read > 0) {
                    hashMsgDigest.update(buffer, 0, read);
                }
            } while (read != -1);
            input.close();

            StringBuffer hexString = new StringBuffer();
            byte[] hash = hashMsgDigest.digest();

            for (int i = 0; i < hash.length; i++) {
                if ((0xff & hash[i]) < 0x10) {
                    hexString.append("0"
                            + Integer.toHexString((0xFF & hash[i])));
                } else {
                    hexString.append(Integer.toHexString(0xFF & hash[i]));
                }
            }

            return hexString.toString();

        } catch (NoSuchAlgorithmException | IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static FileType getFileType(String extension) {

        if (Arrays.asList(imageFormats).contains(extension))
            return FileType.IMAGE;
        else if (Arrays.asList(audioFormats).contains(extension))
            return FileType.AUDIO;
        else if (Arrays.asList(videoFormats).contains(extension))
            return FileType.VIDEO;

        return null;
    }
}
