package ServerObjects;

import net.sf.json.JSONObject;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import LogObjects.LogsManager;

/**
 * Created by Mor on 15/09/2015.
 */
public abstract class PushSender {

    private static final String APPLICATION_ID = "7CL97UlX4EtpMyRJYshNlIQ3T12EEZ0OaZWxZjvR";
    private static final String REST_API_KEY = "XzVRYR8d6IiuKbZE0dYBdjxoyoEwP2ONJFxxHOPW";
    private static final String PUSH_URL = "https://api.parse.com/1/push";
    private static Logger _logger = LogsManager.getServerLogger();

    public static boolean sendPush(final String deviceToken) {

        JSONObject jo = new JSONObject();
        Map<String, String> data = new HashMap();
        data.put("alert", "Mandatory push data");
        HashMap where = new HashMap() {{
            put("channels", "SpecialCall");
            put("deviceToken", deviceToken);
        }};
        jo.put("where", where);
        jo.put("data", data);

        try {
            pushData(jo.toString());
        } catch (Exception e) {
            e.printStackTrace();
            _logger.severe("Failed to send push to token:"+deviceToken+". Exception:"+e.getMessage());
            return false;
        }

        return true;
    }

    private static void pushData(String postData) throws Exception {
        DefaultHttpClient httpclient = new DefaultHttpClient();
        HttpResponse response = null;
        HttpEntity entity = null;
        String responseString = null;
        HttpPost httpost = new HttpPost(PUSH_URL);
        httpost.addHeader("X-Parse-Application-Id", APPLICATION_ID);
        httpost.addHeader("X-Parse-REST-API-Key", REST_API_KEY);
        httpost.addHeader("Content-Type", "application/json");
        StringEntity reqEntity = new StringEntity(postData);
        httpost.setEntity(reqEntity);
        response = httpclient.execute(httpost);
        entity = response.getEntity();
        if (entity != null) {
            responseString = EntityUtils.toString(response.getEntity());
        }
        int statusCode = response.getStatusLine().getStatusCode();
        if(statusCode != HttpStatus.SC_OK) {
            String reason = response.getStatusLine().getReasonPhrase();
            throw new Exception("Push POST failed. Status code:" + statusCode + ". Reason:"+reason);
        }

        _logger.info("Push response:"+responseString);
    }
}
