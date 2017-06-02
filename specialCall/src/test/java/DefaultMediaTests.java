import android.content.Context;
import android.support.annotation.NonNull;

import com.client.DefaultMediaClient;
import com.converters.MediaDataConverter;
import com.converters.MediaDataConverterImpl;
import com.dao.MediaDAO;
import com.data.objects.Contact;
import com.data.objects.DefaultMediaData;
import com.data.objects.PendingDownloadData;
import com.enums.SpecialMediaType;
import com.files.media.MediaFile;
import com.services.ServerProxy;
import com.services.SyncOnDefaultMediaIntentServiceLogic;
import com.utils.MediaFileUtils;
import com.utils.MediaFilesUtilsImpl;
import com.utils.UtilityFactory;

import static org.mockito.Mockito.*;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by Mor on 31/05/2017.
 */

@RunWith(MockitoJUnitRunner.class)
public class DefaultMediaTests {

    @Mock
    DefaultMediaClient defaultMediaClient;

    @Mock
    ServerProxy serverProxy;

    @Mock
    Context context;

    @Mock
    MediaDAO mediaDAO;

    @Mock
    MediaFileUtils mediaFileUtils;

    @Test
    public void initialSingleDefaultMediaTest() {
        final String phoneNumber = "0544556543";
        SpecialMediaType specialMediaType = SpecialMediaType.DEFAULT_CALLER_MEDIA;
        String fileName = "MyDefaultCallerMedia.jpg";
        List<DefaultMediaData> defaultMediaDataList;
        final DefaultMediaData defaultMediaData = new DefaultMediaData();
        defaultMediaDataList = prepareDefaultMediaData(phoneNumber, specialMediaType, fileName, defaultMediaData);

        when(defaultMediaClient.getDefaultMediaData(context, phoneNumber, specialMediaType))
                .thenReturn(defaultMediaDataList);

        MediaDataConverter mediaDataConverter = new MediaDataConverterImpl();
        PendingDownloadData pendingDownloadData = mediaDataConverter.toPendingDownloadData(defaultMediaData);

        List<Contact> allContacts = prepareContacts(
                new Contact("Rony Eidlin", "0544556543"),
                new Contact("Hot Girl", "0501111111"),
                new Contact("Another Hot Girl", "0500000000"));

        SyncOnDefaultMediaIntentServiceLogic logic = prepareLogic(mediaDataConverter, allContacts);

        logic.performSyncOnDefaultMedia();

        verify(serverProxy, times(1)).sendActionDownload(context, pendingDownloadData, defaultMediaData);
        verify(mediaDAO, times(0)).removeMedia(specialMediaType, defaultMediaData.getMediaFile().getFileType(), phoneNumber);

    }

    @Test
    public void initialMultiDefaultMediaTest() {
        final String phoneNumber = "0544556543";
        SpecialMediaType specialMediaType = SpecialMediaType.DEFAULT_CALLER_MEDIA;
        String fileName = "MyDefaultCallerMedia.jpg";
        String audioFileName = "MyDefaultCallerMedia.mp3";
        List<DefaultMediaData> defaultMediaDataList;
        final DefaultMediaData defaultMediaData = new DefaultMediaData();
        defaultMediaDataList = prepareDefaultMediaData(phoneNumber, specialMediaType, fileName, defaultMediaData);
        defaultMediaDataList.addAll(prepareDefaultMediaData(phoneNumber, specialMediaType, audioFileName, defaultMediaData));


        when(defaultMediaClient.getDefaultMediaData(context, phoneNumber, specialMediaType))
                .thenReturn(defaultMediaDataList);

        MediaDataConverter mediaDataConverter = new MediaDataConverterImpl();
        PendingDownloadData pendingDownloadData = mediaDataConverter.toPendingDownloadData(defaultMediaData);

        List<Contact> allContacts = prepareContacts(
                new Contact("Rony Eidlin", "0544556543"),
                new Contact("Hot Girl", "0501111111"),
                new Contact("Another Hot Girl", "0500000000"));

        SyncOnDefaultMediaIntentServiceLogic logic = prepareLogic(mediaDataConverter, allContacts);

        logic.performSyncOnDefaultMedia();

        verify(serverProxy, times(2)).sendActionDownload(context, pendingDownloadData, defaultMediaData);
        verify(mediaDAO, times(0)).removeMedia(specialMediaType, defaultMediaData.getMediaFile().getFileType(), phoneNumber);

    }

