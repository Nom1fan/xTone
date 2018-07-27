package com.xtone;

import android.os.Bundle;
import android.support.annotation.NonNull;

import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public abstract class TestUtils {

    @NonNull
    public static Bundle mockBundle() {
        final Map<String, String> fakeBundle = new HashMap<>();
        Bundle bundle = mock(Bundle.class);
        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) {
                Object[] arguments = invocation.getArguments();
                String key = ((String) arguments[0]);
                String value = ((String) arguments[1]);
                fakeBundle.put(key, value);
                return null;
            }
        }).when(bundle).putString(anyString(), anyString());

        when(bundle.get(anyString())).thenAnswer(new Answer<String>() {
            @Override
            public String answer(InvocationOnMock invocation) {
                Object[] arguments = invocation.getArguments();
                String key = ((String) arguments[0]);
                return fakeBundle.get(key);
            }
        });

        when(bundle.keySet()).then(new Answer<Set<String>>() {

            @Override
            public Set<String> answer(InvocationOnMock invocation) {
                return fakeBundle.keySet();
            }
        });
        return bundle;
    }
}
