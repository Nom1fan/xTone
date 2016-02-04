package ServerObjects;

import com.google.gson.Gson;

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
import DataObjects.PushEventKeys;
import DataObjects.SharedConstants;
import LogObjects.LogsManager;

/**
 * Created by Mor on 15/09/2015.
 */
public abstract class PushSender {

    private static final String APPLICATION_ID = "7CL97UlX4EtpMyRJYshNlIQ3T12EEZ0OaZWxZjvR";
    private static final String REST_API_KEY = "XzVRYR8d6IiuKbZE0dYBdjxoyoEwP2ONJFxxHOPW";
    private static final String PUSH_URL = "https://api.parse.com/1/push";
    private static Logger _logger = LogsManager.get_serverLogger();
    private static class PushObject {

        private Map<String,String> data;
        private Map<String,String> where;

        public PushObject(final String token, final String pushEventAction, final String value) {

            data = new HashMap() {{
                    put(PushEventKeys.PUSH_EVENT_ACTION, pushEventAction);
                    put(PushEventKeys.PUSH_DATA, value);
                }};

            where = new HashMap() {{
                put("channels", SharedConstants.APP_NAME);
                put("deviceToken", token);
            }};

        }

        public PushObject(final String token, final String pushEventAction, final String value, final String jsonExtra) {

            data = new HashMap() {{
                put(PushEventKeys.PUSH_EVENT_ACTION, pushEventAction);
                put(PushEventKeys.PUSH_DATA, value);
                put(PushEventKeys.PUSH_DATA_EXTRA, jsonExtra);
            }};

            where = new HashMap() {{
                put("channels", SharedConstants.APP_NAME);
                put("deviceToken", token);
            }};

        }

        public Map<String, String> getData() {
            return data;
        }

        public Map<String, String> getWhere() {
            return where;
        }
    }


    public static boolean sendPush(final String deviceToken, final String pushEventAction, final String value) {

        if(deviceToken==null || deviceToken.equals("")) {
            _logger.severe("Invalid device token. Aborting push send");
            return false;
        }

        try {
            pushData(new Gson().toJson(new PushObject(deviceToken, pushEventAction, value)));
        } catch (Exception e) {
            e.printStackTrace();
            _logger.severe("Failed to send push to token:"+deviceToken+". Exception:"+e.getMessage());
            return false;
        }

        return true;
    }

    public static boolean sendPush(final String deviceToken, final String pushEventAction, final String value, final String jsonExtra) {

        if(deviceToken==null || deviceToken.equals("")) {
            _logger.severe("Invalid device token. Aborting push send");
            return false;
        }

        try {
            pushData(new Gson().toJson(new PushObject(deviceToken, pushEventAction, value, jsonExtra)));
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
