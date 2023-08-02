package gov.sandia.webcomms.http.internal;

import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;

import gov.sandia.webcomms.http.HttpRequestWrapper;
import gov.sandia.webcomms.http.options.HttpRequestOptions;
import gov.sandia.webcomms.http.rsc.HttpResource;
import replete.text.StringLib;
import replete.text.StringUtil;

public class RequestTrace {


    ////////////
    // FIELDS //
    ////////////

    private static final String[] DEBUG_LEVEL_SYM = {">", "*", "-"};
    private static AtomicInteger nextId = new AtomicInteger(0);

    private int id;
    private HttpRequestWrapper requestWrapper;
    private String redirectRequestUrl;              // Set after construction
    private String providingIpPort;
    private Exception overrideException;
    protected Map<String, String> requestHeaders;   // Lazily instantiated, depending on HttpRequestOptions, init'ed by addHeader
    private int debugLevel = 0;
    private Exception trustManagerException;


    //////////////////
    // CONSTRUCTORS //
    //////////////////

    public RequestTrace(HttpRequestWrapper requestWrapper) {
        id = nextId.getAndIncrement();
        this.requestWrapper = requestWrapper;
    }


    //////////////////////////
    // ACCESSORS / MUTATORS //
    //////////////////////////

    // Accessors

    public HttpResource getResource() {
        return requestWrapper.getResource();
    }
    public HttpRequestOptions getOptions() {
        return requestWrapper.getOptions();
    }
    public String getRedirectRequestUrl() {
        return redirectRequestUrl;
    }
    public String getProvidingIpPort() {
        return providingIpPort;
    }
    public Exception getOverrideException() {
        return overrideException;
    }
    public Map<String, String> getRequestHeaders() {     // Not unmodifiable.  For transfer.
        return requestHeaders;
    }
    public Exception getTrustManagerException() {
        return trustManagerException;
    }

    // Mutators

    public RequestTrace setRedirectRequestUrl(String redirectRequestUrl) {
        this.redirectRequestUrl = redirectRequestUrl;
        return this;
    }
    public RequestTrace setProvidingIpPort(String providingIpPort) {
        this.providingIpPort = providingIpPort;
        return this;
    }
    public RequestTrace setOverrideException(Exception overrideException) {
        this.overrideException = overrideException;
        return this;
    }
    public RequestTrace addRequestHeader(String key, String value) {
        if(requestHeaders == null) {
            requestHeaders = new TreeMap<>();
        }
        requestHeaders.put(key, value);
        return this;
    }
    public RequestTrace clearRequestHeaders() {
        requestHeaders = null;
        return this;
    }
    public RequestTrace setTrustManagerException(Exception trustManagerException) {
        this.trustManagerException = trustManagerException;
        return this;
    }


    ///////////
    // DEBUG //
    ///////////

    public void debugBlockOpen(Object msg) {
        if(requestWrapper.getOptions().isPrintExecutionTrace() || requestWrapper.getTraceCallback() != null) {
            debugStep(msg);
            debugLevel++;
        }
    }

    public void debugStep(Object msg) {
        if(requestWrapper.getOptions().isPrintExecutionTrace() || requestWrapper.getTraceCallback() != null) {
            StringBuilder builder = new StringBuilder();
            builder.append("[HttpRq#");
            builder.append(id);
            builder.append("] ");
            if(debugLevel > 0) {
                String sp = StringUtil.spaces(1 + (debugLevel - 1) *2);
                String sym = DEBUG_LEVEL_SYM[(debugLevel - 1) % DEBUG_LEVEL_SYM.length];
                builder.append(sp);
                builder.append(sym);
                builder.append(" ");
            }
            builder.append(msg == null ? StringLib.NULL : msg.toString());
            String line = builder.toString();
            if(requestWrapper.getOptions().isPrintExecutionTrace()) {
                System.out.println(line);
            }
            if(requestWrapper.getTraceCallback() != null) {
                requestWrapper.getTraceCallback().accept(line);
            }
        }
    }

    public void debugBlockClose() {
        if(requestWrapper.getOptions().isPrintExecutionTrace() || requestWrapper.getTraceCallback() != null) {
            debugLevel--;
        }
    }
}
