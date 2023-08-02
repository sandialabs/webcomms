package gov.sandia.webcomms.http.errors;

public class HttpCircularRedirectException extends RuntimeException {
    public HttpCircularRedirectException(String message, Throwable cause,
                                     boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
