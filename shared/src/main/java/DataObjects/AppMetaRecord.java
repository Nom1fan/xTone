package DataObjects;

import java.io.Serializable;

/**
 * Created by Mor on 26/03/2016.
 */
public class AppMetaRecord implements Serializable {

    private double _appVersion;
    private double _lastSupportedVersion;

    public AppMetaRecord(double _appVersion, double _lastSupportedVersion) {
        this._appVersion = _appVersion;
        this._lastSupportedVersion = _lastSupportedVersion;
    }

    public double get_appVersion() {
        return _appVersion;
    }

    public double get_lastSupportedVersion() {
        return _lastSupportedVersion;
    }

}
