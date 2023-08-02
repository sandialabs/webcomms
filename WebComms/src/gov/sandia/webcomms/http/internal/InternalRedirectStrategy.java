package gov.sandia.webcomms.http.internal;

import java.net.URI;
import java.util.Map;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.ProtocolException;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.DefaultRedirectStrategy;
import org.apache.http.impl.client.RequestWrapper;
import org.apache.http.protocol.HttpContext;

import gov.sandia.webcomms.http.Http;
import gov.sandia.webcomms.http.allowrc.HttpAllowRedirectCriteria;
import gov.sandia.webcomms.http.allowrc.HttpAllowRedirectCriteriaGenerator;
import gov.sandia.webcomms.http.allowrc.HttpAllowRedirectCriteriaParams;
import gov.sandia.webcomms.http.errors.HttpNoExtensionLoadedException;
import gov.sandia.webcomms.http.errors.HttpNoLocationException;
import gov.sandia.webcomms.http.rsc.HttpRedirect;
import gov.sandia.webcomms.http.rsc.HttpResource;
import gov.sandia.webcomms.http.rsc.RequestInfo;
import gov.sandia.webcomms.http.util.UriCleaner;
import replete.io.FileUtil;
import replete.plugins.Generator;

// TODO: Read this http://www.baeldung.com/unshorten-url-httpclient
public class InternalRedirectStrategy extends DefaultRedirectStrategy {


    ////////////
    // FIELDS //
    ////////////

    private Http http;


    /////////////////
    // CONSTRUCTOR //
    /////////////////

    public InternalRedirectStrategy(Http http) {
        this.http = http;
    }


    ////////////////
    // OVERRIDDEN //
    ////////////////

    // http:// can appear to fail with a
    //     javax.net.ssl.SSLPeerUnverifiedException: peer not authenticated
    // but this is due to the redirect to https:// (which then fails)
    // Here is the order of the calls:
    //    1. isRedirected(request, response, context)
    //    2. createLocationURI(location)   ("Location" string from response header)
    //    3. getRedirect(request, response, context)
    // Need a way to tie the original request objects with the chain of redirect
    // response/request pairs, because that latter information will be placed into
    // the Resource object that is ultimately returned.
    // Web crawler has a whole needs to decide if forwarded resource needs to be
    // moved to a different downloader or node depending on new forwarded URL
    // or if it's OK to reside on the Request-URI's downloader on the same node.

