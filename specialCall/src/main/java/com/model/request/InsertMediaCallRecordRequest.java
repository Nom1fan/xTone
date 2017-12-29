package com.model.request;


import com.data.objects.MediaCall;

/**
 * Created by Mor on 17/12/2016.
 */
public class InsertMediaCallRecordRequest extends Request {

    private MediaCall mediaCall;

    public InsertMediaCallRecordRequest(Request request) {
        request.copy(this);
    }

    public MediaCall getMediaCall() {
        return mediaCall;
    }

    public void setMediaCall(MediaCall mediaCall) {
        this.mediaCall = mediaCall;
    }
}


