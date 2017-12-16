package com_international.model.request;

/**
 * Created by Mor on 17/12/2016.
 */
public class IsRegisteredRequest extends Request {

    private String destinationId;

    public IsRegisteredRequest(Request request) {
        request.copy(this);
    }

    public String getDestinationId() {
        return destinationId;
    }

    public void setDestinationId(String destinationId) {
        this.destinationId = destinationId;
    }
}


