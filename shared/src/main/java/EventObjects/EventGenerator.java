package EventObjects;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Enables registering to events and receiving them when fired.
 * @author Mor
 */
public class EventGenerator implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5379262529215003671L;
	private EventReport _report;
    private List<EventListener> _listeners = new ArrayList<EventListener>();
    
    public synchronized void fireEvent(EventReport report) {    	  
	      _report = report;
	      _fireEvent();
    }
    

    public synchronized void addEventListener( EventListener l ) {
        _listeners.add( l );
    }
    
    public synchronized void removeEventListener( EventListener l ) {
        _listeners.remove( l );
    }
     
    private synchronized void _fireEvent() {
        Event event = new Event( this, _report );
        Iterator<EventListener> listeners = _listeners.iterator();
        while( listeners.hasNext() ) {
            ( (EventListener) listeners.next() ).eventReceived( event );
        }
    }
}
	

