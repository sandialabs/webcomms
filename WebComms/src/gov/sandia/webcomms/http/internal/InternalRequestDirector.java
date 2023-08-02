package gov.sandia.webcomms.http.internal;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.http.ConnectionReuseStrategy;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpException;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthProtocolState;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.AuthenticationStrategy;
import org.apache.http.client.HttpRequestRetryHandler;
import org.apache.http.client.NonRepeatableRequestException;
import org.apache.http.client.RedirectStrategy;
import org.apache.http.client.UserTokenHandler;
import org.apache.http.client.methods.AbortableHttpRequest;
import org.apache.http.client.params.ClientPNames;
import org.apache.http.client.params.HttpClientParams;
import org.apache.http.client.protocol.ClientContext;
import org.apache.http.conn.BasicManagedEntity;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.ClientConnectionRequest;
import org.apache.http.conn.ConnectionKeepAliveStrategy;
import org.apache.http.conn.ManagedClientConnection;
import org.apache.http.conn.routing.HttpRoute;
import org.apache.http.conn.routing.HttpRoutePlanner;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.DefaultRequestDirector;
import org.apache.http.impl.client.RequestWrapper;
import org.apache.http.impl.client.RoutedRequest;
import org.apache.http.impl.client.TunnelRefusedException;
import org.apache.http.impl.conn.ConnectionShutdownException;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.ExecutionContext;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpProcessor;
import org.apache.http.protocol.HttpRequestExecutor;
import org.apache.http.util.EntityUtils;

import gov.sandia.webcomms.http.Http;
import replete.util.ReflectionUtil;

// http://grepcode.com/file/repo1.maven.org/maven2/org.apache.httpcomponents/httpclient/4.2.1/org/apache/http/impl/client/DefaultRequestDirector.java

// This class inherits from an HC base class, but then simply
// copies some of its primary methods because of a lack of
// extensibility in the class.  Modified sections indicated
// with "NEW CODE".

public class InternalRequestDirector extends DefaultRequestDirector {


    ////////////
    // FIELDS //
    ////////////

    private Http http;


    //////////////////
    // CONSTRUCTORS //
    //////////////////

    public InternalRequestDirector(Log log, HttpRequestExecutor requestExec, ClientConnectionManager conman,
                                   ConnectionReuseStrategy reustrat, ConnectionKeepAliveStrategy kastrat,
                                   HttpRoutePlanner rouplan, HttpProcessor httpProcessor,
                                   HttpRequestRetryHandler retryHandler, RedirectStrategy redirectStrategy,
                                   AuthenticationStrategy targetAuthStrategy, AuthenticationStrategy proxyAuthStrategy,
                                   UserTokenHandler userTokenHandler, HttpParams params, Http http) {
        super(log, requestExec, conman, reustrat, kastrat, rouplan, httpProcessor, retryHandler,
              redirectStrategy, targetAuthStrategy, proxyAuthStrategy, userTokenHandler, params);
        this.http = http;
    }


    ////////////////
    // OVERRIDDEN //
    ////////////////

