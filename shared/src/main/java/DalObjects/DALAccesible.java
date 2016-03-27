package DalObjects;

import log.Logged;

/**
 * Created by Mor on 26/03/2016.
 */
public abstract class DALAccesible extends Logged {

    protected IDAL _dal;

    public DALAccesible(IDAL dal) {
        super();

        this._dal = dal;
    }

}
