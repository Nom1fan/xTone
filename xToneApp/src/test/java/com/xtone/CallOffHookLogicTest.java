package com.xtone;

import android.content.Context;
import android.telephony.TelephonyManager;

import com.xtone.logging.Logger;
import com.xtone.model.MediaFile;
import com.xtone.service.logic.CallOffHookLogic;
import com.xtone.service.logic.CallOffHookLogicImpl;
import com.xtone.utils.CallSessionUtils;
import com.xtone.utils.Phone2MediaUtils;
import com.xtone.utils.StandOutWindowUtils;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class CallOffHookLogicTest {

    @InjectMocks
    private CallOffHookLogicImpl callOffHookLogic;

    @Mock
    private StandOutWindowUtils standOutWindowUtils;

    @Mock
    private CallSessionUtils callSessionUtils;

    @Mock
    private Context context;

    @Mock
    Logger log;

    @Before
    public void before() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void stateAlreadyRinging_doNothing() {
        String incomingNumber = "050000000";

        callOffHookLogic.handle(context, incomingNumber);

        verify(callSessionUtils, times(1)).setCallState(eq(context), eq(TelephonyManager.CALL_STATE_OFFHOOK));
        verify(standOutWindowUtils, times(1)).stopStandOutWindow(eq(context));
    }

}
