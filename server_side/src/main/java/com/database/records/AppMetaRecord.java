package com.database.records;

import java.io.Serializable;

/**
 * Created by Mor on 26/03/2016.
 */
public class AppMetaRecord implements Serializable {

    private double _minSupportedVersion;

    public AppMetaRecord(double _minSupportedVersion) {
        this._minSupportedVersion = _minSupportedVersion;
    }

    public double get_minSupportedVersion() {
        return _minSupportedVersion;
    }

    @Override
    public String toString() {

        return "[MinSupportedVersion]:" + _minSupportedVersion;
    }
}
