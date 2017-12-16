package com_international.data.objects;

import com_international.enums.SpecialMediaType;

import java.io.Serializable;
import java.util.List;

/**
 * Created by Mor on 10/06/2017.
 */

public class DefaultMediaDataContainer implements Serializable {

    private String uid;

    List<DefaultMediaData> defaultMediaDataList;

    SpecialMediaType specialMediaType;

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public List<DefaultMediaData> getDefaultMediaDataList() {
        return defaultMediaDataList;
    }

    public void setDefaultMediaDataList(List<DefaultMediaData> defaultMediaDataList) {
        this.defaultMediaDataList = defaultMediaDataList;
    }

    public SpecialMediaType getSpecialMediaType() {
        return specialMediaType;
    }

    public void setSpecialMediaType(SpecialMediaType specialMediaType) {
        this.specialMediaType = specialMediaType;
    }

    @Override
    public String toString() {
        return "DefaultMediaDataContainer{" +
                "uid='" + uid + '\'' +
                ", defaultMediaDataList=" + defaultMediaDataList +
                ", specialMediaType=" + specialMediaType +
                '}';
    }
}
