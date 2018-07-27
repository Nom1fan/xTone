package com.xtone;

import android.os.Bundle;

import com.xtone.utils.StringUtils;

import junit.framework.Assert;

import org.junit.Test;

import static org.mockito.Mockito.mock;

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
