package com.xtone.utils;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

/**
 * Created by Mor on 02/06/2017.
 */

public interface BitmapUtils extends Utility {
    int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight);

//    void execBitmapWorkerTask(ImageView imageView, Context context, Resources resources, int resource, boolean makeRound);
//
//    void execBitMapWorkerTask(ImageView imageView, MediaFile.FileType fType, String filePath, boolean makeRound);

    Bitmap decodeSampledBitmapFromImageFile(String filePath, int width, int height);

    Bitmap decodeSampledBitmapFromImageFile(String filePath);

    Bitmap decodeSampledBitmapFromResource(Resources res, int resId, int reqWidth, int reqHeight);

    Bitmap transform(Bitmap bitmap, int radius, int margin);

    Bitmap getImageBitmap(String filePath, int width, int height, boolean makeRound);

    Bitmap getVideoBitmap(String filePath, int width, int height, boolean makeRound);

    Bitmap createScaledBitMap(Bitmap bitmap, int width, int height, boolean makeRound);

    Bitmap convertResourceToBitmap(Resources resources, int resourceId, int width, int height, boolean makeRound);
}
