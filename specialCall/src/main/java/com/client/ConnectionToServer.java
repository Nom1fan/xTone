package com.client;

import com.model.request.DownloadFileRequest;
import com.model.response.Response;

import java.io.IOException;
import java.lang.reflect.Type;

/**
 * Created by Mor on 01/07/2017.
 */

interface ConnectionToServer {

    int READ_TIMEOUT = 10000;
    int CONNECT_TIMEOUT = 6*1000;
    String REQUEST_METHOD_POST = "POST";
    String ENCODING = "UTF-8";

    void setResponseType(Type responseType);

    <T> int sendRequest(String url, T requestBody) throws IOException;

    <T> Response<T> readResponse() throws IOException;

    int sendMultipartToServer(String url, ProgressiveEntity progressiveEntity);

    boolean download(String url, String pathToDownload, long fileSize, DownloadFileRequest request);

    void disconnect();
}
