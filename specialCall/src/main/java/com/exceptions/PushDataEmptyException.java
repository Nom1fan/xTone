package com.exceptions;

/**
 * Created by Mor on 23/12/2017.
 */

public class PushDataEmptyException extends RuntimeException {

    public PushDataEmptyException(String errMsg) {
        super(errMsg);
    }
}
