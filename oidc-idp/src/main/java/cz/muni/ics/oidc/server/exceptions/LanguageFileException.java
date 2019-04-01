package cz.muni.ics.oidc.server.exceptions;

/**
 * Excepiton thrown when controller cannot load properties file for JSP with messages.
 *
 * @author Dominik František Bučík <bucik@ics.muni.cz>
 */
public class LanguageFileException extends RuntimeException {

    public LanguageFileException() {
        super();
    }

    public LanguageFileException(String s) {
        super(s);
    }

    public LanguageFileException(String s, Throwable throwable) {
        super(s, throwable);
    }

    public LanguageFileException(Throwable throwable) {
        super(throwable);
    }

    protected LanguageFileException(String s, Throwable throwable, boolean b, boolean b1) {
        super(s, throwable, b, b1);
    }
}
