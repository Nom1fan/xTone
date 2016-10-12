package com.server.database.dbos;

import lombok.Data;

/**
 * Created by Mor on 27/09/2016.
 */
@Data
public class SmsVerificationDBO {

    String uid;
    int code;

    public SmsVerificationDBO() {}

    public SmsVerificationDBO(String uid, int code) {
        this.uid = uid;
        this.code = code;
    }
}
