package com.validate.media;

import com.utils.MediaFileUtils;
import com.utils.UtilityFactory;

/**
 * Created by Mor on 02/06/2017.
 */

public abstract class BaseValidateFormatBehavior implements ValidateMediaFormatBehavior{

    protected MediaFileUtils mediaFileUtils = UtilityFactory.instance().getUtility(MediaFileUtils.class);
}
