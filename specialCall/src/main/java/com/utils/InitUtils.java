package com.utils;

import android.content.Context;
import android.graphics.Point;
import android.os.Build;
import android.util.Log;
import android.view.Display;
import android.view.WindowManager;

import com.crashlytics.android.Crashlytics;
import com.data.objects.Constants;
import com.nostra13.universalimageloader.cache.disc.naming.Md5FileNameGenerator;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;
import com.nostra13.universalimageloader.core.assist.QueueProcessingType;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.enums.SpecialMediaType;
import com.exceptions.FileInvalidFormatException;
import com.exceptions.FileMissingExtensionException;
import com.files.media.MediaFile;

import static com.crashlytics.android.Crashlytics.log;

/**
 * Created by Mor on 27/02/2016.
 */
public abstract class InitUtils {

    private static final String TAG = InitUtils.class.getSimpleName();

    public static void hideMediaFromGalleryScanner() {

        hideMediaFromGalleryScanner(Constants.INCOMING_FOLDER);
        hideMediaFromGalleryScanner(Constants.OUTGOING_FOLDER);
        hideMediaFromGalleryScanner(Constants.AUDIO_HISTORY_FOLDER);
    }

    public static void initializeSettingsDefaultValues(Context context) {

        SharedPrefUtils.setBoolean(context, SharedPrefUtils.SETTINGS, SharedPrefUtils.DOWNLOAD_ONLY_ON_WIFI, false);
        SharedPrefUtils.setInt(context, SharedPrefUtils.GENERAL, SharedPrefUtils.SAVE_MEDIA_OPTION, 0);
        SharedPrefUtils.setBoolean(context, SharedPrefUtils.GENERAL, SharedPrefUtils.STRICT_RINGING_CAPABILITIES_DEVICES, true);

        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);

        SharedPrefUtils.setInt(context, SharedPrefUtils.SERVICES, SharedPrefUtils.DEVICE_SCREEN_HEIGHET, size.y);
        SharedPrefUtils.setInt(context, SharedPrefUtils.SERVICES, SharedPrefUtils.DEVICE_SCREEN_WIDTH, size.x);

