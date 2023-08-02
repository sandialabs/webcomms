package gov.sandia.webcomms.http.errors;

public class HttpMaximumRedirectException extends RuntimeException {
    public HttpMaximumRedirectException(String message, Throwable cause,
                                     boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