    @Test
    public void removeAllDefaultMediaByEmptyListTest() {
        final String phoneNumber = "0544556543";
        SpecialMediaType specialMediaType = SpecialMediaType.DEFAULT_CALLER_MEDIA;

        List<DefaultMediaData> defaultMediaDataList = new ArrayList<>();;
        final DefaultMediaData defaultMediaData = new DefaultMediaData();
        List<MediaFile> mediaFiles = prepareMediaFiles();

        when(defaultMediaClient.getDefaultMediaData(context, phoneNumber, specialMediaType))
                .thenReturn(defaultMediaDataList);

        when(mediaDAO.getMedia(specialMediaType, phoneNumber))
                .thenReturn(mediaFiles);

        MediaDataConverter mediaDataConverter = new MediaDataConverterImpl();
        PendingDownloadData pendingDownloadData = mediaDataConverter.toPendingDownloadData(defaultMediaData);

        List<Contact> allContacts = prepareContacts(
                new Contact("Rony Eidlin", "0544556543"),
                new Contact("Hot Girl", "0501111111"),
                new Contact("Another Hot Girl", "0500000000"));

        SyncOnDefaultMediaIntentServiceLogic logic = prepareLogic(mediaDataConverter, allContacts);

        logic.performSyncOnDefaultMedia();

        verify(serverProxy, times(0)).sendActionDownload(context, pendingDownloadData, defaultMediaData);
        verify(mediaDAO, times(1)).removeMedia(specialMediaType, phoneNumber);
    }

    @Test
    public void noChangeInDefaultProfileMediaTest() {
        final String phoneNumber = "0544556543";
        SpecialMediaType defaultProfileMedia = SpecialMediaType.DEFAULT_PROFILE_MEDIA;
        String visualFileName = "MyDefaultCallerMedia.jpg";
        String audioFileName = "MyDefaultCallerMedia.mp3";
        List<DefaultMediaData> defaultMediaDataList;
        final DefaultMediaData defaultMediaData = new DefaultMediaData();
        defaultMediaDataList = prepareDefaultMediaData(phoneNumber, defaultProfileMedia, visualFileName, defaultMediaData, 123);
        defaultMediaDataList.addAll(prepareDefaultMediaData(phoneNumber, defaultProfileMedia, audioFileName, defaultMediaData, 123));
        List<MediaFile> mediaFiles = new ArrayList<>();
        MediaFile visualMediaFile = new MediaFile();
        visualMediaFile.setFileType(MediaFile.FileType.IMAGE);
        visualMediaFile.setExtension("jpg");
        MediaFile audioMediaFile = new MediaFile();
        audioMediaFile.setFileType(MediaFile.FileType.AUDIO);
        audioMediaFile.setExtension("mp3");

        mediaFiles.add(visualMediaFile);
        mediaFiles.add(audioMediaFile);

        when(defaultMediaClient.getDefaultMediaData(context, phoneNumber, defaultProfileMedia))
                .thenReturn(defaultMediaDataList);

        when(mediaDAO.getMedia(defaultProfileMedia, phoneNumber)).
                thenReturn(mediaFiles);
        when(mediaFileUtils.getFileCreationDateInUnixTime(visualMediaFile))
                .thenReturn(123L);
        when(mediaFileUtils.getFileCreationDateInUnixTime(audioMediaFile))
                .thenReturn(123L);

        MediaDataConverter mediaDataConverter = new MediaDataConverterImpl();
        PendingDownloadData pendingDownloadData = mediaDataConverter.toPendingDownloadData(defaultMediaData);

        List<Contact> allContacts = prepareContacts(
                new Contact("Rony Eidlin", "0544556543"),
                new Contact("Hot Girl", "0501111111"),
                new Contact("Another Hot Girl", "0500000000"));

        SyncOnDefaultMediaIntentServiceLogic logic = prepareLogic(mediaDataConverter, allContacts);

        logic.performSyncOnDefaultMedia();

        verify(serverProxy, times(0)).sendActionDownload(context, pendingDownloadData, defaultMediaData);
        verify(mediaDAO, times(0)).removeMedia(defaultProfileMedia, visualMediaFile.getFileType(), phoneNumber);
        verify(mediaDAO, times(0)).removeMedia(defaultProfileMedia, audioMediaFile.getFileType(), phoneNumber);
    }

