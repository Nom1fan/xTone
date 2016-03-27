package DataObjects;

import java.io.Serializable;

import FilesManager.FileManager;

/**
 * Created by Mor on 10/03/2016.
 */
public class CallRecord implements Serializable {

    private static final long serialVersionUID = 7408472793374531808L;
    private static final String TAG = CallRecord.class.getSimpleName();

    private String _sourceId;
    private String _destinationId;

    private FileManager _visualMediaFile;
    private FileManager _audioMediaFile;

    private String _visualMd5;
    private String _audioMd5;

    private SpecialMediaType _spMediaType;

    public CallRecord(String source,
                      String destination,
                      FileManager visualMediaFile,
                      String visualM5,
                      FileManager audioMediaFile,
                      String audioMd5,
                      SpecialMediaType spMediaType) {

        _sourceId = source;
        _destinationId = destination;

        _visualMediaFile = visualMediaFile;
        _audioMediaFile = audioMediaFile;

        if(visualM5!=null)
            _visualMd5 = visualM5;

        if(audioMd5!=null)
            _audioMd5 = audioMd5;

        _spMediaType = spMediaType;

        System.out.println("I/" + TAG + ": CallRecord:" + this.toString());
    }

    @Override
    public String toString() {

        StringBuilder builder = new StringBuilder();
        builder.
                append(", [Source]:").append(_sourceId).
                append(", [Destination]:").append(_destinationId).
                append(", [Special Media Type]:").append(_spMediaType.toString());

                if(_visualMediaFile!=null) {
                    builder.append(", [Visual Media File]:").append(_visualMediaFile);
                    builder.append(", [visual_md5]:").append(_visualMd5);
                }
                if (_audioMediaFile !=null) {
                    builder.append(", [Audio Media File]:").append(_audioMediaFile);
                    builder.append(", [audio_md5]:").append(_audioMd5);
                }

        return builder.toString();
    }

    public SpecialMediaType get_spMediaType() {
        return _spMediaType;
    }

    public FileManager get_visualMediaFile() {
        return _visualMediaFile;
    }

    public String get_destinationId() {
        return _destinationId;
    }

    public String get_sourceId() {
        return _sourceId;
    }

    public FileManager get_audioMediaFile() {
        return _audioMediaFile;
    }

    public String get_visualMd5() {
        return _visualMd5;
    }

    public String get_audioMd5() {
        return _audioMd5;
    }
}
