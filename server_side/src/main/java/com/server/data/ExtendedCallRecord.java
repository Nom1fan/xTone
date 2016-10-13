package com.server.data;

import java.io.Serializable;

import DataObjects.CallRecord;
import DataObjects.SpecialMediaType;
import lombok.Data;

/**
 * Created by Mor on 11/10/2016.
 */
@Data
public class ExtendedCallRecord implements Serializable {

    private static final long serialVersionUID = 7408472793374531808L;
    private static final String TAG = ExtendedCallRecord.class.getSimpleName();

    private String sourceId;
    private String destinationId;

    private MediaFile visualMediaFile;
    private MediaFile audioMediaFile;

    private String visualMd5;
    private String audioMd5;

    private SpecialMediaType specialMediaType;

    public ExtendedCallRecord(CallRecord callRecord) {

        sourceId = callRecord.get_sourceId();
        destinationId = callRecord.get_destinationId();

        visualMd5 = callRecord.get_visualMd5();
        audioMd5 = callRecord.get_audioMd5();

        visualMediaFile = new MediaFile(callRecord.get_visualMediaFile().getFile(), visualMd5);
        audioMediaFile = new MediaFile(callRecord.get_audioMediaFile().getFile(), audioMd5);

    }
}

