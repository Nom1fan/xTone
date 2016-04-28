package MessagesToClient;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import DataObjects.DataKeys;

/**
 * Abstract message to the client, containing information and enables generic interface for client actions corresponding to the message
 * @author Mor
 *
 */
public class MessageToClient implements Serializable {

	protected HashMap<DataKeys, Object> _data;
	protected ClientActionType _clientActionType;

	public MessageToClient(ClientActionType clientActionType) {

		_clientActionType = clientActionType;
	}

	public MessageToClient(ClientActionType clientActionType, HashMap<DataKeys, Object> data) {

		_data = data;
		_clientActionType = clientActionType;
	}

	public Map getData() {
		return _data;
	}

	public ClientActionType getActionType() {
		return _clientActionType;
	}

}
