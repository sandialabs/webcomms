package gov.sandia.webcomms.http.options;

import gov.sandia.webcomms.http.Http;
import gov.sandia.webcomms.http.allowrc.HttpAllowRedirectCriteriaParams;
import gov.sandia.webcomms.http.consumeec.HttpConsumeEntityCriteriaParams;
import gov.sandia.webcomms.http.preemptors.HttpRequestPreemptorParams;
import replete.numbers.NumUtil;
import replete.plugins.SerializableEmptyEqualsObject;
import replete.text.StringLib;

public class HttpRequestOptions extends SerializableEmptyEqualsObject {


    ////////////
    // FIELDS //
    ////////////

    // Defaults

    public static final boolean DEFAULT_CLEAN_URLS                     = true;
    public static final boolean DEFAULT_SAVE_CONTENT                   = true;
    public static final boolean DEFAULT_SAVE_RESPONSE_HEADERS          = true;
    public static final boolean DEFAULT_SAVE_REDIRECTS                 = true;
    public static final boolean DEFAULT_SAVE_REDIRECT_RESPONSE_HEADERS = true;
    public static final boolean DEFAULT_SAVE_REDIRECT_RESPONSE_CONTENT = true;
    public static final boolean DEFAULT_SAVE_SECURITY                  = true;
    public static final boolean DEFAULT_SAVE_REQUEST                   = true;
    public static final String  DEFAULT_OVERRIDE_USER_AGENT            = null;
    public static final boolean DEFAULT_REPLACE_AJAX_FRAGMENT          = true;
    public static final long    DEFAULT_MAX_CONTENT_LENGTH             = 10_485_760;
    public static final int     DEFAULT_TIMEOUT                        = Http.DEFAULT_TIMEOUT;
    public static final String  DEFAULT_PROXY_HOST                     = null;
    public static final int     DEFAULT_PROXY_PORT                     = 0;

    // Core

    private boolean cleanUrls                   = DEFAULT_CLEAN_URLS;
    private boolean saveContent                 = DEFAULT_SAVE_CONTENT;
    private boolean saveResponseHeaders         = DEFAULT_SAVE_RESPONSE_HEADERS;
    private boolean saveRedirects               = DEFAULT_SAVE_REDIRECTS;
    private boolean saveRedirectResponseHeaders = DEFAULT_SAVE_REDIRECT_RESPONSE_HEADERS;
    private boolean saveRedirectResponseContent = DEFAULT_SAVE_REDIRECT_RESPONSE_CONTENT;
    private boolean saveSecurity                = DEFAULT_SAVE_SECURITY;
    private boolean saveRequest                 = DEFAULT_SAVE_REQUEST;
    private String  overrideUserAgent           = DEFAULT_OVERRIDE_USER_AGENT;
    private boolean replaceAjaxFragment         = DEFAULT_REPLACE_AJAX_FRAGMENT;
    private long    maxContentLength            = DEFAULT_MAX_CONTENT_LENGTH;
    private int     timeout                     = DEFAULT_TIMEOUT;
    private String  proxyHost                   = DEFAULT_PROXY_HOST;
    private int     proxyPort                   = DEFAULT_PROXY_PORT;
    private boolean ignoreSslProblems           = false;
    private boolean printExecutionTrace         = false;

    // (Blocks of Code)

    private HttpAllowRedirectCriteriaParams allowRedirectCriteriaParams;
    private HttpConsumeEntityCriteriaParams consumeEntityCriteriaParams;
    private HttpRequestPreemptorParams      requestPreemptorParams;

    //Multiple proxy host/port pairs for proxy chaining
    //Map<String, String> additionalHeaders


    /////////////////
    // CONSTRUCTOR //
    /////////////////

    public HttpRequestOptions() {
        // Empty - exists so can easily pull references
    }

    // Copy constructor - at least used by Main.transform and must be
    // kept up to date with every new field that is added.
    public HttpRequestOptions(HttpRequestOptions other) {
        cleanUrls                   = other.cleanUrls;
        saveContent                 = other.saveContent;
        saveResponseHeaders         = other.saveResponseHeaders;
        saveRedirects               = other.saveRedirects;
        saveRedirectResponseHeaders = other.saveRedirectResponseHeaders;
        saveRedirectResponseContent = other.saveRedirectResponseContent;
        saveSecurity                = other.saveSecurity;
        saveRequest                 = other.saveRequest;
        overrideUserAgent           = other.overrideUserAgent;
        replaceAjaxFragment         = other.replaceAjaxFragment;
        maxContentLength            = other.maxContentLength;
        timeout                     = other.timeout;
        proxyHost                   = other.proxyHost;
        proxyPort                   = other.proxyPort;
        ignoreSslProblems           = other.ignoreSslProblems;
        printExecutionTrace         = other.printExecutionTrace;
        allowRedirectCriteriaParams = other.allowRedirectCriteriaParams;
        consumeEntityCriteriaParams = other.consumeEntityCriteriaParams;
        requestPreemptorParams      = other.requestPreemptorParams;
    }


