package DataObjects;

import java.io.Serializable;

import FilesManager.FileManager;

/**
 * Created by Mor on 10/03/2016.
 */
public class CallRecord implements Serializable {

    private static final long serialVersionUID = 7408472793374531808L;
    private static final String TAG = CallRecord.class.getSimpleName();

    private String sourceId;
    private String destinationId;

    private FileManager visualMediaFile;
    private FileManager audioMediaFile;

    private String visualMd5;
    private String audioMd5;

    private SpecialMediaType spMediaType;

    public CallRecord(String source,
                      String destination,
                      FileManager visualMediaFile,
                      String visualM5,
                      FileManager audioMediaFile,
                      String audioMd5,
                      SpecialMediaType spMediaType) {

        sourceId = source;
        destinationId = destination;

        this.visualMediaFile = visualMediaFile;
        this.audioMediaFile = audioMediaFile;

        if(visualM5!=null)
            visualMd5 = visualM5;

        if(audioMd5!=null)
            this.audioMd5 = audioMd5;

        this.spMediaType = spMediaType;

        System.out.println("I/" + TAG + ": CallRecord:" + this.toString());
    }

    @Override
    public String toString() {

        StringBuilder builder = new StringBuilder();
        builder.
                append(", [Source]:").append(sourceId).
                append(", [Destination]:").append(destinationId).
                append(", [Special Media Type]:").append(spMediaType.toString());

                if(visualMediaFile !=null) {
                    builder.append(", [Visual Media File]:").append(visualMediaFile);
                    builder.append(", [visual_md5]:").append(visualMd5);
                }
                if (audioMediaFile !=null) {
                    builder.append(", [Audio Media File]:").append(audioMediaFile);
                    builder.append(", [audio_md5]:").append(audioMd5);
                }

        return builder.toString();
    }

    public SpecialMediaType getSpMediaType() {
        return spMediaType;
    }

    public FileManager getVisualMediaFile() {
        return visualMediaFile;
    }

    public String getDestinationId() {
        return destinationId;
    }

    public String getSourceId() {
        return sourceId;
    }

    public FileManager getAudioMediaFile() {
        return audioMediaFile;
    }

    public String getVisualMd5() {
        return visualMd5;
    }

    public String getAudioMd5() {
        return audioMd5;
    }
}
