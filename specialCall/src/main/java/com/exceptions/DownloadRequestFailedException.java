package com.exceptions;

/**
 * Created by Mor on 14/11/2015.
 */
public class DownloadRequestFailedException extends FileException {

    public DownloadRequestFailedException() {}

    public DownloadRequestFailedException(String msg) {
        super(msg);
    }
}
