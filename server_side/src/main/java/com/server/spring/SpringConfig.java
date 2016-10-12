package com.server.spring;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mchange.v2.c3p0.ComboPooledDataSource;
import com.server.actions.ServerActionFactory;
import com.server.database.Dao;
import com.server.data.ServerConstants;
import com.server.servers.GenericServer;
import com.ui.MainFrame;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ServiceLocatorFactoryBean;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import DataObjects.SharedConstants;
import Exceptions.NoSuchActionException;
import LogObjects.LogsManager;

/**
 * Created by Mor on 25/07/2016.
 */
@Configuration
@ComponentScan(basePackages = "com")
public class SpringConfig {

    @Autowired
    private Dao dao;

    @Bean
    public ServiceLocatorFactoryBean getServiceLocatorFactoryBean()
    {
        ServiceLocatorFactoryBean bean = new ServiceLocatorFactoryBean();
        bean.setServiceLocatorInterface(ServerActionFactory.class);
        bean.setServiceLocatorExceptionClass(NoSuchActionException.class);
        return bean;
    }

    @Bean
    public ServerActionFactory getServerActionFactory()
    {
        return (ServerActionFactory) getServiceLocatorFactoryBean().getObject();
    }

    @Bean
    ComboPooledDataSource getDataSource() {
        ComboPooledDataSource dataSource = new ComboPooledDataSource();
        String jdbcUrl = "jdbc:mysql://" + ServerConstants.DB_SERVER_HOST + ":" + ServerConstants.DB_SERVER_PORT + "/sys";
        dataSource.setJdbcUrl(jdbcUrl);
//        dataSource.setTestConnectionOnCheckin(true);
        dataSource.setUser(ServerConstants.DB_SERVER_USER);
        dataSource.setPassword(ServerConstants.DB_SERVER_PWD);
        return dataSource;
    }

    @Bean
    public Logger logger() {
        try {
            LogsManager.createServerLogsDir();
            //LogsManager.clearLogs();
        } catch (IOException e) {
            e.printStackTrace();
        }

        logger = LogsManager.get_serverLogger(logLevel);
        return logger;
    }

    @Bean
    public Gson gson() {
        GsonBuilder gsonBuilder = new GsonBuilder();
        return gsonBuilder.create();
    }

    @Bean(name = "LogicServer")
    public GenericServer logicServer() {
        GenericServer server = new GenericServer("LogicServer", SharedConstants.LOGIC_SERVER_PORT);
        server.setServerActionFactory(getServerActionFactory());
        server.setDao(dao);
        server.setLogger(logger());

        return server;
    }

    @Bean(name = "StorageServer")
    public GenericServer storageServer() {
        GenericServer server = new GenericServer("StorageServer", SharedConstants.STORAGE_SERVER_PORT);
        server.setServerActionFactory(getServerActionFactory());
        server.setDao(dao);
        server.setLogger(logger());

        return server;
    }


    private static Map<String,Level> logLevelsMap = new HashMap<String,Level>() {{
        put("DEBUG", Level.CONFIG);
        put("CONFIG", Level.CONFIG);
        put("INFO", Level.INFO);
    }};
    private static Logger logger;
    private static Level logLevel = Level.INFO;

    public static void main(String[] args) {
        if(args.length > 0)
            setLogLevel(args[0]);

        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(SpringConfig.class);
        context.getBean(MainFrame.class);

        logger.info("Log level:" + logger.getLevel());
    }

    private static void setLogLevel(String logLevel) {
        SpringConfig.logLevel = logLevelsMap.get(logLevel.toUpperCase());
    }
}
