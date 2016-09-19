package com.client;


import com.google.gson.Gson;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.List;

import MessagesToClient.MessageToClient;
import cz.msebera.android.httpclient.client.methods.CloseableHttpResponse;
import cz.msebera.android.httpclient.client.methods.HttpPost;
import cz.msebera.android.httpclient.impl.client.CloseableHttpClient;
import cz.msebera.android.httpclient.impl.client.HttpClientBuilder;

import static java.util.AbstractMap.SimpleEntry;

public class ConnectionToServer {

    private static final int READ_TIMEOUT = 10000;
    private static final int CONNECT_TIMEOUT = 15000;
    private static final String REQUEST_METHOD_POST = "POST";
    private static final String ENCODING = "UTF-8";

    private IServerProxy serverProxy;
    private Gson gson;
    private HttpURLConnection conn;
    private CloseableHttpClient httpClient = null;
    private HttpPost post;
    private Type responseType;

    /**
     * Constructs the client.
     */
    public ConnectionToServer(IServerProxy serverProxy, Type responseType) {
        this.serverProxy = serverProxy;
        this.responseType = responseType;
        gson = new Gson();
    }

    public void sendToServer(String url, List<SimpleEntry> params) {

        try {
            sendRequest(url, params);
            readResponse();
        } catch (IOException e) {
            connectionException(e);
        } finally {
            if(conn!=null)
                conn.disconnect();
        }
    }

    public void sendMultipartToServer(String url, ProgressiveEntity progressiveEntity) {
        CloseableHttpResponse response = null;
        try {
            httpClient = HttpClientBuilder.create().build();
            post = new HttpPost(url);
            post.setEntity(progressiveEntity);
            response = httpClient.execute(post);
            BufferedReader br = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
            String responseMessage = br.readLine();
            MessageToClient msg = extractResponse(responseMessage);
            serverProxy.handleMessageFromServer(msg, this);

        } catch (IOException e) {
            connectionException(e);
        } finally {
            if(httpClient!=null) {
                try {
                    httpClient.close();
                } catch (IOException ignored) {}
            }
            if(response!=null) {
                try {
                    response.close();
                } catch (IOException ignored) {}
            }
        }
    }

    private void sendRequest(String url, List<SimpleEntry> params) throws IOException {

        conn = (HttpURLConnection) openConnection(new URL(url));

        OutputStream os = conn.getOutputStream();
        BufferedWriter writer = new BufferedWriter(
                new OutputStreamWriter(os, ENCODING));
        writer.write(getQuery(params));
        writer.flush();
        writer.close();
        os.close();
        conn.connect();
    }

    private void readResponse() throws IOException {
        BufferedReader br = new BufferedReader(new InputStreamReader((conn.getInputStream())));
        String responseMessage = br.readLine();
        MessageToClient response = extractResponse(responseMessage);
        serverProxy.handleMessageFromServer(response, this);
    }

    private URLConnection openConnection(URL url) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setReadTimeout(READ_TIMEOUT);
        conn.setConnectTimeout(CONNECT_TIMEOUT);
        conn.setRequestMethod(REQUEST_METHOD_POST);
        conn.setDoInput(true);
        conn.setDoOutput(true);
        return conn;
    }

    public void closeConnection() {
        if(conn!=null)
            conn.disconnect();
        if(httpClient!=null) {
            try {
                httpClient.close();
                if(!post.isAborted())
                    post.abort();
            } catch (IOException ignored) {}
        }
    }

    private void connectionException(Exception e) {

        String errMsg = "Connection error";
        serverProxy.handleDisconnection(this, e != null ? errMsg + ":" + e.toString() : errMsg);
    }

    private String getQuery(List<SimpleEntry> params) throws UnsupportedEncodingException
    {
        StringBuilder result = new StringBuilder();
        boolean first = true;

        for (SimpleEntry pair : params)
        {
            if (first)
                first = false;
            else
                result.append("&");

            result.append(URLEncoder.encode(pair.getKey().toString(), "UTF-8"));
            result.append("=");
            result.append(URLEncoder.encode(pair.getValue().toString(), "UTF-8"));
        }

        return result.toString();
    }

    private MessageToClient extractResponse(String resJson) {
        return gson.fromJson(resJson, responseType);
    }

    public HttpURLConnection getConnection() {
        return conn;
    }
}
