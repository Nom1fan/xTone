package com.data.objects;

import com.enums.CallRecordType;

/**
 * Created by rony on 15/07/2017.
 */

public class CallHistoryRecord {

    private String nameOrNumber;
    private CallRecordType callType;
    private String dateAndTime;
    private String duration;

    public CallHistoryRecord() {
        this.nameOrNumber = "";
        this.callType = CallRecordType.UKNOWN;
        this.dateAndTime = "";
        this.duration = "";
    }

    public CallHistoryRecord(String nameOrNumber, CallRecordType callType,String dateAndTime, String duration) {
        this.nameOrNumber = nameOrNumber;
        this.callType = callType;
        this.dateAndTime = dateAndTime;
        this.duration = duration;
    }

    public String getNameOrNumber() {
        return nameOrNumber;
    }

    public CallRecordType getCallType() {
        return callType;
    }

    public String getDateAndTime() {
        return dateAndTime;
    }

    public String getDuration() {
        return duration;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        CallHistoryRecord callRecord = (CallHistoryRecord) o;

        return nameOrNumber.equals(callRecord.nameOrNumber);

    }

    @Override
    public int hashCode() {
        return nameOrNumber.hashCode();
    }

    @Override
    public String toString() {
        return "CallHistoryRecord{" +
                "nameOrNumber='" + nameOrNumber + '\'' +
                ", callType='" + callType + '\'' +
                '}';
    }


}
