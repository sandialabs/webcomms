
package gov.sandia.webcomms.http;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Timer;
import java.util.TimerTask;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.TrustManager;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.NTCredentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CircularRedirectException;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.RedirectException;
import org.apache.http.client.RedirectStrategy;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpOptions;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.methods.HttpTrace;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.params.ClientPNames;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.DnsResolver;
import org.apache.http.conn.params.ConnRoutePNames;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.PoolingClientConnectionManager;
import org.apache.http.impl.conn.SchemeRegistryFactory;
import org.apache.http.params.CoreProtocolPNames;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.pool.PoolStats;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import gov.sandia.webcomms.http.consumeec.HttpConsumeEntityCriteria;
import gov.sandia.webcomms.http.consumeec.HttpConsumeEntityCriteriaGenerator;
import gov.sandia.webcomms.http.consumeec.HttpConsumeEntityCriteriaParams;
import gov.sandia.webcomms.http.errors.HttpCircularRedirectException;
import gov.sandia.webcomms.http.errors.HttpMaximumRedirectException;
import gov.sandia.webcomms.http.errors.HttpNoExtensionLoadedException;
import gov.sandia.webcomms.http.errors.HttpNoLocationException;
import gov.sandia.webcomms.http.errors.HttpRequestAbortedException;
import gov.sandia.webcomms.http.errors.HttpUrlFormatException;
import gov.sandia.webcomms.http.errors.HttpUrlNullException;
import gov.sandia.webcomms.http.errors.InvalidPreemptionException;
import gov.sandia.webcomms.http.errors.PreemptionStoppedException;
import gov.sandia.webcomms.http.internal.IdleConnectionMonitorThread;
import gov.sandia.webcomms.http.internal.InternalDnsResolver;
import gov.sandia.webcomms.http.internal.InternalHostnameVerifier;
import gov.sandia.webcomms.http.internal.InternalHttpClient;
import gov.sandia.webcomms.http.internal.InternalHttpSocketFactory;
import gov.sandia.webcomms.http.internal.InternalHttpsSocketFactory;
import gov.sandia.webcomms.http.internal.InternalRedirectStrategy;
import gov.sandia.webcomms.http.internal.InternalRequestInterceptor;
import gov.sandia.webcomms.http.internal.InternalRoutePlanner;
import gov.sandia.webcomms.http.internal.InternalTrustManager;
import gov.sandia.webcomms.http.internal.RequestTrace;
import gov.sandia.webcomms.http.options.HttpRequestOptions;
import gov.sandia.webcomms.http.preemptors.HttpRequestPreemptor;
import gov.sandia.webcomms.http.preemptors.HttpRequestPreemptorGenerator;
import gov.sandia.webcomms.http.preemptors.HttpRequestPreemptorParams;
import gov.sandia.webcomms.http.rsc.HttpResource;
import gov.sandia.webcomms.http.stats.UrlStats;
import gov.sandia.webcomms.http.util.UriCleaner;
import gov.sandia.webcomms.logging.LogUtil;
import gov.sandia.webcomms.plugin.WebCommsPlugin;
import replete.collections.mm.MembershipMap;
import replete.io.BeyondMaxException;
import replete.io.FileUtil;
import replete.logging.ExceptionMetaInfoManager;
import replete.logging.LogCode;
import replete.logging.LogCodeManager;
import replete.logging.LoggingInitializer;
import replete.numbers.NumUtil;
import replete.plugins.Generator;
import replete.plugins.PluginManager;
import replete.plugins.RepletePlugin;
import replete.profiler.RProfiler;
import replete.text.StringUtil;
import replete.util.ReflectionUtil;

// Note: User agent is initialized by DefaultHttpClient to:
//   Apache-HttpClient/4.2.1 (java 1.5)
// But can be set to any string afterwards, including "",
// "x", or null.

// NOTE: Apache's HttpClient is at end-of-life at this point.
// Some time in the future we'll have to replace HttpClient
// within WebComms to stay up on newer capabilities
//   http://hc.apache.org/httpclient-3.x/

// Some day, WebComms should do FTP...

// Test client-level parameters some day...
//  HttpParams params = HTTPConnectionManager.getConnectionManager().getParams();
//  params.setParameter("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
//  params.setParameter("Referer", "https://www.google.com/");
//  params.setParameter("Accept-Encoding", "gzip,deflate,sdch");
//  params.setParameter("Accept-Language", "en-US,en;q=0.8");
//  params.setParameter("Accept-Charset", "ISO-8859-1,utf-8;q=0.7,*;q=0.3");
//  params.setParameter("Cache-Control", "max-age=0");
//  params.setParameter("Connection", "keep-alive");
//  params.setParameter("Host", "www.whatismyip.com");

// Figure out difference between InetAddr revdns lookups and JNDI lookups:
// http://stackoverflow.com/questions/7097623/need-to-perform-a-reverse-dns-lookup-of-a-particular-ip-address-in-java

// http://stackoverflow.com/questions/3165520/why-does-httpclient-throw-a-sockettimeoutexception-when-executing-post
// http://brian.olore.net/wp/2009/08/apache-httpclient-timeout/
// https://www.mail-archive.com/httpclient-user@jakarta.apache.org/msg03274.html
// http://hc.apache.org/httpclient-3.x/tutorial.html
// http://hc.apache.org/httpcomponents-client-ga/httpclient/examples/org/apache/http/examples/client/ClientAbortMethod.java
// http://hc.apache.org/httpcomponents-client-ga/tutorial/html/connmgmt.html
// http://hc.apache.org/httpcomponents-client-4.2.x/tutorial/html/httpagent.html
// http://hc.apache.org/httpclient-3.x/preference-api.html
// http://www.baeldung.com/httpclient-timeout
// http://stackoverflow.com/questions/18184899/what-is-the-difference-between-the-setconnectiontimeout-setsotimeout-and-http
// http://www.w3.org/Protocols/rfc2616/rfc2616-sec10.html
// http://www.w3.org/Protocols/rfc2616/rfc2616-sec14.html
// http://grepcode.com/file/repo1.maven.org/maven2/org.apache.httpcomponents/httpclient/4.2.1/org/apache/http/conn/ssl/SSLSocketFactory.java/
// http://grepcode.com/file/repo1.maven.org/maven2/org.apache.httpcomponents/httpclient/4.2.1/org/apache/http/impl/conn/ManagedClientConnectionImpl.java#279
// https://issues.apache.org/jira/browse/HTTPCLIENT-1062
// http://stackoverflow.com/questions/875467/java-client-certificates-over-https-ssl
// http://security.stackexchange.com/questions/5126/whats-the-difference-between-ssl-tls-and-https
// http://javaskeleton.blogspot.com/2010/07/avoiding-peer-not-authenticated-with.html
// http://httpcomponents.10934.n7.nabble.com/quot-Keep-alive-quot-stale-connections-and-socket-reuse-td15315.html

// This method allows you to pass in your already-configured request
// object allowing you to specify custom headers and parameters
// beforehand.
// TODO: This SEEMS like where you would add your additional
// request parameters. However, even though Accept-Encoding
// was working on headers.jsontest.com, it was not actually
// returning that "header".  What's diff between header
// and parameter?
//   HttpParams params = getParams();
//   params.setParameter("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
//   params.setParameter("Referer", "https://www.google.com/");
//   params.setParameter("Accept-Encoding", "gzip,deflate,sdch");
//   request.setParams(params);
//   request.setHeader("Accept-Encoding", "gzip,deflate,sdch");

public class Http {


    ///////////////
    // SINGLETON //
    ///////////////

    private static final Http instance = new Http();
    public static Http getInstance() {
        return instance;
    }


    /////////////
    // LOGGING //
    /////////////

    private static Logger logger = Logger.getLogger(Http.class);
    private static LogCode LC_DM_EX = LogCodeManager.create("WebComms", Http.class, "D-x", "Download failed (by exception)");
    private static LogCode LC_DM_RC = LogCodeManager.create("WebComms", Http.class, "D-r", "Download failed (by HTTP response code)");
    private static LogCode LC_RC_UN = LogCodeManager.create("WebComms", Http.class, "RC!", "Unexpected status code or reason phrase");
    private static LogCode LC_EX    = LogCodeManager.create("WebComms", Http.class, "E!",  "Error aborting request or consuming entity (very rare)", true);
    private static LogCode LC_UP_RQ = LogCodeManager.create("WebComms", Http.class, "U%q", "URL was cleaned (changed) on original request");
    public static  LogCode LC_UP_RD = LogCodeManager.create("WebComms", Http.class, "U%d", "URL was cleaned (changed) on a server redirect");
    public static  LogCode LC_LX    = LogCodeManager.create("WebComms", Http.class, "L!",  "Location header error (very rare)", true);
    public static  LogCode LC_PP    = LogCodeManager.create("WebComms", Http.class, "P+",  "Request was preempted");
    public static  LogCode LC_PX    = LogCodeManager.create("WebComms", Http.class, "P!",  "Invalid preemption result");
    public static  LogCode LC_SX    = LogCodeManager.create("WebComms", Http.class, "S!",  "Unexpected URL stats add error");

