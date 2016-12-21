package com.model.request;


import com.data.objects.CallRecord;

/**
 * Created by Mor on 17/12/2016.
 */
public class InsertMediaCallRecordRequest extends Request {

    public InsertMediaCallRecordRequest(Request request) {
        request.copy(this);
    }

    private CallRecord callRecord;

    public CallRecord getCallRecord() {
        return callRecord;
    }

    public void setCallRecord(CallRecord callRecord) {
        this.callRecord = callRecord;
    }
}


