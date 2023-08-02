package gov.sandia.webcomms.http.rsc;

import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import org.mongodb.morphia.annotations.Embedded;
import org.mongodb.morphia.annotations.PreLoad;
import org.mongodb.morphia.annotations.PreSave;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;

import gov.sandia.webcomms.http.util.EncodingUtil;
import replete.errors.RuntimeConvertedException;

// This class, right now, is a slimmed-down Resource object.
// This is because we haven't developed requirements that
// we save as much information about redirected requests
// as the ultimately-fulfilled requests.
// Future: Remove this class and replace with a standard
// HttpResource class.  This will lend itself to both
// code simplification but also completeness with
// regard to the saving of request/response information.
@Embedded
public class HttpRedirect implements Serializable {


    ////////////
    // FIELDS //
    ////////////

    private String baseUrl;                         // e.g. "http://entertainmentweekly.com"
    private String originalUrl;                     // e.g. "/docs/main page.html"
    private String cleanedUrl;                      // e.g. "http://www.site.com/docs/main%20page.html"
    protected String providingIpPort;               // e.g. "184.73.43.201:80"
    protected String contentType;                   // e.g. "text/html; charset=ISO-8859-1".  Format: "MIME[; ENC]"
    private int responseCode;                       // 301 or 302 usually
    private String responseMessage;                 // "Moved Permanently", "Moved Temporarily", "Found", etc.
    protected Map<String, String> responseHeaders;  // Lazily instantiated, depending on HttpRequestOptions, init'ed by addHeader
    @Embedded protected RequestInfo reqInfo;                  // Lazily instantiated, depending on HttpRequestOptions, init'ed by getRequestInfo
    protected byte[] content;                       // Will be serialized, but should always be small (also, by default this isn't even recorded)
    // ^Doesn't yet have MD5, length, or discarded fields

    // NOTE: We could record the response content as well someday.
    // Not doing it yet as we haven't decided such is a requirement.
    // Although it appears many websites do NOT return content
    // for redirects, some do:
    //     <!DOCTYPE HTML PUBLIC "-//IETF//DTD HTML 2.0//EN">
    //     <html><head>
    //     <title>301 Moved Permanently</title>
    //     </head><body>
    //     <h1>Moved Permanently</h1>
    //     <p>The document has moved <a href="http://www.entertainmentweekly.com/">here</a>.</p>
    //     </body></html>
    // A decision to record this information would make this class
    // start to look a lot more like the Resource class.  This would
    // include the addition of:
    //  * contentEncoding
    //  * reportedSize
    //  * contentMd5Hash
    //  * exception, startDownload, endDownload (potentially?)
    // But not, obviously, the 'redirects' list.


    //////////////////////////
    // ACCESSORS / MUTATORS //
    //////////////////////////

    // Accessors

    public String getBaseUrl() {
        return baseUrl;
    }
    public String getOriginalUrl() {
        return originalUrl;
    }
    public String getCleanedUrl() {
        return cleanedUrl;
    }
    public String getProvidingIpPort() {
        return providingIpPort;
    }
    public String getContentType() {
        return contentType;
    }
    public int getResponseCode() {
        return responseCode;
    }
    public String getResponseMessage() {
        return responseMessage;
    }
    public Map<String, String> getResponseHeaders() {
        if(responseHeaders == null) {
            return null;
        }
        return Collections.unmodifiableMap(responseHeaders);
    }
    public String getResponseHeader(String name) {
        if(responseHeaders == null) {
            return null;
        }
        return responseHeaders.get(name);
    }
    public boolean hasResponseHeader(String name) {
        if(responseHeaders == null) {
            return false;
        }
        return responseHeaders.containsKey(name);
    }
    public RequestInfo getRequestInfo() {
        return reqInfo;
    }
    public byte[] getContent() {
        return content;
    }

    // Accessors (Computed)

