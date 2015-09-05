package DataObjects;

import java.io.Serializable;
import FilesManager.FileManager;

public class TransferDetails implements Serializable {

		private static final long serialVersionUID = 7408472793374531808L;
		private String _sourceId;
		private String _destinationId;		
		private String _extension;
		private double _fileSize;
		private FileManager.FileType _fileType;
		
		public TransferDetails(String source, String destination, 
				long fileSize, String extension, FileManager.FileType fileType) {
			
			_sourceId = source;
			_destinationId = destination;	
			_extension = extension;
			_fileSize = fileSize;
			_fileType = fileType;
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

}
