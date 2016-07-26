package com.server.annotations;

import org.springframework.stereotype.Component;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Created by Mor on 25/07/2016.
 */
@Component
@Retention(RetentionPolicy.RUNTIME)
public @interface Server {
}
