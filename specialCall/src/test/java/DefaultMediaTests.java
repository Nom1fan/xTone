import android.content.Context;
import android.support.annotation.NonNull;

import com.client.DefaultMediaClient;
import com.converters.MediaDataConverter;
import com.converters.MediaDataConverterImpl;
import com.dao.MediaDAO;
import com.data.objects.Contact;
import com.data.objects.DefaultMediaData;
import com.data.objects.DefaultMediaDataContainer;
import com.data.objects.PendingDownloadData;
import com.enums.SpecialMediaType;
import com.files.media.MediaFile;
import com.logger.LoggerFactory;
import com.logger.SystemOutLogger;
import com.services.ServerProxy;
import com.services.SyncOnDefaultMediaIntentServiceLogic;
import com.utils.ContactsUtils;
import com.utils.ContactsUtilsImpl;
import com.utils.MediaFileUtils;
import com.utils.UtilityFactory;

import static org.mockito.Mockito.*;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
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

    @Mock
    ContactsUtils contactsUtils;

    ContactsUtilsImpl realContactsUtils = new ContactsUtilsImpl();

    @Before
    public void beforeTest() {
        LoggerFactory.setLogger(new SystemOutLogger());
    }

    @Test
    public void initialSingleDefaultMediaTest() {


        final String phoneNumber = "0544556543";
        SpecialMediaType specialMediaType = SpecialMediaType.DEFAULT_CALLER_MEDIA;
        String fileName = "MyDefaultCallerMedia.jpg";

        final DefaultMediaData defaultMediaData = new DefaultMediaData();
        List<DefaultMediaDataContainer> defaultMediaDataContainers = prepareDefaultMediaContainer(phoneNumber, specialMediaType, fileName, defaultMediaData);

        when(defaultMediaClient.getDefaultMediaData(context, new ArrayList<String>(){{add(phoneNumber);}}, specialMediaType))
                .thenReturn(defaultMediaDataContainers);

        when(contactsUtils.convertToUids(Mockito.anyList()))
                .thenReturn(new ArrayList<String>(){{add(phoneNumber);}});

        MediaDataConverter mediaDataConverter = new MediaDataConverterImpl();
        PendingDownloadData pendingDownloadData = mediaDataConverter.toPendingDownloadData(phoneNumber, specialMediaType, defaultMediaData);

        List<Contact> allContacts = prepareContacts(
                new Contact("Rony Eidlin", "0544556543"),
                new Contact("Hot Girl", "0501111111"),
                new Contact("Another Hot Girl", "0500000000"));

        SyncOnDefaultMediaIntentServiceLogic logic = prepareLogic(mediaDataConverter, allContacts);

        logic.executeLogic();

        verify(serverProxy, times(1)).sendActionDownload(context, pendingDownloadData, defaultMediaData);
        verify(mediaDAO, times(0)).removeMedia(specialMediaType, defaultMediaData.getMediaFile().getFileType(), phoneNumber);
    }

    @Test
    public void initialMultiDefaultMediaTest() {
        final String phoneNumber = "0544556543";
        SpecialMediaType specialMediaType = SpecialMediaType.DEFAULT_CALLER_MEDIA;
        String fileName = "MyDefaultCallerMedia.jpg";
        String audioFileName = "MyDefaultCallerMedia.mp3";
        final DefaultMediaData defaultMediaData = new DefaultMediaData();
        List<DefaultMediaDataContainer> defaultMediaDataContainers = prepareDefaultMediaContainer(phoneNumber, specialMediaType, fileName, defaultMediaData);
        defaultMediaDataContainers.addAll(prepareDefaultMediaContainer(phoneNumber, specialMediaType, audioFileName, defaultMediaData));


        when(defaultMediaClient.getDefaultMediaData(context, new ArrayList<String>(){{add(phoneNumber);}}, specialMediaType))
                .thenReturn(defaultMediaDataContainers);

        when(contactsUtils.convertToUids(Mockito.anyList()))
                .thenReturn(new ArrayList<String>(){{add(phoneNumber);}});

        MediaDataConverter mediaDataConverter = new MediaDataConverterImpl();
        PendingDownloadData pendingDownloadData = mediaDataConverter.toPendingDownloadData(phoneNumber, specialMediaType, defaultMediaData);

        List<Contact> allContacts = prepareContacts(
                new Contact("Rony Eidlin", "0544556543"),
                new Contact("Hot Girl", "0501111111"),
                new Contact("Another Hot Girl", "0500000000"));

        SyncOnDefaultMediaIntentServiceLogic logic = prepareLogic(mediaDataConverter, allContacts);

        logic.executeLogic();

        verify(serverProxy, times(2)).sendActionDownload(context, pendingDownloadData, defaultMediaData);
        verify(mediaDAO, times(0)).removeMedia(specialMediaType, defaultMediaData.getMediaFile().getFileType(), phoneNumber);

    }

    @Test
    public void removeAllDefaultMediaByEmptyListTest() {
        final String phoneNumber = "0544556543";
        SpecialMediaType specialMediaType = SpecialMediaType.DEFAULT_CALLER_MEDIA;
        ;
        final DefaultMediaData defaultMediaData = new DefaultMediaData();
        List<MediaFile> mediaFiles = prepareMediaFiles();
        List<DefaultMediaDataContainer> defaultMediaDataContainers = prepareEmptyDefaultMediaDataContainer(phoneNumber, specialMediaType);

        when(defaultMediaClient.getDefaultMediaData(context, new ArrayList<String>(){{add(phoneNumber);}}, specialMediaType))
                .thenReturn(defaultMediaDataContainers);

        when(mediaDAO.getMedia(specialMediaType, phoneNumber))
                .thenReturn(mediaFiles);

        when(contactsUtils.convertToUids(Mockito.anyList()))
                .thenReturn(new ArrayList<String>(){{add(phoneNumber);}});

        MediaDataConverter mediaDataConverter = new MediaDataConverterImpl();
        PendingDownloadData pendingDownloadData = mediaDataConverter.toPendingDownloadData(phoneNumber, specialMediaType, defaultMediaData);

        List<Contact> allContacts = prepareContacts(
                new Contact("Rony Eidlin", "0544556543"),
                new Contact("Hot Girl", "0501111111"),
                new Contact("Another Hot Girl", "0500000000"));

        SyncOnDefaultMediaIntentServiceLogic logic = prepareLogic(mediaDataConverter, allContacts);

        logic.executeLogic();

        verify(serverProxy, times(0)).sendActionDownload(context, pendingDownloadData, defaultMediaData);
        verify(mediaDAO, times(1)).removeMedia(specialMediaType, phoneNumber);
    }

    @Test
    public void noChangeInDefaultProfileMediaTest() {
        final String phoneNumber = "0544556543";
        SpecialMediaType defaultProfileMedia = SpecialMediaType.DEFAULT_PROFILE_MEDIA;
        String visualFileName = "MyDefaultCallerMedia.jpg";
        String audioFileName = "MyDefaultCallerMedia.mp3";
        final DefaultMediaData defaultMediaData = new DefaultMediaData();
        List<DefaultMediaDataContainer> defaultMediaDataContainers = prepareDefaultMediaContainer(phoneNumber, defaultProfileMedia, visualFileName, defaultMediaData, 123);
        defaultMediaDataContainers.addAll(prepareDefaultMediaContainer(phoneNumber, defaultProfileMedia, audioFileName, defaultMediaData, 123));
        List<MediaFile> mediaFiles = new ArrayList<>();
        MediaFile visualMediaFile = new MediaFile();
        visualMediaFile.setFileType(MediaFile.FileType.IMAGE);
        visualMediaFile.setExtension("jpg");
        MediaFile audioMediaFile = new MediaFile();
        audioMediaFile.setFileType(MediaFile.FileType.AUDIO);
        audioMediaFile.setExtension("mp3");

        mediaFiles.add(visualMediaFile);
        mediaFiles.add(audioMediaFile);

        when(defaultMediaClient.getDefaultMediaData(context, new ArrayList<String>() {{add(phoneNumber);}}, defaultProfileMedia))
                .thenReturn(defaultMediaDataContainers);

        when(mediaDAO.getMedia(defaultProfileMedia, phoneNumber)).
                thenReturn(mediaFiles);
        when(mediaFileUtils.getFileCreationDateInUnixTime(visualMediaFile))
                .thenReturn(123L);
        when(mediaFileUtils.getFileCreationDateInUnixTime(audioMediaFile))
                .thenReturn(123L);

        when(contactsUtils.convertToUids(Mockito.anyList()))
                .thenReturn(new ArrayList<String>(){{add(phoneNumber);}});

        MediaDataConverter mediaDataConverter = new MediaDataConverterImpl();
        PendingDownloadData pendingDownloadData = mediaDataConverter.toPendingDownloadData(phoneNumber, defaultProfileMedia, defaultMediaData);

        List<Contact> allContacts = prepareContacts(
                new Contact("Rony Eidlin", "0544556543"),
                new Contact("Hot Girl", "0501111111"),
                new Contact("Another Hot Girl", "0500000000"));

        SyncOnDefaultMediaIntentServiceLogic logic = prepareLogic(mediaDataConverter, allContacts);

        logic.executeLogic();

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
        final DefaultMediaData defaultMediaData = new DefaultMediaData();
        List<DefaultMediaDataContainer> defaultMediaDataContainers = prepareDefaultMediaContainer(phoneNumber, defaultCallerMedia, visualFileName, defaultMediaData, 123);
        defaultMediaDataContainers.addAll(prepareDefaultMediaContainer(phoneNumber, defaultCallerMedia, audioFileName, defaultMediaData, 123));
        List<MediaFile> mediaFiles = new ArrayList<>();
        MediaFile visualMediaFile = new MediaFile();
        visualMediaFile.setFileType(MediaFile.FileType.IMAGE);
        visualMediaFile.setExtension("jpg");
        MediaFile audioMediaFile = new MediaFile();
        audioMediaFile.setFileType(MediaFile.FileType.AUDIO);
        audioMediaFile.setExtension("mp3");

        mediaFiles.add(visualMediaFile);
        mediaFiles.add(audioMediaFile);

        when(defaultMediaClient.getDefaultMediaData(context, new ArrayList<String>() {{add(phoneNumber);}}, defaultCallerMedia))
                .thenReturn(defaultMediaDataContainers);

        when(mediaDAO.getMedia(defaultCallerMedia, phoneNumber)).
                thenReturn(mediaFiles);
        when(mediaFileUtils.getFileCreationDateInUnixTime(visualMediaFile))
                .thenReturn(123L);
        when(mediaFileUtils.getFileCreationDateInUnixTime(audioMediaFile))
                .thenReturn(123L);

        MediaDataConverter mediaDataConverter = new MediaDataConverterImpl();
        PendingDownloadData pendingDownloadData = mediaDataConverter.toPendingDownloadData(phoneNumber, defaultCallerMedia, defaultMediaData);

        List<Contact> allContacts = prepareContacts(
                new Contact("Rony Eidlin", "0544556543"),
                new Contact("Hot Girl", "0501111111"),
                new Contact("Another Hot Girl", "0500000000"));

        SyncOnDefaultMediaIntentServiceLogic logic = prepareLogic(mediaDataConverter, allContacts);

        logic.executeLogic();

        verify(serverProxy, times(0)).sendActionDownload(context, pendingDownloadData, defaultMediaData);
        verify(mediaDAO, times(0)).removeMedia(defaultCallerMedia, visualMediaFile.getFileType(), phoneNumber);
        verify(mediaDAO, times(0)).removeMedia(defaultCallerMedia, audioMediaFile.getFileType(), phoneNumber);
    }

    @Test
    public void removeAudioDefaultMediaSyncVisualDefaultMediaTest() {
        final String phoneNumber = "0544556543";
        SpecialMediaType defaultCallerMedia = SpecialMediaType.DEFAULT_CALLER_MEDIA;
        String visualFileName = "MyDefaultCallerMedia.jpg";

        final DefaultMediaData defaultMediaData = new DefaultMediaData();
        List<DefaultMediaDataContainer> defaultMediaDataContainers = prepareDefaultMediaContainer(phoneNumber, defaultCallerMedia, visualFileName, defaultMediaData, 1234);
        List<MediaFile> mediaFiles = new ArrayList<>();
        MediaFile visualMediaFile = new MediaFile();
        visualMediaFile.setFileType(MediaFile.FileType.IMAGE);
        visualMediaFile.setExtension("jpg");
        MediaFile audioMediaFile = new MediaFile();
        audioMediaFile.setFileType(MediaFile.FileType.AUDIO);
        audioMediaFile.setExtension("mp3");

        mediaFiles.add(visualMediaFile);
        mediaFiles.add(audioMediaFile);

        when(defaultMediaClient.getDefaultMediaData(context, new ArrayList<String>() {{add(phoneNumber);}}, defaultCallerMedia))
                .thenReturn(defaultMediaDataContainers);

        when(contactsUtils.convertToUids(Mockito.anyList()))
                .thenReturn(new ArrayList<String>(){{add(phoneNumber);}});

        when(mediaDAO.getMedia(defaultCallerMedia, phoneNumber)).
                thenReturn(mediaFiles);
        when(mediaFileUtils.getFileCreationDateInUnixTime(visualMediaFile))
                .thenReturn(123L);
        when(mediaFileUtils.getFileCreationDateInUnixTime(audioMediaFile))
                .thenReturn(123L);

        MediaDataConverter mediaDataConverter = new MediaDataConverterImpl();
        PendingDownloadData pendingDownloadData = mediaDataConverter.toPendingDownloadData(phoneNumber, defaultCallerMedia, defaultMediaData);

        List<Contact> allContacts = prepareContacts(
                new Contact("Rony Eidlin", "0544556543"),
                new Contact("Hot Girl", "0501111111"),
                new Contact("Another Hot Girl", "0500000000"));

        SyncOnDefaultMediaIntentServiceLogic logic = prepareLogic(mediaDataConverter, allContacts);

        logic.executeLogic();

        verify(serverProxy, times(1)).sendActionDownload(context, pendingDownloadData, defaultMediaData);
        verify(mediaDAO, times(1)).removeMedia(audioMediaFile);
    }

    @Test
    public void removeVideoDefaultMediaSyncAudioDefaultMediaTest() {
        final String phoneNumber = "0544556543";
        SpecialMediaType defaultCallerMedia = SpecialMediaType.DEFAULT_CALLER_MEDIA;
        String audioFileName = "MyDefaultCallerMedia.mp3";
        final DefaultMediaData defaultMediaData = new DefaultMediaData();
        List<DefaultMediaDataContainer> defaultMediaDataContainers = prepareDefaultMediaContainer(phoneNumber, defaultCallerMedia, audioFileName, defaultMediaData, 1234);
        List<MediaFile> mediaFiles = new ArrayList<>();
        MediaFile visualMediaFile = new MediaFile();
        visualMediaFile.setFileType(MediaFile.FileType.IMAGE);
        visualMediaFile.setExtension("jpg");
        MediaFile audioMediaFile = new MediaFile();
        audioMediaFile.setFileType(MediaFile.FileType.AUDIO);
        audioMediaFile.setExtension("mp3");

        mediaFiles.add(visualMediaFile);
        mediaFiles.add(audioMediaFile);

        List<Contact> allContacts = prepareContacts(
                new Contact("Rony Eidlin", "0544556543"),
                new Contact("Hot Girl", "0501111111"),
                new Contact("Another Hot Girl", "0500000000"));

        List<String> uids = realContactsUtils.convertToUids(allContacts);

        when(defaultMediaClient.getDefaultMediaData(context, uids, defaultCallerMedia))
                .thenReturn(defaultMediaDataContainers);

        when(contactsUtils.convertToUids(Mockito.anyList()))
                .thenReturn(uids);

        when(mediaDAO.getMedia(defaultCallerMedia, phoneNumber)).
                thenReturn(mediaFiles);
        when(mediaFileUtils.getFileCreationDateInUnixTime(visualMediaFile))
                .thenReturn(123L);
        when(mediaFileUtils.getFileCreationDateInUnixTime(audioMediaFile))
                .thenReturn(123L);

        MediaDataConverter mediaDataConverter = new MediaDataConverterImpl();
        PendingDownloadData pendingDownloadData = mediaDataConverter.toPendingDownloadData(phoneNumber, defaultCallerMedia, defaultMediaData);


        SyncOnDefaultMediaIntentServiceLogic logic = prepareLogic(mediaDataConverter, allContacts);

        logic.executeLogic();

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

    private List<DefaultMediaDataContainer> prepareEmptyDefaultMediaDataContainer(String uid, SpecialMediaType specialMediaType) {
        List<DefaultMediaDataContainer> defaultMediaDataContainers = new ArrayList<>();
        DefaultMediaDataContainer defaultMediaDataContainer = new DefaultMediaDataContainer();
        defaultMediaDataContainer.setUid(uid);
        defaultMediaDataContainer.setSpecialMediaType(specialMediaType);
        defaultMediaDataContainer.setDefaultMediaDataList(new ArrayList<DefaultMediaData>());
        defaultMediaDataContainers.add(defaultMediaDataContainer);
        return defaultMediaDataContainers;
    }

    @NonNull
    private List<DefaultMediaDataContainer> prepareDefaultMediaContainer(String phoneNumber, SpecialMediaType specialMediaType, String fileName, final DefaultMediaData defaultMediaData) {
        return prepareDefaultMediaContainer(phoneNumber, specialMediaType, fileName, defaultMediaData, 12345);
    }

    @NonNull
    private List<DefaultMediaDataContainer> prepareDefaultMediaContainer(String phoneNumber, SpecialMediaType specialMediaType, String fileName, final DefaultMediaData defaultMediaData, long unixTime) {
        final DefaultMediaDataContainer defaultMediaDataContainer = new DefaultMediaDataContainer();
        MediaFileUtils mediaFileUtils = UtilityFactory.instance().getUtility(MediaFileUtils.class);
        List<DefaultMediaDataContainer> defaultMediaDataContainers;
        final List<DefaultMediaData> defaultMediaDataList;
        defaultMediaData.setFilePathOnServer("C:\\git\\mediacallz_server\\uploads\\" + phoneNumber + "\\my_default_caller_media\\" + fileName);
        MediaFile mediaFile = new MediaFile();
        mediaFile.setSize(15154);
        mediaFile.setExtension(mediaFileUtils.extractExtension(fileName));
        mediaFile.setFileType(mediaFileUtils.getFileType(fileName));
        defaultMediaData.setMediaFile(mediaFile);
        defaultMediaData.setDefaultMediaUnixTime(unixTime);
        defaultMediaDataList = new ArrayList<DefaultMediaData>() {{
            add(defaultMediaData);
        }};
        defaultMediaDataContainer.setUid(phoneNumber);
        defaultMediaDataContainer.setSpecialMediaType(specialMediaType);
        defaultMediaDataContainer.setDefaultMediaDataList(defaultMediaDataList);
        defaultMediaDataContainers = new ArrayList<DefaultMediaDataContainer>() {{
            add(defaultMediaDataContainer);
        }};
        return defaultMediaDataContainers;
    }

    private List<Contact> prepareContacts(final Contact... contacts) {
        return Arrays.asList(contacts);
    }

    @NonNull
    private SyncOnDefaultMediaIntentServiceLogic prepareLogic(MediaDataConverter mediaDataConverter, List<Contact> allContacts) {
        SyncOnDefaultMediaIntentServiceLogic logic = new SyncOnDefaultMediaIntentServiceLogic();
        logic.setContactsUtils(contactsUtils);
        logic.setContext(context);
        logic.setServerProxy(serverProxy);
        logic.setDefaultMediaClient(defaultMediaClient);
        logic.setMediaDAO(mediaDAO);
        logic.setMediaDataConverter(mediaDataConverter);
        logic.setMediaFileUtils(mediaFileUtils);
        return logic;
    }
}

