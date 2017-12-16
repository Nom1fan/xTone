/*******************************************************************************
 * Copyright 2014 Sergey Tarasevich
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package com_international.ui.activities;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.util.Log;

import com_international.data.objects.ActivityRequestCodes;
import com_international.fragment.AbsListViewBaseFragment;
import com_international.fragment.ImageGIFGridFragment;
import com_international.fragment.ImageGIFPagerFragment;
import com_international.fragment.ImageGalleryFragment;
import com_international.fragment.ImageGridFragment;
import com_international.fragment.ImageListFragment;
import com_international.fragment.ImageMusicListFragment;
import com_international.fragment.ImageMusicPagerFragment;
import com_international.fragment.ImagePagerFragment;
import com_international.fragment.VideoGalleryFragment;
import com_international.fragment.VideoPagerFragment;
import com_international.mediacallz.app.R;
import com_international.utils.SharedPrefUtils;

import com_international.files.media.MediaFile;

import static com.crashlytics.android.Crashlytics.log;

/**
 * @author Sergey Tarasevich (nostra13[at]gmail[dot]com)
 */
public class GalleryLauncherFragmentActivity extends FragmentActivity {

    public  static final String RESULT_FILE_PATH = "RESULT_FILE_PATH";

    private static final String TAG = GalleryLauncherFragmentActivity.class.getSimpleName();

    @Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
        log(Log.INFO, TAG, "onCreate");

        int frIndex = getIntent().getIntExtra(AbsListViewBaseFragment.FRAGMENT_INDEX, 0);
		Fragment fr;
		String tag;
		int titleRes;


		switch (frIndex) {
			default:
			case ImageListFragment.INDEX:
				tag = ImageListFragment.class.getSimpleName();
				fr = getSupportFragmentManager().findFragmentByTag(tag);
				if (fr == null) {
					fr = new ImageListFragment();
				}
				titleRes = R.string.ac_name_image_list;
				break;

			case ImageGridFragment.INDEX:
				tag = ImageGridFragment.class.getSimpleName();
				fr = getSupportFragmentManager().findFragmentByTag(tag);
				if (fr == null) {
					fr = new ImageGridFragment();
				}
				titleRes = R.string.ac_name_image_grid;
				break;
            case ImageGIFGridFragment.INDEX:
                tag = ImageGridFragment.class.getSimpleName();
                fr = getSupportFragmentManager().findFragmentByTag(tag);
                if (fr == null) {
                    fr = new ImageGIFGridFragment();
                }
                titleRes = R.string.ac_name_image_gif;
                break;

            case ImagePagerFragment.INDEX:
				tag = ImagePagerFragment.class.getSimpleName();
				fr = getSupportFragmentManager().findFragmentByTag(tag);
				if (fr == null) {
					fr = new ImagePagerFragment();
					fr.setArguments(getIntent().getExtras());
				}
				titleRes = R.string.ac_name_image_pager;
				break;


            case ImageGIFPagerFragment.INDEX:
                tag = ImagePagerFragment.class.getSimpleName();
                fr = getSupportFragmentManager().findFragmentByTag(tag);
                if (fr == null) {
                    fr = new ImageGIFPagerFragment();
                    fr.setArguments(getIntent().getExtras());
                }
                titleRes = R.string.ac_name_image_pager;
                break;

			case ImageGalleryFragment.INDEX:
				tag = ImageGalleryFragment.class.getSimpleName();
				fr = getSupportFragmentManager().findFragmentByTag(tag);
				if (fr == null) {
					fr = new ImageGalleryFragment();
				}
				titleRes = R.string.ac_name_image_gallery;
				break;

            case VideoPagerFragment.INDEX:
                tag = VideoPagerFragment.class.getSimpleName();
                fr = getSupportFragmentManager().findFragmentByTag(tag);
                //fr.setUserVisibleHint(true);
                if (fr == null) {
                    fr = new VideoPagerFragment();
                    fr.setArguments(getIntent().getExtras());
                    fr.isVisible();
//                    fr.onCreate(savedInstanceState);
                    //fr.setUserVisibleHint(false);
                }
                titleRes = R.string.button_video_gallery;
                setTitle(titleRes);
                getSupportFragmentManager().beginTransaction().replace(android.R.id.content, fr, tag).commit();
                break;
			case VideoGalleryFragment.INDEX:
				tag = VideoGalleryFragment.class.getSimpleName();
				fr = getSupportFragmentManager().findFragmentByTag(tag);
				if (fr == null) {
					fr = new VideoGalleryFragment();
                    //fr.setArguments(getIntent().getExtras());
				}
				titleRes = R.string.ac_name_image_gallery;
				break;
            case ImageMusicListFragment.INDEX:
                tag = ImageMusicListFragment.class.getSimpleName();
                fr = getSupportFragmentManager().findFragmentByTag(tag);
                if (fr == null) {
                    fr = new ImageMusicListFragment();
                    //fr.setArguments(getIntent().getExtras());
                }
                titleRes = R.string.ac_name_image_pager;
                break;
            case ImageMusicPagerFragment.INDEX:
                tag = ImageMusicPagerFragment.class.getSimpleName();
                fr = getSupportFragmentManager().findFragmentByTag(tag);
                if (fr == null) {
                    fr = new ImageMusicPagerFragment();
                    fr.setArguments(getIntent().getExtras());
                }
                titleRes = R.string.ac_name_image_pager;
                break;
        }

