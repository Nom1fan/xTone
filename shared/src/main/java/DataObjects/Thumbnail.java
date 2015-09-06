package DataObjects;

import FilesManager.FileManager;

/**
 * Created by Mor on 05/09/2015.
 */
public class Thumbnail {

    private FileManager.FileType _fileType;
    private String _thumbPath;

    public Thumbnail(FileManager.FileType fileType, String thumbPath) {

        _fileType = fileType;
        _thumbPath = thumbPath;
    }

    public FileManager.FileType getFileType() {
        return _fileType;
    }

    public String getThumbPath() {
        return _thumbPath;
    }


}
