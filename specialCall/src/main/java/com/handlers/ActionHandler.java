package com.handlers;

import android.content.Context;
import android.content.Intent;

import com.client.ConnectionToServer;
import com.data.objects.Constants;
import com.model.request.Request;

import java.io.IOException;

/**
 * Created by Mor on 20/12/2016.
 */
public interface ActionHandler {

    String HOST = Constants.SERVER_HOST;
    int PORT = Constants.SERVER_PORT;
    String ROOT_URL = "http://" + HOST + ":" + PORT;

    void handleAction(ActionBundle actionBundle) throws IOException;

    class ActionBundle {
        private Context ctx;
        private Intent intent;
        private Request request;
        private ConnectionToServer connectionToServer;

        public Context getCtx() {
            return ctx;
        }

        public ActionBundle setCtx(Context ctx) {
            this.ctx = ctx;
            return this;
        }

        public Intent getIntent() {
            return intent;
        }

        public ActionBundle setIntent(Intent intent) {
            this.intent = intent;
            return this;
        }

        public Request getRequest() {
            return request;
        }

        public ActionBundle setRequest(Request request) {
            this.request = request;
            return this;
        }

        public ConnectionToServer getConnectionToServer() {
            return connectionToServer;
        }

        public ActionBundle setConnectionToServer(ConnectionToServer connectionToServer) {
            this.connectionToServer = connectionToServer;
            return this;
        }
    }
}
