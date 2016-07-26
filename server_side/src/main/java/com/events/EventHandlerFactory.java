package com.events;


/**
 * Created by Mor on 25/07/2016.
 */
public interface EventHandlerFactory {
    EventHandler getEventHandler(EventType eventType);
}
