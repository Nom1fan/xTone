package com.fragment;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;

import com.felipecsl.gifimageview.library.GifImageView;
import com.mediacallz.app.R;

import java.util.List;

public class GifGridAdapter extends BaseAdapter {

  private Context context;
  private List<String> imageUrls;
    LayoutInflater inflater;
  public GifGridAdapter(Context context, List<String> imageUrls) {
    this.context = context;
    this.imageUrls = imageUrls;
  }

  @Override
  public int getCount() {
    return imageUrls.size();
  }

  @Override
  public Object getItem(int position) {
    return imageUrls.get(position);
  }

  @Override
  public long getItemId(int position) {
    return 0;
  }

  @Override
  public View getView(int position, View convertView, ViewGroup parent) {
    final GifImageView imageView;
    if (convertView == null) {
        inflater = LayoutInflater.from(context);
        convertView = inflater.inflate(R.layout.item_grid_image, parent, false);
      imageView = new GifImageView(context);
      imageView.setScaleType(ImageView.ScaleType.CENTER);
      imageView.setPadding(10, 10, 10, 10);
      int size = AbsListView.LayoutParams.WRAP_CONTENT;
      AbsListView.LayoutParams layoutParams = new GridView.LayoutParams(size, size);
      imageView.setLayoutParams(layoutParams);
    } else {
      imageView = (GifImageView) convertView;
      imageView.clear();
    }
    new GifDataDownloader() {
      @Override
      protected void onPostExecute(final byte[] bytes) {
        imageView.setBytes(bytes);
        imageView.startAnimation();
      }
    }.execute(imageUrls.get(position));
    return imageView;
  }
    public class GifDataDownloader extends AsyncTask<String, Void, byte[]> {
        private static final String TAG = "GifDataDownloader";

        @Override protected byte[] doInBackground(final String... params) {
            final String gifUrl = params[0];

            if (gifUrl == null)
                return null;

            try {
                return ByteArrayHttpClient.get(gifUrl);
            } catch (OutOfMemoryError e) {
                Log.e(TAG, "GifDecode OOM: " + gifUrl, e);
                return null;
            }
        }
    }
}
