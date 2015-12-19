package com.server_side;


import com.almworks.sqlite4java.SQLiteException;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.logging.Logger;
import DataObjects.SharedConstants;
import LogObjects.LogsManager;
import MessagesToServer.MessageToServer;
import ServerObjects.AbstractServer;
import ServerObjects.ClientsManager;
import ServerObjects.ConnectionToClient;

/**
 * Created by Mor on 23/09/2015.
 */
public class LogicServer extends GenericServer {

    private static Logger _logger = null;

    public LogicServer(String serverName, int port) {
        super(serverName, port);

        try {
            ClientsManager.initialize(initDbDAL());
        } catch (SQLiteException e) {
            e.printStackTrace();
        }

    }

    /* LogicServer private methods */

    private SQLiteDAL1 initDbDAL() throws SQLiteException {

        // Initializing General Database
        SQLiteDAL1 dal = new SQLiteDAL1(Paths.get("").toAbsolutePath().toString() + SQLiteDAL1.GENERAL_DB_PATH);
        // Creating tables
        dal.createTable(SQLiteDAL1.TABLE_UID2TOKEN, SQLiteDAL1.COL_UID, SQLiteDAL1.COL_TOKEN);

        return dal;
    }
}
