package com.client;

import android.content.Context;

import com.data.objects.DefaultMediaDataContainer;
import com.enums.SpecialMediaType;
import com.google.gson.reflect.TypeToken;
import com.handlers.background_broadcast_receiver.EventLoadingTimeoutHandler;
import com.logger.Logger;
import com.logger.LoggerFactory;
import com.model.request.GetDefaultMediaDataRequest;
import com.model.response.Response;
import com.utils.ContactsUtils;
import com.utils.RequestUtils;
import com.utils.RequestUtilsImpl;
import com.utils.UtilityFactory;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.List;

import cz.msebera.android.httpclient.HttpStatus;

import static com.crashlytics.android.Crashlytics.log;

/**
 * Created by Mor on 24/05/2017.
 */

public class DefaultMediaClientImpl implements DefaultMediaClient {

    private static final String TAG = DefaultMediaClientImpl.class.getSimpleName();

    private static final String requestUrl = ROOT_URL + "/v1/GetDefaultMediaData";

    private static final Type responseType = new TypeToken<Response<List<DefaultMediaDataContainer>>>() {}.getType();

    private final RequestUtils requestUtils = UtilityFactory.instance().getUtility(RequestUtils.class);
    
    private Logger logger = LoggerFactory.getLogger();

    @Override
    public List<DefaultMediaDataContainer> getDefaultMediaData(Context context, List<String> uids, SpecialMediaType specialMediaType) {
        Response<List<DefaultMediaDataContainer>> response = null;
        GetDefaultMediaDataRequest request = new GetDefaultMediaDataRequest(requestUtils.getDefaultRequest(context));

        request.setContactUids(uids);
        request.setSpecialMediaType(specialMediaType);

        ConnectionToServerImpl connectionToServer = new ConnectionToServerImpl();
        connectionToServer.setResponseType(responseType);

        try {
            int responseCode = connectionToServer.sendRequest(requestUrl, request);
            if (responseCode == HttpStatus.SC_OK) {
                response = connectionToServer.readResponse();
            }
            else {
                logger.error(TAG, "Failed to get default media data. SpecialMediaType:" + specialMediaType + ". Response code was:" + responseCode);
                EventLoadingTimeoutHandler timeoutHandler = new EventLoadingTimeoutHandler();
                timeoutHandler.handle(context);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
        return response != null ? response.getResult() : null;
    }
}
