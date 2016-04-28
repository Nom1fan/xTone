package com.database.records;

import java.io.Serializable;

/**
 * Created by Mor on 26/03/2016.
 */
public class AppMetaRecord implements Serializable {

    private double _appVersion;
    private double _minSupportedVersion;

    public AppMetaRecord(double _appVersion, double _minSupportedVersion) {
        this._appVersion = _appVersion;
        this._minSupportedVersion = _minSupportedVersion;
    }

    public double get_appVersion() {
        return _appVersion;
    }

    public double get_minSupportedVersion() {
        return _minSupportedVersion;
    }

    @Override
    public String toString() {

        return "[App Version]:" + _appVersion + " [MinSupportedVersion]:" + _minSupportedVersion;
    }
}
