package com.client;

import android.content.Context;

import com.data.objects.Contact;
import com.data.objects.DefaultMediaData;
import com.enums.SpecialMediaType;
import com.model.response.Response;

import java.util.List;

/**
 * Created by Mor on 24/05/2017.
 */

public interface MediaClient extends Client {

    List<DefaultMediaData> getDefaultMediaData(String phoneNumber, SpecialMediaType specialMediaType);
}
