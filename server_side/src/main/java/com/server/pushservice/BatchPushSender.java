package com.server.pushservice;

import com.google.gson.Gson;
import com.server.lang.ServerConstants;

import org.apache.http.HttpEntity;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.logging.Logger;



/**
 * Created by Mor on 15/09/2015.
 */
@Service
public class BatchPushSender implements PushSender {

    private static final String REST_API_KEY    =   "2b2efabe088730905d0651656ffb642c";
    private static final String LIVE_API_KEY    =   ServerConstants.LIVE_API_KEY;
    private static final String API_VERSION     =   "1.0";
    private static final String PUSH_URL        =   "https://api.batch.com/" + API_VERSION + "/" + LIVE_API_KEY + "/transactional/send";

    @Autowired
    private Logger logger;

    @Autowired
    private Gson gson;

    static
    {
        System.setProperty("org.apache.commons.logging.Log","org.apache.commons.logging.impl.SimpleLog");
        System.setProperty("org.apache.commons.logging.simplelog.showdatetime", "true");
        System.setProperty("org.apache.commons.logging.simplelog.log.org.apache.http.wire", "DEBUG");
    }

    @Override
    public boolean sendPush(
            final String deviceToken,
            final String pushEventAction,
            String title,
            final String msg,
            final Object pushEventData) {

        if (deviceToken == null || deviceToken.equals("")) {
            logger.severe("Invalid device token. Aborting push send");
            return false;
        }

        try {
            PushEventObject pushEventObject = new PushEventObject(pushEventAction, toJson(pushEventData));
            pushData(toJson(new PushObject(deviceToken, title, msg, toJson(pushEventObject))));

        } catch (Exception e) {
            e.printStackTrace();
            logger.severe("Failed to send push to [Token]:" + deviceToken + ". [Exception]:" + (e.getMessage()!=null? e.getMessage() : e));
            return false;
        }

        return true;
    }

    @Override
    public boolean sendPush(
            final String deviceToken,
            final String pushEventAction,
            final Object pushEventData) {

        if (deviceToken == null || deviceToken.equals("")) {
            logger.severe("Invalid device token. Aborting push send");
            return false;
        }

        try {
            PushEventObject pushEventObject = new PushEventObject(pushEventAction, toJson(pushEventData));
            pushData(toJson(new PushObject(deviceToken, toJson(pushEventObject))));

        } catch (Exception e) {
            e.printStackTrace();
            logger.severe("Failed to send push to token:" + deviceToken + ". Exception:" + e.getMessage());
            return false;
        }

        return true;
    }

    @Override
    public boolean sendPush(
            final String deviceToken,
            final String pushEventAction,
            String title,
            final String msg) {

        if (deviceToken == null || deviceToken.equals("")) {
            logger.severe("Invalid device token. Aborting push send");
            return false;
        }

        try {
            PushEventObject pushEventObject = new PushEventObject(pushEventAction);
            pushData(toJson(new PushObject(deviceToken, title, msg, toJson(pushEventObject))));

        } catch (Exception e) {
            e.printStackTrace();
            logger.severe("Failed to send push to token:" + deviceToken + ". Exception:" + e.getMessage());
            return false;
        }

        return true;
    }


    private void pushData(String postData) throws Exception {
        logger.info("Sending push data:" + postData);
        CloseableHttpClient httpClient = HttpClients.createDefault();
        CloseableHttpResponse response;
        HttpEntity entity;
        String responseString = null;
        HttpPost httpPost = new HttpPost(PUSH_URL);
        httpPost.addHeader("X-Authorization", REST_API_KEY);
        httpPost.addHeader("Content-Type", "application/json");
        StringEntity reqEntity = new StringEntity(postData, ContentType.APPLICATION_JSON);
        httpPost.setEntity(reqEntity);
        response = httpClient.execute(httpPost);
        try {
            entity = response.getEntity();
            if (entity != null) {
                responseString = EntityUtils.toString(response.getEntity());
            }
            int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode != HttpStatus.SC_CREATED) {
                String reason = response.getStatusLine().getReasonPhrase();
                throw new Exception("Push POST failed. Status code:" + statusCode + ". Reason:" + reason);
            }
            logger.info("Push response:" + responseString);

        } finally {
            response.close();
        }
    }

    private String toJson(Object data) {
        return gson.toJson(data);
    }

    private class PushObject {

        private final String push_time = "now";
        private final String group_id = "general";
        private Recipients recipients;
        private Message message;
        private final String priority = "high";
        private Gcm_collapse_key gcm_collapse_key;
        private String custom_payload;

        public PushObject(String token, String title, String body, String custom_payload) {

            recipients = new Recipients(new String[] { token });
            message = new Message(title, body);
            gcm_collapse_key = new Gcm_collapse_key(false,"default"); //  If disabled, it will show all the notifications received when the device was offline. You should disable the collapse key if all your notifications matter
            this.custom_payload = custom_payload;
        }

        public PushObject(String token, String custom_payload) {

            recipients = new Recipients(new String[] { token });
            message = new Message("", "");
            gcm_collapse_key = new Gcm_collapse_key(false,"default"); //  If disabled, it will show all the notifications received when the device was offline. You should disable the collapse key if all your notifications matter
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

        private class Gcm_collapse_key {

            private boolean enabled;
            private String key;

            public Gcm_collapse_key(boolean enabled, String key) {

                this.enabled = enabled;
                this.key = key;
            }
        }

    }

    private class PushEventObject {

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
