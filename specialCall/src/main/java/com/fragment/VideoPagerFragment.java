package com.fragment;

import android.app.Activity;
import android.content.Context;
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
import android.widget.FrameLayout;
import android.widget.MediaController;

import com.async_tasks.PopulateUrlsAsyncTask;
import com.async_tasks.PopulateUrlsAsyncTask.PostPopulateListener;
import com.data_objects.Constants;
import com.mediacallz.app.R;
import com.validate.media.ValidateVideoFormatBehavior;

import java.util.List;

/**
 * Created by Mor on 9/8/16.
 */
public class VideoPagerFragment extends BaseFragment implements PostPopulateListener {

    public static final int INDEX = 9;

    private static final String TAG = VideoPagerFragment.class.getSimpleName();
    private static Integer currentViewPos = null;
    private ViewPager pager;
    private CustomVideoView videoView = null;
    private CustomVideoView[] videoViews;
    private MediaController mediacontroller;
    private List<String> videoUrls;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fr_image_pager, container, false);

        prepareMediaController();

        pager = (ViewPager) rootView.findViewById(R.id.pager);
        pager.setVisibility(View.GONE);

        new PopulateUrlsAsyncTask(new ValidateVideoFormatBehavior(), Constants.VIDEO_LIB_URL, this).execute();

        return rootView;
    }

    @Override
    public void constructPostPopulate(List<String> urls) {
        videoUrls = urls;
        videoViews = new CustomVideoView[videoUrls.size()];

        preparePager();
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

    private void preparePager() {
        pager.setAdapter(new ViewAdapter(getActivity()));
        pager.setVisibility(View.VISIBLE);
        pager.setCurrentItem(getArguments().getInt(Constants.Extra.MEDIA_POSITION));
        pager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                playVideoInPage(position);
            }

            @Override
            public void onPageSelected(int position) {
            }

            @Override
            public void onPageScrollStateChanged(int state) {
            }
        });
    }

    private void prepareMediaController() {
        mediacontroller = new MediaController(getActivity()) {

            @Override
            public void hide() {

                try {
                    mediacontroller.show();
                }catch (Exception e){
                    e.printStackTrace();
                }
            }

            public boolean dispatchKeyEvent(KeyEvent event) {
                if (event.getKeyCode() == KeyEvent.KEYCODE_BACK) {
                    currentViewPos=null;
                    ((Activity) getContext()).finish();
                }
                return super.dispatchKeyEvent(event);
            }
        };
        mediacontroller.setAnchorView(videoView);
    }

    private void playVideoInPage(int position) {
        if (currentViewPos != position) {

            showProgressDialog();

            videoViews[currentViewPos].pause();

            videoViews[position].setVideoURI(Uri.parse(videoUrls.get(position)));
            videoViews[position].setMediaController(mediacontroller);
            videoViews[position].setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                public void onPrepared(MediaPlayer mp) {
                    dismissProgressDialog();
                    mediacontroller.show();
                }
            });

            videoViews[position].start();

            currentViewPos = position;
        }
    }

    private class ViewAdapter extends PagerAdapter {
        private LayoutInflater inflater;

        ViewAdapter(Context context) {
            inflater = LayoutInflater.from(context);
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            container.removeView((View) object);
        }

        @Override
        public int getCount() {
            return videoUrls.size();
        }

        @Override
        public Object instantiateItem(ViewGroup view, final int position) {

            FrameLayout videoPageLayout = (FrameLayout) inflater.inflate(R.layout.item_video_pager, view, false);
            if(videoPageLayout ==null) {
                String errMsg = "VideoLayout returned as null after inflate()";
                Log.e(TAG, errMsg);
                throw new NullPointerException(errMsg);
            }

            videoView = (CustomVideoView) videoPageLayout.findViewById(R.id.videoView);
            videoView.setMediaController(mediacontroller);

            videoView.requestFocus();
            videoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                public void onPrepared(MediaPlayer mp) {
                 try {
                     dismissProgressDialog();
                     mediacontroller.show();
                 } catch (Exception e) {e.printStackTrace();}
                }
            });

            videoViews[position] = videoView;
            if (currentViewPos == null) {
                currentViewPos = position;
                playVideoFirstTime(position);
            }
            videoPageLayout.bringChildToFront(videoView);

            Button downloadButton = (Button) videoPageLayout.findViewById(R.id.downloadButton);
            downloadButton.setOnClickListener(new View.OnClickListener() {
                public void onClick(View arg0) {
                    downloadFile(videoUrls.get(position));
                }
            });

            view.addView(videoPageLayout, 0);
            return videoPageLayout;
        }

        @Override
        public void setPrimaryItem(ViewGroup container, int position, Object object) {
            super.setPrimaryItem(container, position, object);
//            videoView.stopPlayback();
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

        private void playVideoFirstTime(int position) {
            showProgressDialog();
            videoViews[position].setVideoURI(Uri.parse(videoUrls.get(position)));
            videoViews[position].start();
        }
    }
}