    //////////////////////////
    // ACCESSORS / MUTATORS //
    //////////////////////////

    // Accessors

    public boolean isCleanUrls() {
        return cleanUrls;
    }
    public boolean isSaveContent() {
        return saveContent;
    }
    public boolean isSaveResponseHeaders() {
        return saveResponseHeaders;
    }
    public boolean isSaveRedirects() {
        return saveRedirects;
    }
    public boolean isSaveRedirectResponseHeaders() {
        return saveRedirectResponseHeaders;
    }
    public boolean isSaveRedirectResponseContent() {
        return saveRedirectResponseContent;
    }
    public boolean isSaveSecurity() {
        return saveSecurity;
    }
    public boolean isSaveRequest() {
        return saveRequest;
    }
    public String getOverrideUserAgent() {
        return overrideUserAgent;
    }
    public boolean isReplaceAjaxFragment() {
        return replaceAjaxFragment;
    }
    public long getMaxContentLength() {
        return maxContentLength;
    }
    public int getTimeout() {
        return timeout;
    }
    public String getProxyHost() {
        return proxyHost;
    }
    public int getProxyPort() {
        return proxyPort;
    }
    public boolean isIgnoreSslProblems() {
        return ignoreSslProblems;
    }
    public boolean isPrintExecutionTrace() {
        return printExecutionTrace;
    }
    public HttpAllowRedirectCriteriaParams getAllowRedirectCriteriaParams() {
        return allowRedirectCriteriaParams;
    }
    public HttpConsumeEntityCriteriaParams getConsumeEntityCriteriaParams() {
        return consumeEntityCriteriaParams;
    }
    public HttpRequestPreemptorParams getRequestPreemptorParams() {
        return requestPreemptorParams;
    }

    // Mutators (Builder)

    public HttpRequestOptions setCleanUrls(boolean cleanUrls) {
        this.cleanUrls = cleanUrls;
        return this;
    }
    public HttpRequestOptions setSaveContent(boolean saveContent) {
        this.saveContent = saveContent;
        return this;
    }
    public HttpRequestOptions setSaveResponseHeaders(boolean saveHeaders) {
        saveResponseHeaders = saveHeaders;
        return this;
    }
    public HttpRequestOptions setSaveRedirects(boolean saveRedirects) {
        this.saveRedirects = saveRedirects;
        return this;
    }
    public HttpRequestOptions setSaveRedirectResponseHeaders(boolean saveRedirectResponseHeaders) {
        this.saveRedirectResponseHeaders = saveRedirectResponseHeaders;
        return this;
    }
    public HttpRequestOptions setSaveRedirectResponseContent(boolean saveRedirectResponseContent) {
        this.saveRedirectResponseContent = saveRedirectResponseContent;
        return this;
    }
    public HttpRequestOptions setSaveSecurity(boolean saveSecurity) {
        this.saveSecurity = saveSecurity;
        return this;
    }
    public HttpRequestOptions setSaveRequest(boolean saveRequest) {
        this.saveRequest = saveRequest;
        return this;
    }
    public HttpRequestOptions setOverrideUserAgent(String overrideUserAgent) {
        this.overrideUserAgent = overrideUserAgent;
        return this;
    }
    public HttpRequestOptions setReplaceAjaxFragment(boolean replaceAjaxFragment) {
        this.replaceAjaxFragment = replaceAjaxFragment;
        return this;
    }
    public HttpRequestOptions setMaxContentLength(long maxContentLength) {
        this.maxContentLength = maxContentLength;
        return this;
    }
    public HttpRequestOptions setTimeout(int timeout) {
        this.timeout = timeout;
        return this;
    }
    public HttpRequestOptions setProxyHost(String proxyHost) {
        this.proxyHost = proxyHost;
        return this;
    }
    public HttpRequestOptions setProxyPort(int proxyPort) {
        this.proxyPort = proxyPort;
        return this;
    }
    public HttpRequestOptions setIgnoreSslProblems(boolean ignoreSslProblems) {
        this.ignoreSslProblems = ignoreSslProblems;
        return this;
    }
    public HttpRequestOptions setPrintExecutionTrace(boolean printExecutionTrace) {
        this.printExecutionTrace = printExecutionTrace;
        return this;
    }
    public HttpRequestOptions setAllowRedirectCriteriaParams(
             HttpAllowRedirectCriteriaParams allowRedirectCriteriaParams) {
        this.allowRedirectCriteriaParams = allowRedirectCriteriaParams;
        return this;
    }
    public HttpRequestOptions setConsumeEntityCriteriaParams(
             HttpConsumeEntityCriteriaParams consumeEntityCriteriaParams) {
        this.consumeEntityCriteriaParams = consumeEntityCriteriaParams;
        return this;
    }
    public HttpRequestOptions setRequestPreemptorParams(HttpRequestPreemptorParams requestPreemptorParams) {
        this.requestPreemptorParams = requestPreemptorParams;
        return this;
    }

