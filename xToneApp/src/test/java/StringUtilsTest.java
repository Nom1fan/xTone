import android.os.Bundle;
import android.support.annotation.NonNull;

import com.xtone.utils.StringUtils;

import junit.framework.Assert;

import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class StringUtilsTest {

    private static final StringUtils stringUtils = new StringUtils();

    @Test
    public void mapToString() {
        Bundle bundle = TestUtils.mockBundle();
        bundle.putString("House", "Lannister");
        bundle.putString("Sigil", "Lion");

        String bundleToString = stringUtils.toString(bundle);

        String expectedBundleToString = "[{Sigil=Lion}{House=Lannister}]";
        Assert.assertEquals(expectedBundleToString, bundleToString);
    }
}
