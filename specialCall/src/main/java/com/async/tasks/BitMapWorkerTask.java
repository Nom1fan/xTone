package com.async.tasks;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.ImageView;

import com.crashlytics.android.Crashlytics;
import com.utils.BitmapUtils;
import java.lang.ref.WeakReference;

import com.files.media.MediaFile;
import com.utils.UtilityFactory;

import static com.crashlytics.android.Crashlytics.log;

/**
 * Created by mor on 20/09/2015.
 */
public class BitMapWorkerTask extends AsyncTask<Void, Void, Bitmap> {

    private final String TAG = BitMapWorkerTask.class.getSimpleName();
    private final WeakReference<ImageView> _imageComponentWeakReference;
    private MediaFile.FileType _fileType;
    private int _height;
    private int _width;
    private Context _context;
    private int _resourceId;
    private Resources _resources;
    private String _filePath;
    private boolean _makeRound = false;
    private BitmapUtils bitmapUtils = UtilityFactory.instance().getUtility(BitmapUtils.class);
    

    public BitMapWorkerTask(ImageView imageComponent) {
        // Use a WeakReference to ensure the ImageView can be garbage collected
        _imageComponentWeakReference = new WeakReference<>(imageComponent);
    }

    // Decode image in background.
    @Override
    protected Bitmap doInBackground(Void ... voids) {
        try {

            log(Log.INFO,TAG, "Decoding image in background");
            if (validateMembersForFile()) {
                log(Log.INFO,TAG, "[File Type]:"+_fileType + ", [File Path]:"+_filePath);
                switch (_fileType) {
                    case IMAGE:
                        return bitmapUtils.getImageBitmap(_filePath, _width, _height, _makeRound);
                    case VIDEO:
                        return bitmapUtils.getVideoBitmap(_filePath, _width, _height, _makeRound);
                }
            }
            else if(validateMembersForResource()) {
                log(Log.INFO,TAG, "Decoding image from resource");
                return bitmapUtils.convertResourceToBitmap(_resources, _resourceId, _width, _height, _makeRound);
            }
            else {
                Crashlytics.log(Log.ERROR,TAG, "Invalid parameter configuration. Current configuration does not fit either file nor resource.");
            }
        }
        catch (NullPointerException | OutOfMemoryError e) {
            e.printStackTrace();
        }
        finally {
            _context = null;
        }
        return null;
    }

    // Once complete, see if ImageView is still around and set bitmap.
    @Override
    protected void onPostExecute(Bitmap bitmap) {
        if (_imageComponentWeakReference != null && bitmap != null) {
            final ImageView imageComponent = _imageComponentWeakReference.get();

            if (imageComponent != null)
                imageComponent.setImageBitmap(bitmap);

        }
    }


    //region Getters & Setters
    public void set_filePath(String _filePath) {
        this._filePath = _filePath;
    }

    public void set_height(int _height) {
        this._height = _height;
    }

    public void set_width(int _width) {
        this._width = _width;
    }

    public void set_fileType(MediaFile.FileType _fileType) {
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
    //endregion

    //region Inner helper methods
    private boolean validateMembersForFile() {
        return _filePath!=null && _fileType!=null && _height!=0 && _width!=0;
    }

    private boolean validateMembersForResource() {

        return _height!=0 && _width!=0 && _context!=null;
    }
    //endregion
}
