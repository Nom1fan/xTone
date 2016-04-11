package DataObjects;

import java.io.Serializable;

import FilesManager.FileManager;

public class TransferDetails implements Serializable {

    private static final long serialVersionUID = 7408472793374531808L;
    private int _commId;
    private String _sourceId;
    private String _destinationId;
    private String _destinationContactName;
    private String _extension;
    private long _fileSize;
    private FileManager.FileType _fileType;
    private String _filePathOnServer;
    private String _fullFilePathSrcSD;
    private FileManager _managedFile;
    private SpecialMediaType _spMediaType;
    private String _md5;

    /**
     * This constructor should be used in a message to transfer media
     * @param source The media sender id
     * @param destination The media receiver id
     * @param managedFile The managed file descriptor of the media being sent
     * @param spMediaType The special media type
     */
    public TransferDetails(String source, String destination, String destinationContactName ,FileManager managedFile, SpecialMediaType spMediaType) {

        _sourceId = source;
        _destinationId = destination;
        _destinationContactName = destinationContactName;
        _extension = managedFile.getFileExtension();
        _fileSize = managedFile.getFileSize();
        _fileType = managedFile.getFileType();
        _fullFilePathSrcSD = managedFile.getFileFullPath();
        _md5 = FileManager.getMD5(_fullFilePathSrcSD);
        _managedFile = managedFile;
        _spMediaType = spMediaType;

    }

    /**
     * This constructor should be used in a message to clear media
     * @param source The media clearer id
     * @param destination The id of the user for which the media shall be cleared
     * @param spMediaType The special media type fo clear
     */
    public TransferDetails(String source, String destination, SpecialMediaType spMediaType) {

        _sourceId = source;
        _destinationId = destination;
        _spMediaType = spMediaType;
    }

    public String get_fullFilePathSrcSD() {
        return _fullFilePathSrcSD;
    }

    public String getSourceId() { return _sourceId; }

    public String getDestinationId() {

        if(_destinationId==null)
            _destinationId = "";

        return _destinationId;
    }

    public long getFileSize() {
        return _fileSize;
    }

    public String getSourceWithExtension() {
        return _sourceId+"."+_extension;
    }

    public String getExtension() {
        return _extension;
    }

    public FileManager.FileType getFileType() { return _fileType; }

    public String get_filePathOnServer() {
        return _filePathOnServer;
    }

    public void set_filePathOnServer(String _filePathOnServer) {
        this._filePathOnServer = _filePathOnServer;
    }

    public FileManager get_managedFile() {
        return _managedFile;
    }

    public int get_commId() {
        return _commId;
    }

    public void set_commId(int _commId) {
        this._commId = _commId;
    }

    public SpecialMediaType getSpMediaType() {
        return _spMediaType;
    }

    public String getMd5() {
        return _md5;
    }

    @Override
    public String toString() {

        StringBuilder str = new StringBuilder();

        str.
                append("[Source]:").append(_sourceId).
                append(", [Destination]:").append(_destinationId).
                append(", [Special Media Type]:").append(_spMediaType);

        if(_extension!=null) str.append(", [Extension]:").append(_extension);
        if(_fileSize!=0) str.append(", [FileSize]:").append(_fileSize);
        if(_fileType!=null) str.append(", [FileType]:").append(_fileType);
        if(_fullFilePathSrcSD!=null) str.append(", [File Full Path]:").append(_fullFilePathSrcSD);
        if(_md5!=null) str.append(", [Md5]:").append(_md5);
        if(_managedFile!=null) str.append(", [Managed File]:").append(_managedFile);
        if(_spMediaType!=null) str.append(", [Special Media Type]:").append(_spMediaType);

        return str.toString();
    }

}
