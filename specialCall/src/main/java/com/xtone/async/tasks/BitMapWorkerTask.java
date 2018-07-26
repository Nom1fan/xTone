//package com.xtone.async.tasks;
//
//import android.content.res.Resources;
//import android.graphics.Bitmap;
//import android.os.AsyncTask;
//import android.util.Log;
//import android.widget.ImageView;
//
//import com.xtone.logging.Logger;
//import com.xtone.logging.LoggerFactory;
//import com.xtone.utils.BitmapUtils;
//
//import java.lang.ref.WeakReference;
//
//import com.xtone.utils.UtilsFactory;
//
//import static com.crashlytics.android.Crashlytics.log;
//
///**
// * Created by mor on 20/09/2015.
// */
//public class BitMapWorkerTask extends AsyncTask<Void, Void, Bitmap> {
//
//    private static final Logger log = LoggerFactory.getLogger();
//
//    private final String TAG = BitMapWorkerTask.class.getSimpleName();
//    private final WeakReference<ImageView> _imageComponentWeakReference;
//    private int _height;
//    private int _width;
//    private int _resourceId;
//    private Resources _resources;
//    private String _filePath;
//    private boolean _makeRound = false;
//    private BitmapUtils bitmapUtils = UtilsFactory.instance().getUtility(BitmapUtils.class);
//
//
//    public BitMapWorkerTask(ImageView imageComponent) {
//        // Use a WeakReference to ensure the ImageView can be garbage collected
//        _imageComponentWeakReference = new WeakReference<>(imageComponent);
//    }
//
//    // Decode image in background.
//    @Override
//    protected Bitmap doInBackground(Void... voids) {
//        try {
//
//            log(Log.INFO, TAG, "Decoding image in background");
//            if (validateMembersForFile()) {
//                log.info(TAG, "[File Type]:" + _fileType + ", [File Path]:" + _filePath);
//                switch (_fileType) {
//                    case IMAGE:
//                        return bitmapUtils.getImageBitmap(_filePath, _width, _height, _makeRound);
//                    case VIDEO:
//                        return bitmapUtils.getVideoBitmap(_filePath, _width, _height, _makeRound);
//                }
//            } else if (validateMembersForResource()) {
//                log.info(TAG, "Decoding image from resource");
//                return bitmapUtils.convertResourceToBitmap(_resources, _resourceId, _width, _height, _makeRound);
//            } else {
//                log.error(TAG, "Invalid parameter configuration. Current configuration does not fit either file nor resource.");
//            }
//        } catch (NullPointerException | OutOfMemoryError e) {
//            e.printStackTrace();
//        }
//        return null;
//    }
//
//    // Once complete, see if ImageView is still around and set bitmap.
//    @Override
//    protected void onPostExecute(Bitmap bitmap) {
//        if (bitmap != null) {
//            final ImageView imageComponent = _imageComponentWeakReference.get();
//
//            if (imageComponent != null)
//                imageComponent.setImageBitmap(bitmap);
//
//        }
//    }
//
//
//    //region Getters & Setters
//    public void set_filePath(String _filePath) {
//        this._filePath = _filePath;
//    }
//
//    public void set_height(int _height) {
//        this._height = _height;
//    }
//
//    public void set_width(int _width) {
//        this._width = _width;
//    }
//
//    public void set_fileType(MediaFile.FileType _fileType) {
//        this._fileType = _fileType;
//    }
//
//    public void set_resourceId(int _resourceId) {
//        this._resourceId = _resourceId;
//    }
//
//
//    public void set_makeRound(Boolean _makeRound) {
//        this._makeRound = _makeRound;
//    }
//
//    public void set_Resources(Resources resources) {
//        this._resources = resources;
//    }
//    //endregion
//
//    //region Inner helper methods
//    private boolean validateMembersForFile() {
//        return _filePath != null && _fileType != null && _height != 0 && _width != 0;
//    }
//
//    private boolean validateMembersForResource() {
//
//        return _height != 0 && _width != 0;
//    }
//    //endregion
//}
