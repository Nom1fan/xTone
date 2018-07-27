package com.xtone;

import android.content.Context;
import android.telephony.TelephonyManager;

import com.xtone.logging.Logger;
import com.xtone.receiver.IncomingCallReceiver;
import com.xtone.service.logic.CallIdleLogic;
import com.xtone.service.logic.CallOffHookLogic;
import com.xtone.service.logic.CallRingingLogic;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class IncomingCallReceiverTest {

    @InjectMocks
    private IncomingCallReceiver incomingCallService;

    @Mock
    private CallRingingLogic callRingingLogic;

    @Mock
    private CallOffHookLogic callOffHookLogic;

    @Mock
    private CallIdleLogic callIdleLogic;

    @Mock
    private Logger log;

    @Mock
    private Context context;

    @Before
    public void before() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void callRingingReceived_onlyCallRingingLogicCalled() {

        incomingCallService.syncOnCallStateChange(context, TelephonyManager.CALL_STATE_RINGING, "0500000000");

        verify(callRingingLogic, times(1)).handle(any(Context.class), any(String.class));
        verify(callOffHookLogic, times(0)).handle(any(Context.class), any(String.class));
        verify(callIdleLogic, times(0)).handle(any(Context.class), any(String.class));
    }

    @Test
    public void callOffHookReceived_onlyCallOffHookLogicCalled() {

        incomingCallService.syncOnCallStateChange(context, TelephonyManager.CALL_STATE_OFFHOOK, "0500000000");

        verify(callRingingLogic, times(0)).handle(any(Context.class), any(String.class));
        verify(callOffHookLogic, times(1)).handle(any(Context.class), any(String.class));
        verify(callIdleLogic, times(0)).handle(any(Context.class), any(String.class));
    }

    @Test
    public void callIdleReceived_onlyCallidleLogicCalled() {

        incomingCallService.syncOnCallStateChange(context, TelephonyManager.CALL_STATE_IDLE, "0500000000");

        verify(callRingingLogic, times(0)).handle(any(Context.class), any(String.class));
        verify(callOffHookLogic, times(0)).handle(any(Context.class), any(String.class));
        verify(callIdleLogic, times(1)).handle(any(Context.class), any(String.class));
    }

}
