package com.fragment;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.MediaController;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.async.tasks.PopulateMultipleUrlsListsAsyncTask;
import com.data.objects.Constants;
import com.mediacallz.app.R;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.display.CircleBitmapDisplayer;
import com.utils.MediaFilesUtilsImpl;
import com.validate.media.ValidateAudioFormatBehavior;
import com.validate.media.ValidateImageFormatBehavior;
import com.validate.media.ValidateMediaFormatBehavior;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import static com.fragment.AbsListViewBaseFragment.MEDIA_POSITION;

/**
 * Created by maitray on 9/8/16.
 */
public class ImageMusicPagerFragment extends BaseFragment implements PopulateMultipleUrlsListsAsyncTask.PostMultiPopulateListener {

    private static final String TAG = ImageMusicPagerFragment.class.getSimpleName();
    public static final int INDEX = 14;

    private static Integer currentViewPos;
    private ViewPager pager;
    private CustomVideoView videoView = null;
    private CustomVideoView[] videoViews;
    private MediaController mediaController;
    private ImageView mImageViewForMediaController;
    private List<String> fileNames;
    private List<String> audioThumbsUrls;
    private List<String> audioUrls;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.i(TAG, "onCreateView");
        View rootView = inflater.inflate(R.layout.fr_image_pager, container, false);

        prepareMediaController();

        pager = (ViewPager) rootView.findViewById(R.id.pager);
        pager.setVisibility(View.GONE);

        populateUrlsToScan();