    @Override
    public boolean isRedirected(HttpRequest request, HttpResponse response, HttpContext context)
            throws ProtocolException {

        boolean result = super.isRedirected(request, response, context);

//            synchronized(this) {
//                HttpUriRequest uriRequest = (HttpUriRequest) request;
//                HttpUriRequest orig = (HttpUriRequest) ReflectionUtil.get("original", request);
//                System.out.println("[DEBUG] isRedirected");
//                System.out.println("====================");
//                System.out.println("  Thread ID         = " + Thread.currentThread().getId());
//                System.out.println("  RQ                = " + request);
//                System.out.println("  RQ.hashCode       = " + request.hashCode());
//                System.out.println("  RQ.Type           = " + DebugUtil.getTypeInfo(request));
//                System.out.println("  RQ.URI            = " + uriRequest.getURI());
//                System.out.println("  RQ.RL.URI         = " + request.getRequestLine().getUri());
//                System.out.println("  RQ.Orig           = " + orig);
//                System.out.println("  RQ.Orig.hashCode  = " + orig.hashCode());  // same as anyRequest hash on first redirect anyway
//                System.out.println("  RQ.Orig.Type      = " + DebugUtil.getTypeInfo(orig));
//                System.out.println("  RQ.Orig.URI       = " + orig.getURI());
//                System.out.println("  RQ.Orig.RL.URI    = " + orig.getRequestLine().getUri());
//                System.out.println("  RSP               = " + response);
//                System.out.println("  CTX               = " + context);
//                System.out.println("  DO-REDIRECT       ? " + result);
//            }

        RequestTrace trace = http.getRequestTrace();

        // If a redirect will take place increment the appropriate count.
        if(result) {
            trace.debugBlockOpen("REDIRECT CHECK");

            try {  // Debug try-finally block (no indent)

            http.incRedirects();

            HttpUriRequest uriReq = (HttpUriRequest) ((RequestWrapper) request).getOriginal();
            String baseUri = uriReq.getURI().toString();

            String location;
            try {
                location = getLocation(response, baseUri);
            } catch(HttpNoLocationException e) {
                trace.setOverrideException(e);
                recordResourceRequestInfo(trace);
                return false;
            }

//                synchronized(this) {
//                    System.out.println("  >Location         = " + location);
//                    System.out.println("  >RW.Orig.hashCode = " + uriReq.hashCode());
//                    System.out.println("  >baseURI          = " + baseUri);
//                    System.out.println("  >#Redir           = " + numRedirects.get());
//                }

            // Save the base URI for this thread for use in createLocationURI.

            // Sometimes the Redirect URI is ALSO technically invalid from a
            // standards & Java URI(string) standpoint and must be cleaned.  E.g.:
            //   Location: http://sourceforge.net/p/forge/documentation/Docs Home/
            // Clean the location header value.  These values often have
            // invalid URL's too, but can be cleaned.  The clean method
            // below will throw all necessary "invalid URL" exceptions
            // in the same manner that DefaultHttpCilent would.  This
            // allows us to have a consistent top-level exception to look
            // for related to malformed URLs (HttpUrlFormatException).
            // At one point I thought this was necessary for the proper
            // handling of *relative* URLs in the 'Location' header, as
            // I found evidence that relative URLs maybe weren't in the
            // HTTP standard.  However, it does seem that DefaultHttpClient
            // handles this by itself and this is not an issue.  However,
            // for normalization purposes, we want to perform a conversion
            // back to an absolute URL ourselves so that it is consistent
            // with any other data structures in the system.
            // ClientPNames.REJECT_RELATIVE_REDIRECT='http.protocol.reject-relative-redirect':  defines
            // whether relative redirects should be rejected. HTTP specification requires the location
            // value be an absolute URI. This parameter expects a value of type java.lang.Boolean. If
            // this parameter is not set relative redirects will be allowed.
            // http://hc.apache.org/httpcomponents-client-4.2.x/tutorial/html/httpagent.html

            String cleanedLocation =
                http.getDefaultOptions().isCleanUrls() ? UriCleaner.clean(location, baseUri, true) : null;
            HttpAllowRedirectCriteriaParams criteriaParams = trace.getOptions().getAllowRedirectCriteriaParams();
            boolean doRedirect = true;
            if(criteriaParams != null) {
                HttpAllowRedirectCriteriaGenerator generator = Generator.lookup(criteriaParams);
                if(generator == null) {
                    throw new HttpNoExtensionLoadedException(
                        "[Allow Redirect Criteria] No extension loaded for class '" +
                        criteriaParams.getClass().getName() + "'"
                    );
                }
                HttpAllowRedirectCriteria criteria = generator.createCriteria(criteriaParams);
                if(!criteria.doRedirect(location, cleanedLocation)) {
                    doRedirect = false;
                    trace.debugStep("ARC Deny: " + generator.getName());
                } else {
                    trace.debugStep("ARC Accept: " + generator.getName());
                }
            }

            // If there is no registered redirect strategy or there is
            // one and it allows the redirect...
            if(doRedirect) {
                trace.setRedirectRequestUrl(
                    cleanedLocation == null ? location : cleanedLocation);

                // If the cleaning process modified the URL, then record that
                // information in both the counts and the log.
                if(cleanedLocation != null && !cleanedLocation.equals(location)) {
                    http.incCleanRedir();
                    http.log(Http.LC_UP_RD + " " + location + " [=>] " + cleanedLocation + " [USING BASE] " + baseUri);
                }

                // Optionally record this redirect
                if(trace.getOptions().isSaveRedirects()) {
                    HttpRedirect redirect = new HttpRedirect()
                        .setBaseUrl(baseUri)
                        .setOriginalUrl(location)
                        .setCleanedUrl(cleanedLocation)
                        .setProvidingIpPort(trace.getProvidingIpPort())
                        .setResponseCode(response.getStatusLine().getStatusCode())
                        .setResponseMessage(response.getStatusLine().getReasonPhrase());

                    RequestInfo reqInfo = null;
                    if(trace.getOptions().isSaveRequest()) {
                        reqInfo = redirect.getRequestInfo();
                        if(reqInfo == null) {
                            reqInfo = new RequestInfo();
                            redirect.setRequestInfo(reqInfo);
                        }
                    }

                    recordRequestInfo(trace, reqInfo);

                    // Optionally record the redirect's response headers
                    if(trace.getOptions().isSaveRedirectResponseHeaders()) {
                        for(Header header : response.getAllHeaders()) {
                            redirect.addResponseHeader(
                                header.getName(),
                                header.getValue()
                            );
                        }
                    }

                    HttpEntity entity = response.getEntity();

                    if(entity != null) {             // Can be null?
                        String contentType =
                            (entity.getContentType() != null) ?
                                entity.getContentType().getValue() : null;
//                        String contentEncoding =
//                            (entity.getContentEncoding() != null) ?
//                                entity.getContentEncoding().getValue() : null;
                        redirect
                            .setContentType(contentType);
//                            .setContentEncoding(contentEncoding);

                        if(trace.getOptions().isSaveRedirectResponseContent()) {
                            try {
                                byte[] content = FileUtil.readBytes(entity.getContent());
                                redirect.setContent(content);
                            } catch(Exception e) {
                                // TODO: Not sure if worth doing anything here?
                            }
                        }
                    }

                    // Append the redirect object to the resource.
                    trace.getResource().addRedirect(redirect);
                }

                trace.debugStep("TO --> " + cleanedLocation);
                return true;
            }

            // At this point we know that the HTTP client says we should
            // redirect but our own Java code has disallowed that.
            trace.getResource().setRedirectDisallowed(true);

            } finally {  // Debug try-finally block (no indent)
                trace.debugBlockClose();
            }
        }

        recordResourceRequestInfo(trace);

        return false;
    }


