package com.client;

import android.content.Context;
import android.util.Log;

import com.data.objects.DefaultMediaData;
import com.data.objects.User;
import com.enums.SpecialMediaType;
import com.google.gson.reflect.TypeToken;
import com.handlers.background_broadcast_receiver.EventLoadingTimeoutHandler;
import com.model.request.GetDefaultMediaDataRequest;
import com.model.request.Request;
import com.model.response.Response;
import com.utils.RequestUtils;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.List;

import cz.msebera.android.httpclient.HttpStatus;

import static com.crashlytics.android.Crashlytics.log;

/**
 * Created by Mor on 24/05/2017.
 */

class MediaClientImpl extends BaseClient implements MediaClient {

    private static final String TAG = MediaClientImpl.class.getSimpleName();

    private static final String requestUrl = ROOT_URL + "/v1/GetDefaultMediaData";

    private static final Type responseType = new TypeToken<Response<List<DefaultMediaData>>>() {}.getType();

    public MediaClientImpl(Context context) {
        super(context);
    }

    @Override
    public List<DefaultMediaData> getDefaultMediaData(String phoneNumber, SpecialMediaType specialMediaType) {
        Response<List<DefaultMediaData>> response = null;
        GetDefaultMediaDataRequest request = new GetDefaultMediaDataRequest(RequestUtils.getDefaultRequest(context));
        request.setPhoneNumber(phoneNumber);
        request.setSpecialMediaType(specialMediaType);

        ConnectionToServer connectionToServer = new ConnectionToServer();
        connectionToServer.setResponseType(responseType);

        try {
            int responseCode = connectionToServer.sendRequest(requestUrl, request);
            if (responseCode == HttpStatus.SC_OK) {
                response = connectionToServer.readResponse();
            }
            else {
                log(Log.ERROR, TAG, "Failed to get default media data for number:" + phoneNumber + " SpecialMediaType:" + specialMediaType + ". Response code was:" + responseCode);
                EventLoadingTimeoutHandler timeoutHandler = new EventLoadingTimeoutHandler();
                timeoutHandler.handle(context);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
        return response != null ? response.getResult() : null;
    }
}