    @Override
    public HttpResponse execute(HttpHost target, HttpRequest request, HttpContext context)
            throws HttpException, IOException {
        // ----- NEW CODE ------ //
        RequestTrace trace = http.getRequestTrace();
        trace.debugStep("DIRECTOR EXECUTE");
        // ----- NEW CODE ------ //

        context.setAttribute(ClientContext.TARGET_AUTH_STATE, targetAuthState);
        context.setAttribute(ClientContext.PROXY_AUTH_STATE, proxyAuthState);

        HttpRequest orig = request;

        // ----- NEW CODE ----- //
        Log log = ReflectionUtil.get(this, "log");   // log is private in base class
        Object ret2 = ReflectionUtil.invoke(this, "wrapRequest", orig);
        RequestWrapper origWrapper = (RequestWrapper) ret2;
        // Instead of:
        //RequestWrapper origWrapper = wrapRequest(orig);
        // ----- NEW CODE ----- //

        origWrapper.setParams(params);
        HttpRoute origRoute = determineRoute(target, origWrapper, context);

        HttpHost virtualHost = (HttpHost) origWrapper.getParams().getParameter(ClientPNames.VIRTUAL_HOST);

        // HTTPCLIENT-1092 - add the port if necessary
        if (virtualHost != null && virtualHost.getPort() == -1) {
            int port = target.getPort();
            if (port != -1){
                virtualHost = new HttpHost(virtualHost.getHostName(), port, virtualHost.getSchemeName());
            }
        }

        // ----- NEW CODE ----- //
        ReflectionUtil.set(this, "virtualHost", virtualHost);
        // ----- NEW CODE ----- //

        RoutedRequest roureq = new RoutedRequest(origWrapper, origRoute);

        boolean reuse = false;
        boolean done = false;
        try {
            HttpResponse response = null;
            while (!done) {
                // In this loop, the RoutedRequest may be replaced by a
                // followup request and route. The request and route passed
                // in the method arguments will be replaced. The original
                // request is still available in 'orig'.

                RequestWrapper wrapper = roureq.getRequest();
                HttpRoute route = roureq.getRoute();
                response = null;

                // See if we have a user token bound to the execution context
                Object userToken = context.getAttribute(ClientContext.USER_TOKEN);

                // ----- NEW CODE ------ //
                trace.debugBlockOpen("CONN INIT");
                try {  // Debug try-finally block (no indent)
                // ----- NEW CODE ------ //

                // Allocate connection if needed
                if (managedConn == null) {
                    ClientConnectionRequest connRequest = connManager.requestConnection(
                            route, userToken);
                    // ----- NEW CODE ------ //
                    trace.debugStep("REQUEST: " + connRequest.getClass().getName());
                    // ----- NEW CODE ------ //
                    if (orig instanceof AbortableHttpRequest) {
                        ((AbortableHttpRequest) orig).setConnectionRequest(connRequest);
                    }

                    long timeout = HttpClientParams.getConnectionManagerTimeout(params);
                    try {
                        managedConn = connRequest.getConnection(timeout, TimeUnit.MILLISECONDS);
                    } catch(InterruptedException interrupted) {
                        InterruptedIOException iox = new InterruptedIOException();
                        iox.initCause(interrupted);
                        throw iox;
                    }

                    // ----- NEW CODE ------ //
                    Object entry = ReflectionUtil.invoke(managedConn, "getPoolEntry");
                    // For reference's sake:
                    // Object entryConn = ReflectionUtil.invoke(entry, "getConnection");
                    // DebugUtil.printObjectDetails(entryConn);
                    // [*TS=org.apache.http.impl.conn.DefaultClientConnection@76fd8281]
                    //     [C=AbstractHttpClientConnection < SocketHttpClientConnection < DefaultClientConnection]
                    //     [I=OperatedClientConnection, HttpContext, HttpInetConnection, HttpClientConnection]
                    // The instance of ManagedClientConnectionImpl can change (be recreated)
                    // but it can be recreated around a shared instance of an HttpPoolEntry.
                    // The pool entry is what retains the actual connection object, which
                    // is an instance of DefaultClientConnection.
                    trace.debugStep("USE: " +
                        managedConn.getClass().getSimpleName() +
                        " [@" + managedConn.hashCode() + "] [Entry@" + entry.hashCode() + "]");
                    // ----- NEW CODE ------ //

                    if (HttpConnectionParams.isStaleCheckingEnabled(params)) {
                        // validate connection
                        if (managedConn.isOpen()) {
                            log.debug("Stale connection check");
                            if (managedConn.isStale()) {
                                log.debug("Stale connection detected");
                                managedConn.close();
                            }
                        }
                    }

                // ----- NEW CODE ------ //
                } else {
                    Object entry = ReflectionUtil.invoke(managedConn, "getPoolEntry");
                    trace.debugStep("REUSE: " +
                        managedConn.getClass().getSimpleName() +
                        " [@" + managedConn.hashCode() + "] [Entry@" + entry.hashCode() + "]");
                // ----- NEW CODE ------ //
                }

                // ----- NEW CODE ------ //
                } finally {  // Debug try-finally block (no indent)
                    trace.debugBlockClose();
                }
                // ----- NEW CODE ------ //

                if (orig instanceof AbortableHttpRequest) {
                    ((AbortableHttpRequest) orig).setReleaseTrigger(managedConn);
                }

                try {
                    tryConnect(roureq, context);            // This path invokes the InternalHostnameVerifier.verify(...) and
                                                            // InternalTrustManager.checkServerTrusted(...).
                } catch (TunnelRefusedException ex) {
                    if (log.isDebugEnabled()) {
                        log.debug(ex.getMessage());
                    }
                    response = ex.getResponse();
                    break;
                }

                String userinfo = wrapper.getURI().getUserInfo();
                if (userinfo != null) {
                    targetAuthState.update(
                            new BasicScheme(), new UsernamePasswordCredentials(userinfo));
                }

                // Reset headers on the request wrapper
                wrapper.resetHeaders();

                // Re-write request URI if needed
                rewriteRequestURI(wrapper, route);

                // Use virtual host if set
                target = virtualHost;

                if (target == null) {
                    target = route.getTargetHost();
                }

                HttpHost proxy = route.getProxyHost();

                // Populate the execution context
                context.setAttribute(ExecutionContext.HTTP_TARGET_HOST, target);
                context.setAttribute(ExecutionContext.HTTP_PROXY_HOST, proxy);
                context.setAttribute(ExecutionContext.HTTP_CONNECTION, managedConn);

                // Run request protocol interceptors
                requestExec.preProcess(wrapper, httpProcessor, context);

                // ----- NEW CODE ------ //
                if(trace.getOptions().isSaveRequest()) {
                    for(Header header : wrapper.getAllHeaders()) {
                        trace.addRequestHeader(
                            header.getName(),
                            header.getValue()
                        );
                    }
                }
                String ipPort = managedConn.getRemoteAddress().getHostAddress() + ":" + managedConn.getRemotePort();
                trace.setProvidingIpPort(ipPort);
                // ----- NEW CODE ------ //

                response = tryExecute(roureq, context);

                if (response == null) {
                    // ----- NEW CODE ------ //
                    trace.debugStep("START OVER");    // Not sure what this is for yet, debugging it for visibility
                    // ----- NEW CODE ------ //
                    // Need to start over
                    continue;
                }

                // Run response protocol interceptors
                response.setParams(params);
                requestExec.postProcess(response, httpProcessor, context);

                // The connection is in or can be brought to a re-usable state.
                reuse = reuseStrategy.keepAlive(response, context);
                // ----- NEW CODE ------ //
                long saveDuration = -2;
                // ----- NEW CODE ------ //
                if (reuse) {
                    // Set the idle duration of this connection
                    long duration = keepAliveStrategy.getKeepAliveDuration(response, context);
                    // ----- NEW CODE ------ //
                    saveDuration = duration;
                    // ----- NEW CODE ------ //
                    if (log.isDebugEnabled()) {
                        String s;
                        if (duration > 0) {
                            s = "for " + duration + " " + TimeUnit.MILLISECONDS;
                        } else {
                            s = "indefinitely";
                        }
                        log.debug("Connection can be kept alive " + s);
                    }
                    managedConn.setIdleDuration(duration, TimeUnit.MILLISECONDS);
                }

                // ----- NEW CODE ------ //
                trace.debugBlockOpen("CONN FOLLOWUP");
                try {  // Debug try-finally block (no indent)
                // ----- NEW CODE ------ //

                RoutedRequest followup = handleResponse(roureq, response, context);
                if (followup == null) {
                    // ----- NEW CODE ------ //
                    trace.debugStep("NONE");
                    // ----- NEW CODE ------ //
                    done = true;
                } else {
                    if (reuse) {
                        // Make sure the response body is fully consumed, if present
                        HttpEntity entity = response.getEntity();
                        EntityUtils.consume(entity);
                        // entity consumed above is not an auto-release entity,
                        // need to mark the connection re-usable explicitly
                        managedConn.markReusable();
                        // ----- NEW CODE ------ //
                        trace.debugStep("MARKED REUSABLE by " +
                            reuseStrategy.getClass().getSimpleName() +
                            "; KeepAliveDuration=" + saveDuration +
                            " by " + keepAliveStrategy.getClass().getSimpleName());
                        // ----- NEW CODE ------ //
                    } else {
                        managedConn.close();
                        if (proxyAuthState.getState() == AuthProtocolState.SUCCESS
                                && proxyAuthState.getAuthScheme() != null
                                && proxyAuthState.getAuthScheme().isConnectionBased()) {
                            log.debug("Resetting proxy auth state");
                            proxyAuthState.reset();
                        }
                        if (targetAuthState.getState() == AuthProtocolState.SUCCESS
                                && targetAuthState.getAuthScheme() != null
                                && targetAuthState.getAuthScheme().isConnectionBased()) {
                            log.debug("Resetting target auth state");
                            targetAuthState.reset();
                        }
                        // ----- NEW CODE ------ //
                        trace.debugStep("CLOSED");
                        // ----- NEW CODE ------ //
                    }
                    // check if we can use the same connection for the followup
                    if (!followup.getRoute().equals(roureq.getRoute())) {
                        releaseConnection();
                        // ----- NEW CODE ------ //
                        trace.debugStep("RELEASED: Because " + followup.getRoute() + " != " + roureq.getRoute());
                        // ----- NEW CODE ------ //
                    // ----- NEW CODE ------ //
                    } else {
                        trace.debugStep("NOT RELEASED: Because " + followup.getRoute() + " == " + roureq.getRoute());
                    // ----- NEW CODE ------ //
                    }
                    roureq = followup;
                }

                // ----- NEW CODE ------ //
                } finally {  // Debug try-finally block (no indent)
                    trace.debugBlockClose();
                }
                // ----- NEW CODE ------ //

                if (managedConn != null) {
                    if (userToken == null) {
                        userToken = userTokenHandler.getUserToken(context);
                        context.setAttribute(ClientContext.USER_TOKEN, userToken);
                    }
                    if (userToken != null) {
                        managedConn.setState(userToken);
                    }
                }

            } // while not done


            // check for entity, release connection if possible
            if ((response == null) || (response.getEntity() == null) ||
                !response.getEntity().isStreaming()) {
                // connection not needed and (assumed to be) in re-usable state
                if (reuse) {
                    managedConn.markReusable();
                }
                releaseConnection();
            } else {
                // install an auto-release entity
                HttpEntity entity = response.getEntity();
                entity = new BasicManagedEntity(entity, managedConn, reuse);
                response.setEntity(entity);
            }

            return response;

        } catch (ConnectionShutdownException ex) {
            InterruptedIOException ioex = new InterruptedIOException(
                    "Connection has been shut down");
            ioex.initCause(ex);
            throw ioex;
        } catch (HttpException ex) {
            abortConnection();
            throw ex;
        } catch (IOException ex) {
            abortConnection();
            throw ex;
        } catch (RuntimeException ex) {
            abortConnection();
            throw ex;
        // ----- NEW CODE ------ //
        } catch (Exception ex) {
            abortConnection();
            throw new RuntimeException(ex);
        // ----- NEW CODE ------ //
        }
    } // execute

