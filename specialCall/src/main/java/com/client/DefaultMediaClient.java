package com.client;

import android.content.Context;

import com.data.objects.Contact;
import com.data.objects.DefaultMediaData;
import com.data.objects.DefaultMediaDataContainer;
import com.enums.SpecialMediaType;
import com.model.response.Response;

import java.util.List;

/**
 * Created by Mor on 24/05/2017.
 */

public interface DefaultMediaClient extends Client {

    List<DefaultMediaDataContainer> getDefaultMediaData(Context context, List<String> uids, SpecialMediaType specialMediaType);
}