        return rootView;
    }

    @Override
    public void constructPostPopulate(List<List<String>> urlsList) {
        if(urlsList == null || urlsList.isEmpty()) {
            showErrFailedToPopulate();
        } else {
            audioThumbsUrls = urlsList.get(0);
            audioUrls = urlsList.get(1);

            if(audioThumbsUrls == null || audioThumbsUrls.isEmpty() ||
                    audioUrls == null || audioUrls.isEmpty()) {
                showErrFailedToPopulate();
            }
            else {
                videoViews = new CustomVideoView[audioUrls.size()];

                preparePager();

                prepareFileNames();
            }
        }
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        if (isVisibleToUser) {
            videoView.start();
        } else {
            videoView.stopPlayback();
        }
    }

    private void playAudioInPage(int position) {
        if (currentViewPos != position) {

            showProgressDialog();

            if(videoViews[currentViewPos]!=null)
                videoViews[currentViewPos].pause();

            videoViews[position].setVideoURI(Uri.parse(audioUrls.get(position)));
            videoViews[position].setMediaController(mediaController);
            videoViews[position].setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                public void onPrepared(MediaPlayer mp) {
                    dismissProgressDialog();
                    mediaController.show();
                }
            });

            videoViews[position].start();

            currentViewPos = position;
        }
    }

    private void prepareFileNames() {
        fileNames = new ArrayList<>();
        for (String thumbUrl : audioThumbsUrls) {
            String fileName = MediaFilesUtilsImpl.getFileNameWithoutExtensionByUrl(thumbUrl);
            fileNames.add(fileName);
        }
    }

    private void populateUrlsToScan() {
        List<ValidateMediaFormatBehavior> validateMediaFormatBehaviors = new LinkedList<>();
        validateMediaFormatBehaviors.add(new ValidateImageFormatBehavior());
        validateMediaFormatBehaviors.add(new ValidateAudioFormatBehavior());

        List<String> urlsToScan = new LinkedList<String>() {{
            add(Constants.AUDIO_THUMBS_URL);
            add(Constants.AUDIO_LIB_URL);
        }};
        new PopulateMultipleUrlsListsAsyncTask(validateMediaFormatBehaviors, urlsToScan, this).execute();
    }

    private void prepareMediaController() {
        mediaController = new MediaController(getActivity()) {

          /*  @Override
            public void hide() {

                try {
                    mediaController.show();
                }catch (Exception e){
                    e.printStackTrace();
                }
            }*/

            public boolean dispatchKeyEvent(KeyEvent event) {
                if (event.getKeyCode() == KeyEvent.KEYCODE_BACK) {
                    currentViewPos=null;
                    mediaController.hide();
                    ((Activity) getContext()).finish();
                }
                return super.dispatchKeyEvent(event);
            }
        };
        mediaController.setAnchorView(videoView);
    }

    private void preparePager() {
        pager.setAdapter(new ViewAdapter(getActivity()));
        pager.setVisibility(View.VISIBLE);
        pager.setCurrentItem(getArguments().getInt(MEDIA_POSITION, 0));
        pager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                playAudioInPage(position);
            }

            @Override
            public void onPageSelected(int position) {
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });
    }

    private void playAudioFirstTime(int position) {
            showProgressDialog();
            videoViews[position].setVideoURI(Uri.parse(audioUrls.get(position)));
            videoViews[position].start();
    }

    private class ViewAdapter extends PagerAdapter {

        private LayoutInflater inflater;

        private DisplayImageOptions options;

        ViewAdapter(Context context) {
            inflater = LayoutInflater.from(context);

            options = new DisplayImageOptions.Builder()
                    .showImageOnLoading(R.drawable.ic_stub)
                    .showImageForEmptyUri(R.drawable.ic_empty)
                    .showImageOnFail(R.drawable.ic_error)
                    //.resetViewBeforeLoading(true)
                    .cacheInMemory(true)
                    .cacheOnDisk(true)
                    .considerExifParams(true)
                    .displayer(new CircleBitmapDisplayer(Color.WHITE, 5))
                    .build();
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            container.removeView((View) object);
        }

        @Override
        public int getCount() {
            return audioUrls.size();
        }

        @Override
        public Object instantiateItem(ViewGroup view, final int position) {
            RelativeLayout audioPageLayout = (RelativeLayout) inflater.inflate(R.layout.item_audio_pager_new, view, false);
            assert audioPageLayout != null;

            videoView = (CustomVideoView) audioPageLayout.findViewById(R.id.videoView);
            videoView.setMediaController(mediaController);
           // videoView.setVideoURI(Uri.parse(audioUrls.get(position)));

            videoView.requestFocus();
            videoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                public void onPrepared(MediaPlayer mp) {
                    dismissProgressDialog();
                    mediaController.show();
                }
            });

            videoViews[position] = videoView;
            if (currentViewPos == null) {
                currentViewPos = position;
                playAudioFirstTime(position);
            }

            audioPageLayout.bringChildToFront(videoView);

            Button downloadButton = (Button) audioPageLayout.findViewById(R.id.downloadButton);
            downloadButton.setOnClickListener(new View.OnClickListener() {
                public void onClick(View arg0) {
                    videoViews[currentViewPos].pause();
                    downloadFile(audioUrls.get(position));
                }
            });


            mImageViewForMediaController = (ImageView) audioPageLayout.findViewById(R.id.imageView_for_mediacontroller);
            mImageViewForMediaController.setOnClickListener(new View.OnClickListener() {
                public void onClick(View arg0) {

                    if (videoViews[currentViewPos] != null) {
                        mediaController.setMediaPlayer(videoViews[currentViewPos]);
                        mediaController.show();
                    }
                }
            });

            TextView textview = (TextView) audioPageLayout.findViewById(R.id.textview);
            textview.setText(fileNames.get(position));

            ImageView audioImage = (ImageView) audioPageLayout.findViewById(R.id.audioimage);
            ImageLoader.getInstance().displayImage(audioThumbsUrls.get(position), audioImage, options);
            view.addView(audioPageLayout, 0);
            return audioPageLayout;
        }

        @Override
        public boolean isViewFromObject(View view, Object object) {
            return view.equals(object);
        }

        @Override
        public void restoreState(Parcelable state, ClassLoader loader) {
        }

        @Override
        public Parcelable saveState() {
            return null;
        }

    }
}
