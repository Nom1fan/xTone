package com_international.client;

import com_international.logger.Logger;
import com_international.logger.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by Mor on 24/05/2017.
 */

public class ClientFactory {

    private static final String TAG = ClientFactory.class.getSimpleName();

    private static Logger logger = LoggerFactory.getLogger();

    private static ClientFactory instance;

    private static Map<Class<? extends Client>, Class<? extends Client>> class2ObjectMap;

    private ClientFactory() {
        class2ObjectMap = new HashMap<Class<? extends Client>, Class<? extends Client>> () {{
            put(DefaultMediaClient.class, DefaultMediaClientImpl.class);
            put(UsersClient.class, UsersClientImpl.class);
        }};
    }

    public static ClientFactory getInstance() {
        if(instance == null) {
            instance = new ClientFactory();
        }
        return instance;
    }

    public <T extends Client> T getClient(Class<? extends Client> interfaceClass) {
        T result = null;
        try {
            Class<? extends Client> clientClass = class2ObjectMap.get(interfaceClass);
            if(clientClass == null) {
                logger.warn(TAG, "Unable to find client for of interface:" + interfaceClass);
                return null;
            }
            result = (T) clientClass.newInstance();


        } catch (InstantiationException | IllegalAccessException e) {
            e.printStackTrace();
        }
        return result;
    }
}
