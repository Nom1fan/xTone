/*******************************************************************************
 * Copyright 2011-2013 Sergey Tarasevich
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
package com.ui.activities;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.data.objects.ActivityRequestCodes;
import com.data.objects.Constants;
import com.fragment.AbsListViewBaseFragment;
import com.fragment.ImageGIFGridFragment;
import com.fragment.ImageGridFragment;
import com.fragment.ImageMusicListFragment;
import com.fragment.VideoGalleryFragment;
import com.mediacallz.app.R;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.utils.InitUtils;

import com.files.media.MediaFile;

import static com.crashlytics.android.Crashlytics.log;


/**
 * @author Sergey Tarasevich (nostra13[at]gmail[dot]com)
 */
public class HomeActivity extends Activity {

    private static final String TAG = HomeActivity.class.getSimpleName();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        InitUtils.initImageLoader(this);
        setContentView(R.layout.ac_home);
    }

    public void onImageGridClick(View view) {
        Intent intent = new Intent(this, GalleryLauncherFragmentActivity.class);
        intent.putExtra(AbsListViewBaseFragment.FRAGMENT_INDEX, ImageGridFragment.INDEX);
        startActivityForResult(intent, ActivityRequestCodes.PREVIEW_MEDIA);
    }

    public void onImageGIFGridClick(View view) {
        Intent intent = new Intent(this, GalleryLauncherFragmentActivity.class);
        intent.putExtra(AbsListViewBaseFragment.FRAGMENT_INDEX, ImageGIFGridFragment.INDEX);
        startActivityForResult(intent, ActivityRequestCodes.PREVIEW_MEDIA);
    }

    public void onVideoGalleryClick(View view) {
        Intent intent = new Intent(this, GalleryLauncherFragmentActivity.class);
        intent.putExtra(AbsListViewBaseFragment.FRAGMENT_INDEX, VideoGalleryFragment.INDEX);
        startActivityForResult(intent, ActivityRequestCodes.PREVIEW_MEDIA);
    }

    public void onMusicListClick(View view) {
        Intent intent = new Intent(this, GalleryLauncherFragmentActivity.class);
        intent.putExtra(AbsListViewBaseFragment.FRAGMENT_INDEX, ImageMusicListFragment.INDEX);
        startActivityForResult(intent, ActivityRequestCodes.PREVIEW_MEDIA);
    }

    @Override
    public void onBackPressed() {
        ImageLoader.getInstance().stop();
        super.onBackPressed();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.item_clear_memory_cache) {
            ImageLoader.getInstance().clearMemoryCache();
            return true;
        } else if (item.getItemId() == R.id.item_clear_disc_cache) {
            ImageLoader.getInstance().clearDiskCache();
            return true;
        } else
            return false;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        log(Log.INFO, TAG, "onActivityResult");
        if(resultCode == Activity.RESULT_OK) {
            if (requestCode == ActivityRequestCodes.PREVIEW_MEDIA) {
                Intent resultIntent = new Intent();

                String resultFilePath = data.getStringExtra(GalleryLauncherFragmentActivity.RESULT_FILE_PATH);

                MediaFile resultFile = createManagedFile(resultFilePath);
                if(resultFile == null)
                    finish();

                resultIntent.putExtra(PreviewMediaActivity.RESULT_FILE, resultFile);

                if (getParent() == null) {
                    setResult(Activity.RESULT_OK, resultIntent);
                } else {
                    getParent().setResult(Activity.RESULT_OK, resultIntent);
                }
                finish();
            }
        }
    }

    private MediaFile createManagedFile(String resultFilePath) {
        MediaFile managedFile = null;
        try {
            managedFile = new MediaFile(resultFilePath);
        } catch(Exception e) {
            e.printStackTrace();
            log(Log.ERROR, TAG, "Failed to create result managed file");
        }
        return managedFile;
    }

}