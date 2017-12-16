package com_international.data.objects;

import com_international.model.push.AbstractPushData;

/**
 * Created by Mor on 1/3/2017.
 */

public class PushNotificationData extends AbstractPushData {

   private String htmlString;

    public String getHtmlString() {
        return htmlString;
    }

    public void setHtmlString(String htmlString) {
        this.htmlString = htmlString;
    }
}
