package gov.sandia.webcomms.http.internal;

import org.apache.http.ConnectionReuseStrategy;
import org.apache.http.client.AuthenticationStrategy;
import org.apache.http.client.HttpRequestRetryHandler;
import org.apache.http.client.RedirectStrategy;
import org.apache.http.client.RequestDirector;
import org.apache.http.client.UserTokenHandler;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.ConnectionKeepAliveStrategy;
import org.apache.http.conn.routing.HttpRoutePlanner;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.HttpProcessor;
import org.apache.http.protocol.HttpRequestExecutor;

import gov.sandia.webcomms.http.Http;
import replete.util.ReflectionUtil;

public class InternalHttpClient extends DefaultHttpClient {


    ////////////
    // FIELDS //
    ////////////

    private Http http;


    //////////////////
    // CONSTRUCTORS //
    //////////////////

    public InternalHttpClient(ClientConnectionManager conman, Http http) {
        super(conman);
        this.http = http;
    }


    ////////////////
    // OVERRIDDEN //
    ////////////////

    // http://stackoverflow.com/questions/23281462/apache-httpclient-4-3-3-how-to-find-target-ip-of-the-requested-site
    // http://grepcode.com/file/repo1.maven.org/maven2/org.apache.httpcomponents/httpclient/4.2.1/org/apache/http/impl/client/DefaultRequestDirector.java
    @Override
    protected RequestDirector createClientRequestDirector(   // Must be overridden like this, no mutator
            HttpRequestExecutor requestExec,
            ClientConnectionManager conman,
            ConnectionReuseStrategy reustrat,
            ConnectionKeepAliveStrategy kastrat,
            HttpRoutePlanner rouplan,
            HttpProcessor httpProcessor,
            HttpRequestRetryHandler retryHandler,
            RedirectStrategy redirectStrategy,
            AuthenticationStrategy targetAuthStrategy,
            AuthenticationStrategy proxyAuthStrategy,
            UserTokenHandler userTokenHandler,
            HttpParams params) {

        RequestDirector director = new InternalRequestDirector(
            ReflectionUtil.get(this, "log"),
            requestExec, conman, reustrat, kastrat, rouplan,
            httpProcessor, retryHandler, redirectStrategy,
            targetAuthStrategy, proxyAuthStrategy,
            userTokenHandler, params, http);

        return director;
    }

    @Override
    protected HttpRequestRetryHandler createHttpRequestRetryHandler() {    // Can also just be set with client.setHttpRequestRetryHandler(strategy);
        return new InternalHttpRequestRetryHandler(http);
    }

    // Commented out for future reference.
//    @Override
//    protected ConnectionReuseStrategy createConnectionReuseStrategy() {    // Can also just be set with client.setReuseStrategy(strategy);
//        return new DefaultConnectionReuseStrategy() {
//            @Override
//            public boolean keepAlive(HttpResponse response, HttpContext context) {
//                boolean keepAlive = super.keepAlive(response, context);
//                // Possible custom implementation
//                return keepAlive;
//            }
//        };
//    }
//
//    @Override
//    protected ConnectionKeepAliveStrategy createConnectionKeepAliveStrategy() {   // Can also just be set with client.setKeepAliveStrategy(strategy);
//        return new DefaultConnectionKeepAliveStrategy() {
//            @Override
//            public long getKeepAliveDuration(HttpResponse response, HttpContext context) {
//                long dur = super.getKeepAliveDuration(response, context);
//                // Possible custom implementation
//                return dur;
//            }
//        };
//    }
}