    // Mutators (Helper)
    public HttpRequestOptions setProxy(String proxy) {
        int colon = proxy.indexOf(':');
        String host = proxy.substring(0, colon);
        int port = NumUtil.i(proxy.substring(colon + 1));
        setProxyHost(host);
        setProxyPort(port);
        return this;
    }

    public boolean hasProxy() {
        return proxyHost != null && proxyPort != 0;
    }

    public HttpRequestOptions saveAll() {
        return setAllSaveFlags(true);
    }
    public HttpRequestOptions saveNothing() {
        return setAllSaveFlags(false);
    }
    private HttpRequestOptions setAllSaveFlags(boolean enabled) {
        saveContent                 = enabled;
        saveResponseHeaders         = enabled;
        saveRedirects               = enabled;
        saveRedirectResponseHeaders = enabled;
        saveRedirectResponseContent = enabled;
        saveSecurity                = enabled;
        saveRequest                 = enabled;
        return this;
    }

    // This string representation includes all fields regardless
    // of whether their values differ from the default.
    public String toStringLong() {
        StringBuilder b = new StringBuilder();
        append(b, "SC",    saveContent);
        append(b, "SRH",   saveResponseHeaders);
        append(b, "SRD",   saveRedirects);
        append(b, "SRDRH", saveRedirectResponseHeaders);
        append(b, "SRDRC", saveRedirectResponseContent);
        append(b, "SS",    saveSecurity);
        append(b, "SRQ",   saveRequest);
        append(b, "CU",    cleanUrls);
        append(b, "IGSSL", ignoreSslProblems);
        append(b, "PET",   printExecutionTrace);
        b.append(",UA="  + (overrideUserAgent == null ? StringLib.NONE : overrideUserAgent));
        b.append(",PRX=" + (proxyHost == null || proxyPort == 0 ? StringLib.NONE : proxyHost + ":" + proxyPort));
        b.append(",ARC=" + (allowRedirectCriteriaParams == null ? StringLib.NONE : allowRedirectCriteriaParams.getClass().getSimpleName()));
        b.append(",CEC=" + (consumeEntityCriteriaParams == null ? StringLib.NONE : consumeEntityCriteriaParams.getClass().getSimpleName()));
        b.append(",PRE=" + (requestPreemptorParams == null ? StringLib.NONE : requestPreemptorParams.getClass().getSimpleName()));
        return b.toString();
    }
    private void append(StringBuilder b, String name, boolean on) {
        if(on) {
            b.append('+');
        } else {
            b.append('-');
        }
        b.append(name);
    }


