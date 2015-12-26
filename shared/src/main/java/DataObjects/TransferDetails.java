package DataObjects;

import java.io.IOException;
import java.io.Serializable;
import FilesManager.FileManager;

public class TransferDetails implements Serializable {

	private static final long serialVersionUID = 7408472793374531808L;
    private int _commId;
	private String _sourceId;
	private String _destinationId;
	private String _extension;
	private long _fileSize;
	private FileManager.FileType _fileType;
	private String _filePathOnServer;
    private String _fullFilePathSrcSD;
    private FileManager _managedFile;

    public TransferDetails(String source, String destination, FileManager managedFile) {

        _sourceId = source;
        _destinationId = destination;
        _extension = managedFile.getFileExtension();
        _fileSize = managedFile.getFileSize();
        _fileType = managedFile.getFileType();
        _fullFilePathSrcSD = managedFile.getFileFullPath();
        _managedFile = managedFile;

    }

    public String get_fullFilePathSrcSD() {
        return _fullFilePathSrcSD;
    }

    public String getSourceId() { return _sourceId; }

    public String getDestinationId() {
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
}
