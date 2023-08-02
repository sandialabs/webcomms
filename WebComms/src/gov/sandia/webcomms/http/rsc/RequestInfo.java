package gov.sandia.webcomms.http.rsc;

import java.io.Serializable;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.http.HttpHeaders;
import org.apache.log4j.Logger;
import org.mongodb.morphia.annotations.Embedded;

import replete.logging.LogCode;
import replete.logging.LogCodeManager;


@Embedded
public class RequestInfo implements Serializable {


    /////////////
    // LOGGING //
    /////////////

    private static Logger logger = Logger.getLogger(RequestInfo.class);
    private static LogCode LC_HX = LogCodeManager.create("WebComms", RequestInfo.class, "H!", "No host to calculate known IPs", true);
    //private static LogCode LC_IX = LogCodeManager.create("WebComms", RequestInfo.class, "I!", "Exception when calculating known IPs", true);


    ////////////
    // FIELDS //
    ////////////

    private String userAgent;
    protected Map<String, String> headers;  // Lazily instantiated, init'ed by addHeader
    private List<String> knownIps;          // Lazily instantiated, init'ed by initKnownIps
    // Why no HttpRequestOptions??

//    protected String myRemoteIpRemote;       // e.g. "184.73.43.201:80", hard to get...
    // context info used in request?


    //////////////////////////
    // ACCESSORS / MUTATORS //
    //////////////////////////

    // Accessors

    public String getUserAgent() {
        return userAgent;
    }
    public Map<String, String> getHeaders() {
        if(headers == null) {
            return null;
        }
        return Collections.unmodifiableMap(headers);
    }
    public String getHeader(String name) {
        if(headers == null) {
            return null;
        }
        return headers.get(name);
    }
    public boolean hasHeader(String name) {
        if(headers == null) {
            return false;
        }
        return headers.containsKey(name);
    }
    public List<String> getKnownIps() {
        return knownIps;
    }

    // Mutators (Builder)

    public RequestInfo setUserAgent(String userAgent) {
        this.userAgent = userAgent;
        return this;
    }
    public RequestInfo setHeaders(Map<String, String> headers) {
        this.headers = headers;
        return this;
    }
    public RequestInfo initKnownIps() {
        knownIps = null;       // Reset
        String host = getHeader(HttpHeaders.HOST);
        if(host != null) {
            try {
                InetAddress[] addrs = InetAddress.getAllByName(host);
                if(addrs.length != 0) {
                    knownIps = new ArrayList<>();
                    for(InetAddress addr : addrs) {
                        knownIps.add(addr.toString());
                    }
                }
            } catch(Exception e) {
                //logger.error(LC_IX + " " + headers, e);
                // Not worth logging this any longer.  System
                // config will be made transparent before use.
            }
        } else {
            logger.error(LC_HX + " getHeader(" + HttpHeaders.HOST + ")=null; " + headers);
        }
        return this;
    }


    //////////
    // TEST //
    //////////

    public static void main(String[] args) {
        System.out.println("RequestInfo Test");
        if(args.length == 0) {
            System.out.println("  No args provided");
        }
        for(String arg : args) {
            System.out.println("  Trying host '" + arg + "'");
            try {
                InetAddress[] addrs = InetAddress.getAllByName(arg);
                if(addrs.length != 0) {
                    for(InetAddress addr : addrs) {
                        System.out.println("    " + addr.toString());
                    }
                }
            } catch(Exception e) {
                System.out.println("    Error: " + e.getClass().getName() + ": " + e.getMessage());
                e.printStackTrace();
            }
        }
    }
}
