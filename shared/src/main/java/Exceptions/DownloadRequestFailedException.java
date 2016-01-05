package Exceptions;

/**
 * Created by Mor on 14/11/2015.
 */
public class DownloadRequestFailedException extends Exception {

    public DownloadRequestFailedException() {}

    public DownloadRequestFailedException(String msg) {
        super(msg);
    }
}