    // TODO: Convert this entire class over to 4.3 to stay up to date.
    // TODO: Test whether or not you can instantiate a new client once per
    // connection or not.  Kinda would need to in order to have per-request
    // time outs. http://www.baeldung.com/httpclient-timeout
//    int timeout = 5;
//    RequestConfig config = RequestConfig.custom()
//      .setConnectTimeout(timeout * 1000)
//      .setConnectionRequestTimeout(timeout * 1000)
//      .setSocketTimeout(timeout * 1000).build();
//    CloseableHttpClient client =
//      HttpClientBuilder.create().setDefaultRequestConfig(config).build();


    ////////////////
    // EXCEPTIONS //
    ////////////////

    static {
        ExceptionMetaInfoManager.describe(BeyondMaxException.class,            "A requested resource was abandoned due to its large size");
        ExceptionMetaInfoManager.describe(HttpCircularRedirectException.class, "A circular redirect was detected");
        ExceptionMetaInfoManager.describe(HttpMaximumRedirectException.class,  "The maximum number of redirects was reached");
        ExceptionMetaInfoManager.describe(HttpNoLocationException.class,       "A resource redirect failed to contain a 'location' header");
        ExceptionMetaInfoManager.describe(HttpUrlFormatException.class,        "An invalid URL was requested");
        ExceptionMetaInfoManager.describe(SSLPeerUnverifiedException.class,    "Proper certificate probably not installed in this JRE - have you used keytool?", true);
    }


    ////////////
    // FIELDS //
    ////////////

    // Constants
    public static final Map<String, String> DEFAULT_HEADER_COLLAPSE_PATTERNS;
    static {
                                                           //Sat, 26 Mar 2016 15:52:58 GMT
        String allDigitsPattern = "^[0-9]+$";
        String httpDatePattern  = "^[A-Za-z]{3}, [0-9]{1,2}[ -][A-Za-z]{3}[ -][0-9]{4} [0-9]{2}:[0-9]{2}:[0-9]{2} [A-Za-z]{3}$";
        String anythingPattern  = ".*";

        Map<String, String> temp = new TreeMap<>();
        temp.put(HttpHeaders.AGE,            allDigitsPattern);
        temp.put(HttpHeaders.CONTENT_LENGTH, allDigitsPattern);
        temp.put(HttpHeaders.DATE,           httpDatePattern);
        temp.put(HttpHeaders.EXPIRES,        httpDatePattern);
        temp.put(HttpHeaders.LAST_MODIFIED,  httpDatePattern);
        temp.put("Set-Cookie",               anythingPattern);

        DEFAULT_HEADER_COLLAPSE_PATTERNS = Collections.unmodifiableMap(temp);

        // These keys probably should not included in the map:
        //   "Age", "Content-Length", "Date", "ETag"?, "Expires", "Fastly-Debug-Digest",
        //   "Last-Modified", "Set-Cookie", "Via", some "X-nnnn" keys

        // Date Reference: e.g. "Tue, 22 Mar 2016 23:42:05 GMT",
        // https://www.w3.org/Protocols/rfc2616/rfc2616-sec3.html#sec3.3.1
        // Dashes in pattern above technically not valid but putting
        // them in here anyway because witnessed them at one point.
    }

    private static final int DEFAULT_BUFFER_SIZE = 1024 * 4;   // This should allow us to parse the maximum size site map size
    private static final int DEFAULT_CONN_CACHE_SIZE = 100;
    public static final int DEFAULT_TIMEOUT = 50 * 1000;       // ms.  This could be in HttpRequestOptions, but refactor needed.

    private final Object FORCE_SERIAL_LOCK = new Object();

    // Core
    private DefaultHttpClient client;
    private IdleConnectionMonitorThread monitor;
    private boolean usePreemptiveAuth = false;

    // Config
    private HttpRequestOptions defaultOptions;
    private int timeout = DEFAULT_TIMEOUT;                     // ms.  TODO: This could one day be apart of HttpRequestOptions!!
    private int connCacheSize = DEFAULT_CONN_CACHE_SIZE;
    private boolean recordHeaderStats = false;                 // WARNING: Could cause significant performance issues
    private Map<String, String> headerCollapsePatterns = null;
    private Map<String, Pattern> headerCollapsePatternsCompiled = null;
    private boolean forceSerialExecution;

    // Request/Response Counts (for non-preempted requests)
    private AtomicInteger numRequests         = new AtomicInteger();
    private AtomicInteger numActualSizes      = new AtomicInteger();
    private AtomicInteger numReportedSizes    = new AtomicInteger();
    private AtomicInteger numCorrectSizes     = new AtomicInteger();
    private AtomicInteger numIncorrectSizes   = new AtomicInteger();
    private AtomicInteger numIncorrectEntCt   = new AtomicInteger();
    private AtomicInteger numIncorrectEntCl   = new AtomicInteger();
    private AtomicInteger numIncorrectEntCe   = new AtomicInteger();
    private AtomicInteger numUnknownSizes     = new AtomicInteger();
    private AtomicInteger numTooBig           = new AtomicInteger();
    private AtomicInteger numErrors           = new AtomicInteger();
    private AtomicInteger numRedirects        = new AtomicInteger();
    private AtomicInteger numCleanReq         = new AtomicInteger();
    private AtomicInteger numCleanRedir       = new AtomicInteger();
    private AtomicInteger numUnexpectedRc     = new AtomicInteger();

    // Request/Response Sizes (for non-preempted requests)
    private AtomicLong sumActualSizes   = new AtomicLong();
    private AtomicLong sumReportedSizes = new AtomicLong();

    // Request/Response Other (for non-preempted requests)
    private Map<Integer, Integer> statusCodeHistogram = new TreeMap<>();
    private Map<String, Integer> exceptionCounts = new TreeMap<>();
    private MembershipMap<String, String> allSeenHeaders = new MembershipMap<>();
    private UrlStats urlStats = new UrlStats();
    // ^TODO: not completely impl yet....  TODO: Don't really need TreeMap inside...

    // Request/Response Preemption
    private AtomicInteger numRequestsPreempted = new AtomicInteger();

    // Miscellaneous
    private Map<Long, RequestTrace> requestTraces = new ConcurrentHashMap<>();


    /////////////////
    // CONSTRUCTOR //
    /////////////////

    protected Http() {
        ClientConnectionManager cm = createConnectionManager();
        client = new InternalHttpClient(cm, this);
        copyProxyFromSystem();
        client.setCookieStore(null);
        client.setCookieSpecs(null);

        setHeaderCollapsePatterns(DEFAULT_HEADER_COLLAPSE_PATTERNS);

        // Disallow circular redirects for now.  A circular redirect would almost
        // surely result in hitting the maximum number, so this should short-
        // circuit that potentially very long process.  Might be nice to know this
        // happened within the resource though.  I believe you should be able to
        // inspect the redirects list / create a computed accessor to check for you.
        client.getParams().setBooleanParameter(ClientPNames.ALLOW_CIRCULAR_REDIRECTS, false);

        // These seriously just add to a String->Object map with no validation
        // right now, so hard to be  sure which are the right ones.  Found this
        // out inspecting
        //     PlainSocketFactory.createSocket()/((ClientParamsStack)params).getClientParams()
        // This is possibly different in 4.3.x.

        // Set the maximum number of times a request can be redirected to another
        // URL.  Change from default 100.  Can make this configurable.  Maybe
        // the # of redirects can be signal for a misbehaving site.
        client.getParams().setIntParameter(ClientPNames.MAX_REDIRECTS, 20);

        // It's unclear for this version of HTTP client which
        // key is to be used for the connection manager timeout.
        // http://stackoverflow.com/questions/18184899/what-is-the-difference-between-the-setconnectiontimeout-setsotimeout-and-http
        client.getParams().setParameter(ClientPNames.CONN_MANAGER_TIMEOUT, 60000L);   // "http.conn-manager.timeout"
        client.getParams().setParameter("http.connection-manager.timeout", 60000L);   // why not the same?

        registerHttpSocketFactory();
        registerHttpsSocketFactory();

        //HttpClientParams.setRedirecting(client.getParams(), false);  This would turn off redirecting altogether.  Analagous to "http.protocol.handle-redirects" I believe
        //HttpClientParams.setConnectionManagerTimeout(client.getParams(), 60000L);  // not sure how this compares to the above, or setTimeout
        //HttpClientParams.CONNECTION_MANAGER_TIMEOUT             // "http.connection-manager.timeout"  not sure if this is useful

        setTimeout(timeout);
        enablePreemptiveAuthentication();
        setRedirectStrategy(new InternalRedirectStrategy(this));
        monitor = new IdleConnectionMonitorThread(cm);
        monitor.start();

        defaultOptions = new HttpRequestOptions();

        // The following calls prevent these kinds of logging statements
        // from showing up in the console:
        //   EventCountingAppender: <UNKNOWN> Event: (DEBUG) Connection can be kept alive indefinitely
        //     Logger: gov.sandia.webcomms.http.internal.InternalHttpClient [parse error line num: 116]
        //   EventCountingAppender: <UNKNOWN> Event: (DEBUG) Stale connection check
        //     Logger: gov.sandia.webcomms.http.internal.InternalHttpClient [parse error line num: 116]
        //   EventCountingAppender: <UNKNOWN> Event: (DEBUG) Attempt 1 to execute request
        //     Logger: gov.sandia.webcomms.http.internal.InternalHttpClient [parse error line num: 116]
        Logger.getLogger(InternalHttpClient.class.getName()).setLevel(Level.WARN);
        Logger.getLogger(InternalRedirectStrategy.class.getName()).setLevel(Level.WARN);

        SchemeRegistry schemeRegistry =
            ReflectionUtil.get(client.getRoutePlanner(), "schemeRegistry");
        client.setRoutePlanner(new InternalRoutePlanner(schemeRegistry, this));
    }

