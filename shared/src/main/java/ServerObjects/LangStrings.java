package ServerObjects;

/**
 * Created by Mor on 18/04/2016.
 */
public interface LangStrings {

    Languages getLanguage();

    String upload_failed();

    String oops();

    String media_ready_title();

    String media_ready_body();

    String media_undelivered_title();

    String media_undelivered_body();

    String media_cleared_title();

    String media_cleared_body();

    String your_verification_code();

    enum Languages {

        ENGLISH("en"),
        HEBREW("he"),
        IVRIT("iw");

        private final String text;

        Languages(String text) {
            this.text = text;
        }

        @Override
        public String toString() {
            return text;
        }
    }
}