    @Test
    public void noChangeInDefaultCallerMediaTest() {
        final String phoneNumber = "0544556543";
        SpecialMediaType defaultCallerMedia = SpecialMediaType.DEFAULT_CALLER_MEDIA;
        String visualFileName = "MyDefaultCallerMedia.jpg";
        String audioFileName = "MyDefaultCallerMedia.mp3";
        List<DefaultMediaData> defaultMediaDataList;
        final DefaultMediaData defaultMediaData = new DefaultMediaData();
        defaultMediaDataList = prepareDefaultMediaData(phoneNumber, defaultCallerMedia, visualFileName, defaultMediaData, 123);
        defaultMediaDataList.addAll(prepareDefaultMediaData(phoneNumber, defaultCallerMedia, audioFileName, defaultMediaData, 123));
        List<MediaFile> mediaFiles = new ArrayList<>();
        MediaFile visualMediaFile = new MediaFile();
        visualMediaFile.setFileType(MediaFile.FileType.IMAGE);
        visualMediaFile.setExtension("jpg");
        MediaFile audioMediaFile = new MediaFile();
        audioMediaFile.setFileType(MediaFile.FileType.AUDIO);
        audioMediaFile.setExtension("mp3");

        mediaFiles.add(visualMediaFile);
        mediaFiles.add(audioMediaFile);

        when(defaultMediaClient.getDefaultMediaData(context, phoneNumber, defaultCallerMedia))
                .thenReturn(defaultMediaDataList);

        when(mediaDAO.getMedia(defaultCallerMedia, phoneNumber)).
                thenReturn(mediaFiles);
        when(mediaFileUtils.getFileCreationDateInUnixTime(visualMediaFile))
                .thenReturn(123L);
        when(mediaFileUtils.getFileCreationDateInUnixTime(audioMediaFile))
                .thenReturn(123L);

        MediaDataConverter mediaDataConverter = new MediaDataConverterImpl();
        PendingDownloadData pendingDownloadData = mediaDataConverter.toPendingDownloadData(defaultMediaData);

        List<Contact> allContacts = prepareContacts(
                new Contact("Rony Eidlin", "0544556543"),
                new Contact("Hot Girl", "0501111111"),
                new Contact("Another Hot Girl", "0500000000"));

        SyncOnDefaultMediaIntentServiceLogic logic = prepareLogic(mediaDataConverter, allContacts);

        logic.performSyncOnDefaultMedia();

        verify(serverProxy, times(0)).sendActionDownload(context, pendingDownloadData, defaultMediaData);
        verify(mediaDAO, times(0)).removeMedia(defaultCallerMedia, visualMediaFile.getFileType(), phoneNumber);
        verify(mediaDAO, times(0)).removeMedia(defaultCallerMedia, audioMediaFile.getFileType(), phoneNumber);
    }

    @Test
    public void removeAudioDefaultMediaSyncVisualDefaultMediaTest() {
        final String phoneNumber = "0544556543";
        SpecialMediaType defaultCallerMedia = SpecialMediaType.DEFAULT_CALLER_MEDIA;
        String visualFileName = "MyDefaultCallerMedia.jpg";
        List<DefaultMediaData> defaultMediaDataList;
        final DefaultMediaData defaultMediaData = new DefaultMediaData();
        defaultMediaDataList = prepareDefaultMediaData(phoneNumber, defaultCallerMedia, visualFileName, defaultMediaData, 1234);
        List<MediaFile> mediaFiles = new ArrayList<>();
        MediaFile visualMediaFile = new MediaFile();
        visualMediaFile.setFileType(MediaFile.FileType.IMAGE);
        visualMediaFile.setExtension("jpg");
        MediaFile audioMediaFile = new MediaFile();
        audioMediaFile.setFileType(MediaFile.FileType.AUDIO);
        audioMediaFile.setExtension("mp3");

        mediaFiles.add(visualMediaFile);
        mediaFiles.add(audioMediaFile);

        when(defaultMediaClient.getDefaultMediaData(context, phoneNumber, defaultCallerMedia))
                .thenReturn(defaultMediaDataList);

        when(mediaDAO.getMedia(defaultCallerMedia, phoneNumber)).
                thenReturn(mediaFiles);
        when(mediaFileUtils.getFileCreationDateInUnixTime(visualMediaFile))
                .thenReturn(123L);
        when(mediaFileUtils.getFileCreationDateInUnixTime(audioMediaFile))
                .thenReturn(123L);

        MediaDataConverter mediaDataConverter = new MediaDataConverterImpl();
        PendingDownloadData pendingDownloadData = mediaDataConverter.toPendingDownloadData(defaultMediaData);

        List<Contact> allContacts = prepareContacts(
                new Contact("Rony Eidlin", "0544556543"),
                new Contact("Hot Girl", "0501111111"),
                new Contact("Another Hot Girl", "0500000000"));

        SyncOnDefaultMediaIntentServiceLogic logic = prepareLogic(mediaDataConverter, allContacts);

        logic.performSyncOnDefaultMedia();

        verify(serverProxy, times(1)).sendActionDownload(context, pendingDownloadData, defaultMediaData);
        verify(mediaDAO, times(1)).removeMedia(audioMediaFile);
    }

