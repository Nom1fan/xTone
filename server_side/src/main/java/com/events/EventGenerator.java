package com.events;

/**
 * Created by Mor on 25/07/2016.
 */
public interface EventGenerator {
    void register(EventsListener listener);
    void unregister(EventsListener listener);
    void fireEvent(Event event);
}
