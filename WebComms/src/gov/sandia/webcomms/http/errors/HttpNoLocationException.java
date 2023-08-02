package gov.sandia.webcomms.http.errors;

public class HttpNoLocationException extends RuntimeException {
    public HttpNoLocationException(String message) {
        super(message, null, true, false);
    }
}
