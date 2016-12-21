package com.model.request;

/**
 * Created by Mor on 17/12/2016.
 */
public class IsRegisteredRequest extends Request {

    public IsRegisteredRequest(Request request) {
        request.copy(this);
    }

    private String destinationId;

    public String getDestinationId() {
        return destinationId;
    }

    public void setDestinationId(String destinationId) {
        this.destinationId = destinationId;
    }
}


