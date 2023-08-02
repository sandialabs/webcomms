package gov.sandia.webcomms.http.rsc;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.bson.types.ObjectId;
import org.mongodb.morphia.annotations.Embedded;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Field;
import org.mongodb.morphia.annotations.Id;
import org.mongodb.morphia.annotations.Index;
import org.mongodb.morphia.annotations.Indexes;
import org.mongodb.morphia.annotations.PostLoad;
import org.mongodb.morphia.annotations.PostPersist;
import org.mongodb.morphia.annotations.PreLoad;
import org.mongodb.morphia.annotations.PreSave;
import org.mongodb.morphia.annotations.Transient;
import org.mongodb.morphia.utils.IndexType;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;

import gov.sandia.cortext.plugin.CortextPlugin;
import gov.sandia.cortext.tika.old.MimeTypesConverter;
import gov.sandia.mongo.MongoConnectionSettings;
import gov.sandia.mongo.MongoGridFsUtil;
import gov.sandia.webcomms.http.Http;
import gov.sandia.webcomms.http.RequestMethod;
import gov.sandia.webcomms.http.util.EncodingUtil;
import gov.sandia.webcomms.plugin.WebCommsPlugin;
import replete.collections.ListUtil;
import replete.errors.ExceptionUtil;
import replete.errors.RuntimeConvertedException;
import replete.errors.ThrowableSnapshot;
import replete.hash.Md5Util;
import replete.plugins.PluginManager;
import replete.plugins.RepletePlugin;
import replete.text.RStringBuilder;
import replete.text.StringLib;
import replete.text.StringUtil;
import replete.util.DateUtil;
import replete.util.ElapsedVerbosity;

// TODO: One day consider including a copy of the HttpRequestOptions that were
// used to fetch the resource.

// TODO: One day consider including any default Http.getInstance()
// properties that were set that are not a part of the HttpRequestOptions
// object that could influence the response (e.g. default/global proxy).

// I have noticed that one of the headers returned from:
//   http://harrypotter.wikia.com/wiki/Rufus_Scrimgeour
// was "Content-language" instead of "Content-Language".
// need some normalization here too I guess...

// http://www.iana.org/assignments/character-sets/character-sets.xhtml
// https://www.w3.org/Protocols/rfc2616/rfc2616-sec7.html

// This class should probably be refactored someday according to {artf194123}.

@Entity(noClassnameStored=true)
@Indexes({
    @Index(fields=@Field(value="cleanedUrl",type=IndexType.HASHED))
})
public class HttpResource implements Serializable {


    ////////////
    // FIELDS //
    ////////////

    @Id private ObjectId id;                        // For mongo storage
    protected String originalUrl;                   // e.g. "htp://www.SITE.com?q=foo bar"
    protected String cleanedUrl;                    // e.g. "http://www.site.com/?q=foo%20bar", After full construction, this should ONLY be null if isError() == true
    protected RequestMethod method;                 // e.g. GET, POST
    protected String providingIpPort;               // e.g. "184.73.43.201:80"
    protected String contentType;                   // e.g. "text/html; charset=ISO-8859-1".  Format: "MIME[; ENC]"
    protected String contentEncoding;               // e.g. "gzip", "deflate".  Rarely seen.
    protected int responseCode;                     // e.g. 200, 301, 404. Eventually should be renamed to "statusCode".
    protected String responseMessage;               // e.g. "OK", "Moved Permanently", "Not Found". Eventually should be renamed to "reasonPhrase".
    @Transient protected Map<String, String> responseHeaders;  // Lazily instantiated, depending on HttpRequestOptions, init'ed by addHeader, do we need key in the map AND in the value?
    protected byte[] content;
    protected int contentLength = -1;               // So we can save the length of the content even if it gets discarded.
    protected boolean contentDeclined;              // True if the "consume entity criteria" returned false, thus preventing the consumption of the entity.
    protected long reportedSize;
    @Transient protected transient Throwable exception;  // Not serialized - some exception information serialized via exceptionClassName and exceptionMessage.
    protected ThrowableSnapshot exceptionSnapshot;       // Serialized - the exception converted to strings.
    protected long startDownload;
    protected long endDownload;
    protected String contentMd5Hash;
    @Embedded protected List<HttpRedirect> redirects;   // Lazily instantiated, depending on HttpRequestOptions, init'ed by addRedirect
    protected boolean redirectDisallowed;
    protected SecurityInfo secInfo;                 // This might go away some day if they become shared.
    private boolean ignoredSslProblems;             // true if there was an SSL problem AND the options directed us to ignore it
                                                    // Not in SecurityInfo so it can't be ignored when -SS.  The placement of all
                                                    // fields ultimately needs to be reevaluated according to {artf194123}.
    @Embedded protected RequestInfo reqInfo;        // Lazily instantiated, depending on HttpRequestOptions

