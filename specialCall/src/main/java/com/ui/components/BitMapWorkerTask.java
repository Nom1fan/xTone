package com.ui.components;

import android.content.Context;
import android.content.res.Resources;
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

import com.utils.BitmapUtils;

import java.io.ByteArrayOutputStream;
import java.lang.ref.WeakReference;

import DataObjects.SpecialMediaType;
import FilesManager.FileManager;

/**
 * Created by mor on 20/09/2015.
 */
public class BitMapWorkerTask extends AsyncTask<Void, Void, Bitmap> {

    private final String TAG = BitMapWorkerTask.class.getSimpleName();
    private final WeakReference<ImageView> imageComponentWeakReference;
    private FileManager.FileType _fileType;
    private int _height;
    private int _width;
    private Context _context;
    private int _resourceId;
    private Resources _resources;
    private String _filePath;
    private boolean _makeRound = false;

    public BitMapWorkerTask(ImageView imageComponent) {
        // Use a WeakReference to ensure the ImageView can be garbage collected
        imageComponentWeakReference = new WeakReference<>(imageComponent);
    }

    // Decode image in background.
    @Override
    protected Bitmap doInBackground(Void ... voids) {

        try {

            Log.i(TAG, "Decoding image in background");
            if (validateMembersForFile()) {
                Log.i(TAG, "[File Type]:"+_fileType + ", [File Path]:"+_filePath);
                switch (_fileType) {
                    case IMAGE:
                                return getImageBitmap(_filePath);
                    case VIDEO:
                                return getVideoBitmap(_filePath);
                }
            }
            else if(validateMembersForResource()) {
                Log.i(TAG, "Decoding image from resource");
                return convertResourceToBitmap();
            }
            else {
                Log.e(TAG, "Invalid parameter configuration. Current configuration does not fit either file nor resource.");
            }
        }
        catch (NullPointerException | OutOfMemoryError e) {
            return null;
        }
        return null;
    }

    // Once complete, see if ImageView is still around and set bitmap.
    @Override
    protected void onPostExecute(Bitmap bitmap) {
        if (imageComponentWeakReference != null && bitmap != null) {
            final ImageView imageComponent = imageComponentWeakReference.get();

            if (imageComponent != null)
                    imageComponent.setImageBitmap(bitmap);
        }
    }


    /* Getters & Setters */

    public void set_filePath(String _filePath) {
        this._filePath = _filePath;
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

    public void set_resourceId(int _resourceId) {
        this._resourceId = _resourceId;
    }

    public void set_context(Context _context) {
        this._context = _context;
    }

    public void set_makeRound(Boolean _makeRound) {
        this._makeRound = _makeRound;
    }

    public void set_Resources(Resources resources) {
        this._resources = resources;
    }


    /* Inner helper methods */

    private Bitmap decodeSampledBitmapFromImageFile(String filePath) {

        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(filePath, options);
        options.inSampleSize = BitmapUtils.calculateInSampleSize(options, _width, _height);

        options.inJustDecodeBounds = false;
        return BitmapFactory.decodeFile(filePath, options);
    }

    private Bitmap decodeSampledBitmapFromResource(Resources res, int resId, int reqWidth, int reqHeight) {

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

    private boolean validateMembersForFile() {
        return _filePath!=null && _fileType!=null && _height!=0 && _width!=0;
    }

    private boolean validateMembersForResource() {

        return _height!=0 && _width!=0 && _context!=null;
    }

    private Bitmap transform(Bitmap bitmap, int margin) {

        int thumbnailSize = _height *9/10;
        int radius = (int) (thumbnailSize*0.8);
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

    private Bitmap getImageBitmap(String filePath) {

        Bitmap bitmap = decodeSampledBitmapFromImageFile(filePath);
        if(bitmap==null) {
            Log.w(TAG, "Failed to get optimal bitmap, attempting to decode inefficiently");
            bitmap = BitmapFactory.decodeFile(filePath);
        }

       return createScaledBitMap(bitmap);
    }

    private Bitmap getVideoBitmap(String filePath) {

        Bitmap bitmap = ThumbnailUtils.createVideoThumbnail(filePath, MediaStore.Images.Thumbnails.MINI_KIND);

        return createScaledBitMap(bitmap);
    }

    private Bitmap createScaledBitMap(Bitmap bitmap) {

        if (_makeRound)
            return transform(bitmap, 0);

        return Bitmap.createScaledBitmap(bitmap, _width, _height, false);
    }

    private Bitmap convertResourceToBitmap() {

        Bitmap bitmap = decodeSampledBitmapFromResource(_resources, _resourceId, _width, _height);
        //ByteArrayOutputStream baos = new ByteArrayOutputStream();
        //bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
        return createScaledBitMap(bitmap);
    }

}
