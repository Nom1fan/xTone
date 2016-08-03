package com.server.spring;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.server.actions.ActionFactory;
import com.server.database.DAO;
import com.server.database.DaoFactory;
import com.server.servers.GenericServer;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.util.logging.Logger;

import DataObjects.SharedConstants;
import LogObjects.LogsManager;

/**
 * Created by Mor on 25/07/2016.
 */
@Configuration
@ComponentScan(basePackages = "com")
public class SpringConfig {

    @Autowired
    private ActionFactory actionFactory;

    @Autowired
    private DaoFactory daoFactory;

    @Autowired
    private DAO dao;


    @Bean
    public Logger logger() {
        try {
            LogsManager.createServerLogsDir();
            //LogsManager.clearLogs();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return LogsManager.get_serverLogger();
    }

    @Bean
    public Gson gson() {
        GsonBuilder gsonBuilder = new GsonBuilder();
        return gsonBuilder.create();
    }

    @Bean(name = "LogicServer")
    public GenericServer logicServer() {
        GenericServer server = new GenericServer("LogicServer", SharedConstants.LOGIC_SERVER_PORT);
        server.setActionFactory(actionFactory);
        server.setDao(dao);
        server.setLogger(logger());

        return server;
    }

    @Bean(name = "StorageServer")
    public GenericServer storageServer() {
        GenericServer server = new GenericServer("StorageServer", SharedConstants.STORAGE_SERVER_PORT);
        server.setActionFactory(actionFactory);
        server.setDao(dao);
        server.setLogger(logger());

        return server;
    }
}
