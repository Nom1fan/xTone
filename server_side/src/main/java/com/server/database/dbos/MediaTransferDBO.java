package com.server.database.dbos;


import java.util.Date;

import DataObjects.SpecialMediaType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;

/**
 * Created by Mor on 31/03/2016.
 */
@Data
@AllArgsConstructor
@RequiredArgsConstructor
public class MediaTransferDBO {

    int transfer_id;
    final SpecialMediaType type;
    final String md5;
    final String uid_src;
    final String uid_dest;
    final Date datetime;
    boolean transfer_success;
    Date transfer_datetime;
}
