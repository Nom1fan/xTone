package com_international.client;

import com_international.data.objects.Constants;

/**
 * Created by Mor on 24/05/2017.
 */

public interface Client {

    String HOST = Constants.SERVER_HOST;
    int PORT = Constants.SERVER_PORT;
    String ROOT_URL = "http://" + HOST + ":" + PORT;
}