		setTitle(titleRes);
		getSupportFragmentManager().beginTransaction().replace(android.R.id.content, fr, tag).commit();
	}

    @Override
    public void onBackPressed() {
        int count = getSupportFragmentManager().getBackStackEntryCount();

        if (count == 0) {
            super.onBackPressed();
            //additional code
        } else {
            getSupportFragmentManager().popBackStack();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        log(Log.INFO, TAG, "onActivityResult");
        if(resultCode == Activity.RESULT_OK) {
            if (requestCode == ActivityRequestCodes.PREVIEW_MEDIA) {
                MediaFile resultFile = (MediaFile) data.getSerializableExtra(PreviewMediaActivity.RESULT_FILE);
                setResultFilePathFromContentStore(resultFile.getFile().getAbsolutePath());
                finish();
            }
        }
    }

    @Override
    protected void onPostResume() {
        super.onPostResume();
        log(Log.INFO, TAG, "onPostResume");
        String resultFilePath;
        if(!(resultFilePath = getResultFilePathFromContentStore()).isEmpty()) {
            Intent resultIntent = new Intent();
            resultIntent.putExtra(RESULT_FILE_PATH, resultFilePath);

            if (getParent() == null) {
                setResult(Activity.RESULT_OK, resultIntent);
            } else {
                getParent().setResult(Activity.RESULT_OK, resultIntent);
            }

            setResultFilePathFromContentStore("");

            finish();
        }

//        if (mReturningWithResult) {
//            if (getParent() == null) {
//                setResult(Activity.RESULT_OK, data);
//            } else {
//                getParent().setResult(Activity.RESULT_OK, data);
//            }
//            markForFinish2ndTime();
//            getSupportFragmentManager().popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
//            finish();
//        }
        // Reset the boolean flag back to false for next time.
//        mReturningWithResult = false;
    }

    private void setResultFilePathFromContentStore(String filePath) {
        SharedPrefUtils.setString(this, SharedPrefUtils.CONTENT_STORE, SharedPrefUtils.RESULT_FILE_PATH_FROM_CONTENT_STORE, filePath);
    }

    private String getResultFilePathFromContentStore() {
        return SharedPrefUtils.getString(this, SharedPrefUtils.CONTENT_STORE, SharedPrefUtils.RESULT_FILE_PATH_FROM_CONTENT_STORE);
    }
}