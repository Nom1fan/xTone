import android.content.Context;
import android.support.annotation.NonNull;

import com.client.ClientFactory;
import com.client.DefaultMediaClient;
import com.client.UsersClient;
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
import com.services.SyncContactsLogic;
import com.services.SyncOnDefaultMediaIntentServiceLogic;
import com.utils.ContactsUtils;
import com.utils.ContactsUtilsImpl;
import com.utils.MediaFileUtils;
import com.utils.UtilityFactory;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.mockito.Mockito.*;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Created by Mor on 31/05/2017.
 */

@RunWith(MockitoJUnitRunner.class)
public class SyncContactsTests {

    @Mock
    Context context;

    @Mock
    UsersClient usersClient;

    UsersClient realUsersClient = ClientFactory.getInstance().getClient(UsersClient.class);

    @Mock
    ContactsUtils contactsUtils;

    @Before
    public void beforeTest() {
        LoggerFactory.setLogger(new SystemOutLogger());
    }

    @Test
    public void syncAllContactsTest() {
        List<Contact> allContacts = prepareContacts(
                new Contact("Rony Eidlin", "0544556543"),
                new Contact("Hot Girl", "0501111111"),
                new Contact("Another Hot Girl", "0500000000"));

        when(contactsUtils.getAllContacts(context))
                .thenReturn(allContacts);

        SyncContactsLogic logic = prepareLogic();
        logic.executeLogic();

        verify(usersClient, times(1)).syncContacts(context, allContacts);
    }

    @Test
    public void integration_syncAllContactsTest() {
        List<Contact> allContacts = prepareContacts(
                new Contact("Rony Eidlin", "0544556543"),
                new Contact("Hot Girl", "0501111111"),
                new Contact("Another Hot Girl", "0500000000"));

        when(contactsUtils.getAllContacts(context))
                .thenReturn(allContacts);

        SyncContactsLogic logic = prepareLogic();
        logic.setUsersClient(realUsersClient);
        logic.executeLogic();
    }

    private SyncContactsLogic prepareLogic() {
        SyncContactsLogic logic = new SyncContactsLogic();
        logic.setContext(context);
        logic.setContactsUtils(contactsUtils);
        logic.setUsersClient(usersClient);
        return logic;
    }

    private List<Contact> prepareContacts(final Contact... contacts) {
        return Arrays.asList(contacts);
    }

}

