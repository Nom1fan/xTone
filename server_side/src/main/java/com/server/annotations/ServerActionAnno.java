package com.server.annotations;

import org.springframework.stereotype.Component;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import MessagesToServer.ServerActionType;

/**
 * Created by Mor on 16/07/2016.
 */
@Component
@Retention(RetentionPolicy.RUNTIME)
public @interface ServerActionAnno {
    ServerActionType actionType();
}
