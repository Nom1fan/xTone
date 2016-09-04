//package com.utils;
//
//import android.app.Activity;
//import android.content.Intent;
//import android.graphics.Color;
//import android.widget.Toast;
//
//import com.data_objects.ActivityRequestCodes;
//import com.mediacallz.app.R;
//import com.ui.activities.PreviewMediaActivity;
//
//import Exceptions.FileDoesNotExistException;
//import Exceptions.FileExceedsMaxSizeException;
//import Exceptions.FileInvalidFormatException;
//import Exceptions.FileMissingExtensionException;
//import FilesManager.FileManager;
//
///**
// * Created by Mor on 03/09/2016.
// */
//public abstract class ActivityUtils {
//
//    public static void startPreviewActivity(Activity activity, String filepath) {
//
//        FileManager managedFile;
//        try {
//            managedFile = new FileManager(filepath);
//
//            Intent previewIntentActivity = new Intent(activity, activity.getClass());
//            previewIntentActivity.putExtra(PreviewMediaActivity.MANAGED_MEDIA_FILE, managedFile);
//            previewIntentActivity.putExtra(SPECIAL_MEDIA_TYPE, SMTypeCode);
//            startActivityForResult(previewIntentActivity, ActivityRequestCodes.PREVIEW_MEDIA);
//
//        } catch (FileExceedsMaxSizeException e) {
//            e.printStackTrace();
//            String errMsg = String.format(getResources().getString(R.string.file_over_max_size),
//                    FileManager.getFileSizeFormat(FileManager.MAX_FILE_SIZE));
//
//            UI_Utils.callToast(errMsg, Color.RED, Toast.LENGTH_LONG, getApplicationContext());
//            finish();
//
//        } catch (FileMissingExtensionException | FileDoesNotExistException | FileInvalidFormatException e) {
//            e.printStackTrace();
//            showInvalidFileOrPathToast();
//            finish();
//        }
//    }
//}