    private void recordResourceRequestInfo(RequestTrace trace) {
        HttpResource resource = trace.getResource();

        // At this point we know that we are not continuing on to request
        // another resource from a different URL so we know that the
        // previous providing IP/Port will be for the resource itself.
        resource.setProvidingIpPort(trace.getProvidingIpPort());

        RequestInfo reqInfo = null;
        if(trace.getOptions().isSaveRequest()) {
            reqInfo = resource.getRequestInfo();
            if(reqInfo == null) {
                reqInfo = new RequestInfo();
                resource.setRequestInfo(reqInfo);
            }
        }

        recordRequestInfo(trace, reqInfo);
    }

    private void recordRequestInfo(RequestTrace trace, RequestInfo reqInfo) {

        // Clear the current thread's providing IP/port now that it has been
        // read to hopefully spot any code-path issues used by the IP/port
        // recording strategy.
        trace.setProvidingIpPort(null);

        if(reqInfo != null) {
            Map<String, String> rHeaders = trace.getRequestHeaders();
            String ua = rHeaders.get(HttpHeaders.USER_AGENT);
            reqInfo.setUserAgent(ua);
            reqInfo.setHeaders(rHeaders);
            trace.clearRequestHeaders();
            reqInfo.initKnownIps();
        }
    }

    private String getLocation(HttpResponse response, String baseUri)
            throws HttpNoLocationException {

        // Get the value of the "Location" header in the response.
        Header locHeader = response.getFirstHeader(HttpHeaders.LOCATION);

        // This case is technically possible given the implementation
        // seen in DefaultRedirectStrategy:4.2.1.  If status code
        // is "Moved Permanently" or "Temporary Redirect" then the
        // location field is not used in the return value of
        // super.isRedirected(...).
        if(locHeader == null) {
            String codeMsg;
            try {
                codeMsg =
                    "<" + response.getStatusLine().getStatusCode() + ":" +
                    response.getStatusLine().getReasonPhrase() + ">";
            } catch(Exception e) {
                http.log(Http.LC_LX + "", e);   // Temporary... just in case.
                codeMsg = "<ERR>";
            }
            throw new HttpNoLocationException(codeMsg + " No location in redirect for request to " + baseUri);
        }

        return locHeader.getValue();
    }

    // Pass the cleaned version back to the base class to
    // continue the redirect process.
    @Override
    protected URI createLocationURI(String location) throws ProtocolException {
        RequestTrace trace = http.getRequestTrace();
        String nextLocation = trace.getRedirectRequestUrl();
        return super.createLocationURI(nextLocation);
    }

    @Override
    public HttpUriRequest getRedirect(HttpRequest request,
            HttpResponse response, HttpContext context) throws ProtocolException {
        return super.getRedirect(request, response, context);
    }
}