    private ClientConnectionManager createConnectionManager() {
        SchemeRegistry registry = SchemeRegistryFactory.createDefault();
        DnsResolver resolver = new InternalDnsResolver();
        PoolingClientConnectionManager cm = new PoolingClientConnectionManager(registry, resolver);
        cm.setMaxTotal(connCacheSize);
        cm.setDefaultMaxPerRoute(10);
        return cm;
    }

    private void registerHttpSocketFactory() {
        Scheme httpScheme = new Scheme("http", 80, new InternalHttpSocketFactory(this));
        client.getConnectionManager().getSchemeRegistry().register(httpScheme);
    }

    private void registerHttpsSocketFactory() {
        try {
            SSLContext ctx = SSLContext.getInstance("TLS");   // or SSL?
            TrustManager tm = new InternalTrustManager(this);
            ctx.init(null, new TrustManager[] {tm}, null /* new SecureRandom() */);
            SSLSocketFactory ssf = new InternalHttpsSocketFactory(ctx, new InternalHostnameVerifier(this), this);
            Scheme httpsScheme = new Scheme("https", 443, ssf);
            client.getConnectionManager().getSchemeRegistry().register(httpsScheme);
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    public void setProxy(String proxyHost, int port) {
        HttpHost proxy = new HttpHost(proxyHost, port);
        client.getParams().setParameter(ConnRoutePNames.DEFAULT_PROXY, proxy);
    }
    public void clearProxy() {
        client.getParams().setParameter(ConnRoutePNames.DEFAULT_PROXY, null);
    }

    public void copyProxyFromSystem() {
        String proxyHost = System.getProperty("http.proxyHost");
        String proxyPort = System.getProperty("http.proxyPort");
        if(proxyHost != null && proxyPort != null) {
            setProxy(proxyHost, Integer.parseInt(proxyPort));
        }
    }

    public void copyProxyToSystem() {
        String host = getProxyHost();
        int port = getProxyPort();
        if(host != null && port != 0) {
            setSystemProxy(host, port);
        }
    }

    public void setSystemProxy(String proxyHost, int proxyPort) {
        if(proxyHost != null && proxyPort != 0) {
            System.setProperty("http.proxyHost", proxyHost);
            System.setProperty("http.proxyPort", "" + proxyPort);
        }
    }

    public void enablePreemptiveAuthentication() {
        usePreemptiveAuth = true;
        client.addRequestInterceptor(new InternalRequestInterceptor(this), 0);
    }

    // Convenience method that can be removed someday.
    public void useSandiaProxy() {
        Http.getInstance().setProxy("wwwproxy.sandia.gov", 80);
    }


    //////////////
    // REQUESTS //
    //////////////

    // All the below methods can throw an HttpUrlFormatException
    // which is a RuntimeException.

    // These methods allow you to see how Http *would* clean your
    // URL's in the below methods.
    public String clean(String originalUrl) throws HttpUrlFormatException {
        return UriCleaner.clean(originalUrl, null, true);     // Restrict to http & https
    }
    public String clean(String originalUrl, String baseUrl) throws HttpUrlFormatException {
        return UriCleaner.clean(originalUrl, baseUrl, true);  // Restrict to http & https
    }

    // Each of these methods are convenience methods for the request method.
    public HttpResource doHead(String originalUrl) {
        return doHead(originalUrl, null);
    }
    public HttpResource doHead(String originalUrl, HttpRequestOptions options) {
        return convenienceRequest(RequestMethod.HEAD, originalUrl, options);
    }
    public HttpResource doGet(String originalUrl) {
        return doGet(originalUrl, null);
    }
    public HttpResource doGet(String originalUrl, HttpRequestOptions options) {
        return convenienceRequest(RequestMethod.GET, originalUrl, options);
    }
    public HttpResource doPost(String originalUrl) {
        return doPost(originalUrl, null);
    }
    public HttpResource doPost(String originalUrl, HttpRequestOptions options) {
        return convenienceRequest(RequestMethod.POST, originalUrl, options);
    }
    public HttpResource doPut(String originalUrl) {
        return doPut(originalUrl, null);
    }
    public HttpResource doPut(String originalUrl, HttpRequestOptions options) {
        return convenienceRequest(RequestMethod.PUT, originalUrl, options);
    }
    public HttpResource doDelete(String originalUrl) {
        return doDelete(originalUrl, null);
    }
    public HttpResource doDelete(String originalUrl, HttpRequestOptions options) {
        return convenienceRequest(RequestMethod.DELETE, originalUrl, options);
    }
    public HttpResource doOptions(String originalUrl) {
        return doOptions(originalUrl, null);
    }
    public HttpResource doOptions(String originalUrl, HttpRequestOptions options) {
        return convenienceRequest(RequestMethod.OPTIONS, originalUrl, options);
    }
    public HttpResource doTrace(String originalUrl) {
        return doTrace(originalUrl, null);
    }
    public HttpResource doTrace(String originalUrl, HttpRequestOptions options) {
        return convenienceRequest(RequestMethod.TRACE, originalUrl, options);
    }

    // Helper

    private HttpResource convenienceRequest(RequestMethod method, String originalUrl,
                                            HttpRequestOptions options) {
        HttpRequestWrapper requestWrapper = create(method, originalUrl, options);
        return request(requestWrapper);
    }

    public HttpRequestWrapper create(RequestMethod method, String originalUrl) {
        return create(method, originalUrl, null);
    }
    public HttpRequestWrapper create(RequestMethod method, String originalUrl, HttpRequestOptions options) {
        RProfiler P = RProfiler.get();
        P.block("Http.createReq");
        HttpResource resource = null;
        HttpUriRequest request = null;    // Can be null if URL syntax error

        numRequests.incrementAndGet();

        try {

            // Use the default options if none were supplied.  This
            // happens here because this is the first time the options
            // are used (for the create request path).
            if(options == null) {
                options = defaultOptions;
            }

            // Get the original URL string and clean it.
            resource = new HttpResource(originalUrl);
            if(originalUrl == null) {
                throw new HttpUrlNullException("Resource cannot have null original URL");
            }
            resource.setMethod(method);
            String cleanedUrl =
                options.isCleanUrls() ?
                    UriCleaner.clean(originalUrl, null, true) :      // Can throw an exception
                    null;
            resource.setCleanedUrl(cleanedUrl);
            String reqUrl = resource.getUrl();   // Will return either original or cleaned URL.

            // Construct the appropriate request object.  This should
            // no longer EVER throw an exception because the same code
            // that would have thrown an exception is within 'clean'
            // above.
            switch(method) {
                case DELETE:  request = new HttpDelete(reqUrl);  break;
                case GET:     request = new HttpGet(reqUrl);     break;
                case HEAD:    request = new HttpHead(reqUrl);    break;
                case POST:    request = new HttpPost(reqUrl);    break;
                case PUT:     request = new HttpPut(reqUrl);     break;
                case OPTIONS: request = new HttpOptions(reqUrl); break;
                case TRACE:   request = new HttpTrace(reqUrl);   break;
            }

        } catch(Throwable t) {
            handleErrorCatch(resource, t);

        } finally {
            P.end();
            handleErrorFinally(resource);
        }

        return new HttpRequestWrapper(request, resource, options);
    }

    // Every request ends up going through this method, and it takes care
    // of the force serial execution feature.
    public HttpResource request(HttpRequestWrapper requestWrapper) {
        if(forceSerialExecution) {
            synchronized(FORCE_SERIAL_LOCK) {
                return requestInner0(requestWrapper);
            }
        }
        return requestInner0(requestWrapper);
    }

    // This method it takes care of creating the trace objects for this thread.
    private HttpResource requestInner0(HttpRequestWrapper requestWrapper) {
        RequestTrace trace = registerRequestTrace(requestWrapper);
        HttpResource       resource = requestWrapper.getResource();
        HttpRequestOptions options  = requestWrapper.getOptions();
        trace.debugBlockOpen("REQUEST " + resource.getUrl());
        try {
            trace.debugStep("Options: " + options.toStringLong());
            return requestInner1(requestWrapper);
        } finally {
            traceResult(resource);
            trace.debugBlockClose();
            removeRequestTrace();
        }
    }

    private void traceResult(HttpResource resource) {
        RequestTrace trace = getRequestTrace();
        boolean hasSecInfo = resource.getSecurityInfo() != null;
        int rc = resource.getResponseCode();
        Throwable ex = resource.getException();
        String res =
            "RC: " + rc +
            (ex != null ? ", Error: " + ex.getClass().getSimpleName() : "") +
            (resource.getReturnedSize() >= 0 ? ", CL: " + StringUtil.commas(resource.getReturnedSize()) + " B" : "");
        trace.debugBlockOpen("RESULT: " + res);
        try {
            trace.debugStep("SEC INFO: " + (hasSecInfo ? "++YES++" : "--NO--"));
        } finally {
            trace.debugBlockClose();
        }
    }

    private HttpResource requestInner1(HttpRequestWrapper requestWrapper) {
        HttpResource resource = requestWrapper.getResource();

        // If this resource is already in an error state due to problems
        // encountered in the create method (i.e. a URL syntax issue),
        // then simply return the resource.  Error count will be incremented
        // but not the requests count.
        if(resource.isError()) {
            return resource;
        }

        HttpResource preemptedResource = checkPreemption(requestWrapper);
        if(preemptedResource != null) {
            return preemptedResource;
        }

        // Start process for a normal, non-preempted request
        resource.setStartDownload(System.currentTimeMillis());

        try {
            RProfiler P = RProfiler.get();
            P.block("Http.reqIntoRes");
            try {
                requestIntoResource(requestWrapper);
            } finally {
                P.end();
            }

//            if(resource.hasContent()) {
//                String[] charsetsToBeTested = {"UTF-8", "windows-1253", "ISO-8859-7"};
//                CharsetDetector cd = new CharsetDetector();
//                Charset charset = cd.detectCharset(resource.getContent(), charsetsToBeTested);
//                System.out.println(charset);
//            }

        } catch(Throwable t) {
            handleErrorCatch(resource, t);

        } finally {
            resource.setEndDownload(System.currentTimeMillis());
            handleErrorFinally(resource);
        }

        // TODO check performance impact of recording this info (sync slowdown issues)
        if(recordHeaderStats) {
            RProfiler P = RProfiler.get();
            P.block("Record Header Stats");
            try {
                recordResponseHeaderStats(resource.getResponseHeaders());
            } finally {
                P.end();
            }
        }

        return resource;
    }

    private void handleErrorCatch(HttpResource resource, Throwable t) {
        t = replaceKnownThrowables(t);
        resource.setException(t);
        logger.error(LC_DM_EX + " " + resource.getOriginalUrl(), t);
        recordStatsException(t);
    }

    private void handleErrorFinally(HttpResource resource) {
        if(resource.isError()) {
            numErrors.incrementAndGet();   // RC >= 400 or has Exception
        }
    }

    private HttpResource checkPreemption(HttpRequestWrapper requestWrapper) {
        RequestTrace trace = getRequestTrace();
        HttpResource       resource    = requestWrapper.getResource();
        HttpRequestOptions options     = requestWrapper.getOptions();
        String             originalUrl = resource.getOriginalUrl();

        HttpRequestPreemptorParams preemptorParams = options.getRequestPreemptorParams();
        if(preemptorParams != null) {
            HttpRequestPreemptorGenerator generator = Generator.lookup(preemptorParams);
            if(generator == null) {
                throw new HttpNoExtensionLoadedException(
                    "[Request Preemptor] No extension loaded for class '" +
                        preemptorParams.getClass().getName() + "'"
                );
            }

            HttpRequestPreemptor preemptor = generator.createPreemptor(preemptorParams);
            if(preemptor != null) {
                try {
                    HttpResource preemptedResource = preemptor.premptRequest(requestWrapper);
                    if(preemptedResource == null) {
                        trace.debugStep("PREEMPTOR DECLINED: " + generator.getName());
                        return null;   // Signals no preemption due to preemptor declines to provide
                    }
                    trace.debugStep("PREEMPTOR PROVIDED: " + generator.getName());
                    numRequestsPreempted.incrementAndGet();
                    logger.info(LC_PP + " " + originalUrl + " > " + preemptor.getClass().getName());
                    return preemptedResource;

                } catch(PreemptionStoppedException e) {
                    logger.info(LC_PX + " " + originalUrl + " > " + preemptor.getClass().getName(), e);
                    return
                        new HttpResource(originalUrl)     // Any other fields needed to be set?
                            .setException(e)
                    ;

                } catch(Exception e) {
                    logger.info(LC_PX + " " + originalUrl + " > " + preemptor.getClass().getName(), e);
                    return
                        new HttpResource(originalUrl)     // Any other fields needed to be set?
                            .setException(new InvalidPreemptionException(
                                "Preemptor has thrown an exception on '" + originalUrl + "'", e
                            ))
                    ;
                }
            } // else shouldn't ever happen (perhaps need to make API more strict)
        }

        return null;               // Signals no preemption due to no preemptor
    }

    // Many exceptions are expected when downloading content from the
    // internet.  We want to:
    //   1) remove more generic terminology like "ClientProtocolException" and
    //   2) eliminate stack traces for these exceptions as we know exactly where
    //      and why they happen.
    // This is an effort to:
    //   1) reduce and simplify the content in the log file and
    //   2) make catching and responding to exceptions easier.
    private Throwable replaceKnownThrowables(Throwable t) {

        if(t.getClass().equals(ClientProtocolException.class)) {
            if(t.getCause() != null) {
                if(t.getCause().getClass().equals(CircularRedirectException.class)) {
                    t = new HttpCircularRedirectException(
                        t.getCause().getMessage(), null, true, false);
                } else if(t.getCause().getClass().equals(RedirectException.class)) {
                    if(t.getCause().getMessage().startsWith("Maximum redirects")) {
                        t = new HttpMaximumRedirectException(
                            t.getCause().getMessage(), null, true, false);
                    }
                }
            }

        } else if(t instanceof HttpUrlFormatException) {
            t = new HttpUrlFormatException(t.getMessage(), null, true, false);

        } else if(t instanceof HttpUrlNullException) {
            t = new HttpUrlNullException(t.getMessage(), null, true, false);

        } else if(t instanceof IOException) {
            if(t.getMessage().equals("Request already aborted")) {
                t = new HttpRequestAbortedException(
                    t.getMessage(), t, true, false);
            }
        }

        return t;
    }

    private void requestIntoResource(HttpRequestWrapper requestWrapper) throws Exception {
        RequestTrace trace = getRequestTrace();
        HttpResource       resource = requestWrapper.getResource();
        HttpUriRequest     request  = requestWrapper.getRequest();
        HttpRequestOptions options  = requestWrapper.getOptions();

        // Record whether or not the original URL was changed
        // by the cleaning process.
        if(resource.hasDifferentCleanedUrl()) {
            numCleanReq.incrementAndGet();
            logger.debug(LC_UP_RQ + " " + resource.getOriginalUrl() +
                " [=>] " + resource.getCleanedUrl());
        }

        // If options provide a user agent to override the default
        // user agent, set the corresponding header on the request.
        if(options.getOverrideUserAgent() != null) {
            request.setHeader(HttpHeaders.USER_AGENT, options.getOverrideUserAgent());
        }

        try {
            urlStats.add(resource.getUrl());
        } catch(Exception e) {
            logger.error(LC_SX + " " + resource.getCleanedUrl(), e);
            e.printStackTrace();
        }

        RProfiler P = RProfiler.get();
        try {
            HttpResponse response;

//request.abort();
            try {
                HttpContext context = createHttpContext();

                // TODO: This line can sometimes log warnings of its own.  For example:
                //   29 Jul 2014 06:39:28,730 [WARN ] org.apache.http.client.protocol.ResponseProcessCookies  - Cookie rejected: "[version: 0][name: NYT-S][value: deleted][domain: www.stg.nytimes.com][path: /][expiry: Wed Dec 31 17:00:01 MST 1969]". Illegal domain attribute "www.stg.nytimes.com". Domain of origin: "www.nytimes.com"
                // This is somewhat annoying.  Might be nice to be able to configure
                // this somehow and check impact on production logs.
                // TODO: Getting a lot of ConnectionPoolTimeoutException's here!!
                // 9,300 over 3 days 300K-800K pages "Timeout waiting for connection"
                // TODO: This is also where the SocketTimeoutException's happen!
                trace.debugBlockOpen("EXECUTE");
                P.block("Http.client.execute");
                try {
                    response = client.execute(request, context);
                } finally {
                    trace.debugBlockClose();
                    P.end();
                }
//System.out.println("IA: " + request.isAborted());
            // The inner catch is just to catch when we don't
            // even get a response back, and thus can't process
            // it.  Need to abort the request, and then later
            // we'll still release the connection.  Should not
            // need to abort anytime after this, as we are
            // always going to read the full response body
            // one way or another.
            } catch(Exception e) {
//System.out.println("x1:" + request.isAborted());  // researching future idea
//System.out.println("x2:" + request.isAborted());
                abortRequest(request);
//System.out.println("x3:" + request.isAborted());
                throw e;
            }

            P.block("Http.RIR");
            try {
                responseIntoResource(requestWrapper, response);
            } finally {
                P.end();
            }

            if(trace.getOverrideException() != null) {
                throw trace.getOverrideException();
            }

        } finally {
//System.out.println("x4:" + request.isAborted());
            HttpRequestBase requestBase = (HttpRequestBase) request;
            requestBase.releaseConnection();
//System.out.println("x5:" + request.isAborted());
        }
    }

    // This method cannot throw an exception written as is.
    private HttpContext createHttpContext() {
        HttpContext context = new BasicHttpContext();
        if(usePreemptiveAuth) {
            BasicScheme scheme = new BasicScheme();
            context.setAttribute("preemptive-auth", scheme);
        }
        return context;
    }

    private void abortRequest(HttpUriRequest request) {
        if(request != null) {
            try {
                request.abort();          // UnsupportedOperationException probably not an issue
            } catch(Exception e) {
                logger.error(LC_EX, e);   // This should be very rare
                throw e;
            }
        }
    }

    private void responseIntoResource(HttpRequestWrapper requestWrapper, HttpResponse response) throws Exception {
        RequestTrace trace = getRequestTrace();
        HttpResource       resource = requestWrapper.getResource();
        HttpRequestOptions options  = requestWrapper.getOptions();

        try {
            int responseCode = recordResponseCodeMessage(response, resource);
            recordStatsStatusCode(responseCode);
            if(options.isSaveResponseHeaders()) {
                recordHeaders(response, resource);
            }

            HttpEntity entity = response.getEntity();

            if(entity == null) {
                // Most likely this case is due to a response code of 204 or 205.
                // Content will remain null to indicate this state, separate
                // from the case of having content but that content being 0 bytes
                // in length.
                // This situation also occurs if the method is HEAD, which is
                // supposed to only return header information and NO body.
                // Response code can be 200, 404, etc. but no entity will be present.

            } else {
                recordContentTypeEnc(resource, entity);
                recordEntityErrors(response, entity);
                long reportedSize = recordReportedSize(resource, entity);
                if(options.isSaveContent()) {
                    HttpConsumeEntityCriteriaParams criteriaParams = options.getConsumeEntityCriteriaParams();
                    boolean doConsume = true;
                    if(criteriaParams != null) {
                        HttpConsumeEntityCriteriaGenerator generator = Generator.lookup(criteriaParams);
                        if(generator == null) {
                            throw new HttpNoExtensionLoadedException(
                                "[Consume Entity Criteria] No extension loaded for class '" +
                                criteriaParams.getClass().getName() + "'"
                            );
                        }
                        HttpConsumeEntityCriteria criteria = generator.createCriteria(criteriaParams);
                        if(!criteria.doConsume(requestWrapper, response, entity)) {
                            resource.setContentDeclined(true);
                            doConsume = false;
                        }
                    }
                    if(doConsume) {
                        trace.debugStep("ENTITY READ");
                        readEntity(requestWrapper, entity, reportedSize);
                    } else {
                        trace.debugStep("ENTITY IGNORE");
                    }
                }
            }

        // We must always fully consume the response no matter what happens.
        } finally {
            try {
                HttpEntity entity = response.getEntity();
                EntityUtils.consume(entity);  // This method ignores null arguments
            } catch(Exception e) {
                logger.error(LC_EX, e);      // This should be very rare
                throw e;
            }
        }
    }

    private void readEntity(HttpRequestWrapper requestWrapper, HttpEntity entity,
                           long reportedSize) throws IOException, BeyondMaxException {
        HttpResource resource = requestWrapper.getResource();
        InputStream is = entity.getContent();
        if(is == null) {
            // TODO: what to do here?  Not sure if can happen but....
        } else {
            try {
                byte[] content = readBytes(requestWrapper, is);
                resource.setContent(content);
                recordStatsSizes(reportedSize, content);
            } catch(BeyondMaxException e) {
                numTooBig.incrementAndGet();
                throw e;
            }
        }
    }

    private byte[] readBytes(HttpRequestWrapper requestWrapper,
                             InputStream is) throws BeyondMaxException, IOException {
        HttpRequestOptions options = requestWrapper.getOptions();
        long max = options.getMaxContentLength();
        // This line we get a LOT of the SocketTimeoutExceptions, 'read timed out'
        return FileUtil.readBytes(is, DEFAULT_BUFFER_SIZE, max);
    }


    ///////////////
    // RECORDING //
    ///////////////

    private void recordResponseHeaderStats(Map<String, String> responseHeaders) {
        if(responseHeaders != null) {
            if(true) {
                return;      // TODO: FEATURE DISABLED DUE TO MEMORY CONCERNS.  FIX AND RENABLE {}
            }
            synchronized(allSeenHeaders) {                 // TODO: Don't really need TreeMap inside...
                allSeenHeaders.incrementProvidingSets();
                for(Entry<String, String> entrySet : responseHeaders.entrySet()) {
                    String key = entrySet.getKey();
                    if(key == null) {
                        key = "(-null-)";     // Wish didn't have to do this, and TreeMap could just sort null's to top or bottom of tree
                    }

                    String value = entrySet.getValue();
                    if(value == null) {
                        value = "(-null-)";     // Wish didn't have to do this, and TreeMap could just sort null's to top or bottom of tree
                    }

                    Pattern checkPattern = headerCollapsePatternsCompiled.get(key.toLowerCase());
                    if(checkPattern != null) {
                        boolean record = true;
                        String pattern = checkPattern.pattern();
                        if(pattern.equals(".*") || pattern.equals("^.*$")) {
                            record = false;
                        } else {
                            Matcher m = checkPattern.matcher(value);
                            if(m.find()) {
                                record = false;
                            }
                        }
                        if(!record) {
                            value = "(Not Individually Recorded; Matches: " + pattern + ")";
                        }
                    }

                    allSeenHeaders.addMembership(key, value);
                }
            }
        }
    }

    private void recordContentTypeEnc(HttpResource resource, HttpEntity entity) {
        String contentType =
            (entity.getContentType() != null) ?
                entity.getContentType().getValue() : null;
        String contentEncoding =
            (entity.getContentEncoding() != null) ?
                entity.getContentEncoding().getValue() : null;
        resource
            .setContentType(contentType)
            .setContentEncoding(contentEncoding);
    }

    private void recordEntityErrors(HttpResponse response, HttpEntity entity) {
        Header entityEnc = entity.getContentEncoding();
        long entityLength = entity.getContentLength();
        Header entityType = entity.getContentType();
        Header headerEnc = null;
        Header headerLength = null;
        Header headerType = null;

        for(Header header : response.getAllHeaders()) {
            if(header.getName().equalsIgnoreCase(HttpHeaders.CONTENT_ENCODING)) {
                headerEnc = header;
            } else if(header.getName().equalsIgnoreCase(HttpHeaders.CONTENT_LENGTH)) {
                headerLength = header;
            } else if(header.getName().equalsIgnoreCase(HttpHeaders.CONTENT_TYPE)) {
                headerType = header;
            }
        }

        boolean encError = false;
        if(entityEnc != null || headerEnc != null) {
            if(entityEnc != null && headerEnc != null) {
                // Strict assumption / guess without looking at source code
                if(entityEnc != headerEnc) {
                    encError = true;
                }
            } else {
                encError = true;
            }
        }
        if(encError) {
            numIncorrectEntCe.incrementAndGet();
        }

        boolean typeError = false;
        if(entityType != null || headerType != null) {
            if(entityType != null && headerType != null) {
                // Strict assumption / guess without looking at source code
                if(entityType != headerType) {
                    typeError = true;
                }
            } else {
                typeError = true;
            }
        }
        if(typeError) {
            numIncorrectEntCt.incrementAndGet();
        }

        boolean lengthError = false;
        if(entityLength != -1 || headerLength != null) {
            if(headerLength != null) {
                String val = headerLength.getValue();
                try {
                    if(entityLength != NumUtil.l(val)) {
                        lengthError = true;
                    }
                } catch(Exception e) {   // Should never happen
                    lengthError = true;
                }
            } else {
                lengthError = true;
            }
        }
        if(lengthError) {
            numIncorrectEntCl.incrementAndGet();
        }
    }

    private long recordReportedSize(HttpResource resource, HttpEntity entity) {
        long reportedSize = entity.getContentLength();
        resource.setReportedSize(reportedSize);
        return reportedSize;
    }

    private void recordStatsSizes(long reportedSize, byte[] content) {
        sumActualSizes.addAndGet(content.length);
        numActualSizes.incrementAndGet();

        // Get the size the remote server said it was going to send.
        // This is often wrong (reportedSize != content.length) and
        // is very often reported as -1 (which means unknown or
        // unreported).
        if(reportedSize >= 0) {
            sumReportedSizes.addAndGet(reportedSize);
            numReportedSizes.incrementAndGet();
        }

        // Count which were actually accurate.
        if(content.length == reportedSize) {
            numCorrectSizes.incrementAndGet();
        } else if(reportedSize >= 0) {
            numIncorrectSizes.incrementAndGet();
        } else {
            numUnknownSizes.incrementAndGet();
        }
    }

    private int recordResponseCodeMessage(HttpResponse response, HttpResource resource) {
        int statusCode = response.getStatusLine().getStatusCode();
        String reasonPhrase = response.getStatusLine().getReasonPhrase();

        resource
            .setResponseCode(statusCode)
            .setResponseMessage(reasonPhrase);

        // Log error status codes
        if(statusCode >= 400) {
            logger.error(LC_DM_RC + " <" + statusCode + "> " + resource.getUrl());
        }

        HttpResponseCode responseCode = StandardHttpResponseCodes.getByCode(statusCode);
        if(responseCode == null) {
            numUnexpectedRc.incrementAndGet();
            logger.error(LC_RC_UN + " <" + statusCode + "> " + resource.getUrl());
        } else {
            if(!responseCode.getReasonPhrase().equals(reasonPhrase)) {
                numUnexpectedRc.incrementAndGet();
                logger.error(LC_RC_UN + " <" + statusCode + "> {" + reasonPhrase + "} " + resource.getUrl());
            }
        }

        return statusCode;
    }

    private void recordHeaders(HttpResponse response, HttpResource resource) {
        for(Header header : response.getAllHeaders()) {
            resource.addResponseHeader(
                header.getName(),
                header.getValue()
            );

            // Based on looking at why and how often web sites return
            // multiple values for a header value,
            // the header "elements" in Apache's API are not useful to
            // keep separately in our Resource objects.  They are simply
            // the result of a parsing of the header value.  This parsing
            // can be done at any time.  Moreover, Apache's parsing has
            // known inaccuracies causing us to have to write a custom
            // parser anyway.
//            try {
//                // Apache header parser has known incompatibility with many
//                // header fields.  This explains the issue concerning the
//                // cookie field at least:
//                // http://stackoverflow.com/questions/7612667/what-is-an-http-header-element
//                HeaderElement[] elems = apacheHeader.getElements();  // Can throw parse exception
//                if(elems.length > 1) {
//                    logger.debug("-H!} " + elems.length + " header elements found");
//                    logger.debug("-H!} headerName = " + apacheHeader.getName());
//                    logger.debug("-H!} headerValue = " + apacheHeader.getValue());
//                    logger.debug("-H!} url = " + resource.getCleanedUrl());
//                    int i = 0;
//                    for(HeaderElement elem : elems) {
//                        logger.debug("-H!}   HE#" + i + ": " + elem.getName() + " = " + elem.getValue());
//                        for(int p = 0; p < elem.getParameterCount(); p++) {
//                            logger.debug("-H!}     P#" + p + ": " + elem.getParameter(p));
//                        }
//                        i++;
//                    }
//                }
        }
    }

    private void recordStatsStatusCode(int responseCode) {
        synchronized(statusCodeHistogram) {
            Integer prev = statusCodeHistogram.get(responseCode);
            if(prev == null) {
                prev = 0;
            }
            statusCodeHistogram.put(responseCode, prev + 1);
        }
    }

    private void recordStatsException(Throwable t) {
        synchronized(exceptionCounts) {
            String name = "^" + t.getClass().getName();
            Integer prev = exceptionCounts.get(name);
            if (prev == null) {
                prev = 0;
            }
            exceptionCounts.put(name, prev + 1);

            Throwable cause = t.getCause();
            while(cause != null) {

                name = "~" + cause.getClass().getName();
                prev = exceptionCounts.get(name);
                if (prev == null) {
                    prev = 0;
                }
                exceptionCounts.put(name, prev + 1);

                cause = cause.getCause();
            }
        }
    }

    private String replaceAjaxFragment(String reqUrl) {
        int hashBang = reqUrl.indexOf("#!");
        if(hashBang == -1) {
            return reqUrl;
        }
        String fragState = reqUrl.substring(hashBang + 2);
        reqUrl = reqUrl.substring(0, hashBang);
        int q = reqUrl.indexOf('?');
        String sep;
        if(q == -1) {
            sep = "?";
        } else if(q == reqUrl.length() - 1) {
            sep = "";
        } else {
            sep = "&";
        }
        reqUrl = reqUrl + sep + "_escaped_fragment_=" + fragState;
        return reqUrl;
    }


    /////////////////
    // CREDENTIALS //
    /////////////////

    public void setBasicCredentials(String username, String password) {
        client.getCredentialsProvider().setCredentials(AuthScope.ANY,
            new UsernamePasswordCredentials(username, password));
    }

    public void setBasicCredentials(String username, String password, String host, int port) {
        client.getCredentialsProvider().setCredentials(
            new AuthScope(("-".equals(host) ? AuthScope.ANY_HOST : host), port),
            new UsernamePasswordCredentials(username, password));
    }

    public void setNTCredentials(String username, String password, String workstation, String domain) {
        client.getCredentialsProvider().setCredentials(AuthScope.ANY,
            new NTCredentials(username, password, workstation, domain));
    }

    public void setNTCredentials(String username, String password, String workstation,
            String domain, String host, int port) {
        client.getCredentialsProvider().setCredentials(
            new AuthScope(("-".equals(host) ? AuthScope.ANY_HOST : host), port),
            new NTCredentials(username, password, workstation, domain));
    }


    //////////////////////////
    // ACCESSORS / MUTATORS //
    //////////////////////////

    // Accessors

    // Config
    public HttpRequestOptions getDefaultOptions() {
        return defaultOptions;
    }
    public int getTimeout() {
        return timeout;
    }
    public int getConnCacheSize() {
        return connCacheSize;
    }
    public boolean isRecordHeaderStats() {
        return recordHeaderStats;
    }
    public Map<String, String> getHeaderCollapsePatterns() {
        synchronized(headerCollapsePatterns) {
            return new TreeMap<>(headerCollapsePatterns);
        }
    }

    // Request/Response Counts (for non-preempted requests)
    public int getNumRequests() {
        return numRequests.get();
    }
    public int getNumActualSizes() {
        return numActualSizes.get();
    }
    public int getNumReportedSizes() {
        return numReportedSizes.get();
    }
    public int getNumCorrectSizes() {
        return numCorrectSizes.get();
    }
    public int getNumIncorrectSizes() {
        return numIncorrectSizes.get();
    }
    public int getNumIncorrectEntCe() {
        return numIncorrectEntCe.get();
    }
    public int getNumIncorrectEntCl() {
        return numIncorrectEntCl.get();
    }
    public int getNumIncorrectEntCt() {
        return numIncorrectEntCt.get();
    }
    public int getNumUnknownSizes() {
        return numUnknownSizes.get();
    }
    public int getNumUnexpectedRc() {
        return numUnexpectedRc.get();
    }
    public int getNumTooBig() {
        return numTooBig.get();
    }
    public int getNumErrors() {
        return numErrors.get();
    }
    public int getNumRedirects() {
        return numRedirects.get();
    }
    public int getNumCleanReq() {
        return numCleanReq.get();
    }
    public int getNumCleanRedir() {
        return numCleanRedir.get();
    }

    // Request/Response Sizes (for non-preempted requests)
    public long getSumActualSizes() {
        return sumActualSizes.get();
    }
    public long getSumReportedSizes() {
        return sumReportedSizes.get();
    }
    public boolean isForceSerialExecution() {
        return forceSerialExecution;
    }

    // Request/Response Other (for non-preempted requests)
    public Map<Integer, Integer> getStatusCodeHistogram() {
        synchronized(statusCodeHistogram) {
            return new TreeMap<>(statusCodeHistogram);
        }
    }
    public Map<String, Integer> getExceptionCounts() {
        synchronized(exceptionCounts) {
            return new TreeMap<>(exceptionCounts);
        }
    }
    public MembershipMap<String, String> getAllSeenHeaders() {
        if(!recordHeaderStats) {
            return null;
        }
        synchronized(allSeenHeaders) {
            return allSeenHeaders.copy();
        }
    }
    public UrlStats getUrlStats() {
        return urlStats.copy();
    }

    // Request/Response Preemption
    public int getNumRequestsPreempted() {
        return numRequestsPreempted.get();
    }

    // Accessors (Computed)

    // Config/Pass Thru
    public String getUserAgent() {
        return (String) client.getParams().getParameter(CoreProtocolPNames.USER_AGENT);
    }
    public HttpHost getProxy() {
        return (HttpHost) client.getParams().getParameter(ConnRoutePNames.DEFAULT_PROXY);
    }
    public String getProxyHost() {
        HttpHost proxy = getProxy();
        if(proxy != null) {
            return proxy.getHostName();
        }
        return null;
    }
    public int getProxyPort() {
        HttpHost proxy = getProxy();
        if(proxy != null) {
            return proxy.getPort();
        }
        return 0;
    }
    public PoolStats getPoolStats() {
        PoolingClientConnectionManager poolManager =
            (PoolingClientConnectionManager) client.getConnectionManager();
        PoolStats stats = poolManager.getTotalStats();
        return stats;
    }

    // Other
    public long getAvgActualSize() {
        long total = numActualSizes.get();
        return (total > 0L) ? sumActualSizes.get() / total : 0L;
    }
    public long getAvgReportedSize() {
        long total = numReportedSizes.get();
        return (total > 0L) ? sumReportedSizes.get() / total : 0L;
    }
    public HttpSummaryState createSummaryState() {

        // Change pool stats object into a serializable array.
        PoolStats poolStats = getPoolStats();
        int[] poolStats2 = new int[] {
            poolStats.getAvailable(),
            poolStats.getLeased(),
            poolStats.getPending(),
            poolStats.getMax()
        };

        HttpHost proxy = getProxy();
        String proxyStr = proxy == null ? "(none)" : proxy.getHostName() + ":" + proxy.getPort();

        return new HttpSummaryState()

            // Config
            .setDefaultOptions(getDefaultOptions())
            .setTimeout(getTimeout())
            .setUserAgent(getUserAgent())
            .setProxy(proxyStr)
            .setPoolStats(poolStats2)
            .setRecordHeaderStats(isRecordHeaderStats())
            .setHeaderCollapsePatterns(getHeaderCollapsePatterns())

            // Request/Response Counts (for non-preempted requests)
            .setNumRequests(getNumRequests())
            .setNumActualSizes(getNumActualSizes())
            .setNumReportedSizes(getNumReportedSizes())
            .setNumCorrectSizes(getNumCorrectSizes())
            .setNumIncorrectSizes(getNumIncorrectSizes())
            .setNumIncorrectEntCe(getNumIncorrectEntCe())
            .setNumIncorrectEntCl(getNumIncorrectEntCl())
            .setNumIncorrectEntCt(getNumIncorrectEntCt())
            .setNumUnknownSizes(getNumUnknownSizes())
            .setNumUnexpectedRc(getNumUnexpectedRc())
            .setNumTooBig(getNumTooBig())
            .setNumErrors(getNumErrors())
            .setNumRedirects(getNumRedirects())
            .setNumCleanReq(getNumCleanReq())
            .setNumCleanRedir(getNumCleanRedir())

            // Request/Response Sizes (for non-preempted requests)
            .setSumActualSizes(getSumActualSizes())
            .setSumReportedSizes(getSumReportedSizes())
            .setAvgActualSize(getAvgActualSize())
            .setAvgReportedSize(getAvgReportedSize())

            // Request/Response Other (for non-preempted requests)
            .setStatusCodeHistogram(getStatusCodeHistogram())
            .setExceptionCounts(getExceptionCounts())
            .setAllSeenHeaders(getAllSeenHeaders())
            //.setUrlStats(getUrlStats())             // Not included due to memory concerns (might be premature optimization)

            // Request/Response Preemption
            .setNumRequestsPreempted(getNumRequestsPreempted())
        ;
    }

    // Mutators

//    RequestConfig requestConfig = RequestConfig.custom().setConnectTimeout(30 * 1000).build();  version 4.3
//    HttpClient httpClient = HttpClientBuilder.create().setDefaultRequestConfig(requestConfig).build();
    public Http setDefaultOptions(HttpRequestOptions defaultOptions) {
        if(defaultOptions == null) {
            throw new IllegalArgumentException("Default options cannot be null");
        }
        this.defaultOptions = defaultOptions;
        return this;
    }
    public Http setTimeout(int ms) {
        timeout = ms;
        HttpConnectionParams.setSoTimeout(client.getParams(), timeout);
        HttpConnectionParams.setConnectionTimeout(client.getParams(), timeout);
        return this;
    }
    public Http setRecordHeaderStats(boolean recordHeaderStats) {
        this.recordHeaderStats = recordHeaderStats;
        return this;
    }
    public Http clearAllSeenHeaders() {
        synchronized(allSeenHeaders) {
            allSeenHeaders.clear();
        }
        return this;
    }
    public Http setHeaderCollapsePatterns(Map<String, String> headerCollapsePatterns) {
        synchronized(allSeenHeaders) {
            this.headerCollapsePatterns = headerCollapsePatterns;
            if(headerCollapsePatterns == null) {
                headerCollapsePatternsCompiled = null;
            } else {
                headerCollapsePatternsCompiled = new HashMap<>();
                for(String key : headerCollapsePatterns.keySet()) {
                    String pattern = headerCollapsePatterns.get(key);
                    headerCollapsePatternsCompiled.put(
                        key.toLowerCase(), Pattern.compile(pattern));
                }
            }
        }
        return this;
    }

    // Mutators (Pass Thru)

    // Can be used to change the default redirect strategy.
    // Not sure why this is here... this would break contracts
    // related to what information goes into Resource object
    // and what fields are respected in HttpRequestOptions.
    public void setRedirectStrategy(RedirectStrategy rs) {
        client.setRedirectStrategy(rs);
    }

    // This was wrong for a long period of time.
    public void setUserAgent(String userAgent) {
        client.getParams().setParameter(CoreProtocolPNames.USER_AGENT, userAgent);
    }

    public void setConnCacheSize(int connCacheSize) {
        this.connCacheSize = connCacheSize;
        ((PoolingClientConnectionManager) client.getConnectionManager())
            .setMaxTotal(connCacheSize);
    }
    public Http setForceSerialExecution(boolean forceSerialExecution) {
        this.forceSerialExecution = forceSerialExecution;
        return this;
    }

    public void incRedirects() {
        numRedirects.incrementAndGet();
    }

    public void incCleanRedir() {
        numCleanRedir.incrementAndGet();
    }


    //////////
    // MISC //
    //////////

    public void shutdown() {
        monitor.shutdown();
    }

    private RequestTrace registerRequestTrace(HttpRequestWrapper requestWrapper) {
        RequestTrace trace = new RequestTrace(requestWrapper);
        requestTraces.put(Thread.currentThread().getId(), trace);
        return trace;
    }
    private void removeRequestTrace() {
        requestTraces.remove(Thread.currentThread().getId());
    }
    public RequestTrace getRequestTrace() {
        return requestTraces.get(Thread.currentThread().getId());
    }

    public void log(String message) {
        logger.debug(message);
    }
    public void log(String message, Throwable t) {
        logger.debug(message, t);
    }


    //////////
    // TEST //
    //////////

    // A useful system property that will cause the JVM to produce
    // a large amount of SSL debugging information to standard out:
    //     -Djavax.net.debug=ssl:handshake
    // Must be set *before* the application launches using -D.

    public static void main(String[] args) throws Exception {
        LoggingInitializer.init(LogUtil.INTERNAL_PROPERTIES_PATH);
        PluginManager.initialize(
            RepletePlugin.class,
            WebCommsPlugin.class
        );


//        SystemDefaultDnsResolver rsv = new SystemDefaultDnsResolver();
//        InetAddress[] resolve = rsv.resolve("www.entertainmentweekly.com");
//        System.out.println(Arrays.toString(resolve));
//        if(true) {
//            return;
//        }

        Http http = Http.getInstance();
        http.useSandiaProxy();

        String[] urls = {
//            null,
//            "http://www.google.com/",
//            "http://harrypotter.wikia.com/wiki/Rufus_Scrimgeour",
//            "http://www.cnn.com/#hi",
//            "http://colorado.edu/cs",
//            "http://www.nytimes.com/2014/07/29/world/europe/us-says-russia-tested-cruise-missile-in-violation-of-treaty.html?hp&action=click&pgtype=Homepage&version=LedeSum&module=first-column-region&region=top-news&WT.nav=top-news&_r=0",
//            "http://rss.slashdot.org/Slashdot/slashdot",
//            "http://www.whatismyip.com/",
//            "http://ip.jsontest.com/",
//            "HTTP://headers.jsontest.com/",
//            "http://date.jsontest.com/",
//            "http://http-echo.jgate.de",  // Sandia blocks if user agent is HC
//            "http://74.125.207.121/",
//            "http://echo.jsontest.com/red/bad/green/good",
//            "http://www.procato.com/my+headers/",
//            "http://media.fastclick.net/robots.txt",     // <-- 204 / no content
//            "http://entertainmentweekly.com",  // These two content MD5's have been equal in the past as
//            "http://www.ew.com",                // expected, even with all the timestamps in the web page's body.
//            "http://somwhere3438ufhk.com/artists/lester joseph-chaney/past-auction-results",
//            "http%//dude/huh.html",
//            "http://www.beckncall.com/",
//            "http://muzipedia.net/artist.php?artist=Kottonmouth Kings",
//            "http://www.nowher34so3d.com",
//            "http://livescience.com/robots.txt",
//            "http://www.washingtonpost.com/how-can-i-opt-out-of-online-advertising-co\nokies/2011/11/18/gIQABECbiN_story.html",
//            "http://papers.ssrn.com/sol3/papers.cfm?abstract_id=2188796##",
//            "http://wikipedia.org/wiki/Wikipedia:Yll%C3%A4pit%C3%A4j%C3%A4t/", (see reso dir for spec char)
//            "http://united.com",
//            "http://localhost:8080/Phisherman",
//            "-al!#4sf38ja@#$AF213hakjsf23",
//            "https://www.lostdogrescue.org/",
//            "https://www.lostdogrescue.org/",
//            "http://www.capitalone.com",
//            "http://dx.doi.org/10.1016%2FS0378-4320%2800%2900101-9",
//            "http://www.esquire.com/features/american-dog-pitbull-photos-0814#slide-2",  // I don't even understand this web page, how it downloads the images!
//            "http://pets.groups.yahoo.com/group/rawfeeding/",  // 301 w/o Location
//            "http://robot_guy.blogspot.com/4",  // Strange how new URI(<-this) gives null host/non-null authority, but HttpClient fetches fine... I guess we need to parse authority like HttpClient?
//            "https://time.org/",
//            "http://www.sandia.gov/mission/",
//            "http://www.cnn.com/",
//            "ftp://ftp.nrcan.gc.ca/ess/geochem/files/miscel/chem_analysis/abbey_et_al_1977.pdf",  // browsers easily grab this web resource... why can't the crawler?  WebComms?
//            "http://www.latinamericanstudies.org/dialogue/BAM-8-4-1978.pdf",  // example of "too-big", also, org.apache.http.ConnectionClosedException: Premature end of Content-Length delimited message body (expected: 112239; received: 73177
//            "http://www.cnn.com/aaaa#!bbbbbbbbbbb",
//            "http://www.sandia.gov:8080",
//            "http://instagram.com/robots.txt"  // TODO: Various weirdnesses
//            "http://news.sciencemag.org/sciencenow/2010/04/good-dogs-live-longer.html",
//            "http://www.sciencemag.org/sciencenow/2010/04/good-dogs-live-longer.html"
//            "http://localhost:8080/Lighthouse/ping?delay=10000",
//            "http://localhost:8080/Lighthouse/ping?delay=10000",
//            "htp://localhost:8080/Lighthouse/ping?delay=10000",
//            "http://localhost:8080/Lighthouse/redirect?count=2",
//            "http://localhost:8080/Lighthouse/redirect?to=http://localhost:8080/Lighthouse/empty",
//            "https://localhost:8080/Lighthouse/empty",
//            "https://localhost:8443/Lighthouse/empty",
//            "https://localhost:8443/Lighthouse/ping",
            "https://google.com/",
//            "https://www.google.com/about",
//            "http://localhost:8080/Lighthouse/file",
//            "http://localhost:8080/Lighthouse",
//            "http://archive.apache.org/dist/httpcomponents/httpclient/source/httpcomponents-client-4.2.1-src.zip"
//            "http://www.apache.org/dist/httpcomponents/httpclient/KEYS"
//            "https://ml.wikipedia.org/robots.txt"
        };

//        mgr.setUserAgent("");
//        mgr.setUserAgent("abc123");
//        mgr.setUserAgent(null);
//        mgr.setUserAgent(
//            "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.1 (KHTML, like Gecko) Chrome/21.0.1180.83 Safari/537.1");

        http.setConnCacheSize(10);
//        http.setCleanUrls(false);
//        http.setForceSerialExecution(true);

//        boolean multiThreaded = true;
        boolean multiThreaded = false;
//        boolean poolPrint = true;
        boolean poolPrint = false;
        boolean useConvenience = true;

        if(poolPrint) {
            Timer timer = new Timer(true);
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    System.out.println(http.getPoolStats());
                }
            }, 1000, 1000);
        }

//        final HttpRequestOptions options = null;
        HttpRequestOptions options = new HttpRequestOptions()
//            .saveNothing()
//            .setSaveRequest(true)
//            .setOverrideUserAgent("Mozilla/5.0 (Windows NT 6.1; WOW64; rv:34.0) Gecko/20100101 Firefox/34.0")
//            .setReplaceAjaxFragment(true)
//            .setUserAgent("MySuperUserAgent")
//            .setSaveResponseHeaders(true)
//            .setSaveRedirectResponseHeaders(true)
//            .setSaveRedirectResponseContent(false)
//            .setSaveRequest(false)
//            .setAllowRedirectCriteria(new NoFollowHttpAllowRedirectCriteriaParams())
//            .setIgnoreSslProblems(false)   // Will help troubleshoot SSL
//            .setSaveSecurity(false)        // Will help troubleshoot SSL
            .setPrintExecutionTrace(true)
        ;