    //boolean isAborted  // Future idea


    //////////////////
    // CONSTRUCTORS //
    //////////////////

    public HttpResource(String originalUrl) {
        this.originalUrl = originalUrl;
    }

    // For serialization APIs
    private HttpResource() {}


    //////////////////////////
    // ACCESSORS / MUTATORS //
    //////////////////////////

    // Accessors

    public ObjectId getId() {
        return id;
    }
    public String getOriginalUrl() {
        return originalUrl;
    }
    public String getCleanedUrl() {
        return cleanedUrl;                  // After full construction, this should ONLY be null if isError() == true
    }
    public RequestMethod getMethod() {
        return method;
    }
    public String getProvidingIpPort() {
        return providingIpPort;
    }
    public String getContentType() {
        return contentType;
    }
    public String getContentEncoding() {
        return contentEncoding;
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
    public byte[] getContent() {
        return content;
    }
    public boolean isContentDeclined() {
        return contentDeclined;
    }
    public int getReturnedSize() {
        return contentLength;
    }
    public long getReportedSize() {
        return reportedSize;
    }
    public Throwable getException() {
        return exception;
    }
    public ThrowableSnapshot getExceptionSnapshot() {
        return exceptionSnapshot;
    }
    public long getStartDownload() {
        return startDownload;
    }
    public long getEndDownload() {
        return endDownload;
    }
    public String getContentMd5Hash() {
        return contentMd5Hash;
    }
    public Collection<HttpRedirect> getRedirects() {
        if(redirects == null) {
            return null;
        }
        return Collections.unmodifiableCollection(redirects);
    }
    public boolean isRedirectDisallowed() {
        return redirectDisallowed;
    }
    public SecurityInfo getSecurityInfo() {
        return secInfo;
    }
    public boolean isIgnoredSslProblems() {
        return ignoredSslProblems;
    }
    public RequestInfo getRequestInfo() {
        return reqInfo;
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
                InetAddress ia = InetAddress.getByName(
                    providingIpPort.substring(0, providingIpPort.indexOf(':')));
                return ia.getCanonicalHostName();
            } catch(Exception e) {
                // do nothing
            }
        }
        return null;
    }
    public boolean isError() {
        return exception != null || exceptionSnapshot != null || responseCode >= 400;
    }
    // Use this as a guard against a potentially null content field
    public boolean hasContent() {
        return content != null;
    }
    public boolean hasEverHadContent() {
        return contentLength >= 0;  // If !hasContent && hasEverHadContent Then isContentDiscarded = true
    }
    public boolean isContentDiscarded() {
        return content == null && contentLength >= 0;
    }
    // Given the response header:
    //    Content Type: text/html; charset=utf-8
    // This method returns "utf-8"
    public String getCharacterEncoding() {                // What does the HttpResource say?
        return EncodingUtil.findCharSet(contentType);
    }
    public String getCharacterEncodingWithDefault() {     // What will we ultimately interpret it as?
        String encoding = getCharacterEncoding();
        if(encoding == null) {
            encoding = EncodingUtil.DEFAULT_CHARSET_HTTP_1_1;
        }
        return encoding;
    }
    public long getDownloadDuration() {
        return endDownload - startDownload;
    }

    // Someday, take into consideration the character encoding.
    public String getContentAsString() {
        if(content == null) {
            return null;
        }
        try {
            String encoding = getCharacterEncodingWithDefault();     // If left null, String() would use
            return new String(content, encoding);               // "ISO-8859-1" anyway, but this
        } catch(UnsupportedEncodingException e) {                    // is a little more transparent.
            throw new RuntimeConvertedException(e);
        }
    }
    public String getProvidingUrl() {
        if(redirects == null || redirects.size() == 0) {              // size = 0 should never happen
            return getUrl();
        }
        return redirects.get(redirects.size() - 1).getUrl();
    }
    public HttpRedirect getLastRedirect() {
        if(redirects == null || redirects.size() == 0) {              // size = 0 should never happen
            return null;
        }
        return redirects.get(redirects.size() - 1);
    }

    // Return the MIME type of the resource.  Prefer the reported
    // MIME type over Tika's judgment right now.
    public String getMimeType() {
        String mimeType = getMimeTypeFromContentType();
        if(mimeType == null) {
            return getMimeTypeFromTika();
        }
        return mimeType;
    }
    public String getMimeTypeLower() {   // Special case often used for comparisons
        String mimeType = getMimeType();
        if(mimeType != null) {
            return mimeType.toLowerCase();
        }
        return null;
    }
    public String getMimeTypeFromContentType() {
        if(contentType != null) {
            int semi = contentType.indexOf(';');
            String pre = (semi == -1) ? contentType : contentType.substring(0, semi);
            return pre.trim();  // trim here is super cautious
        }
        return null;
    }
    public String getMimeTypeFromTika() {      // Currently only thing creating hard dependency on Cortext.
        if(content != null) {
            try(ByteArrayInputStream is = new ByteArrayInputStream(content)) {
                return MimeTypesConverter.getMimeType(is);
            } catch(IOException e) {
                // Do nothing; return null
            }
        }
        return null;
    }

    // Mutators (Builder)

    public HttpResource setId(ObjectId id) {
        this.id = id;
        return this;
    }
    public HttpResource setOriginalUrl(String originalUrl) {  // This method exists only due to current
        this.originalUrl = originalUrl;                   // API flaw that might require us to set
        return this;                                      // the orig URL.
    }
    public HttpResource setCleanedUrl(String cleanedUrl) {
        this.cleanedUrl = cleanedUrl;
        return this;
    }
    public HttpResource setMethod(RequestMethod method) {
        this.method = method;
        return this;
    }
    public HttpResource setProvidingIpPort(String providingIpPort) {
        this.providingIpPort = providingIpPort;
        return this;
    }
    public HttpResource setContentType(String contentType) {
        this.contentType = contentType;
        return this;
    }
    public HttpResource setContentEncoding(String contentEncoding) {
        this.contentEncoding = contentEncoding;
        return this;
    }
    public HttpResource setResponseCode(int responseCode) {
        this.responseCode = responseCode;
        return this;
    }
    public HttpResource setResponseMessage(String responseMessage) {
        this.responseMessage = responseMessage;
        return this;
    }
    public HttpResource setResponseHeaders(Map<String, String> responseHeaders) {
        this.responseHeaders = responseHeaders;
        return this;
    }
    public HttpResource addResponseHeader(String key, String value) {
        if(responseHeaders == null) {
            responseHeaders = new TreeMap<>();
        }
        responseHeaders.put(key, value);
        return this;
    }
    public HttpResource setContent(byte[] content) {
        this.content = content;
        if(content != null){
            contentMd5Hash = Md5Util.getMd5(content);
            contentLength = content.length;   // Keep cached so discarding doesn't affect
        } else {
            contentMd5Hash = null;
            contentLength = -1;
        }
        return this;
    }
    public void discardContent() {
        content = null;     // This leaves the contentLength & contentMd5Hash as is
    }
    public HttpResource setContentDeclined(boolean contentDeclined) {
        this.contentDeclined = contentDeclined;
        return this;
    }
    public HttpResource setReportedSize(long reportedSize) {
        this.reportedSize = reportedSize;
        return this;
    }
    public HttpResource setException(Throwable exception) {
        this.exception = exception;
        if(exception == null) {
            exceptionSnapshot = null;
        } else {
            exceptionSnapshot = new ThrowableSnapshot(exception);
        }
        return this;
    }
    public HttpResource setStartDownload(long startDownload) {
        this.startDownload = startDownload;
        return this;
    }
    public HttpResource setEndDownload(long endDownload) {
        this.endDownload = endDownload;
        return this;
    }
    public HttpResource addRedirect(HttpRedirect redirect) {
        if(redirects == null) {
            redirects = new ArrayList<>();
        }
        redirects.add(redirect);
        return this;
    }
    public HttpResource setRedirectDisallowed(boolean redirectDisallowed) {
        this.redirectDisallowed = redirectDisallowed;
        return this;
    }
    public HttpResource setSecurityInfo(SecurityInfo secInfo) {
        this.secInfo = secInfo;
        return this;
    }
    public HttpResource setIgnoredSslProblems(boolean ignoredSslProblems) {
        this.ignoredSslProblems = ignoredSslProblems;
        return this;
    }
    public HttpResource setRequestInfo(RequestInfo reqInfo) {
        this.reqInfo = reqInfo;
        return this;
    }

    // Some day you could imagine a 'clearSecurityInfo' method because
    // maybe you need to initially fetch all that content via 'Http',
    // but then after some evaluation process deems that information
    // unnecessary, you can dispose of it for a memory savings.
    // Same with RequestInfo.


    //////////
    // MISC //
    //////////

    public void discardContentAndDeleteMongoContent() {
        discardContent();
        pm.deleteContent(this);
    }


    //////////////////////////////////
    // MORPHIA CUSTOM SERIALIZATION //
    //////////////////////////////////

    @PreSave
    private void preSave(DBObject obj) {
        if(content != null) {
            obj.removeField("content");    // We always write null for content to Mongo
        }

        if(responseHeaders != null) {
            DBObject headers = new BasicDBObject();
            for(String headerName : responseHeaders.keySet()) {
                String cleanedHeaderName = encodeHeaderName(headerName);
                headers.put(cleanedHeaderName, responseHeaders.get(headerName));
            }
            obj.put("responseHeaders", headers);
        }
    }

    @PostPersist
    private void postPersist() {
        if(content != null) {
            MongoGridFsUtil.saveGridFsFile(
                pm.getDb(), pm.prefix("resource"), cleanedUrl,
                content, contentType, id, null
            );
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

    // Either we load the content here or we create an *entirely separate
    // method* to load later - not related to getContent().
    @PostLoad
    private void postLoad() {
        byte[] content = MongoGridFsUtil.fetchGridFsFileContent(
            pm.getDb(), pm.prefix("resource"), id
        );

        // Do not want to setContent if null because we'd blow away
        // contentLength & contentMd5Hash if they had been set
        if(content != null) {
            setContent(content);
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


    public String toStringShort() {
        int rh = responseHeaders == null || responseHeaders.size() == 0 ? 0 : responseHeaders.size();
        int rd = redirects == null || redirects.size() == 0 ? 0 : redirects.size();
        StringBuffer buffer = new StringBuffer();
        if(hasContent()) {
            buffer.append("<");
            buffer.append(StringUtil.commas(content.length));
            buffer.append(" bytes>");
        } else {
            buffer.append(StringLib.NONE);
            if(contentDeclined) {
                buffer.append(" (was declined to be consumed from web server)");
            }
            if(isContentDiscarded()) {
                buffer.append(" (was discarded)");
            }
        }
        // Show content preview
        if(hasContent() && content.length > 0) {
            String str = getContentAsString();
            str = StringUtil.max(str, 80);
            str = StringUtil.toReadableChars(str);
            buffer.append(" (Preview: ");
            buffer.append(str);
            buffer.append(")");
        }

        String E = isError() ? "Yes" : "No";
        if(exception == null && exceptionSnapshot == null) {
            E += " (No Exception)";
        } else {
            String className = exception != null ? exception.getClass().getName() : exceptionSnapshot.getClassName();
            String message = exception != null ? exception.getMessage() : exceptionSnapshot.getMessage();
            String text = exception != null ? ExceptionUtil.toCompleteString(exception, 4) : exceptionSnapshot.getCompleteText();
            E += " (" + className + ": " + message + ")\n" + text.trim();
        }

        return
            "[" + getUrl() + "]\n  Basic:   " +
            responseCode + " " +
            (responseMessage == null ? StringLib.NONE : responseMessage) + "; " +
            (contentType == null ? StringLib.NONE : contentType) + "; RH: " + rh +
            "; RD: " + rd + "\n  Content: " + buffer.toString() + "\n  Errors:  " + E
        ;
    }

    ////////////////
    // OVERRIDDEN //
    ////////////////

    @Override
    public String toString() {
        return toString(500);
    }

    public String toString(int maxContentShowLen) {
        RStringBuilder buffer = new RStringBuilder(2000);
        appendUrl(buffer);
        appendBasic(buffer);
        appendHeaders(buffer);
        appendRedirects(buffer, maxContentShowLen);
        appendSecurity(buffer);
        appendContent(buffer, maxContentShowLen);
        appendTiming(buffer);
        appendRequest(buffer, 0, reqInfo);
        appendError(buffer);
        return buffer.toString();
    }

    private void appendUrl(RStringBuilder buffer) {
        String hn = getProvidingHostName();

        buffer.appendln("HTTP Resource");
        buffer.appendln("=============");
        buffer.append  ("Original URL:       ");
        buffer.appendln(originalUrl == null ? StringLib.NONE : originalUrl);
        buffer.append  ("Cleaned URL:        ");
        buffer.appendln(cleanedUrl == null ? StringLib.NONE : cleanedUrl);
        buffer.append  ("Method:             ");
        buffer.appendln(method == null ? StringLib.NONE : method);
        buffer.append  ("Providing URL:      ");
        buffer.appendln(getProvidingUrl() == null ? StringLib.NONE : getProvidingUrl());
        buffer.append  ("Providing IP/Port:  ");
        buffer.append  (providingIpPort == null ? StringLib.NONE : providingIpPort);
        if(hn != null) {
            buffer.append(" (");
            buffer.append(hn);
            buffer.append(")");
        }
        buffer.appendln();
    }

    private void appendBasic(RStringBuilder buffer) {
        String ce   = getCharacterEncoding();
        String mtCt = getMimeTypeFromContentType();
        String mtTk = getMimeTypeFromTika();

        buffer.append  ("Content Type:       ");
        buffer.appendln(contentType == null ? StringLib.NONE : contentType);
        buffer.append  ("MIME Type (^ConTp): ");
        buffer.appendln(mtCt == null ? StringLib.NONE : mtCt);
        buffer.append  ("MIME Type (Tika):   ");
        buffer.appendln(mtTk == null ? StringLib.NONE : mtTk);
        buffer.append  ("Character Encoding: ");
        buffer.appendln(ce == null ? StringLib.NONE + " (Default: " + getCharacterEncodingWithDefault() + ")" : ce);
        buffer.append  ("Content Encoding:   ");
        buffer.appendln(contentEncoding == null ? StringLib.NONE : contentEncoding);
        buffer.append  ("Response Code:      ");
        buffer.appendln(responseCode);
        buffer.append  ("Response Message:   ");
        buffer.appendln(responseMessage == null ? StringLib.NONE : responseMessage);
    }

    private void appendHeaders(RStringBuilder buffer) {
        buffer.append("Response Headers:");
        if(responseHeaders == null || responseHeaders.size() == 0) {
            buffer.append  ("   ");
            buffer.appendln(StringLib.NONE);
        } else {
            buffer.append  ("   (Count: ");
            buffer.append  (responseHeaders.size());
            buffer.appendln(")");
            int maxLength = StringUtil.maxLength(responseHeaders.keySet()) + 2;  // +2 for colon and space
            String fmt = "%-" + maxLength + "s%s";
            for(String key : responseHeaders.keySet()) {
                String value = responseHeaders.get(key);
                RStringBuilder keyBuffer = new RStringBuilder(key).append(": "); // Overkill! :)
                buffer.append  ("    ");
                buffer.appendln(String.format(fmt, keyBuffer, value));
            }
        }
    }

    private void appendRedirects(RStringBuilder buffer, int maxContentShowLen) {
        buffer.append("Redirects:");
        if(redirects == null || redirects.size() == 0) {
            buffer.append  ("          ");
            buffer.appendln(StringLib.NONE);
        } else {
            buffer.append  ("          (Count: ");
            buffer.append  (redirects.size());
            buffer.appendln(")");
            int r = 0;
            for(HttpRedirect redirect : redirects) {
                String ce   = redirect.getCharacterEncoding();
                String rhn = redirect.getProvidingHostName();
                buffer.append  ("    #");
                buffer.append  (r);
                buffer.append  (": ");
                buffer.appendln(redirect);
                buffer.append  ("        Source Cleaned URL: ");
                buffer.appendln(redirect.getBaseUrl());
                buffer.append  ("        Dest. Original URL: ");
                buffer.appendln(redirect.getOriginalUrl());
                buffer.append  ("        Dest. Cleaned URL:  ");
                if(redirect.getCleanedUrl() == null) {
                    buffer.append(StringLib.NONE);
                } else {
                    buffer.append(redirect.getCleanedUrl());
                }
                buffer.appendln();
                buffer.append  ("        Providing IP/Port:  ");
                if(redirect.getProvidingIpPort() == null) {
                    buffer.append(StringLib.NONE);
                } else {
                    buffer.append(redirect.getProvidingIpPort());
                }
                if(rhn != null) {
                    buffer.append(" (");
                    buffer.append(rhn);
                    buffer.append(")");
                }
                buffer.appendln();
                buffer.append  ("        Character Encoding: ");
                buffer.appendln(ce == null ? StringLib.NONE + " (Default: " + redirect.getCharacterEncodingWithDefault() + ")" : ce);
                buffer.append  ("        Response Code:      ");
                buffer.appendln(redirect.getResponseCode());
                buffer.append  ("        Response Message:   ");
                buffer.appendln(redirect.getResponseMessage());

                // Redirect Headers
                Map<String, String> rHeaders = redirect.getResponseHeaders();
                buffer.append("        Response Headers:");
                if(rHeaders == null || rHeaders.size() == 0) {
                    buffer.append  ("   ");
                    buffer.appendln(StringLib.NONE);
                } else {
                    buffer.append  ("   (Count: ");
                    buffer.append  (rHeaders.size());
                    buffer.appendln(")");
                    Set<String> keys = rHeaders.keySet();
                    int maxLength = StringUtil.maxLength(keys) + 2;  // +2 for colon and space
                    String fmt = "%-" + maxLength + "s%s";
                    for(String key : keys) {
                        RStringBuilder keyBuffer = new RStringBuilder(key).append(": "); // Overkill! :)
                        buffer.append  ("            ");
                        buffer.appendln(String.format(fmt, keyBuffer, rHeaders.get(key)));
                    }
                }

                byte[] redirContent = redirect.getContent();
                buffer.append("        Response Content:   ");
                if(redirect.hasContent()) {
                    buffer.append("<");
                    buffer.append(StringUtil.commas(redirContent.length));
                    buffer.append(" bytes>");
                } else {
                    buffer.append(StringLib.NONE);
//                    if(redirect.isContentDiscarded()) {    // Not impl yet
//                        buffer.append(" (was discarded)");
//                    }
                }

                // Show content preview
                if(redirect.hasContent() && redirContent.length > 0) {
                    String str = redirect.getContentAsString();
                    str = StringUtil.max(str, maxContentShowLen);
                    str = StringUtil.toReadableChars(str);
                    buffer.append(" (Preview: ");
                    buffer.append(str);
                    buffer.append(")");
                }
                buffer.appendln();

                appendRequest(buffer, 2, redirect.getRequestInfo());

                r++;
            }
        }
        buffer.append  ("Redir Disallowed?   ");
        buffer.appendln(redirectDisallowed ? "Yes" : "No");
    }

    private void appendSecurity(RStringBuilder buffer) {
        buffer.append("Security Info:");
        if(secInfo == null) {
            buffer.append  ("      ");
            buffer.appendln(StringLib.NONE);
            buffer.appendln("    * For https URLs, this will not be populated if the connection to the server was already");
            buffer.appendln("    * open and reused due to another URL being recently fetched from the same host over SSL.");
            buffer.appendln("    * This will be corrected in the future.  This also affects the 'ignoreSslProblems' flag.");
        } else {
            buffer.appendln();
            buffer.append  ("    Host Name:               ");
            buffer.appendln(secInfo.getHostName() == null ? StringLib.NONE : secInfo.getHostName());
            buffer.append  ("    Auth Type:               ");
            buffer.appendln(secInfo.getAuthType() == null ? StringLib.NONE : secInfo.getAuthType());
            buffer.append  ("    Trust Manager Error:     ");
            if(secInfo.getTrustManagerExceptionText() == null) {
                buffer.appendln(StringLib.NONE);
            } else {
                buffer.appendln();
                buffer.appendln(StringUtil.padNewLines(secInfo.getTrustManagerExceptionText(), 8, true).replaceAll("\\s+$", ""));
            }
            buffer.append  ("    Hostname Verifier Error: ");
            if(secInfo.getHostnameVerifierExceptionText() == null) {
                buffer.appendln(StringLib.NONE);
            } else {
                buffer.appendln();
                buffer.appendln(StringUtil.padNewLines(secInfo.getHostnameVerifierExceptionText(), 8, true).replaceAll("\\s+$", ""));
            }
            buffer.append  ("    Certificates:");
            if(secInfo.getNumCerts() == 0) {
                buffer.append  ("            ");
                buffer.appendln(StringLib.NONE);
            } else {
                buffer.append  ("            (Count: ");
                buffer.append  (secInfo.getNumCerts());   // This # can be different than # of certs below.  An effect of {artf194123} & {artf194149}.
                buffer.appendln(")");
                int c = 0;
                for(CertificateInfo cInfo : secInfo.getCerts()) {
                    buffer.append  ("        #");
                    buffer.append  (c);
                    buffer.append  (": ");
                    buffer.appendln(cInfo);
                    buffer.append  ("            Issuer:          ");
                    buffer.appendln(cInfo.getIssuerDn());
                    buffer.append  ("            Subject:         ");
                    buffer.appendln(cInfo.getSubjectDn());
                    buffer.append  ("            SubjectAltNames: ");
                    buffer.appendln(cInfo.getSubjectAltDnsNames().isEmpty() ? StringLib.NONE : ListUtil.toString(cInfo.getSubjectAltDnsNames()));
                    buffer.append  ("            Valid Dates:     ");
                    buffer.append  (DateUtil.toLongString(cInfo.getNotBefore()));
                    buffer.append  (" <to> ");
                    buffer.appendln(DateUtil.toLongString(cInfo.getNotAfter()));
                    c++;
                }
            }
        }
        buffer.append  ("SSL Probs. Ignored: ");
        buffer.appendln(ignoredSslProblems);
    }

    private void appendContent(RStringBuilder buffer, int maxContentShowLen) {
        buffer.append("Content:            ");
        if(hasContent()) {
            buffer.append("<");
            buffer.append(StringUtil.commas(content.length));
            buffer.append(" bytes>");
        } else {
            buffer.append(StringLib.NONE);
            if(contentDeclined) {
                buffer.append(" (was declined to be consumed from web server)");
            }
            if(isContentDiscarded()) {
                buffer.append(" (was discarded)");
            }
        }
        // Show content preview
        if(hasContent() && content.length > 0) {
            String str = getContentAsString();
            str = StringUtil.max(str, maxContentShowLen);
            str = StringUtil.toReadableChars(str);
            buffer.append(" (Preview: ");
            buffer.append(str);
            buffer.append(")");
        }
        buffer.appendln();
        buffer.append  ("Content MD5:        ");
        buffer.appendln(contentMd5Hash == null ? StringLib.NONE : contentMd5Hash);
        buffer.append  ("Returned Size:      ");
        if(getReturnedSize() == -1) {
            buffer.append(StringLib.N_A);
        } else {
            buffer.append(StringUtil.commas(getReturnedSize()));
            buffer.append(" bytes");
        }
        buffer.appendln();
        buffer.append  ("Reported Size:      ");
        if(reportedSize < 0) {
            buffer.append(StringLib.NONE);
        } else {
            buffer.append(StringUtil.commas(reportedSize));
            buffer.append(" bytes");
        }
        buffer.appendln();
    }

    private void appendTiming(RStringBuilder buffer) {
        buffer.append  ("DL Start Time:      ");
        if(startDownload == 0) {
            buffer.appendln(StringLib.N_A);
        } else {
            buffer.append  (startDownload);
            buffer.append  (" (");
            buffer.append  (DateUtil.toLongString(startDownload));
            buffer.appendln(")");
        }
        buffer.append  ("DL End Time:        ");
        if(endDownload == 0) {
            buffer.appendln(StringLib.N_A);
        } else {
            buffer.append  (endDownload);
            buffer.append  (" (");
            buffer.append  (DateUtil.toLongString(endDownload));
            buffer.appendln(")");
        }
        buffer.append  ("DL Duration:        ");
        if(startDownload == 0 || endDownload == 0) {
            buffer.appendln(StringLib.N_A);
        } else {
            buffer.append  (DateUtil.toElapsedString(getDownloadDuration(), ElapsedVerbosity.SHORT, true, true));
            buffer.appendln();
        }
    }

    private void appendRequest(RStringBuilder buffer, int level, RequestInfo info) {
        String tabs = StringUtil.spaces(4 * level);

        buffer.append(tabs);
        buffer.append("Request Info:");
        if(reqInfo == null) {
            buffer.append  ("       ");
            buffer.appendln(StringLib.NONE);
        } else {
            Map<String, String> rHeaders = info.getHeaders();

            buffer.appendln();
            buffer.append  (tabs);
            buffer.append  ("    User Agent:      ");
            buffer.appendln(info.getUserAgent());

            // Request Info Headers
            buffer.append(tabs);
            buffer.append("    Request Headers:");

            if(rHeaders == null || rHeaders.size() == 0) {
                buffer.append  (" ");
                buffer.appendln(StringLib.NONE);
            } else {
                buffer.append  (" (Count: ");
                buffer.append  (rHeaders.size());
                buffer.append  (")");
                buffer.appendln();
                Set<String> keys = rHeaders.keySet();
                int maxLength = StringUtil.maxLength(keys) + 2;  // +2 for colon and space
                String fmt = "%-" + maxLength + "s%s";
                for(String key : keys) {
                    RStringBuilder keyBuffer = new RStringBuilder(key).append(": "); // Overkill! :)
                    buffer.append  (tabs);
                    buffer.append  ("        ");
                    buffer.append  (String.format(fmt, keyBuffer, rHeaders.get(key)));
                    buffer.appendln();
                }
            }

            // Known IPs
            buffer.append(tabs);
            buffer.append("    Known IPs:");
            if(info.getKnownIps() == null) {
                buffer.append  ("       ");
                buffer.appendln(StringLib.NONE);
            } else {
                buffer.append  ("       (Count: ");
                buffer.append  (info.getKnownIps().size());
                buffer.append  (")");
                buffer.appendln();
                for(String ip : info.getKnownIps()) {
                    buffer.append  (tabs);
                    buffer.append  ("        ");
                    buffer.appendln(ip);
                }
            }
        }
    }

    private void appendError(RStringBuilder buffer) {
        buffer.append  ("IsError?            ");
        buffer.appendln(isError() ? "Yes" : "No");
        buffer.append  ("Exception:          ");
        if(exception == null && exceptionSnapshot == null) {
            buffer.append(StringLib.NONE);
        } else {
            String className = exception != null ? exception.getClass().getName() : exceptionSnapshot.getClassName();
            String message = exception != null ? exception.getMessage() : exceptionSnapshot.getMessage();
            String text = exception != null ? ExceptionUtil.toCompleteString(exception, 4) : exceptionSnapshot.getCompleteText();

            buffer.append  (className);
            buffer.append  (": ");
            buffer.append  (message);
            buffer.appendln();
            buffer.append  (text.trim());
        }
    }


    /////////////////
    // PERSISTENCE //
    /////////////////

    private static HttpResourceMongoPersistenceManager pm = new HttpResourceMongoPersistenceManager();
    public static HttpResourceMongoPersistenceManager getPersistenceManager() {
        return pm;
    }


    //////////
    // TEST //
    //////////

    public static void main(String[] args) {
        PluginManager.initialize(
            RepletePlugin.class,
            CortextPlugin.class,
            WebCommsPlugin.class
        );
        MongoConnectionSettings mongoSettings = new MongoConnectionSettings()
            .setDb("test")
//            .setCollectionPrefix("cluster")
        ;
        HttpResource.getPersistenceManager().initialize(mongoSettings);
        HttpResource reso = Http.getInstance().doGet("http://localhost:8080/Lighthouse/ping");
        pm.save(reso);
//        reso.discardContent();

        System.out.println("ID           = " + reso.getId());
        System.out.println("hasContent   = " + reso.hasContent());
        System.out.println("getContent   = " + reso.getContent());
        System.out.println("declined     = " + reso.isContentDeclined());
        System.out.println("discarded    = " + reso.isContentDiscarded());
        System.out.println("returnedSize = " + reso.getReturnedSize());
        System.out.println("reportedSize = " + reso.getReportedSize());
        System.out.println("MD5          = " + reso.getContentMd5Hash());

//        pm.delete(reso);

        HttpResource reso2 = pm.fetch(reso.getId());
        System.out.println(reso2);
    }
}
