package com.event;

import java.io.Serializable;
import java.util.EventObject;

public class Event extends EventObject implements Serializable {
	

	public static String EVENT_ACTION = "com.android.mediacallz.EVENT_ACTION";
	public static String SP_CALL_EVENT_ACTION = "com.android.mediacallz.SP_CALL_EVENT_ACTION";
	public static String EVENT_REPORT = "EVENT_REPORT";
	private static final long serialVersionUID = -226097840659292994L;
	private EventReport _report;
	 
	
	public Event(Object source, EventReport report) {
		super(source);
		_report = report;
		
	}
	
	public EventReport report() {
		return _report;
	}
	
	

}