    private void tryConnect(final RoutedRequest req, final HttpContext context) throws HttpException, IOException {
        // ----- NEW CODE ------ //
        RequestTrace trace = http.getRequestTrace();
        trace.debugBlockOpen("TRY-CONNECT");
        Log log = ReflectionUtil.get(this, "log");   // log is private in base class
        try {  // Debug try-finally block (w/ indent)
        // ----- NEW CODE ----- //

            HttpRoute route = req.getRoute();
            HttpRequest wrapper = req.getRequest();

            int connectCount = 0;
            for (;;) {
                context.setAttribute(ExecutionContext.HTTP_REQUEST, wrapper);
                // Increment connect count
                connectCount++;
                try {
                    if (!managedConn.isOpen()) {
                        // ----- NEW CODE ----- //
                        trace.debugStep("NOT OPEN -> OPEN");
                        // ----- NEW CODE ----- //
                        managedConn.open(route, context, params);
                    } else {
                        // ----- NEW CODE ----- //
                        int timeout = HttpConnectionParams.getSoTimeout(params);
                        trace.debugStep("OPEN -> RESET TIME OUT = " + timeout);
                        // ----- NEW CODE ----- //
                        managedConn.setSocketTimeout(timeout);
                    }
                    establishRoute(route, context);
                    break;
                } catch (IOException ex) {
                    // ----- NEW CODE ----- //
                    trace.debugStep("ERROR: " + ex.getClass().getSimpleName());
                    // ----- NEW CODE ----- //
                    try {
                        managedConn.close();
                    } catch (IOException ignore) {
                    }
                    if (retryHandler.retryRequest(ex, connectCount, context)) {
                        if (log.isInfoEnabled()) {
                            log.info("I/O exception ("+ ex.getClass().getName() +
                                    ") caught when connecting to the target host: "
                                    + ex.getMessage());
                            if (log.isDebugEnabled()) {
                                log.debug(ex.getMessage(), ex);
                            }
                            log.info("Retrying connect");
                        }
                    } else {
                        throw ex;
                    }
                }
            }

        // ----- NEW CODE ----- //
        } finally {  // Debug try-finally block (w/ indent)
            trace.debugBlockClose();
        }
        // ----- NEW CODE ----- //
    }

