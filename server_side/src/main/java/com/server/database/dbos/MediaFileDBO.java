package com.server.database.dbos;

import lombok.Data;

/**
 * Created by Mor on 27/09/2016.
 */
@Data
public class MediaFileDBO {

    String md5;
    String content_ext;
    long content_size;
    int transfer_count;
    int call_count;


    public MediaFileDBO(String md5, String fileExtension, long fileSize) {
        this.md5 = md5;
        content_ext = fileExtension;
        content_size = fileSize;
        transfer_count = 0;
    }
}
