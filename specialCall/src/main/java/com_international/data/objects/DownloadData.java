package com_international.data.objects;

import java.io.Serializable;

/**
 * Created by Mor on 26/05/2017.
 */
public class DownloadData implements Serializable {

    private DefaultMediaData defaultMediaData;

    private PendingDownloadData pendingDownloadData;

    public DefaultMediaData getDefaultMediaData() {
        return defaultMediaData;
    }

    public void setDefaultMediaData(DefaultMediaData defaultMediaData) {
        this.defaultMediaData = defaultMediaData;
    }

    public PendingDownloadData getPendingDownloadData() {
        return pendingDownloadData;
    }

    public void setPendingDownloadData(PendingDownloadData pendingDownloadData) {
        this.pendingDownloadData = pendingDownloadData;
    }
}
