package com.xtone;

import android.content.Context;
import android.telephony.TelephonyManager;

import com.xtone.logging.Logger;
import com.xtone.model.MediaFile;
import com.xtone.model.MediaFileType;
import com.xtone.service.logic.CallRingingLogic;
import com.xtone.service.logic.CallRingingLogicImpl;
import com.xtone.utils.CallSessionUtils;
import com.xtone.utils.Phone2MediaUtils;
import com.xtone.utils.StandOutWindowUtils;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.File;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class CallRingingLogicTest {

    @InjectMocks
    private CallRingingLogicImpl callRingingLogic;

    @Mock
    private Phone2MediaUtils phone2MediaUtils;

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

        when(callSessionUtils.getCallState(eq(context))).thenReturn(TelephonyManager.CALL_STATE_RINGING);

        callRingingLogic.handle(context, incomingNumber);

        verify(callSessionUtils, times(1)).getCallState(eq(context));
        verify(callSessionUtils, times(0)).setCallState(eq(context), any(Integer.class));
        verify(phone2MediaUtils, times(0)).getMediaFile(eq(context), eq(incomingNumber));
        verify(standOutWindowUtils, times(0)).startStandOutWindow(eq(context), eq(incomingNumber), any(MediaFile.class));
    }

    @Test
    public void stateIdleNoMediaFile_setRingingDontDisplayWindow() {
        String incomingNumber = "050000000";

        when(callSessionUtils.getCallState(eq(context))).thenReturn(TelephonyManager.CALL_STATE_IDLE);
        when(phone2MediaUtils.getMediaFile(eq(context), eq(incomingNumber))).thenReturn(null);

        callRingingLogic.handle(context, incomingNumber);

        verify(callSessionUtils, times(1)).getCallState(eq(context));
        verify(callSessionUtils, times(1)).setCallState(eq(context), eq(TelephonyManager.CALL_STATE_RINGING));
        verify(phone2MediaUtils, times(1)).getMediaFile(eq(context), eq(incomingNumber));
        verify(standOutWindowUtils, times(0)).startStandOutWindow(eq(context), eq(incomingNumber), any(MediaFile.class));
    }

    @Test
    public void stateIdleMediaFileFound_setRingingDisplayWindow() {
        String incomingNumber = "050000000";
        MediaFile mediaFile = new MediaFile(new File(""), new File(""), MediaFileType.IMAGE);

        when(callSessionUtils.getCallState(eq(context))).thenReturn(TelephonyManager.CALL_STATE_IDLE);
        when(phone2MediaUtils.getMediaFile(eq(context), eq(incomingNumber))).thenReturn(mediaFile);

        callRingingLogic.handle(context, incomingNumber);

        verify(callSessionUtils, times(1)).getCallState(eq(context));
        verify(callSessionUtils, times(1)).setCallState(eq(context), eq(TelephonyManager.CALL_STATE_RINGING));
        verify(phone2MediaUtils, times(1)).getMediaFile(eq(context), eq(incomingNumber));
        verify(standOutWindowUtils, times(1)).startStandOutWindow(eq(context), eq(incomingNumber), eq(mediaFile));
    }

}
