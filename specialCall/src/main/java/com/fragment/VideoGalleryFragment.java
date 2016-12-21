package com.fragment;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.ProgressBar;

import com.async.tasks.PopulateUrlsAsyncTask;
import com.data.objects.Constants;
import com.mediacallz.app.R;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.assist.FailReason;
import com.nostra13.universalimageloader.core.listener.ImageLoadingProgressListener;
import com.nostra13.universalimageloader.core.listener.SimpleImageLoadingListener;
import com.validate.media.ValidateImageFormatBehavior;

import java.util.List;

/**
 * Created by maitray on 9/8/16.
 */
public class VideoGalleryFragment extends AbsListViewBaseFragment implements PopulateUrlsAsyncTask.PostPopulateListener {
    public static final int INDEX = 6;
//    public Bitmap[] bitmapslist;
//    public ProgressDialog progressDialog;
    private List<String> videoThumbsUrl;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fr_image_grid, container, false);
        listView = (GridView) rootView.findViewById(R.id.grid);

        new PopulateUrlsAsyncTask(new ValidateImageFormatBehavior(), Constants.VIDEO_THUMBS_URL, this).execute();

        return rootView;
    }

    @Override
    public void constructPostPopulate(List<String> urls) {
        videoThumbsUrl = urls;
        listView.setAdapter(new ImageAdapter(getActivity()));
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                startVideoPagerActivity(position);
            }
        });
    }

    private class ImageAdapter extends BaseAdapter {

        private LayoutInflater inflater;

        private DisplayImageOptions options;

        ImageAdapter(Context context) {
            inflater = LayoutInflater.from(context);

            options = new DisplayImageOptions.Builder()
                    .showImageOnLoading(R.drawable.ic_stub)
                    .showImageForEmptyUri(R.drawable.ic_empty)
                    .showImageOnFail(R.drawable.ic_error)
                    .cacheInMemory(true)
                    .cacheOnDisk(true)
                    .considerExifParams(true)
                    .bitmapConfig(Bitmap.Config.RGB_565)
                    .build();
        }

        @Override
        public int getCount() {
            return videoThumbsUrl.size();
        }

        @Override
        public Object getItem(int position) {
            return null;
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            final ViewHolder holder;
            View view = convertView;
            if (view == null) {
                view = inflater.inflate(R.layout.item_grid_image, parent, false);
                holder = new ViewHolder();
                assert view != null;
                holder.imageView = (ImageView) view.findViewById(R.id.image);
                holder.progressBar = (ProgressBar) view.findViewById(R.id.progress);
                view.setTag(holder);
            } else {
                holder = (ViewHolder) view.getTag();
            }

            ImageLoader.getInstance()
                    .displayImage(videoThumbsUrl.get(position), holder.imageView, options, new SimpleImageLoadingListener() {
                        @Override
                        public void onLoadingStarted(String imageUri, View view) {
                            holder.progressBar.setProgress(0);
                            holder.progressBar.setVisibility(View.VISIBLE);
                        }

                        @Override
                        public void onLoadingFailed(String imageUri, View view, FailReason failReason) {
                            holder.progressBar.setVisibility(View.GONE);
                        }

                        @Override
                        public void onLoadingComplete(String imageUri, View view, Bitmap loadedImage) {
                            holder.progressBar.setVisibility(View.GONE);
                        }
                    }, new ImageLoadingProgressListener() {
                        @Override
                        public void onProgressUpdate(String imageUri, View view, int current, int total) {
                            holder.progressBar.setProgress(Math.round(100.0f * current / total));
                        }
                    });

            return view;
        }
    }

    static class ViewHolder {
        ImageView imageView;
        ProgressBar progressBar;
    }

//    class GenerateVideoThumbnailsFromUrlsAsyncTask extends AsyncTask<String, Void, ArrayList<Bitmap>> {
//        @Override
//        protected void onPreExecute(){
//            super.onPreExecute();
//            progressDialog = new ProgressDialog(getContext());
//            progressDialog.setMessage("Getting First Frame Of Videos...");
//            progressDialog.show();
//        }
//
//        @Override
//        protected ArrayList<Bitmap> doInBackground(String... params) {
//            ArrayList<Bitmap> bitmaps = new ArrayList<>();
//            for(String videoThumbsUrl : params){
//                Bitmap bitmap = null;
//                MediaMetadataRetriever mediaMetadataRetriever = null;
//                try
//                {
//                    mediaMetadataRetriever = new MediaMetadataRetriever();
//                    if (Build.VERSION.SDK_INT >= 14)
//                        mediaMetadataRetriever.setDataSource(videoThumbsUrl, new HashMap<String, String>());
//                    else
//                        mediaMetadataRetriever.setDataSource(videoThumbsUrl);
//                    //   mediaMetadataRetriever.setDataSource(videoPath);
//                    bitmap = mediaMetadataRetriever.getFrameAtTime();
//                }
//                catch (Exception e)
//                {
//                    e.printStackTrace();
//                }
//                finally
//                {
//                    if (mediaMetadataRetriever != null)
//                    {
//                        mediaMetadataRetriever.release();
//                    }
//                }
//                bitmaps.add(bitmap);
//            }
//            return bitmaps;
//        }
//
//        @Override
//        protected void onPostExecute(ArrayList<Bitmap> result) {
//
//            bitmapslist=result.toArray(new Bitmap[0]);
//
//            listView.setAdapter(new ViewAdapter(getActivity()));
//            listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
//                @Override
//                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
//                    Intent intent = new Intent(getActivity(), GalleryLauncherFragmentActivity.class);
//                    intent.putExtra(Constants.Extra.FRAGMENT_INDEX, VideoPagerFragment.INDEX);
//                    intent.putExtra(Constants.Extra.MEDIA_POSITION, position);
//                    startActivity(intent);
//                }
//            });
//            progressDialog.dismiss();
//
//        }
//    }
}
