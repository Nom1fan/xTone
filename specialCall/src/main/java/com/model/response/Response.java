package com.model.response;

import java.io.Serializable;

/**
 * Abstract response to the client, containing generic result
 *
 * @author Mor
 */
public class Response<T> implements Serializable {

    private T result;

    public Response(T result) {
        this.result = result;
    }

    public T getResult() {
        return result;
    }
}
