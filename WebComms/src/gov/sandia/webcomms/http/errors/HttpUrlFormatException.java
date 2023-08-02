package gov.sandia.webcomms.http.errors;

public class HttpUrlFormatException extends RuntimeException {


    ///////////
    // FIELD //
    ///////////

    private String uriSynExMsg = null;


    /////////////////
    // CONSTRUCTOR //
    /////////////////

    public HttpUrlFormatException(String message) {
        super(message);
    }
    public HttpUrlFormatException(String message, String uriSynExMsg) {
        super(message);
        this.uriSynExMsg = uriSynExMsg;        // This is also in the message, so this field is a bonus
    }
    public HttpUrlFormatException(String message, Throwable cause) {
        super(message, cause);
    }
    public HttpUrlFormatException(String message, Throwable cause,
                                  boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }


    ///////////////
    // ACCESSORS //
    ///////////////

    public String getUriSynExMsg() {
        return uriSynExMsg;
    }
}
