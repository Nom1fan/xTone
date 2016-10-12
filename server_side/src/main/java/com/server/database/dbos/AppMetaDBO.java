package com.server.database.dbos;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.Serializable;

/**
 * Created by Mor on 26/03/2016.
 */
@Data
@AllArgsConstructor
public class AppMetaDBO implements Serializable {

    private double last_supported_version;
}
