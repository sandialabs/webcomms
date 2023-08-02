package gov.sandia.webcomms.http;

import org.apache.http.client.methods.HttpUriRequest;

import gov.sandia.webcomms.http.options.HttpRequestOptions;
import gov.sandia.webcomms.http.rsc.HttpResource;

public class HttpRequestWrapper {


    ////////////
    // FIELDS //
    ////////////

    private HttpUriRequest request;
    private HttpRequestOptions options;
    private HttpResource resource;
    private RequestExecutionTraceCallback traceCallback;


    //////////////////
    // CONSTRUCTORS //
    //////////////////

    public HttpRequestWrapper(HttpUriRequest request, HttpResource resource,
                              HttpRequestOptions options) {
        this.request = request;
        this.options = options;
        this.resource = resource;
    }


    //////////////////////////
    // ACCESSORS / MUTATORS //
    //////////////////////////

    // Accessors

    public HttpUriRequest getRequest() {
        return request;
    }
    public HttpRequestOptions getOptions() {
        return options;
    }
    public HttpResource getResource() {
        return resource;
    }
    public RequestExecutionTraceCallback getTraceCallback() {
        return traceCallback;
    }

    // Mutators

    public HttpRequestWrapper setTraceCallback(RequestExecutionTraceCallback traceCallback) {
        this.traceCallback = traceCallback;
        return this;
    }


    //////////
    // MISC //
    //////////

    public void abort() {
        request.abort();
    }
}
