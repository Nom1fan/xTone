package annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import MessagesToServer.ServerActionType;

/**
 * Created by Mor on 16/07/2016.
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface ServerActionAnno {
    ServerActionType actionType();
}
