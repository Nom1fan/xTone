package DataObjects;

import java.io.IOException;
import java.io.Serializable;
import FilesManager.FileManager;

public class TransferDetails implements Serializable {

	private static final long serialVersionUID = 7408472793374531808L;
	private String _sourceId;
	private String _destinationId;
	private String _extension;
	private double _fileSize;
	private FileManager.FileType _fileType;
	private String _filePathOnServer;
    private String _fullFilePathSrcSD;

    public TransferDetails(String source, String destination, FileManager managedFile) throws IOException {

        _sourceId = source;
        _destinationId = destination;
        _extension = managedFile.getFileExtension();
        _fileSize = managedFile.getFileSize();
        _fileType = managedFile.getFileType();
        _fullFilePathSrcSD = managedFile.getFileFullPath();

    }

    public String get_fullFilePathSrcSD() {
        return _fullFilePathSrcSD;
    }

    public String getSourceId() { return _sourceId; }

    public String getDestinationId() {
        return _destinationId;
    }

    public double getFileSize() {
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
}
