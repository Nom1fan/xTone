package com_international.client;

import android.content.Context;

import com_international.data.objects.DefaultMediaDataContainer;
import com_international.enums.SpecialMediaType;

import java.util.List;

/**
 * Created by Mor on 24/05/2017.
 */

public interface DefaultMediaClient extends Client {

    List<DefaultMediaDataContainer> getDefaultMediaData(Context context, List<String> uids, SpecialMediaType specialMediaType);
}