    @Test
    public void removeVideoDefaultMediaSyncAudioDefaultMediaTest() {
        final String phoneNumber = "0544556543";
        SpecialMediaType defaultCallerMedia = SpecialMediaType.DEFAULT_CALLER_MEDIA;
        String audioFileName = "MyDefaultCallerMedia.mp3";
        List<DefaultMediaData> defaultMediaDataList;
        final DefaultMediaData defaultMediaData = new DefaultMediaData();
        defaultMediaDataList = prepareDefaultMediaData(phoneNumber, defaultCallerMedia, audioFileName, defaultMediaData, 1234);
        List<MediaFile> mediaFiles = new ArrayList<>();
        MediaFile visualMediaFile = new MediaFile();
        visualMediaFile.setFileType(MediaFile.FileType.IMAGE);
        visualMediaFile.setExtension("jpg");
        MediaFile audioMediaFile = new MediaFile();
        audioMediaFile.setFileType(MediaFile.FileType.AUDIO);
        audioMediaFile.setExtension("mp3");

        mediaFiles.add(visualMediaFile);
        mediaFiles.add(audioMediaFile);

        when(defaultMediaClient.getDefaultMediaData(context, phoneNumber, defaultCallerMedia))
                .thenReturn(defaultMediaDataList);

        when(mediaDAO.getMedia(defaultCallerMedia, phoneNumber)).
                thenReturn(mediaFiles);
        when(mediaFileUtils.getFileCreationDateInUnixTime(visualMediaFile))
                .thenReturn(123L);
        when(mediaFileUtils.getFileCreationDateInUnixTime(audioMediaFile))
                .thenReturn(123L);

        MediaDataConverter mediaDataConverter = new MediaDataConverterImpl();
        PendingDownloadData pendingDownloadData = mediaDataConverter.toPendingDownloadData(defaultMediaData);

        List<Contact> allContacts = prepareContacts(
                new Contact("Rony Eidlin", "0544556543"),
                new Contact("Hot Girl", "0501111111"),
                new Contact("Another Hot Girl", "0500000000"));

        SyncOnDefaultMediaIntentServiceLogic logic = prepareLogic(mediaDataConverter, allContacts);

        logic.performSyncOnDefaultMedia();

        verify(serverProxy, times(1)).sendActionDownload(context, pendingDownloadData, defaultMediaData);
        verify(mediaDAO, times(1)).removeMedia(visualMediaFile);
    }

    private List<MediaFile> prepareMediaFiles() {
        List<MediaFile> mediaFiles = new ArrayList<>();
        MediaFile mediaFile = new MediaFile();
        mediaFile.setFileType(MediaFile.FileType.IMAGE);
        mediaFile.setFile(new File(""));
        mediaFile.setExtension(".jpg");
        mediaFile.setSize(12345);
        mediaFiles.add(mediaFile);
        return mediaFiles;
    }

    @NonNull
    protected List<DefaultMediaData> prepareDefaultMediaData(String phoneNumber, SpecialMediaType specialMediaType, String fileName, final DefaultMediaData defaultMediaData) {
        return prepareDefaultMediaData(phoneNumber, specialMediaType, fileName, defaultMediaData, 12345);
    }

    @NonNull
    protected List<DefaultMediaData> prepareDefaultMediaData(String phoneNumber, SpecialMediaType specialMediaType, String fileName, final DefaultMediaData defaultMediaData, long unixTime) {
        MediaFileUtils mediaFileUtils = UtilityFactory.getUtility(MediaFileUtils.class);
        List<DefaultMediaData> defaultMediaDataList;
        defaultMediaData.setFilePathOnServer("C:\\git\\mediacallz_server\\uploads\\" + phoneNumber + "\\my_default_caller_media\\" + fileName);
        MediaFile mediaFile = new MediaFile();
        mediaFile.setSize(15154);
        mediaFile.setExtension(mediaFileUtils.extractExtension(fileName));
        mediaFile.setFileType(mediaFileUtils.getFileType(fileName));
        defaultMediaData.setMediaFile(mediaFile);
        defaultMediaData.setSpecialMediaType(specialMediaType);
        defaultMediaData.setDefaultMediaUnixTime(unixTime);
        defaultMediaData.setUid(phoneNumber);
        defaultMediaDataList = new ArrayList<DefaultMediaData>() {{
            add(defaultMediaData);
        }};
        return defaultMediaDataList;
    }

    protected List<Contact> prepareContacts(final Contact... contacts) {
        return Arrays.asList(contacts);
    }

    @NonNull
    protected SyncOnDefaultMediaIntentServiceLogic prepareLogic(MediaDataConverter mediaDataConverter, List<Contact> allContacts) {
        SyncOnDefaultMediaIntentServiceLogic logic = new SyncOnDefaultMediaIntentServiceLogic();
        logic.setContext(context);
        logic.setAllContacts(allContacts);
        logic.setServerProxy(serverProxy);
        logic.setDefaultMediaClient(defaultMediaClient);
        logic.setMediaDAO(mediaDAO);
        logic.setMediaDataConverter(mediaDataConverter);
        logic.setMediaFileUtils(mediaFileUtils);
        return logic;
    }
}

