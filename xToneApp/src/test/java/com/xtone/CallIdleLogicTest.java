package com.xtone;

import android.content.Context;
import android.telephony.TelephonyManager;

import com.xtone.logging.Logger;
import com.xtone.service.logic.CallIdleLogicImpl;
import com.xtone.utils.CallSessionUtils;
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

public class CallIdleLogicTest {

    @InjectMocks
    private CallIdleLogicImpl callIdleLogic;

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
    public void stateAlreadyIdle_doNothing() {
        String incomingNumber = "050000000";

        when(callSessionUtils.getCallState(eq(context))).thenReturn(TelephonyManager.CALL_STATE_IDLE);

        callIdleLogic.handle(context, incomingNumber);

        verify(callSessionUtils, times(1)).getCallState(eq(context));
        verify(callSessionUtils, times(0)).setCallState(any(Context.class),any(Integer.class));
        verify(standOutWindowUtils, times(0)).stopStandOutWindow(any(Context.class));
    }

    @Test
    public void stateRinging_setStateIdleAndStopStandOutWindow() {
        String incomingNumber = "050000000";

        when(callSessionUtils.getCallState(eq(context))).thenReturn(TelephonyManager.CALL_STATE_RINGING);

        callIdleLogic.handle(context, incomingNumber);

        verify(callSessionUtils, times(1)).getCallState(eq(context));
        verify(callSessionUtils, times(1)).setCallState(eq(context), eq(TelephonyManager.CALL_STATE_IDLE));
        verify(standOutWindowUtils, times(1)).stopStandOutWindow(eq(context));
    }

}
