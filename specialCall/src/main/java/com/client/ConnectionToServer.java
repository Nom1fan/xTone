package com.client;


import android.util.Log;

import com.google.gson.Gson;
import com.model.request.DownloadFileRequest;
import com.model.response.Response;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.text.DecimalFormat;

import cz.msebera.android.httpclient.HttpResponse;
import cz.msebera.android.httpclient.HttpStatus;
import cz.msebera.android.httpclient.client.HttpClient;
import cz.msebera.android.httpclient.client.methods.HttpPost;
import cz.msebera.android.httpclient.impl.client.HttpClientBuilder;

import static com.crashlytics.android.Crashlytics.log;

public class ConnectionToServer {

    private static final String TAG = ConnectionToServer.class.getSimpleName();

    private static final int READ_TIMEOUT = 10000;
    private static final int CONNECT_TIMEOUT = 15000;
    private static final String REQUEST_METHOD_POST = "POST";
    private static final String ENCODING = "UTF-8";

    private Gson gson;
    private HttpURLConnection conn;

    public void setResponseType(Type responseType) {
        this.responseType = responseType;
    }

    private Type responseType;

    /**
     * Constructs the client.
     */
    public ConnectionToServer(Type responseType) {
        this.responseType = responseType;
        gson = new Gson();
    }

    public ConnectionToServer() {
        gson = new Gson();
    }

    public <T> int sendRequest(String url, T requestBody) throws IOException {
        sendRequestBody(url, requestBody);
        logErrors();
        return conn.getResponseCode();
    }

    public <T> Response<T> readResponse() throws IOException {
        BufferedReader br = new BufferedReader(new InputStreamReader((conn.getInputStream())));
        String responseBody = br.readLine();
        return extractResponse(responseBody);
    }

    public int sendMultipartToServer(String url, ProgressiveEntity progressiveEntity) {

        HttpPost post = null;
        int responseCode = -1;
        try {
            HttpClient client = HttpClientBuilder.create().build();
            post = new HttpPost(url);
            post.setEntity(progressiveEntity);
            HttpResponse httpResponse = client.execute(post);
            //  BufferedReader br = new BufferedReader(new InputStreamReader(httpResponse.getEntity().getContent()));
            //  String responseMessage = br.readLine();
            //  Response response = extractResponse(responseMessage);
            responseCode = httpResponse.getStatusLine().getStatusCode();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (post != null)
                post.releaseConnection();
        }
        return responseCode;
    }

    public boolean download(String url, String pathToDownload, long fileSize, DownloadFileRequest request) {
        boolean success = false;
        BufferedOutputStream bos = null;
        try {
            sendRequestBody(url, request);
            int responseCode = conn.getResponseCode();
            if (responseCode == HttpStatus.SC_OK) {

                // Creating file and directories for downloaded file
                File newDir = new File(pathToDownload);
                newDir.getParentFile().mkdirs();
                File newFile = new File(pathToDownload);
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
                    if (progPercent - prevProgPercent >= 1) {
                        publishProgress(progPercent);
                        prevProgPercent = progPercent;
                    }
                    fileSize -= bytesRead;
                }
                if (fileSize > 0)
                    throw new IOException("download was stopped abruptly. " + fileSize + " bytes left.");

                success = true;
            } else
                log(Log.ERROR, TAG, "Download failed. Response code:" + responseCode);

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (bos != null)
                try {
                    bos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            disconnect();
        }
        return success;
    }

    public void disconnect() {
        if (conn != null) {
            conn.disconnect();
        }
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

    private URLConnection openConnection(URL url) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setReadTimeout(READ_TIMEOUT);
        conn.setConnectTimeout(CONNECT_TIMEOUT);
        conn.setRequestMethod(REQUEST_METHOD_POST);
        conn.setDoInput(true);
        conn.setDoOutput(true);
        return conn;
    }

    private <T> Response<T> extractResponse(String resJson) {
        return gson.fromJson(resJson, responseType);
    }

    private void publishProgress(double progPercent) {
        System.out.println("Download progress:" + new DecimalFormat("#").format(progPercent) + "%");
    }

    private double calcProgressPercentage(long fileSize, long fileSizeConst) {
        return ((fileSizeConst - fileSize) / (double) fileSizeConst) * 100;
    }

    private void logErrors() {
        if(conn.getErrorStream()!=null)
            log(Log.INFO, TAG, "Response errors:" + readStream(conn.getErrorStream()));
    }

    private String readStream(InputStream stream)  {
        StringBuilder builder = new StringBuilder();
        try (BufferedReader in = new BufferedReader(new InputStreamReader(stream))) {
            String line;
            while ((line = in.readLine()) != null) {
                builder.append(line); // + "\r\n"(no need, json has no line breaks!)
            }

        } catch(Exception e) {
            e.printStackTrace();
        }
        return builder.toString();
    }
}