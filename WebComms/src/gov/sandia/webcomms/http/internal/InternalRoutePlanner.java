package gov.sandia.webcomms.http.internal;

import org.apache.http.HttpException;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.conn.routing.HttpRoute;
import org.apache.http.conn.routing.RouteInfo.LayerType;
import org.apache.http.conn.routing.RouteInfo.TunnelType;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.impl.conn.DefaultHttpRoutePlanner;
import org.apache.http.protocol.HttpContext;

import gov.sandia.webcomms.http.Http;
import gov.sandia.webcomms.http.options.HttpRequestOptions;

public class InternalRoutePlanner extends DefaultHttpRoutePlanner {


    ////////////
    // FIELDS //
    ////////////

    private Http http;


    //////////////////
    // CONSTRUCTORS //
    //////////////////

    public InternalRoutePlanner(SchemeRegistry schreg, Http http) {
        super(schreg);
        this.http = http;
    }


    ////////////////
    // OVERRIDDEN //
    ////////////////

    @Override
    public HttpRoute determineRoute(HttpHost host, HttpRequest request, HttpContext context) throws HttpException {
        RequestTrace trace = http.getRequestTrace();
        trace.debugBlockOpen("ROUTE PLANNER to " + host);

        try {  // Debug try-finally block (w/ indent)

            // Get the default route first, what it would have been without
            // our custom logic.  This call takes into consideration our
            // default proxy set with
            //   client.getParams().setParameter(ConnRoutePNames.DEFAULT_PROXY, proxy);
            // by executing commands like
            //   Http.getInstance().setProxy(host, port)
            //   Http.getInstance().useSandiaProxy()
            //   Http.getInstance().copyProxyFromSystem()   // Called always by default, which reads:
            //     System.getProperty("http.proxyHost") and System.getProperty("http.proxyPort")
            // But this could also have not been set appropriately and
            // the requisite proxy information is contained within the
            // request options, which are inspected after this call.  We
            // call this before the custom route planning before because
            // use some information from this default route in our custom
            // route planning.
            HttpRoute route = super.determineRoute(host, request, context);
            trace.debugStep("DEFAULT = " + route);

            // If this request's proxy options are set, then create a new
            // route with the appropriate proxy and tunneling/layering
            // options.
            HttpRequestOptions options = trace.getOptions();
            String proxyHost = options.getProxyHost();
            int proxyPort = options.getProxyPort();
            if(proxyHost != null && proxyPort != 0) {
                HttpHost newProxyHost = new HttpHost(proxyHost, proxyPort);
                route = new HttpRoute(
                    route.getTargetHost(),      // Host with resource being fetched (http://example.com)
                    route.getLocalAddress(),    // Always seems to be null
                    newProxyHost,               // Single new proxy as defined by options
                    route.isSecure(),           // We can rely on the default route's creation to at least indicate https or not (I think)
                    route.isSecure() ? TunnelType.TUNNELLED : TunnelType.PLAIN,
                    route.isSecure() ? LayerType.LAYERED : LayerType.PLAIN
                    // ^Every time a connection both HAS A PROXY & IS SECURE, we need to
                    //  turn on both tunneling and layering.
                );
                trace.debugStep("OVERRIDE = " + route);
            }

            return route;

        } finally {  // Debug try-finally block (w/ indent)
            trace.debugBlockClose();
        }
    }
}
