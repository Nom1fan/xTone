package com.server.sms_service;

/**
 * Created by Mor on 26/07/2016.
 */
public interface SmsSender {
    void sendSms(final String dest, final String msg);
}
