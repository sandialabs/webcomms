package gov.sandia.webcomms.http.errors;

public class PreemptionStoppedException extends RuntimeException {

    public PreemptionStoppedException() {
    }

    public PreemptionStoppedException(String message) {
        super(message);
    }

    public PreemptionStoppedException(Throwable cause) {
        super(cause);
    }

    public PreemptionStoppedException(String message, Throwable cause) {
        super(message, cause);
    }

    public PreemptionStoppedException(String message, Throwable cause, boolean enableSuppression,
                                      boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

}
