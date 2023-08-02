package gov.sandia.webcomms.http;

// https://www.w3.org/Protocols/rfc2616/rfc2616-sec6.html

public class HttpResponseCode {


    ////////////
    // FIELDS //
    ////////////

    private int statusCode;
    private String reasonPhrase;
    private String description;


    //////////////////
    // CONSTRUCTORS //
    //////////////////

    public HttpResponseCode(int statusCode, String reasonPhrase) {
        this(statusCode, reasonPhrase, null);
    }
    public HttpResponseCode(int statusCode, String reasonPhrase, String description) {
        this.statusCode   = statusCode;
        this.reasonPhrase = reasonPhrase;
        this.description  = description;
    }


    ///////////////
    // ACCESSORS //
    ///////////////

    public int getStatusCode() {
        return statusCode;
    }
    public String getReasonPhrase() {
        return reasonPhrase;
    }
    public String getDescription() {
        return description;
    }


    ////////////////
    // OVERRIDDEN //
    ////////////////

    @Override
    public String toString() {
        return statusCode + " " + reasonPhrase + (description != null ? " (" + description + ")" : "");
    }
}
