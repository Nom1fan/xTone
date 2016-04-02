package ServerObjects;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.w3c.dom.Document;

import java.net.URLEncoder;
import java.util.logging.Logger;

import DataObjects.SharedConstants;
import LogObjects.LogsManager;
import utils.XmlUtils;


/**
 * Created by Mor on 28/03/2016.
 */
public abstract class SmsSender {

    private static final String USER        =   "ronyahae";
    private static final String PASSWORD    =   "zzY7e0vk";
    private static final String TYPE        =   "longsms";
    private static final String SENDER      =   SharedConstants.APP_NAME;
    private static final String API         =   "http://193.105.74.159/api/v3/sendsms/plain?user=" + USER + "&password=" + PASSWORD +
                                                "&sender=" + SENDER + "&SMSText=%s" + "&type=" + TYPE + "&GSM=%s";
    private static Logger _logger           =   LogsManager.get_serverLogger();

    public static void sendSms(final String dest, final String msg) {

        new Thread() {

            @Override
            public void run() {

                try {
                    String encodedMsg = URLEncoder.encode(msg, "UTF-8");
                    sendSmsGET(String.format(API, encodedMsg, dest));
                } catch (Exception e) {
                    e.printStackTrace();
                    _logger.severe("Failed to send SMS to [User]:" + dest + ". [Message]:" + msg + ". [Exception]:" + (e.getMessage() != null ? e.getMessage() : e));
                }
            }
        }.start();

    }

    private static void sendSmsGET(String getData) throws Exception {
        DefaultHttpClient httpClient = new DefaultHttpClient();
        HttpResponse response;
        HttpEntity entity;
        String responseString = null;
        HttpGet httpGet = new HttpGet(getData);
        response = httpClient.execute(httpGet);
        entity = response.getEntity();
        if (entity != null) {
            responseString = EntityUtils.toString(response.getEntity());
        }
        int statusCode = response.getStatusLine().getStatusCode();
        if (statusCode != HttpStatus.SC_OK) {
            String reason = response.getStatusLine().getReasonPhrase();
            throw new Exception("SMS GET failed. [Status code]:" + statusCode + ". [Reason]:" + reason);
        }

        Document resXml = XmlUtils.loadXMLFromString(responseString);
        String status = resXml.getElementsByTagName("status").item(0).getTextContent();
        String messageId = resXml.getElementsByTagName("messageid").item(0).getTextContent();

        if(!status.equals("0")) {
            System.out.println("SMS GET failed. [Response String]:" + responseString);
            throw new Exception("SMS GET failed. [Status]:" + status + ". [Message Id]:" + messageId);
        }

        _logger.info("SMS GET succeeded. [Message Id]:" + messageId);
    }
}
