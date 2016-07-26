package com.server.database;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Created by Mor on 28/03/2016.
 */
@Component
public class DaoFactoryImpl implements DaoFactory {

    @Autowired
    private DAO dao;

    @Override
    public DAO getDAO() {
        return dao;
    }
}
