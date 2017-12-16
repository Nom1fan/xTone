package com_international.handlers.server_proxy_service;

import android.content.Context;
import android.util.Log;

import com_international.app.AppStateManager;
import com_international.client.ConnectionToServerImpl;
import com_international.data.objects.Constants;
import com_international.data.objects.SnackbarData;
import com_international.enums.SpecialMediaType;
import com_international.event.EventReport;
import com_international.event.EventType;
import com.google.gson.reflect.TypeToken;
import com_international.handlers.ActionHandler;
import com_international.model.request.ClearMediaRequest;
import com_international.data.objects.AppMeta;
import com_international.model.response.Response;
import com_international.utils.BroadcastUtils;
import com_international.utils.ContactsUtils;
import com_international.utils.UI_Utils;
import com_international.utils.UtilityFactory;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Locale;

import cz.msebera.android.httpclient.HttpStatus;

import static com.crashlytics.android.Crashlytics.log;
import static com_international.services.ServerProxyService.DESTINATION_ID;
import static com_international.services.ServerProxyService.SPECIAL_MEDIA_TYPE;

/**
 * Created by Mor on 20/12/2016.
 */
public class ClearMediaActionHandler implements ActionHandler {

    private static final String URL_CLEAR_MEDIA = ROOT_URL + "/v1/ClearMedia";
    private static final String TAG = ClearMediaActionHandler.class.getSimpleName();
    private static final Type responseType = new TypeToken<Response<AppMeta>>() {
    }.getType();

    private final ContactsUtils contactsUtils = UtilityFactory.instance().getUtility(ContactsUtils.class);


    @Override
    public void handleAction(ActionBundle actionBundle) throws IOException {
        Context ctx = actionBundle.getCtx();
        ConnectionToServerImpl connectionToServer = actionBundle.getConnectionToServer();
        connectionToServer.setResponseType(responseType);

        String destId = actionBundle.getIntent().getStringExtra(DESTINATION_ID);
        SpecialMediaType specialMediaType = (SpecialMediaType) actionBundle.getIntent().getSerializableExtra(SPECIAL_MEDIA_TYPE);

        ClearMediaRequest request = new ClearMediaRequest(actionBundle.getRequest());
        request.setLocale(Locale.getDefault().getLanguage());
        request.setDestinationId(destId);
        request.setSourceId(Constants.MY_ID(ctx));
        request.setSpecialMediaType(specialMediaType);
        request.setDestinationContactName(contactsUtils.getContactName(ctx, destId));

        int responseCode = connectionToServer.sendRequest(URL_CLEAR_MEDIA, request);
        if(responseCode == HttpStatus.SC_OK) {
            AppStateManager.setAppState(ctx, TAG, AppStateManager.STATE_READY);
            UI_Utils.refreshUI(ctx, new SnackbarData(SnackbarData.SnackbarStatus.CLOSE));
        }
        else {
            log(Log.ERROR, TAG, "Clear media failed. [Response code]:" + responseCode);
            BroadcastUtils.sendEventReportBroadcast(ctx, TAG, new EventReport(EventType.CLEAR_FAILURE, destId, specialMediaType));
        }

    }
}
