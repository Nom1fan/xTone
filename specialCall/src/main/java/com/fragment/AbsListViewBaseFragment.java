/*******************************************************************************
 * Copyright 2011-2014 Sergey Tarasevich
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package com.fragment;

import android.content.Intent;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.AbsListView;

import com.data_objects.Constants;
import com.mediacallz.app.R;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.listener.PauseOnScrollListener;
import com.ui.activities.GalleryLauncherFragmentActivity;

/**
 * @author Sergey Tarasevich (nostra13[at]gmail[dot]com)
 */
public abstract class AbsListViewBaseFragment extends BaseFragment {

    protected static final String STATE_PAUSE_ON_SCROLL = "STATE_PAUSE_ON_SCROLL";
    protected static final String STATE_PAUSE_ON_FLING = "STATE_PAUSE_ON_FLING";

    protected AbsListView listView;

    protected boolean pauseOnScroll = false;
    protected boolean pauseOnFling = true;

    @Override
    public void onResume() {
        super.onResume();
        applyScrollListener();
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        MenuItem pauseOnScrollItem = menu.findItem(R.id.item_pause_on_scroll);
        pauseOnScrollItem.setVisible(true);
        pauseOnScrollItem.setChecked(pauseOnScroll);

        MenuItem pauseOnFlingItem = menu.findItem(R.id.item_pause_on_fling);
        pauseOnFlingItem.setVisible(true);
        pauseOnFlingItem.setChecked(pauseOnFling);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.item_pause_on_scroll) {
            pauseOnScroll = !pauseOnScroll;
            item.setChecked(pauseOnScroll);
            applyScrollListener();
            return true;
        } else if (item.getItemId() == R.id.item_pause_on_fling) {
            pauseOnFling = !pauseOnFling;
            item.setChecked(pauseOnFling);
            applyScrollListener();
            return true;
        } else
            return super.onOptionsItemSelected(item);
    }

    protected void startImagePagerActivity(int position) {
        Intent intent = new Intent(getActivity(), GalleryLauncherFragmentActivity.class);
        intent.putExtra(Constants.Extra.FRAGMENT_INDEX, ImagePagerFragment.INDEX);
        intent.putExtra(Constants.Extra.MEDIA_POSITION, position);
        startActivity(intent);
    }

    protected void startGIFImagePagerActivity(int position) {
        Intent intent = new Intent(getActivity(), GalleryLauncherFragmentActivity.class);
        intent.putExtra(Constants.Extra.FRAGMENT_INDEX, ImageGIFPagerFragment.INDEX);
        intent.putExtra(Constants.Extra.MEDIA_POSITION, position);
        startActivity(intent);
    }


    protected void startVideoPagerActivity(int position) {
        Intent intent = new Intent(getActivity(), GalleryLauncherFragmentActivity.class);
        intent.putExtra(Constants.Extra.FRAGMENT_INDEX, VideoPagerFragment.INDEX);
        intent.putExtra(Constants.Extra.MEDIA_POSITION, position);
        startActivity(intent);
    }


    protected void startAudioPagerActivity(int position) {
        Intent intent = new Intent(getActivity(), GalleryLauncherFragmentActivity.class);
        intent.putExtra(Constants.Extra.FRAGMENT_INDEX, ImageMusicPagerFragment.INDEX);
        intent.putExtra(Constants.Extra.MEDIA_POSITION, position);
        startActivity(intent);
    }

    private void applyScrollListener() {
        listView.setOnScrollListener(new PauseOnScrollListener(ImageLoader.getInstance(), pauseOnScroll, pauseOnFling));
    }
}
