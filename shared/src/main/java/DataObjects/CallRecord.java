package DataObjects;

import com.google.gson.Gson;

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

    private SpecialMediaType specialMediaType;

    public CallRecord(String source,
                      String destination,
                      FileManager visualMediaFile,
                      String visualM5,
                      FileManager audioMediaFile,
                      String audioMd5,
                      SpecialMediaType specialMediaType) {

        sourceId = source;
        destinationId = destination;

        this.visualMediaFile = visualMediaFile;
        this.audioMediaFile = audioMediaFile;

        if(visualM5!=null)
            visualMd5 = visualM5;

        if(audioMd5!=null)
            this.audioMd5 = audioMd5;

        this.specialMediaType = specialMediaType;

        System.out.println("I/" + TAG + ": CallRecord:" + this.toString());
    }

    @Override
    public String toString() {
        return new Gson().toJson(this);
    }

    public SpecialMediaType getSpecialMediaType() {
        return specialMediaType;
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
