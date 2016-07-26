package com.server.servers;

import com.events.Event;
import com.events.EventGenerator;
import com.events.EventType;
import com.server.spring.SpringConfig;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Created by Mor on 16/10/2015.
 */
@Component
public class ServerRunnerImpl implements ServerRunner {

    private static final String TAG = ServerRunnerImpl.class.getCanonicalName();

    @Autowired
    @Qualifier("LogicServer")
    private GenericServer logicServer;

    @Autowired
    @Qualifier("StorageServer")
    private GenericServer storageServer;

    @Autowired
    private EventGenerator eventGenerator;

    private void _runServer() {
        logicServer.runServer();
        storageServer.runServer();

        reportServerStatus();
    }

    private void _stopServer() {
        try {
            logicServer.close();
            storageServer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        reportServerStatus();
    }

    @Override
    public void runServer() {
        new Thread() {
            @Override
            public void run() {
                _runServer();
            }
        }.start();
    }

    @Override
    public void stopServer() {
        new Thread() {
            @Override
            public void run() {
                _stopServer();
            }
        }.start();
    }

    private void reportServerStatus() {
        if(isServerRunning())
            eventGenerator.fireEvent(new Event(TAG, EventType.CONNECTED));
        else
            eventGenerator.fireEvent(new Event(TAG, EventType.DISCONNECTED));
    }

    @Override
    public boolean isServerRunning() {
        return logicServer.isListening() && storageServer.isListening();
    }

    @Override
    public boolean isServerStopped() {
        return !logicServer.isListening() && !storageServer.isListening();
    }

    @Override
    public GenericServer getStorageServer() {
        return storageServer;
    }

    @Override
    public GenericServer getLogicServer() {
        return logicServer;
    }

    public static void main(String[] args) {

        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(SpringConfig.class);
        context.getBean(ServerRunnerImpl.class);
    }
}
