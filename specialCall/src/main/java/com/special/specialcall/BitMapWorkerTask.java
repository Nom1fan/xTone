package com.special.specialcall;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.ThumbnailUtils;
import android.os.AsyncTask;
import android.provider.MediaStore;
import android.widget.ImageButton;
import android.widget.ImageView;

import java.lang.ref.WeakReference;

import FilesManager.FileManager;

/**
 * Created by mor on 20/09/2015.
 */
class BitmapWorkerTask extends AsyncTask<String, Void, Bitmap> {
    private final WeakReference<ImageButton> imageButtonWeakReference;
    private String _filePath;
    private FileManager.FileType _fileType;
    private int _height;
    private int _width;

    public BitmapWorkerTask(ImageButton imageButton) {
        // Use a WeakReference to ensure the ImageView can be garbage collected
        imageButtonWeakReference = new WeakReference<>(imageButton);
    }

    // Decode image in background.
    @Override
    protected Bitmap doInBackground(String ... params) {
        _filePath = params[0];

        if(membersOK()) {
            Bitmap tmp_bitmap;
            switch (_fileType) {

                case IMAGE:
                    final BitmapFactory.Options options = new BitmapFactory.Options();
                    options.inJustDecodeBounds = true;
                    BitmapFactory.decodeFile(_filePath, options);

                    // Calculate inSampleSize
                    options.inSampleSize = calculateInSampleSize(options, _width, _height);
                    if(options.inSampleSize > 1)
                        tmp_bitmap = BitmapFactory.decodeFile(_filePath, options);
                    else
                        tmp_bitmap = BitmapFactory.decodeFile(_filePath);
                    return Bitmap.createScaledBitmap(tmp_bitmap, _width, _height, false);

                case VIDEO:
                    tmp_bitmap = ThumbnailUtils.createVideoThumbnail(_filePath, MediaStore.Images.Thumbnails.MINI_KIND);
                    return Bitmap.createScaledBitmap(tmp_bitmap, _width, _height, false);
            }
        }

            return null;
    }

    private boolean membersOK() {
        return _filePath!=null && _fileType!=null && _height!=0 && _width!=0;
    }

    // Once complete, see if ImageView is still around and set bitmap.
    @Override
    protected void onPostExecute(Bitmap bitmap) {
        if (imageButtonWeakReference != null && bitmap != null) {
            final ImageButton imageButton = imageButtonWeakReference.get();
            if (imageButton != null) {
                imageButton.setImageBitmap(bitmap);
            }
        }
    }

    public int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
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

    public void set_height(int _height) {
        this._height = _height;
    }

    public void set_width(int _width) {
        this._width = _width;
    }

    public void set_fileType(FileManager.FileType _fileType) {
        this._fileType = _fileType;
    }
}
