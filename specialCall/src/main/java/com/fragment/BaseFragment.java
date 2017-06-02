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

import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;

import com.async.tasks.DownloadFileAsyncTask.PostDownloadCallBackListener;
import com.data.objects.ActivityRequestCodes;
import com.flows.DownloadFileFlow;
import com.mediacallz.app.R;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.ui.activities.PreviewMediaActivity;

import java.io.File;

import com.files.media.MediaFile;
import com.utils.MediaFileUtils;
import com.utils.NetworkingUtils;
import com.utils.UI_Utils;
import com.utils.UtilityFactory;

import static com.crashlytics.android.Crashlytics.log;

/**
 * @author Sergey Tarasevich (nostra13[at]gmail[dot]com)
 */
public abstract class BaseFragment extends Fragment implements PostDownloadCallBackListener {

    private ProgressDialog progressDialog;

    protected MediaFileUtils mediaFileUtils = UtilityFactory.instance().getUtility(MediaFileUtils.class);

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setHasOptionsMenu(true);

        progressDialog = new ProgressDialog(getActivity());
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.main_menu, menu);
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

    protected void downloadFile(final String url) {
        DownloadFileFlow downloadFileFlow = new DownloadFileFlow();
        downloadFileFlow.setListener(this);
        downloadFileFlow.startDownloadFileFlow(getActivity(), url);
    }

    protected void showProgressDialog() {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                progressDialog.setIndeterminate(false);
                progressDialog.setCancelable(false);
                progressDialog.setTitle(getActivity().getResources().getString(R.string.buffering));
                progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
                progressDialog.show();
            }
        });
    }

    protected void dismissProgressDialog() {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                progressDialog.dismiss();
            }
        });
    }

    protected void showErrFailedToPopulate() {
        String errMsg;
        if(!NetworkingUtils.isNetworkAvailable(getActivity()))
            errMsg = getResources().getString(R.string.disconnected);
        else
            errMsg = getResources().getString(R.string.oops_try_again);

        UI_Utils.callToast(errMsg, Color.RED, getActivity());
    }

    @Override
    public void doCallBack(File file) {
        try {
            MediaFile managedFile = new MediaFile(file);
            if (mediaFileUtils.canMediaBePrepared(getActivity(), managedFile)) {
                Intent i = new Intent(getActivity(), PreviewMediaActivity.class);
                i.putExtra(PreviewMediaActivity.MANAGED_MEDIA_FILE, managedFile);
                getActivity().startActivityForResult(i, ActivityRequestCodes.PREVIEW_MEDIA);
            } else
                UI_Utils.callToast(getActivity().getResources().getString(R.string.file_invalid), Color.RED, Toast.LENGTH_LONG, getActivity());

        } catch(Exception e) {
            e.printStackTrace();
            log(Log.ERROR, getClass().getSimpleName(), "Failed to create managed file from downloaded file");
        }
    }
}
