package gov.sandia.webcomms.http.internal;

import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.http.Header;
import org.apache.http.HttpException;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.auth.AuthScheme;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.AuthState;
import org.apache.http.auth.Credentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.protocol.ClientContext;
import org.apache.http.impl.client.ClientParamsStack;
import org.apache.http.impl.client.RequestWrapper;
import org.apache.http.message.BasicHttpRequest;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.SyncBasicHttpParams;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.DefaultedHttpContext;
import org.apache.http.protocol.ExecutionContext;
import org.apache.http.protocol.HttpContext;

import gov.sandia.webcomms.http.Http;
import replete.util.DebugUtil;
import replete.util.ReflectionUtil;

// This class isn't used much yet.

public class InternalRequestInterceptor implements HttpRequestInterceptor {


    ////////////
    // FIELDS //
    ////////////

    private Http http;


    /////////////////
    // CONSTRUCTOR //
    /////////////////

    public InternalRequestInterceptor(Http http) {
        this.http = http;
    }


    ////////////////
    // OVERRIDDEN //
    ////////////////

    @Override
    public void process(HttpRequest request, HttpContext context)
            throws HttpException, IOException {

//        printRequestDetailsFromIntercept(request, context);

        AuthState authState = (AuthState) context
            .getAttribute(ClientContext.TARGET_AUTH_STATE);

        // If no auth scheme available yet, try to initialize it
        // preemptively.
        if (authState.getAuthScheme() == null) {
            AuthScheme authScheme = (AuthScheme) context.getAttribute("preemptive-auth");
            CredentialsProvider credsProvider = (CredentialsProvider) context
                    .getAttribute(ClientContext.CREDS_PROVIDER);
            HttpHost targetHost = (HttpHost) context
                    .getAttribute(ExecutionContext.HTTP_TARGET_HOST);
            if (authScheme != null) {
                Credentials creds = credsProvider.getCredentials(new AuthScope(targetHost
                        .getHostName(), targetHost.getPort()));
                if (creds == null) {
                    return;
                }
                authState.update(authScheme, creds);
            }
        }
    }

    private void printRequestDetailsFromIntercept(HttpRequest request, HttpContext context) {
        RequestTrace trace = http.getRequestTrace();
        System.out.println("RD==="+trace.getResource().getRedirects());

        if(request instanceof BasicHttpRequest) {
            BasicHttpRequest uriRequest = (BasicHttpRequest) request;
            System.out.println("[DEBUG] Basic Request");
            System.out.println("=====================");
            System.out.println("  Thread ID         = " + Thread.currentThread().getId());
            System.out.println("  RQ                = " + request);
            System.out.println("  RQ.hashCode       = " + request.hashCode());
            System.out.println("  RQ.Type           = " + DebugUtil.getTypeInfo(request));
            System.out.println("  RQ.PV             = " + uriRequest.getProtocolVersion());
            System.out.println("  RQ.RL.URI         = " + request.getRequestLine().getUri());
            System.out.println("  RQ.RL.MTH         = " + request.getRequestLine().getMethod());
            System.out.println("  RQ.RL.PV          = " + request.getRequestLine().getProtocolVersion());
            System.out.println("  CTX               = " + DebugUtil.getTypeInfo(context));
            System.out.println("  Headers:");
            printHeaders(uriRequest.getAllHeaders());
            printParams(uriRequest.getParams());
            printContextAttributes(context);

        } else if(request instanceof RequestWrapper) {
            RequestWrapper wrapper = (RequestWrapper) request;
            HttpUriRequest orig = ReflectionUtil.get(request, "original");
            System.out.println("[DEBUG] Real Request");
            System.out.println("====================");
            System.out.println("  Thread ID         = " + Thread.currentThread().getId());
            System.out.println("  RQ                = " + wrapper);
            System.out.println("  RQ.hashCode       = " + wrapper.hashCode());
            System.out.println("  RQ.Type           = " + DebugUtil.getTypeInfo(wrapper));
            System.out.println("  RQ.URI            = " + wrapper.getURI());
            System.out.println("  RQ.MTH            = " + wrapper.getMethod());
            System.out.println("  RQ.isAb           = " + wrapper.isAborted());
            System.out.println("  RQ.isR            = " + wrapper.isRepeatable());
            System.out.println("  RQ.PV             = " + wrapper.getProtocolVersion());
            System.out.println("  RQ.EC             = " + wrapper.getExecCount());
            System.out.println("  RQ.RL.URI         = " + wrapper.getRequestLine().getUri());
            System.out.println("  RQ.RL.MTH         = " + wrapper.getRequestLine().getMethod());
            System.out.println("  RQ.RL.PV          = " + wrapper.getRequestLine().getProtocolVersion());
            System.out.println("  Headers:");
            printHeaders(wrapper.getAllHeaders());
            printParams(wrapper.getParams());
            System.out.println("  RQ                = " + orig);
            System.out.println("  RQ.OR.hashCode       = " + orig.hashCode());
            System.out.println("  RQ.OR.Type           = " + DebugUtil.getTypeInfo(orig));
            System.out.println("  RQ.OR.URI            = " + orig.getURI());
            System.out.println("  RQ.OR.MTH            = " + orig.getMethod());
            System.out.println("  RQ.OR.isAb           = " + orig.isAborted());
//            System.out.println("  RQ.OR.isR            = " + orig.isRepeatable());
            System.out.println("  RQ.OR.PV             = " + orig.getProtocolVersion());
//            System.out.println("  RQ.OR.EC             = " + orig.getExecCount());
            System.out.println("  RQ.OR.RL.URI         = " + orig.getRequestLine().getUri());
            System.out.println("  RQ.OR.RL.MTH         = " + orig.getRequestLine().getMethod());
            System.out.println("  RQ.OR.RL.PV          = " + orig.getRequestLine().getProtocolVersion());
            System.out.println("  Headers:");
            printHeaders(orig.getAllHeaders());
            printParams(orig.getParams());
            System.out.println("  CTX               = " + context);
            printContextAttributes(context);
        }
    }