    public String getUrl() {
        if(cleanedUrl != null) {
            return cleanedUrl;
        }
        return originalUrl;
    }
    public boolean hasDifferentCleanedUrl() {
        return cleanedUrl != null && !cleanedUrl.equals(originalUrl);
    }
    public String getProvidingHostName() {
        if(providingIpPort != null) {
            try {
                // TODO: Investigate if these can be slow or not...
                InetAddress ia = InetAddress.getByName(providingIpPort.substring(0, providingIpPort.indexOf(':')));
                return ia.getCanonicalHostName();
            } catch(Exception e) {
                // do nothing
            }
        }
        return null;
    }
    public boolean hasContent() {
        return content != null;
    }
    public String getCharacterEncoding() {                // What does the HttpRedirect say?
        return EncodingUtil.findCharSet(contentType);
    }
    public String getCharacterEncodingWithDefault() {     // What will we ultimately interpret it as?
        String encoding = getCharacterEncoding();
        if(encoding == null) {
            encoding = EncodingUtil.DEFAULT_CHARSET_HTTP_1_1;
        }
        return encoding;
    }
    public String getContentAsString() {
        if(!hasContent()) {
            return null;
        }
        try {
            String encoding = getCharacterEncodingWithDefault();     // If left null, String() would use
            return new String(getContent(), encoding);               // "ISO-8859-1" anyway, but this
        } catch(UnsupportedEncodingException e) {                    // is a little more transparent.
            throw new RuntimeConvertedException(e);
        }
    }

    // Mutators

    public HttpRedirect setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
        return this;
    }
    public HttpRedirect setOriginalUrl(String originalUrl) {
        this.originalUrl = originalUrl;
        return this;
    }
    public HttpRedirect setCleanedUrl(String cleanedUrl) {
        this.cleanedUrl = cleanedUrl;
        return this;
    }
    public HttpRedirect setProvidingIpPort(String providingIpPort) {
        this.providingIpPort = providingIpPort;
        return this;
    }
    public HttpRedirect setContentType(String contentType) {
        this.contentType = contentType;
        return this;
    }
    public HttpRedirect setResponseCode(int responseCode) {
        this.responseCode = responseCode;
        return this;
    }
    public HttpRedirect setResponseMessage(String responseMessage) {
        this.responseMessage = responseMessage;
        return this;
    }
    public HttpRedirect setResponseHeaders(Map<String, String> responseHeaders) {
        this.responseHeaders = responseHeaders;
        return this;
    }
    public HttpRedirect addResponseHeader(String key, String value) {
        if(responseHeaders == null) {
            responseHeaders = new TreeMap<>();
        }
        responseHeaders.put(key, value);
        return this;
    }
    public HttpRedirect setRequestInfo(RequestInfo reqInfo) {
        this.reqInfo = reqInfo;
        return this;
    }
    public HttpRedirect setContent(byte[] content) {
        this.content = content;
        return this;
    }


    //////////
    // MISC //
    //////////

    @PreSave
    private void preSave(DBObject obj) {
        if(responseHeaders != null) {
            DBObject headers = new BasicDBObject();
            for(String headerName : responseHeaders.keySet()) {
                String cleanedHeaderName = encodeHeaderName(headerName);
                headers.put(cleanedHeaderName, responseHeaders.get(headerName));
            }
            obj.put("responseHeaders", headers);
        }
    }

    @PreLoad
    private void preLoad(DBObject obj) {
        if(obj.containsField("responseHeaders")) {
            DBObject headers = (DBObject)obj.get("responseHeaders");
            Map<String, String> rHeaders = new HashMap<>();
            for(String headerName : headers.keySet()) {
                String cleanedHeaderName = decodeHeaderName(headerName);
                rHeaders.put(cleanedHeaderName, headers.get(headerName).toString());
            }
            setResponseHeaders(rHeaders);
        }
    }

    private String encodeHeaderName(String headerName) {
        String cleaned = headerName.replace(".", "\\uff0e");
        cleaned = cleaned.replace("$", "\\uff04");
        return cleaned;
    }

    private String decodeHeaderName(String headerName) {
        String cleaned = headerName.replace("\\uff0e", ".");
        cleaned = cleaned.replace("\\uff04", "$");
        return cleaned;
    }


    ////////////////
    // OVERRIDDEN //
    ////////////////

    @Override
    public String toString() {
        return "<" + responseCode + ":" + responseMessage + "> (to-->) " + getUrl();
    }
}
