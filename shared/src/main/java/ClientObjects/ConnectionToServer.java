package ClientObjects;


import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.List;

import MessagesToClient.MessageToClient;

import static java.util.AbstractMap.SimpleEntry;

public class ConnectionToServer {

    private static final int READ_TIMEOUT = 10000;
    private static final int CONNECT_TIMEOUT = 15000;
    private static final String REQUEST_METHOD_POST = "POST";
    private static final String ENCODING = "UTF-8";

    private IServerProxy serverProxy;
    private Gson gson;
    private HttpURLConnection conn;

    /**
     * Constructs the client.
     */
    public ConnectionToServer(IServerProxy serverProxy) {
        this.serverProxy = serverProxy;
        gson = new Gson();
    }

    public <RESPONSE> void sendToServer(URL url, List<SimpleEntry> params, TypeToken<MessageToClient<RESPONSE>> responseTypeToken) {

        try {
            sendRequest(url, params);
            readResponse(responseTypeToken);
        } catch (IOException e) {
            connectionException(e);
        } finally {
            if(conn!=null)
                conn.disconnect();
        }
    }

    public void sendToServer(URL url, List<SimpleEntry> params) {
        sendToServer(url, params, new TypeToken<MessageToClient<Void>>(){});
    }

    private void sendRequest(URL url, List<SimpleEntry> params) throws IOException {
        conn = (HttpURLConnection) openConnection(url);

        OutputStream os = conn.getOutputStream();
        BufferedWriter writer = new BufferedWriter(
                new OutputStreamWriter(os, ENCODING));
        writer.write(getQuery(params));
        writer.flush();
        writer.close();
        os.close();
        conn.connect();
    }

    public void asyncSendToServer(URL url, List<SimpleEntry> params) {
        try {
            sendRequest(url, params);
        } catch (IOException e) {
            connectionException(e);
        }
    }

    public <RESPONSE> void readResponse(TypeToken<MessageToClient<RESPONSE>> responseTypeToken) throws IOException {
        BufferedReader br = new BufferedReader(new InputStreamReader((conn.getInputStream())));
        String responseMessage = br.readLine();
        MessageToClient<RESPONSE> response = extractResponse(responseMessage, responseTypeToken);
        serverProxy.handleMessageFromServer(response, this);
    }

    public URLConnection openConnection(URL url) throws IOException {
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

    public void connectionException(Exception e) {

        String errMsg = "Connection error";
        serverProxy.handleDisconnection(this, e != null ? errMsg + ":" + e.toString() : errMsg);
    }

    public boolean ping(String host, int port) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), CONNECT_TIMEOUT);
            return true;
        } catch (IOException e) {
            return false; // Either timeout or unreachable or failed DNS lookup.
        }
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

    private <RESPONSE> MessageToClient<RESPONSE> extractResponse(String resJson, TypeToken<MessageToClient<RESPONSE>> resType) {
        return gson.fromJson(resJson, resType.getType());
    }

    public HttpURLConnection getConnection() {
        return conn;
    }
}
