package com.database;

import log.Logged;

/**
 * Created by Mor on 26/03/2016.
 */
public abstract class DALAccesible extends Logged {

    protected IDAO _dal;

    public DALAccesible(IDAO dal) {
        super();

        this._dal = dal;
    }

}