    ////////////////
    // OVERRIDDEN //
    ////////////////

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result +
            ((allowRedirectCriteriaParams == null) ? 0 : allowRedirectCriteriaParams.hashCode());
        result = prime * result + (cleanUrls ? 1231 : 1237);
        result = prime * result +
            ((consumeEntityCriteriaParams == null) ? 0 : consumeEntityCriteriaParams.hashCode());
        result = prime * result + (ignoreSslProblems ? 1231 : 1237);
        result = prime * result + (int) (maxContentLength ^ (maxContentLength >>> 32));
        result = prime * result + ((overrideUserAgent == null) ? 0 : overrideUserAgent.hashCode());
        result = prime * result + (printExecutionTrace ? 1231 : 1237);
        result = prime * result + ((proxyHost == null) ? 0 : proxyHost.hashCode());
        result = prime * result + proxyPort;
        result = prime * result + (replaceAjaxFragment ? 1231 : 1237);
        result = prime * result +
            ((requestPreemptorParams == null) ? 0 : requestPreemptorParams.hashCode());
        result = prime * result + (saveContent ? 1231 : 1237);
        result = prime * result + (saveRedirectResponseContent ? 1231 : 1237);
        result = prime * result + (saveRedirectResponseHeaders ? 1231 : 1237);
        result = prime * result + (saveRedirects ? 1231 : 1237);
        result = prime * result + (saveRequest ? 1231 : 1237);
        result = prime * result + (saveResponseHeaders ? 1231 : 1237);
        result = prime * result + (saveSecurity ? 1231 : 1237);
        result = prime * result + timeout;
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if(this == obj) {
            return true;
        }
        if(!super.equals(obj)) {
            return false;
        }
        if(getClass() != obj.getClass()) {
            return false;
        }
        HttpRequestOptions other = (HttpRequestOptions) obj;
        if(allowRedirectCriteriaParams == null) {
            if(other.allowRedirectCriteriaParams != null) {
                return false;
            }
        } else if(!allowRedirectCriteriaParams.equals(other.allowRedirectCriteriaParams)) {
            return false;
        }
        if(cleanUrls != other.cleanUrls) {
            return false;
        }
        if(consumeEntityCriteriaParams == null) {
            if(other.consumeEntityCriteriaParams != null) {
                return false;
            }
        } else if(!consumeEntityCriteriaParams.equals(other.consumeEntityCriteriaParams)) {
            return false;
        }
        if(ignoreSslProblems != other.ignoreSslProblems) {
            return false;
        }
        if(maxContentLength != other.maxContentLength) {
            return false;
        }
        if(overrideUserAgent == null) {
            if(other.overrideUserAgent != null) {
                return false;
            }
        } else if(!overrideUserAgent.equals(other.overrideUserAgent)) {
            return false;
        }
        if(printExecutionTrace != other.printExecutionTrace) {
            return false;
        }
        if(proxyHost == null) {
            if(other.proxyHost != null) {
                return false;
            }
        } else if(!proxyHost.equals(other.proxyHost)) {
            return false;
        }
        if(proxyPort != other.proxyPort) {
            return false;
        }
        if(replaceAjaxFragment != other.replaceAjaxFragment) {
            return false;
        }
        if(requestPreemptorParams == null) {
            if(other.requestPreemptorParams != null) {
                return false;
            }
        } else if(!requestPreemptorParams.equals(other.requestPreemptorParams)) {
            return false;
        }
        if(saveContent != other.saveContent) {
            return false;
        }
        if(saveRedirectResponseContent != other.saveRedirectResponseContent) {
            return false;
        }
        if(saveRedirectResponseHeaders != other.saveRedirectResponseHeaders) {
            return false;
        }
        if(saveRedirects != other.saveRedirects) {
            return false;
        }
        if(saveRequest != other.saveRequest) {
            return false;
        }
        if(saveResponseHeaders != other.saveResponseHeaders) {
            return false;
        }
        if(saveSecurity != other.saveSecurity) {
            return false;
        }
        if(timeout != other.timeout) {
            return false;
        }
        return true;
    }

    // This method is implemented to be compatible with the
    // output goals of gov.sandia.webcomms.http.HttpAppMain.  This
    // method only includes information that differs from
    // the default options.
    @Override
    public String toString() {
        StringBuilder b = new StringBuilder();
        if(!saveContent) {
            b.append("-SC");
        }
        if(!saveResponseHeaders) {
            b.append("-SRH");
        }
        if(!saveRedirects) {
            b.append("-SRD");
        }
        if(!saveRedirectResponseHeaders) {
            b.append("-SRDRH");
        }
        if(!saveRedirectResponseContent) {
            b.append("-SRDRC");
        }
        if(!saveSecurity) {
            b.append("-SS");
        }
        if(!saveRequest) {
            b.append("-SRQ");
        }
        if(!cleanUrls) {
            b.append("-CU");
        }
        if(ignoreSslProblems) {
            b.append("+IGSSL");
        }
        if(printExecutionTrace) {
            b.append("+PET");
        }
        if(overrideUserAgent != null) {
            b.append("+UA");
        }
        if(proxyHost != null && proxyPort != 0) {
            b.append("+PRX");
        }
        if(allowRedirectCriteriaParams != null) {
            b.append("+ARC");
        }
        if(consumeEntityCriteriaParams != null) {
            b.append("+CEC");
        }
        if(requestPreemptorParams != null) {
            b.append("+PRE");
        }
        String ret = b.toString().trim();
        if(ret.isEmpty()) {
            ret = "(Default)";
        }
        return ret;
    }
}
