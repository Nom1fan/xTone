package com.server.sms_service;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.impl.client.HttpClients;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.logging.Logger;

/**
 * Created by Mor on 28/03/2016.
 */


/**
 * Created by Mor on 28/03/2016.
 */
@Service
public class TeleMessageSmsSender implements SmsSender {

    private static final String USER        =   "972544556543";
    private static final String PASSWORD    =   "Xla@123$$";
    private static final String URL         =   "https://secure.telemessage.com/jsp/receiveSMS.jsp";

    @Autowired
    private Logger logger;

    @Override
    public void sendSms(final String dest, final String msg) {

        new Thread() {

            @Override
            public void run() {

                try {
                    sendSmsGET(URL,USER,PASSWORD, dest, msg);
                } catch (Exception e) {
                    e.printStackTrace();
                    logger.severe("Failed to send SMS to [User]:" + dest + ". [Message]:" + msg + ". [Exception]:" + (e.getMessage() != null ? e.getMessage() : e));
                }
            }
        }.start();

    }

    private void sendSmsGET(String url, String userid,String password,String to,String text) throws Exception {

        RequestBuilder builder = RequestBuilder.get().setUri(url);
        builder.addParameter("userid", userid);
        builder.addParameter("password", password);
        builder.addParameter("to", to);
        builder.addParameter("text", text);
        HttpResponse response = HttpClients.createDefault().execute(builder.build());

        if (response != null && response.getStatusLine().getStatusCode() == 200) {

            response.getEntity().writeTo(System.out);

        } else if (response != null){

            BufferedReader reader = new BufferedReader(new InputStreamReader(response.getEntity().getContent(), "UTF-8"));
            StringBuilder sb = new StringBuilder();
            for (String line; (line = reader.readLine()) != null; ) {
                sb.append(line).append("\n");
            }

            logger.severe("SMS GET failed. [Response String]:" + sb.toString());
            throw new Exception("SMS GET failed. [Status]:" + response.getStatusLine().getStatusCode() + ". [Message Id]:" + sb.toString());

        } else{

            logger.severe("SMS GET failed. Response  is NULL");
            throw new Exception("SMS GET failed. Response  is NULL");

        }
    }
}