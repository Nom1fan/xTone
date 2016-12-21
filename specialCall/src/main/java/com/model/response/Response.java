package com.model.response;

import java.io.Serializable;

/**
 * Abstract response to the client, containing information and enables generic interface for client actions corresponding to the message
 *
 * @author Mor
 */
public class Response<T> implements Serializable {

    private T result;
    private ClientActionType actionType;
    private int responseCode;
    private String message;

    public Response(ClientActionType actionType) {

        this.actionType = actionType;
    }

    public Response(ClientActionType actionType, T result) {

        this.result = result;
        this.actionType = actionType;
    }

    public T getResult() {
        return result;
    }

    public ClientActionType getActionType() {
        return actionType;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public int getResponseCode() {
        return responseCode;
    }

    public void setResponseCode(int responseCode) {
        this.responseCode = responseCode;
    }
}
