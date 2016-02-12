package com.utils;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.BitmapFactory;
import android.widget.ImageButton;
import android.widget.ImageView;

import com.special.app.R;
import com.ui.components.BitMapWorkerTask;

import DataObjects.SpecialMediaType;
import FilesManager.FileManager;

/**
 * Created by Mor on 07/01/2016.
 */
public abstract class BitmapUtils {

    public static int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        // Raw height and width of image
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {

            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than the requested height and width.
            while ((halfHeight / inSampleSize) > reqHeight
                    && (halfWidth / inSampleSize) > reqWidth) {
                inSampleSize *= 2;
            }
        }

        return inSampleSize;
    }

    public static void execBitmapWorkerTask(ImageView imageView, Context context, Resources resources, int resource, boolean makeRound) {

        BitMapWorkerTask task = new BitMapWorkerTask(imageView);
        task.set_context(context);
        task.set_height(imageView.getHeight());
        task.set_width(imageView.getWidth());
        task.set_makeRound(makeRound);
        task.set_Resources(resources);
        task.set_resourceId(resource);
        task.execute();
    }

    public static void execBitMapWorkerTask(ImageView imageView, FileManager.FileType fType, String filePath, boolean makeRound) {

        BitMapWorkerTask task = new BitMapWorkerTask(imageView);
        task.set_width(imageView.getWidth());
        task.set_height(imageView.getHeight());
        task.set_fileType(fType);
        task.set_makeRound(makeRound);
        task.set_filePath(filePath);
        task.execute();
    }
}