        if(multiThreaded) {
            List<Thread> threads = new ArrayList<>();
            for(String originalUrl : urls) {
                threads.add(new Thread() {
                    @Override
                    public void run() {
                        HttpResource resource = getResource(useConvenience, options, originalUrl);
                        synchronized(Http.getInstance()) {
                            System.out.println("TID=" + Thread.currentThread().getId());
                            System.out.println(resource);
                //            printContent(r);
                            System.out.println();
                        }
                    }
                });
            }
            long T = System.currentTimeMillis();
            for(Thread t : threads) {
                t.start();
            }
            for(Thread t : threads) {
                try {
                    t.join();
                } catch(InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
            System.out.println("TOTAL DUR = " + (System.currentTimeMillis() - T));
        } else {
            long T = System.currentTimeMillis();
            for(String originalUrl : urls) {
                HttpResource resource = getResource(useConvenience, options, originalUrl);
//                resource.discardContent();
                System.out.println(resource.toString());
                System.out.println();
//                OrbweaverFrame f = Orbweaver.getOrbFrame();
//                f.getModel().addDocumentTab("result", r.getContent());
//                System.out.println(Arrays.toString(http.getSummaryState().getPoolStats()));
            }
            System.out.println("TOTAL DUR = " + (System.currentTimeMillis() - T));
        }
        System.out.println("# Redirects = " + http.getNumRedirects());

        printStatus(http);
//        System.out.println("User Agent: " + http.getUserAgent());
//        testThreads("HTTP://headers.jsontest.com/");

        http.shutdown();
    }

    private static HttpResource getResource(boolean useConvenience, HttpRequestOptions options,
                                            String originalUrl) {
        if(useConvenience) {
            return Http.getInstance().doGet(originalUrl, options);
        }
        HttpRequestWrapper requestWrapper =
            Http.getInstance().create(RequestMethod.GET, originalUrl, options);
        return Http.getInstance().request(requestWrapper);
    }

    private static void printStatus(Http http) {
        PoolStats stats = http.getPoolStats();

        String httpStatus =
            "#rq=" + http.getNumRequests() + " " +
            "#as=" + http.getNumActualSizes() + " " +
            "#rs=" + http.getNumReportedSizes() + " " +
            "#cs=" + http.getNumCorrectSizes() + " " +
            "#is=" + http.getNumIncorrectSizes() + " " +
            "#ice=" + http.getNumIncorrectEntCe() + " " +
            "#icl=" + http.getNumIncorrectEntCl() + " " +
            "#ict=" + http.getNumIncorrectEntCt() + " " +
            "#us=" + http.getNumUnknownSizes() + "\n" +
            "#tb=" + http.getNumTooBig() + " " +
            "#er=" + http.getNumErrors() + " " +
            "#rd=" + http.getNumRedirects() + " " +
            "#crq=" + http.getNumCleanReq() + " " +
            "#crd=" + http.getNumCleanRedir() + " " +
            "#urc=" + http.getNumUnexpectedRc() + " " +
            "Saz=" + http.getSumActualSizes() + " " +
            "Srz=" + http.getSumReportedSizes() + " " +
            "Pre=" + http.getNumRequestsPreempted() + " " +
            "sch=" + http.getStatusCodeHistogram() + "\n" +
            "PS{Avail=" + stats.getAvailable() + ", " +
            "Leased="   + stats.getLeased() + ", " +
            "Pending="  + stats.getPending() + ", " +
            "Max="      + stats.getMax() + "}";
        System.out.println(httpStatus);
//        System.out.println(http.getUrlStats().toString());
    }

    private static void testThreadsForPooling(final String url) {
        for(int i = 0; i < 100; i++) {
            final int T = i;
            new Thread() {
                @Override
                public void run() {
                    for(int i = 0; i < 5; i++) {
                        HttpResource r = Http.getInstance().doGet(url);
                        PoolStats stats = Http.getInstance().getPoolStats();
                        System.out.println("T[" + T + "],I[" + i + "],R[" + r.getResponseCode() + "] => " +
                        "PS{Avail=" + stats.getAvailable() + ", " +
                        "Leased="   + stats.getLeased() + ", " +
                        "Pending="  + stats.getPending() + ", " +
                        "Max="      + stats.getMax() + "}");
                        try {
                            Thread.sleep(5000);
                        } catch(Exception e) {
                            throw new RuntimeException(e);
                        }
                    }
                }
            }.start();
        }
    }

    private static void printContent(HttpResource r) {
        if(!r.isError()) {
            System.out.println("------------------------------");
            if(r.hasContent()) {
                System.out.println(r.getContentAsString().trim());
            } else {
                System.out.println("<NO CONTENT>");
            }
            System.out.println("------------------------------");
        }
    }
}
