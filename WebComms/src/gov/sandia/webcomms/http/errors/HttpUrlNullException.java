package gov.sandia.webcomms.http.errors;

public class HttpUrlNullException extends RuntimeException {
    public HttpUrlNullException(String message) {
        super(message);
    }
    public HttpUrlNullException(String message, Throwable cause,
                                  boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
