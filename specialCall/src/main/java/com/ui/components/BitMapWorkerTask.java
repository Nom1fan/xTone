package com.ui.components;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.media.ThumbnailUtils;
import android.os.AsyncTask;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.ImageView;
import java.io.ByteArrayOutputStream;
import java.lang.ref.WeakReference;

import DataObjects.SpecialMediaType;
import FilesManager.FileManager;

/**
 * Created by mor on 20/09/2015.
 */
public class BitMapWorkerTask extends AsyncTask<String, Void, Bitmap> {
    private final String TAG = BitMapWorkerTask.class.getSimpleName();
    private final WeakReference<ImageView> imageComponentWeakReference;
    private String _filePath;
    private FileManager.FileType _fileType;
    private int _height;
    private int _width;
    private SpecialMediaType _specialMediaType;

    public BitMapWorkerTask(ImageView imageComponent) {
        // Use a WeakReference to ensure the ImageView can be garbage collected
        imageComponentWeakReference = new WeakReference<>(imageComponent);
    }

    // Decode image in background.
    @Override
    protected Bitmap doInBackground(String ... params) {

        try {

            _filePath = params[0];

            if (membersOK()) {
                Bitmap tmp_bitmap;
                switch (_fileType) {

                    case IMAGE:
                        final BitmapFactory.Options options = new BitmapFactory.Options();
                        options.inJustDecodeBounds = true;
                        BitmapFactory.decodeFile(_filePath, options);
                        options.inSampleSize = calculateInSampleSize(options, _width, _height);

                        options.inJustDecodeBounds = false;
                        tmp_bitmap = BitmapFactory.decodeFile(_filePath, options);
                        if(tmp_bitmap!=null)
                            return Bitmap.createScaledBitmap(tmp_bitmap, _width, _height, false);
                        else
                            tmp_bitmap = BitmapFactory.decodeFile(_filePath);
                        return Bitmap.createScaledBitmap(tmp_bitmap, _width, _height, false);

                    case VIDEO:
                        tmp_bitmap = ThumbnailUtils.createVideoThumbnail(_filePath, MediaStore.Images.Thumbnails.MINI_KIND);
                        return Bitmap.createScaledBitmap(tmp_bitmap, _width, _height, false);
                }
            }
        }
        catch (NullPointerException | OutOfMemoryError e) {
            return null;
        }
        return null;
    }

    private boolean membersOK() {
        return _filePath!=null && _fileType!=null && _height!=0 && _width!=0;
    }

    // Once complete, see if ImageView is still around and set bitmap.
    @Override
    protected void onPostExecute(Bitmap bitmap) {
        if (imageComponentWeakReference != null && bitmap != null) {
            final ImageView imageComponent = imageComponentWeakReference.get();

            if (imageComponent != null) {

                switch (_specialMediaType){

                    case PROFILE_MEDIA:

                        int width = imageComponent.getWidth();
                        if ( width < 1)
                            width = 900; // default

                        int thumbnailSize = width*6/10;
                        int radius = (int) (thumbnailSize*0.8);
                        Log.i(TAG, "thumbnailSize: " + thumbnailSize + " width: " + width + " radius: " + radius);

                        bitmap = Bitmap.createScaledBitmap(bitmap, thumbnailSize, thumbnailSize, false); // TODO: code review on the relative sizes that we deliver for the circular profile media draw, to see calculation are good
                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
                        imageComponent.setImageBitmap(transform(bitmap, radius, 0));
                        break;

                    case CALLER_MEDIA:
                        imageComponent.setImageBitmap(bitmap);
                        break;
                    default:
                        Log.e(TAG, "Invalid SpecialCallMedia");
                        imageComponent.setImageBitmap(bitmap);
                }
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

    public void set_specialMediaType(SpecialMediaType _specialMediaType) {
        this._specialMediaType = _specialMediaType;
    }

    public Bitmap transform(Bitmap source, int radius, int margin) {
        final Paint paint = new Paint();
        paint.setAntiAlias(true);
        paint.setShader(new BitmapShader(source, Shader.TileMode.MIRROR,
                Shader.TileMode.MIRROR));

        Bitmap output = Bitmap.createBitmap(source.getWidth(),
                source.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(output);
        canvas.drawRoundRect(new RectF(margin, margin, source.getWidth()
                - margin, source.getHeight() - margin), radius, radius, paint);

        if (source != output) {
            source.recycle();
        }
        return output;
    }


}
