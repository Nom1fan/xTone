package com.server.database.dbos;

import java.util.Date;

import DataObjects.UserStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * Created by Mor on 01/04/2016.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class UserDBO {

    private String uid;
    private String token;
    private Date registered_date;
    private UserStatus userStatus;
    private Date unregistered_date;
    private int unregistered_count;
    private String deviceModel;
    private String androidVersion;
}
