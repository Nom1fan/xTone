package com.client;

import android.content.Context;

import com.dao.DAO;
import com.dao.MediaDAO;
import com.dao.MediaDAOImpl;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by Mor on 24/05/2017.
 */

public abstract class ClientFactory {

    private static Map<Class<? extends Client>, Client> class2ObjectMap = new HashMap<Class<? extends Client>, Client>() {{
        put(DefaultMediaClient.class, new DefaultMediaClientImpl());
    }};

    public static <T extends Client> T getClient(Class<? extends Client> clientClass) {
        return (T) class2ObjectMap.get(clientClass);
    }
}
