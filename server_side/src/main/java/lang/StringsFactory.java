package lang;

import ServerObjects.ILangStrings;
import ServerObjects.IStringsFactory;
import log.Logged;

/**
 * Created by Mor on 21/04/2016.
 */
public class StringsFactory extends Logged implements IStringsFactory {

    private static StringsFactory _instance;

    private StringsFactory() {
        super();
    }

    public static StringsFactory instance() {

        if(_instance == null)
            _instance = new StringsFactory();
        return _instance;
    }

    @Override
    public ILangStrings getStrings(String locale) {

        switch(locale)
        {
            case ILangStrings.ENGLISH:
                return new EnglishStrings();

            case ILangStrings.HEBREW:
            case ILangStrings.IVRIT:
                return new HebrewStrings();
            default:
                _logger.warning(String.format("Invalid locale '%s'. Assuming English", locale));
                return new EnglishStrings();
        }
    }
}
