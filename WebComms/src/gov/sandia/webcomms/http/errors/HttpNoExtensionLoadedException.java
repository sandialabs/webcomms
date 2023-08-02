package gov.sandia.webcomms.http.errors;

public class HttpNoExtensionLoadedException extends RuntimeException {
    public HttpNoExtensionLoadedException(String message) {
        super(message, null, true, false);
    }
}
