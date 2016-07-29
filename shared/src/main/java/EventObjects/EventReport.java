package EventObjects;

import java.io.Serializable;

public class EventReport implements Serializable {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -224696528176497072L;
	private EventType _status;
	private String _desc;
	private Object _data;
	
	public EventReport(EventType status, String desc, Object data) {
		_status = status;
		_desc = desc;
		_data = data;
		
	}

	public EventReport(EventType status, Object data) {
		_status = status;
		_data = data;
	}

	public EventReport(EventType status, String desc) {
		this._desc = desc;
		this._status = status;
	}

    public EventReport(EventType status) {
        this._status = status;
    }

    public EventType status() {
		return _status;
	}
	
	public String desc() {
		return _desc;
	}
	
	public Object data() {
		return _data;
	}

	@Override
	public String toString() {

		String str;
		if(_desc!=null)
			str = String.format("[EventType]: %s [EventDesc]: %s [EventData]: %s", _status, _desc, _desc);
		else
			str = String.format("[EventType]: %s [EventData]: %s", _status, _data);
		return str;
	}

	public void set_data(String _data) {
		this._data = _data;
	}
}