        Crashlytics.log(Log.INFO, TAG, "DEVICE_SCREEN_HEIGHET: " + size.y + "DEVICE_SCREEN_WIDTH: " + size.x);

    }

    public static void populateSavedMcFromDiskToSharedPrefs(Context context) {

        try {
            List<File> outgoingDirectories = getDirectories(new File(Constants.OUTGOING_FOLDER));
            List<File> incomingDirectories = getDirectories(new File(Constants.INCOMING_FOLDER));

            //populating Outgoing SharedPref with existing media files
            populateSharedPrefMedia(context, outgoingDirectories, SpecialMediaType.PROFILE_MEDIA);

            //populating Incoming SharedPref with existing media files
            populateSharedPrefMedia(context, incomingDirectories, SpecialMediaType.CALLER_MEDIA);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void saveAndroidVersion(Context context) {

        Constants.MY_ANDROID_VERSION(context, Build.VERSION.RELEASE);
    }

    public static void initImageLoader(Context context) {
        // This configuration tuning is custom. You can tune every option, you may tune some of them,
        // or you can create default configuration by
        //  ImageLoaderConfiguration.createDefault(this);
        // method.
        ImageLoaderConfiguration.Builder config = new ImageLoaderConfiguration.Builder(context);
        config.threadPriority(Thread.NORM_PRIORITY - 2);
        config.denyCacheImageMultipleSizesInMemory();
        config.diskCacheFileNameGenerator(new Md5FileNameGenerator());
        config.diskCacheSize(50 * 1024 * 1024); // 50 MiB
        config.tasksProcessingOrder(QueueProcessingType.LIFO);
        config.writeDebugLogs(); // Remove for release app

        // Initialize ImageLoader with configuration.
        ImageLoader.getInstance().init(config.build());
    }

    private static void populateSharedPrefMedia(Context context, List<File> Directories, SpecialMediaType specialMediaType) {

        for (int i = 0; i < Directories.size(); i++) {

            List<File> DirFiles = getSpecificFolderFiles(new File(Directories.get(i).getAbsolutePath()));

            for (int x = 0; x < DirFiles.size(); x++) {
                MediaFile.FileType fType = null;

                try {
                    String extension = MediaFilesUtils.extractExtension(DirFiles.get(x).getAbsolutePath());
                    fType = MediaFilesUtils.getFileTypeByExtension(extension);
                } catch (FileMissingExtensionException e) {
                    log(Log.INFO, TAG, "FileMissingExtensionException in populateSavedMcFromDiskToSharedPrefs in" + specialMediaType.toString());
                    e.printStackTrace();
                } catch (FileInvalidFormatException e) {
                    log(Log.INFO, TAG, "FileInvalidFormatException in populateSavedMcFromDiskToSharedPrefs in" + specialMediaType.toString());
                    e.printStackTrace();
                } catch (Exception e) {
                    log(Log.INFO, TAG, "populateSharedPrefMedia bad file  in populateSavedMcFromDiskToSharedPrefs in" + specialMediaType.toString());
                    e.printStackTrace();
                }

                if (fType != null)
                    switch (fType) {
                        case AUDIO:

                            if (specialMediaType == SpecialMediaType.PROFILE_MEDIA) {
                                SharedPrefUtils.setString(context, SharedPrefUtils.FUNTONE_FILEPATH, DirFiles.get(x).getName().split("\\.")[0], DirFiles.get(x).getAbsolutePath());
                                log(Log.INFO, TAG, "populateSharedPrefMedia FUNTONE_FILEPATH: " + specialMediaType.toString() + " for: " + DirFiles.get(x).getName().split("\\.")[0] + " file: " + DirFiles.get(x).getAbsolutePath());
                            } else {
                                SharedPrefUtils.setString(context, SharedPrefUtils.RINGTONE_FILEPATH, DirFiles.get(x).getName().split("\\.")[0], DirFiles.get(x).getAbsolutePath());
                                log(Log.INFO, TAG, "populateSharedPrefMedia RINGTONE_FILEPATH: " + specialMediaType.toString() + " for: " + DirFiles.get(x).getName().split("\\.")[0] + " file: " + DirFiles.get(x).getAbsolutePath());
                            }
                            break;

                        case VIDEO:
                        case IMAGE:

                            if (specialMediaType == SpecialMediaType.PROFILE_MEDIA) {
                                SharedPrefUtils.setString(context, SharedPrefUtils.PROFILE_MEDIA_FILEPATH, DirFiles.get(x).getName().split("\\.")[0], DirFiles.get(x).getAbsolutePath());
                                log(Log.INFO, TAG, "populateSharedPrefMedia PROFILE_MEDIA_FILEPATH: " + specialMediaType.toString() + " for: " + DirFiles.get(x).getName().split("\\.")[0] + " file: " + DirFiles.get(x).getAbsolutePath());
                            } else {
                                SharedPrefUtils.setString(context, SharedPrefUtils.CALLER_MEDIA_FILEPATH, DirFiles.get(x).getName().split("\\.")[0], DirFiles.get(x).getAbsolutePath());
                                log(Log.INFO, TAG, "populateSharedPrefMedia CALLER_MEDIA_FILEPATH: " + specialMediaType.toString() + " for: " + DirFiles.get(x).getName().split("\\.")[0] + " file: " + DirFiles.get(x).getAbsolutePath());
                            }
                            break;
                    }
            }
        }
    }

    private static List<File> getDirectories(File parentDir) {
        ArrayList<File> inFiles = new ArrayList<>();
        File[] files = parentDir.listFiles();
        for (File file : files) {
            if (file.isDirectory())
                inFiles.add(file);
        }
        return inFiles;
    }

    private static List<File> getSpecificFolderFiles(File parentDir) {
        ArrayList<File> inFiles = new ArrayList<File>();
        File[] files = parentDir.listFiles();
        for (File file : files) {
            if (!file.isDirectory()) {
                inFiles.add(file);
            }
        }
        return inFiles;
    }

    /**
     * Prevents Android's media scanner from reading media files and including them in apps like Gallery or Music.
     *
     * @param path The path to set the media scanner to ignore
     */
    private static void hideMediaFromGalleryScanner(String path) {

        log(Log.INFO, TAG, "create file : " + path + "/" + ".nomedia");

        File new_file = new File(path + "/" + ".nomedia");
        try {
            if (new_file.createNewFile())
                log(Log.INFO, TAG, ".nomedia Created !");
            else
                log(Log.INFO, TAG, ".nomedia Already Exists !");
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