    private HttpResponse tryExecute(final RoutedRequest req, final HttpContext context) throws HttpException, IOException {
        // ----- NEW CODE ------ //
        RequestTrace trace = http.getRequestTrace();
        trace.debugBlockOpen("TRY-EXECUTE");
        Log log = ReflectionUtil.get(this, "log");   // log is private in base class
        try {  // Debug try-finally block (w/ indent)
        // ----- NEW CODE ----- //

            RequestWrapper wrapper = req.getRequest();
            HttpRoute route = req.getRoute();
            HttpResponse response = null;

            Exception retryReason = null;
            for (;;) {
                // Increment total exec count (with redirects)
                // ----- NEW CODE ----- //
                Integer execCount = ReflectionUtil.get(this, "execCount");
                execCount = execCount + 1;
                ReflectionUtil.set(this, "execCount", execCount);
                // Instead of: execCount++;    // execCount is private in base class
                // ----- NEW CODE ----- //
                // Increment exec count for this particular request
                wrapper.incrementExecCount();
                if (!wrapper.isRepeatable()) {
                    log.debug("Cannot retry non-repeatable request");
                    if (retryReason != null) {
                        throw new NonRepeatableRequestException("Cannot retry request " +
                            "with a non-repeatable request entity.  The cause lists the " +
                            "reason the original request failed.", retryReason);
                    } else {
                        throw new NonRepeatableRequestException("Cannot retry request " +
                                "with a non-repeatable request entity.");
                    }
                }

                try {
                    if (!managedConn.isOpen()) {
                        // ----- NEW CODE ----- //
                        trace.debugStep("NOT OPEN!");
                        // ----- NEW CODE ----- //

                        // If we have a direct route to the target host
                        // just re-open connection and re-try the request
                        if (!route.isTunnelled()) {
                            log.debug("Reopening the direct connection.");
                            managedConn.open(route, context, params);
                        } else {
                            // otherwise give up
                            log.debug("Proxied connection. Need to start over.");
                            break;
                        }
                    }

                    if (log.isDebugEnabled()) {
                        log.debug("Attempt " + execCount + " to execute request");
                    }
                    response = requestExec.execute(wrapper, managedConn, context);
                    break;

                } catch (IOException ex) {
                    // ----- NEW CODE ----- //
                    trace.debugStep("ERROR: " + ex.getClass().getSimpleName());
                    // ----- NEW CODE ----- //

                    log.debug("Closing the connection.");
                    try {
                        managedConn.close();
                    } catch (IOException ignore) {
                    }
                    if (retryHandler.retryRequest(ex, wrapper.getExecCount(), context)) {
                        if (log.isInfoEnabled()) {
                            log.info("I/O exception ("+ ex.getClass().getName() +
                                    ") caught when processing request: "
                                    + ex.getMessage());
                        }
                        if (log.isDebugEnabled()) {
                            log.debug(ex.getMessage(), ex);
                        }
                        log.info("Retrying request");
                        retryReason = ex;
                    } else {
                        throw ex;
                    }
                }
            }
            return response;

        // ----- NEW CODE ----- //
        } finally {  // Debug try-finally block (w/ indent)
            trace.debugBlockClose();
        }
        // ----- NEW CODE ----- //
    }

    // Copied verbatim to remove some reflection - no changes besides "log"
    private void abortConnection() {
        // ----- NEW CODE ------ //
        Log log = ReflectionUtil.get(this, "log");   // log is private in base class
        // ----- NEW CODE ----- //

        ManagedClientConnection mcc = managedConn;
        if (mcc != null) {
            // we got here as the result of an exception
            // no response will be returned, release the connection
            managedConn = null;
            try {
                mcc.abortConnection();
            } catch (IOException ex) {
                if (log.isDebugEnabled()) {
                    log.debug(ex.getMessage(), ex);
                }
            }
            // ensure the connection manager properly releases this connection
            try {
                mcc.releaseConnection();
            } catch(IOException ignored) {
                log.debug("Error releasing connection", ignored);
            }
        }
    } // abortConnection
}
