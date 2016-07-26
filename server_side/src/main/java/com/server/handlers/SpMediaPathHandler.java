package com.server.handlers;

import java.util.Map;

import DataObjects.DataKeys;
import DataObjects.SpecialMediaType;

/**
 * Created by Mor on 25/07/2016.
 */
public interface SpMediaPathHandler {
    StringBuilder appendPathForMedia(Map<DataKeys, Object> data);
    SpecialMediaType getHandledSpMediaType();
}
