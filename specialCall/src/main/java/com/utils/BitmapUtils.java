package com.utils;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.media.ThumbnailUtils;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.ImageView;

import com.async_tasks.BitMapWorkerTask;
import com.crashlytics.android.Crashlytics;
import com.data_objects.Constants;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import FilesManager.FileManager;

import static com.crashlytics.android.Crashlytics.log;

/**
 * Created by Mor on 07/01/2016.
 */
public abstract class BitmapUtils {

    private static final String TAG = BitmapUtils.class.getSimpleName();

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

    public static Bitmap decodeSampledBitmapFromImageFile(String filePath, int width, int height) {

        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(filePath, options);
        options.inSampleSize = BitmapUtils.calculateInSampleSize(options, width, height);

        options.inJustDecodeBounds = false;
        return BitmapFactory.decodeFile(filePath, options);
    }

    public static Bitmap decodeSampledBitmapFromImageFile(String filePath) {

        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(filePath, options);
        int width = options.outWidth;
        int height = options.outHeight;
        options.inSampleSize = BitmapUtils.calculateInSampleSize(options, width, height);

        options.inJustDecodeBounds = false;
        return BitmapFactory.decodeFile(filePath, options);
    }

    public static Bitmap decodeSampledBitmapFromResource(Resources res, int resId, int reqWidth, int reqHeight) {

        // First decode with inJustDecodeBounds=true to check dimensions
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeResource(res, resId, options);

        // Calculate inSampleSize
        options.inSampleSize = BitmapUtils.calculateInSampleSize(options, reqWidth, reqHeight);

        // Decode bitmap with inSampleSize set
        options.inJustDecodeBounds = false;
        return BitmapFactory.decodeResource(res, resId, options);
    }

    public static Bitmap transform(Bitmap bitmap, int radius, int margin) {

        final Paint paint = new Paint();
        paint.setAntiAlias(true);
        paint.setShader(new BitmapShader(bitmap, Shader.TileMode.MIRROR,
                Shader.TileMode.MIRROR));

        Bitmap output = Bitmap.createBitmap(bitmap.getWidth(),
                bitmap.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(output);
        canvas.drawRoundRect(new RectF(margin, margin, bitmap.getWidth()
                - margin, bitmap.getHeight() - margin), radius, radius, paint);

        if (bitmap != output) {
            bitmap.recycle();
        }
        return output;
    }

    public static Bitmap getImageBitmap(String filePath, int width, int height, boolean makeRound) {

        Bitmap bitmap = decodeSampledBitmapFromImageFile(filePath, width, height);
        if (bitmap == null) {
            Crashlytics.log(Log.WARN, TAG, "Failed to get optimal bitmap, attempting to decode inefficiently");
            bitmap = BitmapFactory.decodeFile(filePath);
        }

        return createScaledBitMap(bitmap, width, height, makeRound);
    }

    public static Bitmap getVideoBitmap(String filePath, int width, int height, boolean makeRound) {

        Bitmap bitmap = ThumbnailUtils.createVideoThumbnail(filePath, MediaStore.Images.Thumbnails.MINI_KIND);

        return createScaledBitMap(bitmap, width, height, makeRound);
    }

    public static Bitmap createScaledBitMap(Bitmap bitmap, int width, int height, boolean makeRound) {

        if (makeRound) {
            log(Log.INFO, TAG, "Creating round bitmap");
            int thumbnailSize = height * 9 / 10;
            int radius = (int) (thumbnailSize * 0.8);
            Bitmap bitmapForRounding = Bitmap.createScaledBitmap(bitmap, thumbnailSize, thumbnailSize, false);
            return transform(bitmapForRounding, radius, 0);
        }

        return Bitmap.createScaledBitmap(bitmap, width, height, false);
    }

    public static Bitmap convertResourceToBitmap(Resources resources, int resourceId, int width, int height, boolean makeRound) {

        Bitmap bitmap = decodeSampledBitmapFromResource(resources, resourceId, width, height);

        return createScaledBitMap(bitmap, width, height, makeRound);
    }

    public static boolean isRotationNeeded(Context ctx, FileManager.FileType fileType) {
        return fileType.equals(FileManager.FileType.IMAGE) &&
                SharedPrefUtils.getInt(ctx, SharedPrefUtils.GENERAL, SharedPrefUtils.IMAGE_ROTATION_DEGREE) != 0;
    }

    public static FileManager rotateImage(Context ctx, FileManager fm) {
        if (!fm.getFileType().equals(FileManager.FileType.IMAGE))
            return fm;
        int degrees = SharedPrefUtils.getInt(ctx, SharedPrefUtils.GENERAL, SharedPrefUtils.IMAGE_ROTATION_DEGREE);
        if (degrees == 0)
            return fm;

        String imagePath = fm.getFileFullPath();
        Bitmap bmp = decodeSampledBitmapFromImageFile(imagePath);

        Matrix matrix = new Matrix();
        matrix.postRotate(degrees);
        bmp = Bitmap.createBitmap(bmp, 0, 0, bmp.getWidth(), bmp.getHeight(), matrix, true);

        FileOutputStream fos = null;

        try {

            File rotatedFile = new File(Constants.HISTORY_FOLDER + fm.getMd5() + "." + fm.getFileExtension());
            fos = new FileOutputStream(rotatedFile);
            bmp.compress(Bitmap.CompressFormat.PNG, 100, fos); // PNG is a lossless format, the compression factor (100) is ignored
            return new FileManager(rotatedFile);

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return fm;
    }
}
