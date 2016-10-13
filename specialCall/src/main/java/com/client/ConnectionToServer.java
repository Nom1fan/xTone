package com.client;


import android.util.Log;

import com.google.gson.Gson;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileOutputStream;
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
import java.text.DecimalFormat;
import java.util.List;

import MessagesToClient.MessageToClient;
import cz.msebera.android.httpclient.HttpResponse;
import cz.msebera.android.httpclient.client.HttpClient;
import cz.msebera.android.httpclient.client.methods.HttpPost;
import cz.msebera.android.httpclient.impl.client.HttpClientBuilder;

import static com.crashlytics.android.Crashlytics.log;
import static java.util.AbstractMap.SimpleEntry;

public class ConnectionToServer {

    private static final String TAG = ConnectionToServer.class.getSimpleName();

    private static final int READ_TIMEOUT = 10000;
    private static final int CONNECT_TIMEOUT = 15000;
    private static final String REQUEST_METHOD_POST = "POST";
    private static final String ENCODING = "UTF-8";

    private IServerProxy serverProxy;
    private Gson gson;
    private HttpURLConnection conn;
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
            sendRequestParams(url, params);
            readResponse();
        } catch (IOException e) {
            connectionException(e);
        } finally {
            if(conn!=null)
                conn.disconnect();
        }
    }

    public <T> void sendToServer(String url, T requestBody) {

        try {
            sendRequestBody(url, requestBody);
            readResponse();
        } catch (IOException e) {
            connectionException(e);
        } finally {
            if(conn!=null)
                conn.disconnect();
        }
    }

    public void sendMultipartToServer(String url, ProgressiveEntity progressiveEntity) {

        HttpPost post = null;
        try {
            HttpClient client = HttpClientBuilder.create().build();
            post = new HttpPost(url);
            post.setEntity(progressiveEntity);
            HttpResponse response = client.execute(post);
            BufferedReader br = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
            String responseMessage = br.readLine();
            MessageToClient msg = extractResponse(responseMessage);
            serverProxy.handleMessageFromServer(msg, this);
        } catch (IOException e) {
            connectionException(e);
        } finally {
            if(post!=null)
                post.releaseConnection();

        }
    }

    public void download(String url, String pathToDownload, String fileName, long fileSize ,List<SimpleEntry> data) {

        BufferedOutputStream bos = null;
        try
        {
            sendRequestParams(url, data);
            int responseCode = conn.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {

                // Creating file and directories for downloaded file
                File newDir = new File(pathToDownload);
                newDir.mkdirs();
                String downloadFilePath = pathToDownload + "/" + fileName;
                File newFile = new File(downloadFilePath);
                newFile.createNewFile();

                FileOutputStream fos = new FileOutputStream(newFile);
                bos = new BufferedOutputStream(fos);
                DataInputStream dis = new DataInputStream(conn.getInputStream());

                System.out.println("Reading data...");
                byte[] buf = new byte[1024 * 8];
                long fileSizeConst = fileSize;
                int bytesRead;
                Double progPercent, prevProgPercent = 0.0;

                while (fileSize > 0 && (bytesRead = dis.read(buf, 0, (int) Math.min(buf.length, fileSize))) != -1) {
                    bos.write(buf, 0, bytesRead);
                    progPercent = calcProgressPercentage(fileSize, fileSizeConst);
                    if(progPercent - prevProgPercent >= 1) {
                        publishProgress(progPercent);
                        prevProgPercent = progPercent;
                    }
                    fileSize -= bytesRead;
                }
                if (fileSize > 0)
                    throw new IOException("download was stopped abruptly. " + fileSize + " bytes left.");
            }
            else
                log(Log.ERROR, TAG, "Download failed. Response code:" + responseCode);

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if(bos!=null)
                try {
                    bos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            closeConnection();
        }
    }

    public HttpURLConnection getConnection() {
        return conn;
    }

    private void sendRequestParams(String url, List<SimpleEntry> params) throws IOException {

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

    private <T> void sendRequestBody(String url, T requestBody) throws IOException {

        conn = (HttpURLConnection) openConnection(new URL(url));
        conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");

        OutputStream os = conn.getOutputStream();
        BufferedWriter writer = new BufferedWriter(
                new OutputStreamWriter(os, ENCODING));
        writer.write(gson.toJson(requestBody));
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
        conn.disconnect();
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

    private void publishProgress(double progPercent) {
        System.out.println("Download progress:" + new DecimalFormat("#").format(progPercent) + "%");
    }

    private double calcProgressPercentage(long fileSize, long fileSizeConst) {

        return ((fileSizeConst - fileSize) / (double) fileSizeConst) * 100;
    }
}