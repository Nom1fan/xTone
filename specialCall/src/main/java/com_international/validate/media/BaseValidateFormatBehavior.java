package com_international.validate.media;

import com_international.utils.MediaFileUtils;
import com_international.utils.UtilityFactory;

/**
 * Created by Mor on 02/06/2017.
 */

public abstract class BaseValidateFormatBehavior implements ValidateMediaFormatBehavior{

    protected MediaFileUtils mediaFileUtils = UtilityFactory.instance().getUtility(MediaFileUtils.class);
}
