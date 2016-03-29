package ServerObjects;

import com.google.gson.Gson;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;

import java.util.logging.Logger;

import DataObjects.PushEventKeys;
import DataObjects.SharedConstants;
import LogObjects.LogsManager;

/**
 * Created by Mor on 15/09/2015.
 */
public abstract class BatchPushSender {

    private static final String REST_API_KEY    =   "2b2efabe088730905d0651656ffb642c";
    private static final String LIVE_API_KEY    =   SharedConstants.LIVE_API_KEY;
    private static final String API_VERSION     =   "1.0";
    private static final String PUSH_URL        =   "https://api.batch.com/" + API_VERSION + "/" + LIVE_API_KEY + "/transactional/send";
    private static Logger _logger               =   LogsManager.get_serverLogger();

    static
    {
        System.setProperty("org.apache.commons.logging.Log","org.apache.commons.logging.impl.SimpleLog");
        System.setProperty("org.apache.commons.logging.simplelog.showdatetime", "true");
        System.setProperty("org.apache.commons.logging.simplelog.log.org.apache.http.wire", "DEBUG");
    }

    /**
     * Sending a push containing title, message and data
     * @param deviceToken The device token of the push will be sent to
     * @param pushEventAction The push event action. Valid values are in PushEventKeys class.
     * @param title The title of the push notification
     * @param msg The body of the push notification
     * @param pushEventData The data payload to add to the push
     * @see PushEventKeys
     * @return
     */
    public static boolean sendPush(
            final String deviceToken,
            final String pushEventAction,
            String title,
            final String msg,
            final Object pushEventData) {

        if (deviceToken == null || deviceToken.equals("")) {
            _logger.severe("Invalid device token. Aborting push send");
            return false;
        }

        try {
            PushEventObject pushEventObject = new PushEventObject(pushEventAction, toJson(pushEventData));
            pushData(toJson(new PushObject(deviceToken, title, msg, toJson(pushEventObject))));

        } catch (Exception e) {
            e.printStackTrace();
            _logger.severe("Failed to send push to [Token]:" + deviceToken + ". [Exception]:" + (e.getMessage()!=null? e.getMessage() : e));
            return false;
        }

        return true;
    }

    /**
     * Sending a push containing only data
     * @param deviceToken The device token of the push will be sent to
     * @param pushEventAction The push event action. Valid values are in PushEventKeys class.
     * @param pushEventData The data payload to add to the push
     * @see PushEventKeys
     * @return
     */
    public static boolean sendPush(
            final String deviceToken,
            final String pushEventAction,
            final Object pushEventData) {

        if (deviceToken == null || deviceToken.equals("")) {
            _logger.severe("Invalid device token. Aborting push send");
            return false;
        }

        try {
            PushEventObject pushEventObject = new PushEventObject(pushEventAction, toJson(pushEventData));
            pushData(toJson(new PushObject(deviceToken, toJson(pushEventObject))));

        } catch (Exception e) {
            e.printStackTrace();
            _logger.severe("Failed to send push to token:" + deviceToken + ". Exception:" + e.getMessage());
            return false;
        }

        return true;
    }

    /**
     * Sending a push containing only title and message
     * @param deviceToken The device token of the push will be sent to
     * @param pushEventAction The push event action. Valid values are in PushEventKeys class.
     * @param title The title of the push notification
     * @param msg The body of the push notification
     * @see PushEventKeys
     * @return
     */
    public static boolean sendPush(
            final String deviceToken,
            final String pushEventAction,
            String title,
            final String msg) {

        if (deviceToken == null || deviceToken.equals("")) {
            _logger.severe("Invalid device token. Aborting push send");
            return false;
        }

        try {
            PushEventObject pushEventObject = new PushEventObject(pushEventAction);
            pushData(toJson(new PushObject(deviceToken, title, msg, toJson(pushEventObject))));

        } catch (Exception e) {
            e.printStackTrace();
            _logger.severe("Failed to send push to token:" + deviceToken + ". Exception:" + e.getMessage());
            return false;
        }

        return true;
    }



    private static void pushData(String postData) throws Exception {
        DefaultHttpClient httpClient = new DefaultHttpClient();
        HttpResponse response = null;
        HttpEntity entity = null;
        String responseString = null;
        HttpPost httpPost = new HttpPost(PUSH_URL);
        httpPost.addHeader("X-Authorization", REST_API_KEY);
        httpPost.addHeader("Content-Type", "application/json");
        StringEntity reqEntity = new StringEntity(postData);
        httpPost.setEntity(reqEntity);
        response = httpClient.execute(httpPost);
        entity = response.getEntity();
        if (entity != null) {
            responseString = EntityUtils.toString(response.getEntity());
        }
        int statusCode = response.getStatusLine().getStatusCode();
        if (statusCode != HttpStatus.SC_CREATED) {
            String reason = response.getStatusLine().getReasonPhrase();
            throw new Exception("Push POST failed. Status code:" + statusCode + ". Reason:" + reason);
        }

        _logger.info("Push response:" + responseString);
    }

    private static String toJson(Object data) {

        return new Gson().toJson(data);
    }

    private static class PushObject {

        private final String push_time = "now";
        private final String group_id = "general";
        private Recipients recipients;
        private Message message;
        private String custom_payload;

        public PushObject(String token, String title, String body, String custom_payload) {

            recipients = new Recipients(new String[] { token });
            message = new Message(title, body);
            this.custom_payload = custom_payload;
        }

        public PushObject(String token, String custom_payload) {

            recipients = new Recipients(new String[] { token });
            message = new Message("", "");
            this.custom_payload = custom_payload;

        }

        private class Recipients {

            private String[] install_ids;

            public Recipients(String[] tokens) {

                this.install_ids = tokens;
            }
        }

        private class Message {

            private String title;
            private String body;

            public Message(String title, String body) {

                this.title = title;
                this.body = body;
            }
        }

    }

    private static class PushEventObject {

        private String pushEventAction;
        private String pushEventData;

        public PushEventObject(final String pushEventAction, final String pushEventData) {

            this.pushEventAction = pushEventAction;
            this.pushEventData = pushEventData;
        }

        public PushEventObject(final String pushEventAction) {

            this.pushEventAction = pushEventAction;
            this.pushEventData = "";
        }

    }
}
