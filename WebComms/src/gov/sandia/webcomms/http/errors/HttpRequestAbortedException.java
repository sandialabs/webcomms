package gov.sandia.webcomms.http.errors;

public class HttpRequestAbortedException extends RuntimeException {
    public HttpRequestAbortedException(String message, Throwable cause,
                                     boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