    public static void printParams(HttpParams params) {
        if(params instanceof ClientParamsStack) {
            System.out.println("  Params/RequestParams:");
            printParams(ReflectionUtil.get(params, "requestParams"));
            System.out.println("  Params/ClientParams:");
            printParams(ReflectionUtil.get(params, "clientParams"));
   //              System.out.println("  Params/AppParams:"); // also, override params
   //              SyncBasicHttpParams ap = (SyncBasicHttpParams) ReflectionUtil.get("clientParams", params);
   //              if(ap != null) {
   //                  for(String name : ap.getNames()) {
   //                      System.out.println("    " + name + " = " + ap.getParameter(name));
   //                  }
   //              }
        } else if(params instanceof SyncBasicHttpParams) {
            SyncBasicHttpParams cp = (SyncBasicHttpParams) params;
            for(String name : cp.getNames()) {
                System.out.println("    " + name + " = " + cp.getParameter(name));
            }
        } else if(params instanceof BasicHttpParams) {
            BasicHttpParams bp = (BasicHttpParams) params;
            for(String name : bp.getNames()) {
                System.out.println("    " + name + " = " + bp.getParameter(name));
            }
        } else {
            System.out.println("HUH? " + DebugUtil.getTypeInfo(params));
        }
    }


    public static void printHeaders(Header[] headers) {
        System.out.println("Headers:");
        for(Header header : headers) {
            System.out.println("    " + header.getName() + " = " + header.getValue());
        }
    }

    private void printContextAttributes(HttpContext context) {
        Set<String> attrs = new HashSet<>();
        printContextAttributes(context, attrs);
        System.out.println("  Context Attributes:");
        for(String attr : attrs) {
            System.out.println("    " + attr + " = " + context.getAttribute(attr));
        }
    }
    private void printContextAttributes(HttpContext context, Set<String> attrs) {
        if(context instanceof DefaultedHttpContext) {
            DefaultedHttpContext dc = (DefaultedHttpContext) context;
            HttpContext defaults = ReflectionUtil.get(dc, "defaults");
            printContextAttributes(defaults, attrs);
            HttpContext local = ReflectionUtil.get(dc, "local");
            printContextAttributes(local, attrs);
        } else if(context instanceof BasicHttpContext) {
            BasicHttpContext bc = (BasicHttpContext) context;
            HttpContext parent = ReflectionUtil.get(bc, "parentContext");
            printContextAttributes(parent, attrs);
            Map map = ReflectionUtil.get(bc, "map");
            for(Object key : map.keySet()) {
                attrs.add((String) key);
            }
        } else if(context != null) {
            DebugUtil.printObjectDetails(context);
            ReflectionUtil.printMembers(context);
        }
    }
}
