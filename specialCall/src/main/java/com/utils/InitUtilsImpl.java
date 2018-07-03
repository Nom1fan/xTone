package com.utils;

import android.content.Context;
import android.graphics.Point;
import android.os.Build;
import android.util.Log;
import android.view.Display;
import android.view.WindowManager;

import com.crashlytics.android.Crashlytics;
import com.data.objects.Constants;
import com.enums.SaveMediaOption;
import com.enums.SpecialMediaType;
import com.files.media.MediaFile;
import com.nostra13.universalimageloader.cache.disc.naming.Md5FileNameGenerator;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;
import com.nostra13.universalimageloader.core.assist.QueueProcessingType;
import com.receivers.SyncDefaultMediaReceiver;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static com.crashlytics.android.Crashlytics.log;
import static com.receivers.SyncDefaultMediaReceiver.SYNC_REPEAT_INTERVAL;

/**
 * Created by Mor on 27/02/2016.
 */
public class InitUtilsImpl implements InitUtils {

    private static final String TAG = InitUtilsImpl.class.getSimpleName();

    private AlarmUtils alarmUtils = UtilityFactory.instance().getUtility(AlarmUtils.class);

    private MediaFileUtils mediaFileUtils = UtilityFactory.instance().getUtility(MediaFileUtils.class);

    private Phone2MediaPathMapperUtils phone2MediaPathMapperUtils = UtilityFactory.instance().getUtility(Phone2MediaPathMapperUtils.class);

    @Override
    public void hideMediaFromGalleryScanner() {

        hideMediaFromGalleryScanner(Constants.INCOMING_FOLDER);
        hideMediaFromGalleryScanner(Constants.OUTGOING_FOLDER);
        hideMediaFromGalleryScanner(Constants.AUDIO_HISTORY_FOLDER);
    }

    @Override
    public void initializeSettingsDefaultValues(Context context) {

        SettingsUtils.setDownloadOnlyOnWifi(context, false);
        SettingsUtils.setSaveMediaOption(context, SaveMediaOption.ALWAYS);
        SettingsUtils.setStrictRingingCapabilitiesDevice(context, true);
        SettingsUtils.setAskBeforeShowingMedia(context, true);


        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);

        SharedPrefUtils.setInt(context, SharedPrefUtils.SERVICES, SharedPrefUtils.DEVICE_SCREEN_HEIGHET, size.y);
        SharedPrefUtils.setInt(context, SharedPrefUtils.SERVICES, SharedPrefUtils.DEVICE_SCREEN_WIDTH, size.x);

        Crashlytics.log(Log.INFO, TAG, "DEVICE_SCREEN_HEIGHET: " + size.y + "DEVICE_SCREEN_WIDTH: " + size.x);

    }

    @Override
    public void populateSavedMcFromDiskToSharedPrefs(Context context) {

        //TODO improve code + add default media handling
        try {
            List<File> outgoingDirectories = getDirectories(new File(Constants.OUTGOING_FOLDER));
            List<File> incomingDirectories = getDirectories(new File(Constants.INCOMING_FOLDER));
//            List<File> outgoingDefaultDirs = getDirectories(new File(Constants.DEFAULT_OUTGOING_FOLDER));
//            List<File> incomingDefaultDirs = getDirectories(new File(Constants.DEFAULT_INCOMING_FOLDER));

            //populating Outgoing SharedPref with existing media files
            populateSharedPrefMedia(context, outgoingDirectories, SpecialMediaType.PROFILE_MEDIA);

            //populating Incoming SharedPref with existing media files
            populateSharedPrefMedia(context, incomingDirectories, SpecialMediaType.CALLER_MEDIA);

//            populateSharedPrefMedia(context, outgoingDefaultDirs, SpecialMediaType.DEFAULT_PROFILE_MEDIA);
//
//            populateSharedPrefMedia(context, incomingDefaultDirs, SpecialMediaType.DEFAULT_CALLER_MEDIA);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void saveAndroidVersion(Context context) {
        Constants.MY_ANDROID_VERSION(context, Build.VERSION.RELEASE);
    }

    @Override
    public void initImageLoader(Context context) {
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

    @Override
    public void initSyncDefaultMediaReceiver(Context context) {
        alarmUtils.setAlarm(context, SyncDefaultMediaReceiver.class, SYNC_REPEAT_INTERVAL, SyncDefaultMediaReceiver.SYNC_ACTION);
    }

    private void populateSharedPrefMedia(Context context, List<File> Directories, SpecialMediaType specialMediaType) {

        for (int i = 0; i < Directories.size(); i++) {

            List<File> DirFiles = getSpecificFolderFiles(new File(Directories.get(i).getAbsolutePath()));

            for (int j = 0; j < DirFiles.size(); j++) {
                MediaFile.FileType fType;
                String extension = mediaFileUtils.extractExtension(DirFiles.get(j).getAbsolutePath());
                fType = mediaFileUtils.getFileTypeByExtension(extension);
                String phoneNumber = DirFiles.get(j).getName().split("\\.")[0];

                phoneNumber = phoneNumber.substring(phoneNumber.length() - 6); /// TODO:  ADDED THIS FOR INTERNATIONAL OPTIONS (HACKED)

                String mediaFilePath = DirFiles.get(j).getAbsolutePath();

                if (fType != null)
                    switch (fType) {
                        case AUDIO: {

                            if (specialMediaType == SpecialMediaType.PROFILE_MEDIA) {
                                phone2MediaPathMapperUtils.setProfileAudioMediaPath(context, phoneNumber, mediaFilePath);
                            } else {
                                phone2MediaPathMapperUtils.setCallerAudioMediaPath(context, phoneNumber, mediaFilePath);
                            }
                        }
                        break;

                        case VIDEO:
                        case IMAGE:

                            if (specialMediaType == SpecialMediaType.PROFILE_MEDIA) {
                                phone2MediaPathMapperUtils.setProfileVisualMediaPath(context, phoneNumber, mediaFilePath);
                            } else {
                                phone2MediaPathMapperUtils.setCallerVisualMediaPath(context, phoneNumber, mediaFilePath);
                            }
                            break;
                    }
            }
        }
    }

    private List<File> getDirectories(File parentDir) {
        ArrayList<File> inFiles = new ArrayList<>();
        File[] files = parentDir.listFiles();
        for (File file : files) {
            if (file.isDirectory())
                inFiles.add(file);
        }
        return inFiles;
    }

    private List<File> getSpecificFolderFiles(File parentDir) {
        ArrayList<File> inFiles = new ArrayList<>();
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
    private void hideMediaFromGalleryScanner(String path) {

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
