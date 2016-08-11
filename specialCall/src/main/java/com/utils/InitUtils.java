package com.utils;

import android.content.Context;
import android.graphics.Point;
import android.os.Build;
import android.util.Log;
import android.view.Display;
import android.view.WindowManager;

import com.data_objects.Constants;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import DataObjects.SpecialMediaType;
import Exceptions.FileInvalidFormatException;
import Exceptions.FileMissingExtensionException;
import FilesManager.FileManager;

import static com.crashlytics.android.Crashlytics.log;

/**
 * Created by Mor on 27/02/2016.
 */
public abstract class InitUtils {

    private static final String TAG = InitUtils.class.getSimpleName();

    public static void hideMediaFromGalleryScanner() {

        hideMediaFromGalleryScanner(Constants.INCOMING_FOLDER);
        hideMediaFromGalleryScanner(Constants.OUTGOING_FOLDER);
    }

    /**
     * Prevents Android's media scanner from reading media files and including them in apps like Gallery or Music.
     * @param path The path to set the media scanner to ignore
     */
    private static void hideMediaFromGalleryScanner(String path) {

        log(Log.INFO,TAG, "create file : " + path + "/" + ".nomedia");

        File new_file = new File(path + "/" + ".nomedia");
        try {
            if (new_file.createNewFile())
                log(Log.INFO,TAG, ".nomedia Created !");
            else
                log(Log.INFO,TAG, ".nomedia Already Exists !");
        } catch (IOException e) {
            e.printStackTrace();
        }

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

    }


    public static void initializeLoadingSavedMCFromDiskToSharedPrefs(Context context) {

        try {
            List<File> outgoingDirectories = getDirectories(new File(Constants.OUTGOING_FOLDER));
            List<File> incomingDirectories = getDirectories(new File(Constants.INCOMING_FOLDER));

            //populating Outgoing SharedPref with existing media files
            populateSharedPrefMedia(context, outgoingDirectories, SpecialMediaType.PROFILE_MEDIA);

            //populating Incoming SharedPref with existing media files
            populateSharedPrefMedia(context, incomingDirectories, SpecialMediaType.CALLER_MEDIA);
        }catch (Exception e)
        {
            e.printStackTrace();
        }
    }


    public static void saveAndroidVersion(Context context) {

        Constants.MY_ANDROID_VERSION(context, Build.VERSION.RELEASE);
    }

    private static void populateSharedPrefMedia(Context context,List<File> Directories, SpecialMediaType specialMediaType) {

        for (int i=0; i<Directories.size(); i++) {

            List<File> DirFiles = getSpecificFolderFiles(new File(Directories.get(i).getAbsolutePath()));

            for (int x=0; x<DirFiles.size(); x++) {
                FileManager.FileType fType = null;

                try {
                    String extension = FileManager.extractExtension(DirFiles.get(x).getAbsolutePath());
                    fType = FileManager.getFileTypeByExtension(extension);
                }
                catch(FileMissingExtensionException e)
                {
                    log(Log.INFO,TAG , "FileMissingExtensionException in initializeLoadingSavedMCFromDiskToSharedPrefs in" + Constants.OUTGOING_FOLDER);
                    e.printStackTrace();
                } catch(FileInvalidFormatException e)
                {
                    log(Log.INFO,TAG , "FileInvalidFormatException in initializeLoadingSavedMCFromDiskToSharedPrefs in" + Constants.OUTGOING_FOLDER);
                    e.printStackTrace();
                }

                switch (fType) {
                    case AUDIO:

                        if (specialMediaType == SpecialMediaType.PROFILE_MEDIA)
                            SharedPrefUtils.setString(context,SharedPrefUtils.FUNTONE_FILEPATH, DirFiles.get(x).getName().split("\\.")[0], DirFiles.get(x).getAbsolutePath());
                        else
                            SharedPrefUtils.setString(context,SharedPrefUtils.RINGTONE_FILEPATH, DirFiles.get(x).getName().split("\\.")[0], DirFiles.get(x).getAbsolutePath());

                        break;

                    case VIDEO:
                    case IMAGE:

                        if (specialMediaType == SpecialMediaType.PROFILE_MEDIA)
                            SharedPrefUtils.setString(context,SharedPrefUtils.PROFILE_MEDIA_FILEPATH, DirFiles.get(x).getName().split("\\.")[0], DirFiles.get(x).getAbsolutePath());
                        else
                            SharedPrefUtils.setString(context,SharedPrefUtils.CALLER_MEDIA_FILEPATH, DirFiles.get(x).getName().split("\\.")[0], DirFiles.get(x).getAbsolutePath());

                        break;
                }
            }
        }
    }

    private static List<File> getDirectories(File parentDir) {
        ArrayList<File> inFiles = new ArrayList<File>();
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
}
