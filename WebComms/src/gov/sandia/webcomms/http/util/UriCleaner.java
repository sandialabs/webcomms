package gov.sandia.webcomms.http.util;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import gov.sandia.webcomms.http.errors.HttpUrlFormatException;
import replete.text.StringUtil;

// TODO: There are limited needs for cleaning non-URL URI's
// like "file://..." (see ImageCategorizer-type stuff).  These
// Don't follow all the URL rules but could still use some
// sort of cleaning possibly?

// An opaque URI is an absolute URI whose scheme-specific part
// does not begin with a slash character ('/'). Opaque URIs are
// not subject to further parsing. Some examples of opaque URIs are:
//
//    mailto:java-net@java.sun.com
//    news:comp.lang.java
//    urn:isbn:096139210x

public class UriCleaner {


    //////////
    // ENUM //
    //////////

    private static enum FirstSegmentGuess {
        HOST,
        PATH,
        UNKNOWN
    }


    ////////////
    // FIELDS //
    ////////////

    private static char HASH = '#';
    private static char BANG = '!';

    private static String[] gtldArray = {
        "com", "net", "org", "edu", "gov", "mil",    // 1984: Original 6 TLDs
        "int",                                       // 1988: Request of NATO
        "arpa",                                      // 2000
        "aero", "biz", "coop", "info",               // 2002 application period
            "museum", "name", "pro", "xxx",
        "asia", "cat", "jobs", "mobi", "tel",        // 2004 application period
            "travel",

        // "us", "uk", "ca" // There are so many of these, it could easily
        // block many of the standard extensions for web pages.  We could easily
        // enable this (besides it being a very long list), but it's impossible
        // to really tell what's a host and what's a web page:
        //     "http://www.yoursite.com/johnny/myhome.info" => "/myhome.info"
        //     "http://myhome.info" => "/myhome.info"
    };
    private static String[] extArray = {
        "asp", "aspx", "axd", "asx", "asmx", "ashx",   // This does not have to be
        "css", "cfm", "swf", "html", "htm", "xhtml",   // an exhaustive list.  Merely
        "jhtml", "jsp", "jspx", "js", "pl", "php",     // trying to nail down some types
        "php3", "php4", "phtml", "py", "rb", "rhtml",  // we know are very common indicators
        "xml", "rss", "svg", "cgi", "pdf", "doc",      // in the name of a file/end to a URL
        "atom", "action"                               // that a given URL segment is not a
    };                                                 // host, but rather a path.
    private static List<String> gtlds = Arrays.asList(gtldArray);
    private static List<String> exts = Arrays.asList(extArray);

    public static final String HTTP = "http";
    public static final String HTTPS = "https";
    public static final String HTTP_CSS = HTTP + "://";
    public static final String HTTPS_CSS = HTTPS + "://";
    public static final int HTTP_PORT = 80;
    public static final int HTTPS_PORT = 443;

    public static final String HOST_PART_TLD_PATTERN = "^[a-zA-Z]{1,}$";
    public static final String HOST_PART_OTHER_PATTERN = "^([a-zA-Z0-9]{1,2}?|[a-zA-Z0-9][a-zA-Z0-9-]+[a-zA-Z0-9])$";
    public static final Map<String, Character> pctEncodingUnreserved = new LinkedHashMap<>();
    public static final Set<String> pctEncodingUpperCase = new HashSet<>();

    static {
        char[] chs = new char[66];
        int i = 0;
        for(char ch = 'A'; ch <= 'Z'; ch++, i++) {
            chs[i] = ch;
        }
        for(char ch = 'a'; ch <= 'z'; ch++, i++) {
            chs[i] = ch;
        }
        for(char ch = '0'; ch <= '9'; ch++, i++) {
            chs[i] = ch;
        }
        chs[62] = '-';
        chs[63] = '.';
        chs[64] = '_';
        chs[65] = '~';
        for(char ch : chs) {
            String pctEncoding = Integer.toHexString(ch).toUpperCase();
            pctEncodingUnreserved.put(pctEncoding, ch);
        }

        // The percent-encodings for those reserved characters that have
        // an alphabetic character in them.
        pctEncodingUpperCase.add("2A");  // *
        pctEncodingUpperCase.add("2B");  // +
        pctEncodingUpperCase.add("2C");  // ,
        pctEncodingUpperCase.add("2F");  // /
        pctEncodingUpperCase.add("3A");  // :
        pctEncodingUpperCase.add("3B");  // ;
        pctEncodingUpperCase.add("3D");  // =
        pctEncodingUpperCase.add("3F");  // ?
        pctEncodingUpperCase.add("40");  // @
        pctEncodingUpperCase.add("5B");  // [
        pctEncodingUpperCase.add("5D");  // ]
    }


    ///////////
    // CLEAN //
    ///////////

    // Attempt to clean an absolute URL (only http & https allowed)
    public static String clean(String url) throws HttpUrlFormatException {
        return clean(url, null, true);
    }
    public static String clean(String url, boolean restrictHttp) throws HttpUrlFormatException {
        return clean(url, null, restrictHttp);
    }
    public static String clean(String url, String baseUrl) throws HttpUrlFormatException {
        return clean(url, baseUrl, true);
    }
    // Attempt to clean a potentially relative URL (choose to restrict protocol)
    public static String clean(String url, String baseUrl, boolean restrictHttp) throws HttpUrlFormatException {
        String cleanedUrl = cleanInner(url, baseUrl, restrictHttp);
        if(cleanedUrl.equals(url)) {
            return url;
        }
        return cleanedUrl;
    }

    // Inner

    private static String cleanInner(String url, String baseUrl, boolean restrictHttp) {

        // -- Step 0 --
        // Cannot handle null string, throw exception
        if(url == null) {
            throw new IllegalArgumentException("Cannot have null URL");
        }

        // -- Step 1 --
        url = cleanWhiteSpaceQuotes(url);

        // -- Step 2 --
        // Rare case where the protocol portion of the string is encoded
        // for some reason.  Perform this separately, ahead of percent-
        // encoding cleaning so that the : and // get properly recognized.
        if(url.startsWith("%")) {
            url = decodeProtocol(url);
        }

        // -- Step 3 --
        // Handle all other percent-encoding sequences.  This will
        // remove sequences for unreserved characters and upper-case
        // the sequences of reserved characters.
        // (see http://en.wikipedia.org/wiki/URL_normalization)
        if(url.indexOf('%') != -1) {
            url = percentEncodingCleaning(url);
        }

        // Attempt to handle what seem to be URLs that leave out the
        // protocol part. When starts with /, there are 5 options here:
        //   1.  "//www.cnn.com/path?query"   <-- seen before
        //   2.  "//path?query"               <-- haven't seen this yet
        //   3.  "/www.cnn.com/path?query"    <-- haven't seen this yet
        //   4.  "/path?query"                <-- very common
        //   4b. "/"                          <-- just represents web app root
        //   5.  Truly malformed

        // -- Step 4 --
        // This method possibly can have no change to the
        // URL if no base URL is provided.  This step fixes
        // a relative path using the base URL as long as it
        // at least starts with a "/".
        if(url.startsWith("/")) {
            url = handleNoProtocolSlash(url, baseUrl);
        }

        // -- Step 5 --
        url = fixProtocol(url);

        // If the URI does not start with either "http://" or "https://"
        // attempt to clean that.  Also, any URL that still begins with
        // a forward slash (/) is uncleanable by this block of code.
        if(!url.startsWith("/") && !url.startsWith(HTTP_CSS) && !url.startsWith(HTTPS_CSS)) {

            // -- Step 6 --
            // Only do the lowering if the URL doesn't already match one
            // of valid headings.
            url = lowerProtocol(url);

            // If the URL is still invalid, attempt to clean.
            if(!url.startsWith(HTTP_CSS) && !url.startsWith(HTTPS_CSS)) {

                // -- Step 7 --
                url = cleanHttpProtocolNoProtocol(url, baseUrl);
            }
        }

        // -- Step 8 --
        url = escapeCharacters(url);

        // Last Resort / Special Cases (things seen in wild)

        // -- Step 9 --
        // Remove accidental malformed URLs where the HTML element
        // syntax (< or >) has invaded the URL href attribute.
        url = stripCaretEndings(url);

        // -- Step 10 --
        // Remove duplicate trailing # (as having a # in the fragment is invalid,
        // and this is one of the few problems with that that we can fix).
        url = randomCleanings(url);

        // -- Step 11 --
        // CANNOT do this as a cleaning step.  We want to use the clean method
        // to also clean malformed redirect URLs as well.  However, most
        // directories without a trailing slash (e.g. http://www.cnn.com/video)
        // get directed to the same URL with the trailing slash
        // (e.g. http://www.cnn.com/video/).  So by cleaning the latter, we would
        // get caught in an infinite loop.  It is better to merely allow the
        // redirect aliasing system to work as normal.
//        urlStr = removeTrailingSlash(urlStr);

        int firstHost = url.startsWith(HTTP_CSS) ? HTTP_CSS.length() : HTTPS_CSS.length();
        while(firstHost < url.length() && url.charAt(firstHost) == '/') {
            url = url.substring(0, firstHost) + url.substring(firstHost + 1);
        }

        // -- Step 12 --
        // Throw exceptions by executing same code that Apache's
        // HTTP client does to validate URL's for a consistent
        // exception handling.
        url = validateAsUriLowerAuthHttpProtRemoveDefltPort(url, restrictHttp);

        // -- Step 13 --
        url = cleanFragment(url);

        return url;
    }

    // -- Step 1 --
    private static String cleanWhiteSpaceQuotes(String urlStr) {

        // Remove all whitespace characters (\n, \r, \t, SPACE, etc.) from ends
        // (this could technically be wrong, but probably not - I wouldn't think
        // allowing white space at ends of string would be part of HTTP protocol).
        urlStr = urlStr.trim();

        // Remove leading and trailing? "%20" from the URL too, as this are spurious
        // "HTML Spaces" that essentially need to be trimmed as well.
        urlStr = urlStr.replaceAll("^(%20)*", "");
        //urlStr = urlStr.replaceAll("(%20)*$", "");    // Unsure about this one, think it gets sent to server
        urlStr = urlStr.trim();                         // Trim again

        // Many URLs on the internet have random carriage returns in the middle of them.
        // Both FF and Chrome will flat-out remove these characters from URLs as well.
        urlStr = urlStr.replaceAll("[\n\r]", "");

        // Remove all quote characters from the front of the URL.  These are
        // almost assuredly spurious.
        urlStr = urlStr.replaceAll("^[\"'`]+", "");

        return urlStr;
    }

    // -- Step 2 --
    private static String decodeProtocol(String urlStr) {

        // TODO: decode(String) is deprecated due to using the system-
        // default encoding.  Check into this some day...

        // Order important for loop used below.
        String[] enc = {
            "%68%74%74%70%73%3A%2F%2F",    // https://
            "%68%74%74%70%73%3A%2F",       // https:/
            "%68%74%74%70%3A%2F%2F",       // http://
            "%68%74%74%70%3A%2F",          // http:/
            "%68%74%74%70%73%3A",          // https:
            "%68%74%74%70%3A",             // http:
            "%68%74%74%70%73",             // https
            "%68%74%74%70",                // http
            "%68%74%70",                   // htp
            "%74%74%70",                   // ttp
            "%2F%2F",                      // //
            "%2F",                         // /
        };

        // Upper-case in case starts with "%2f", etc.  Only used in comparison.
        String urlStrUpper = urlStr.toUpperCase();

        for(String e : enc) {
            if(urlStrUpper.startsWith(e)) {
                return URLDecoder.decode(e /*, encoding*/) + urlStr.substring(e.length());
            }
        }

        return urlStr;
    }

    // -- Step 3 --
    private static String percentEncodingCleaning(String urlStr) {
        int p = urlStr.lastIndexOf('%');
        int prevP = urlStr.length();
        while(p != -1) {
            if(p < prevP - 2) {

                // Extract just the code portion of the percent-
                // encoded sequence.
                String code = urlStr.substring(p + 1, p + 3).toUpperCase();

                // Remove sequences for unreserved characters.
                if(pctEncodingUnreserved.containsKey(code)) {
                    urlStr = urlStr.substring(0, p) +
                        pctEncodingUnreserved.get(code) +
                        urlStr.substring(p + 3, urlStr.length());

                // Ensure code is upper case.
                } else if(pctEncodingUpperCase.contains(code)) {
                    urlStr = urlStr.substring(0, p + 1) +
                        code +
                        urlStr.substring(p + 3, urlStr.length());
                }
            }

            // Save current % position and look for next one.
            prevP = p;
            p = urlStr.lastIndexOf('%', p - 1);
        }

        return urlStr;
    }

    // -- Step 4 --
    // Attempts to take a malformed or relative URL and prepend the
    // appropriate/missing information onto the front of it.  This
    // method can possibly have no effect on the URL if the URL
    // looks like a path and no base URL is provided.
    private static String handleNoProtocolSlash(String urlStr, String baseUrl) {
        URL url = baseUrl == null ? null : getBaseUriAsUrl(baseUrl);
        String protocol = url == null ? HTTP : url.getProtocol();

        // When start with "//" most likely should start with the host.
        if(urlStr.startsWith("//")) {
            FirstSegmentGuess guess = guessFirstSement(urlStr, 2);
            switch(guess) {
                case HOST:
                case UNKNOWN:
                    urlStr = protocol + ":" + urlStr;    // (has two "//" in front already)
                    break;
                case PATH:
                    if(url != null) {
                        String auth = url.getHost();
                        urlStr = protocol + "://" + auth + urlStr.substring(1);  // Remove 1 "/"
                    }
                    break;
            }

        } else {
            FirstSegmentGuess guess = guessFirstSement(urlStr, 1);
            switch(guess) {
                case HOST:
                    urlStr = protocol + ":/" + urlStr;    // (has one "/" in front already)
                    break;
                case PATH:
                case UNKNOWN:
                    if(url != null) {
                        String auth = url.getHost();
                        urlStr = protocol + "://" + auth + urlStr;
                    }
                    break;
            }
        }

        return urlStr;    // Possibly unmodified if base URL not provided
    }

    // -- Step 5 --
    private static String fixProtocol(String urlStr) {

        // I don't know when to draw the line on cleaning
        // these "bad-but-human-recognizable" protocols.
        // Could do others, could use RegEx.
        // TECHNICALLY this process could change already
        // valid URLs.  We are using heuristics to decide
        // what the URL *probably* means.  For example:
        //     http://http/www.dogs4dogs.com/bark.html
        // Is *probably* a typo.  However, it *could*
        // be a URL that points to a web server on a
        // person's own LAN where local DNS resolves the
        // server "http" to an IP address, and the path
        // "/www.dogs4dogs.com/bark.html" is fetched
        // from that server.  This is completely feasible.
        // Most likely, this is not the case and we're
        // relying on that belief.  This process could also
        // by that same logic convert relative paths that
        // start with the below prefixes into invalid
        // URLs.  For example:
        //     <a href="hhttp:hi.com" ...>

        String[] prefixes = {
            "hhttp",
            "hthttp",
            "http:http",
            "https:https",

            "http:// http://",
            "http://http://",
            "http://http:/",
            "http://http//",
            "http://http/",
            "http:/http://",
            "http:/http:/",
            "http:/http//",
            "http:/http/",

            "https:// https://",
            "https://https://",
            "https://https:/",
            "https://https//",
            "https://https/",
            "https:/https://",
            "https:/https:/",
            "https:/https//",
            "https:/https/"
        };

        // Notice we're not requiring a : to follow the prefix
        // because technically that could be broken too, and
        // possibly fixed in cleanProtocol.  E.g.:
        //   "hthttp//www.cnn.com/videos"
        // TODO: There's technically a chance that this strategy can
        // improperly change a URL that is technically a relative
        // path.  Or possibly not if the relative paths have been
        // fixed by this point?
        for(String prefix : prefixes) {
            if(urlStr.toLowerCase().startsWith(prefix)) {
                String newPrefix;
                if(prefix.endsWith("/")) {
                    if(prefix.contains("https")) {
                        newPrefix = HTTPS_CSS;
                    } else {
                        newPrefix = HTTP_CSS;
                    }
                } else {
                    if(prefix.contains("https")) {
                        newPrefix = HTTPS;
                    } else {
                        newPrefix = HTTP;
                    }
                }
                return newPrefix + urlStr.substring(prefix.length());
            }
        }

        return urlStr;
    }

    private static FirstSegmentGuess guessFirstSement(String urlStr, int start) {
        int end = urlStr.indexOf('/', start);
        end = end != -1 ? end : urlStr.indexOf('?', start);
        end = end != -1 ? end : urlStr.length();

        String firstSegment = urlStr.substring(start, end);
        int dot = firstSegment.indexOf('.');
        int lastDot = firstSegment.lastIndexOf('.');

        // If there is no dot, then most likely it is a path segment
        // This case covers these situations:
        //     "/path/to/file.html"
        //     "/path./to/file.html"
        //     "//networkserver/path/to/file.html"    <-- we don't handle network server names
        if(dot == -1 || dot == 0 || lastDot == firstSegment.length() - 1) {
            return FirstSegmentGuess.PATH;
        }

        // There is a dot, now try to find evidence it's the host.
        String[] parts = firstSegment.split("\\.");
        for(String part : parts) {
            if(!part.matches(HOST_PART_OTHER_PATTERN)) {
                return FirstSegmentGuess.PATH;
            }
        }

        String last = parts[parts.length - 1];
        if(gtlds.contains(last)) {
            return FirstSegmentGuess.HOST;
        }

        if(exts.contains(last)) {
            return FirstSegmentGuess.PATH;
        }

        return FirstSegmentGuess.UNKNOWN;
    }

    // -- Step 6 --
    private static String lowerProtocol(String urlStr) {

        // If the URL has a protocol, make sure it is lower case.
        String lowerUrlStr = urlStr.toLowerCase();
        if(lowerUrlStr.startsWith(HTTPS)) {
            urlStr = HTTPS + urlStr.substring(HTTPS.length());
        } else if(lowerUrlStr.startsWith(HTTP)) {
            urlStr = HTTP + urlStr.substring(HTTP.length());
        }

        return urlStr;
    }

    // -- Step 7 --
    // Given an absolute URI that we already know not to begin
    // with either "http://" or "https://" and attempt to correct
    // the problem.  The protocol must already be in lower
    // case.
    private static String cleanHttpProtocolNoProtocol(String urlStr, String baseUrl) {

        String[] par = {
            "https//",
            "https:/",
            "htps://",
            "htps:/",
            "https:",
            "http//",
            "http:/",
            "htp://",
            "htp:/",
            "http:",
        };
        
        // Removed these two options from the list above as they are
        // the most aggressive and questionable cleaning steps to take
        // on a URL and it fails, for example on this relative URL that
        // justifiably begins with "http":
        //   <a href="httpcomponents-client-4.5-src.zip">httpcomponents-client-4.5-src.zip</a>
//            "https",
//            "http"

        final int LAST_HTTPS_IDX = par.length / 2 - 1;
        int i = 0;
        for(String p : par) {
            if(urlStr.startsWith(p)) {
                if(i <= LAST_HTTPS_IDX) {
                    return HTTPS_CSS + urlStr.substring(p.length());
                }
                return HTTP_CSS + urlStr.substring(p.length());
            }
            i++;
        }

        // If no replacement was found for the standard HTTP/S protocols
        // but it still resembles a absolute URL, just with a different
        // protocol, no cleaning is possible at this stage.
        // TODO: This can technically also be heavily improved, as you
        // could say that a relative URL "File:2_84-morn.jpg" is perfectly
        // valid given a base URL.  We could examine the part after the
        // : and notice it's not a valid host, and allow the URL onto
        // the next step.
        // TODO: Also, this pattern needs a little enhancing.  +, . and -
        // cannot go on end of protocol name.
        if(urlStr.matches("^[a-zA-Z][a-zA-Z0-9+.-]*:.*$")) {
            return urlStr;
        }

        FirstSegmentGuess guess = guessFirstSement(urlStr, 0);
        switch(guess) {
            case HOST:
                urlStr = HTTP_CSS + urlStr;   // Turns "www.cnn.com" into "http://www.cnn.com"
                break;

            // Turns "public/index.php" into "http://www.cnn.com/public/index.php"
            case PATH:
            case UNKNOWN:                    // will handle "index.shtml"
                if(baseUrl != null) {
                    URL bUrl = getBaseUriAsUrl(baseUrl);
                    URL combinedUrl = getCombinedUrl(bUrl, urlStr);
                    urlStr = combinedUrl.toExternalForm();  // TODO: toString() ? other?
                }
                break;
        }

        return urlStr;
    }

    // TODO: Switch to all URI someday.  Then can also use simplifyUriExMessage.
    private static URL getBaseUriAsUrl(String baseUri) throws HttpUrlFormatException {
        try {
            return new URL(baseUri);
        } catch(MalformedURLException e) {
            throw new HttpUrlFormatException("Bad base URI: " + baseUri + " ! " + e.getMessage());
        }
    }

    private static URL getCombinedUrl(URL base, String path) throws HttpUrlFormatException {
        try {
            return new URL(base, path);
        } catch(MalformedURLException e) {
            throw new HttpUrlFormatException("Bad combined URI: base=" + base + ", path=" + path + " ! " + e.getMessage());
        }
    }

    // -- Step 8 --   What about | and ^ to help with URI parsing issues?
    private static String escapeCharacters(String urlStr) {
        urlStr = urlStr
            .replace(" ", "%20")
            .replace("{", "%7B")
            .replace("}", "%7D");
        return urlStr;
    }

    // -- Step 9 --
    private static String stripCaretEndings(String urlStr) {
        int lc = urlStr.indexOf('<');
        if(lc != -1) {
            urlStr = urlStr.substring(0, lc);
        }
        int rc = urlStr.indexOf('>');
        if(rc != -1) {
            urlStr = urlStr.substring(0, rc);
        }
        return urlStr;
    }

    // -- Step 10 --
    private static String randomCleanings(String urlStr) {
        if(urlStr.endsWith("#")) {
            urlStr = urlStr.replaceAll("##*$", "#");
        }
        if(urlStr.indexOf("\"") == urlStr.length() - 1) {
            urlStr = urlStr.replaceAll("\"$", "");
        } else {
            urlStr = urlStr.replaceAll("\"", "%22");
        }
        if(urlStr.indexOf("'") == urlStr.length() - 1) {
            urlStr = urlStr.replaceAll("'$", "");
        } else {
            urlStr = urlStr.replaceAll("'", "%27");
        }
        if(urlStr.startsWith("http://%20") || urlStr.startsWith("http:// ")) {
            urlStr = urlStr.replaceAll("^http://(%20| )+", "http://");
        }
        if(urlStr.startsWith("https://%20") || urlStr.startsWith("https:// ")) {
            urlStr = urlStr.replaceAll("^https://(%20| )+", "https://");
        }
        return urlStr;
    }

    // -- Step 11 --
//    private static String removeTrailingSlash(String urlStr) {
//        if(urlStr.endsWith("/")) {
//            urlStr = urlStr.substring(0, urlStr.length() - 1);
//        }
//        return urlStr;
//    }

    // -- Step 12 --
    private static String validateAsUriLowerAuthHttpProtRemoveDefltPort(
            String urlStr, boolean restrictHttp) throws HttpUrlFormatException {

        try {

            // This code is used in the request constructors
            // (e.g. HttpGet(String)) and when parsing a redirect
            // location.
            URI uri = new URI(urlStr);

            if(uri.getScheme() == null) {
                throw new HttpUrlFormatException("No protocol: " + urlStr);
            }

            // If this URI must be of the http or https protocols,
            // perform additional validation on those URI's.
            if(restrictHttp) {
                validateHttpScheme(urlStr, uri, false);
            }

            // If the final, validated URI is of the http or https
            // protocols, perform some final normalization & revalidation.
            if(uri.getScheme().equals(HTTP) || uri.getScheme().equals(HTTPS)) {

                validateHttpHost(urlStr, uri, false);

                String normalized = finalHttpNormalization(urlStr, uri);

                // Revalidate, as the normalization could have damaged
                // the URI.
                uri = new URI(normalized);
                if(uri.getScheme() == null) {
                    throw new HttpUrlFormatException("No protocol [after nmzn]: " + urlStr);
                }
                validateHttpScheme(urlStr, uri, true);
                validateHttpHost(urlStr, uri, true);

                return normalized;
            }

            // Otherwise, simply return as is.
            return urlStr;

        } catch(URISyntaxException e) {
            String msg = simplifyUriExMessage(urlStr, e);
            throw new HttpUrlFormatException("(UriSynEx) " + urlStr + " !! " + msg, msg);

        } catch(HttpUrlFormatException e) {
            throw e;
// temp change?
        } catch(Exception e) {
            throw new HttpUrlFormatException("(Unexpected) " + urlStr + " !! " + e.getMessage());

        }
    }

    // https://blog.httpwatch.com/2011/03/01/6-things-you-should-know-about-fragment-urls/

    // -- Step 13 --
    private static String cleanFragment(String url) {
        int hash = url.indexOf(HASH);
        if(hash == -1) {
            return url;     // No fragment
        }
        if(hash < url.length() - 1 && url.charAt(hash + 1) == BANG) {
            return url;
        }
        return url.substring(0, hash);
    }

    private static String simplifyUriExMessage(String urlStr, URISyntaxException e) {
        String msg = e.getMessage();
        String marker = "Illegal character in ";
        if(msg.startsWith(marker)) {
            int sp = msg.indexOf(' ', marker.length());
            String where = msg.substring(marker.length(), sp);
            if(where.equals("path") || where.equals("query") ||
                    where.equals("fragment") || where.equals("opaque")) {
                int c = msg.indexOf(':');
                int s = msg.lastIndexOf(' ', c);
                int index = Integer.parseInt(msg.substring(s + 1, c));
                String ch = urlStr.substring(index, index + 1);
                char ch2 = ch.charAt(0);
                if(ch2 < 32) {
                    switch(ch2) {
                        case 0:  ch = "NUL"; break;
                        case 8:  ch = "BS";  break;
                        case 9:  ch = "\\t"; break;
                        case 10: ch = "\\n"; break;
                        case 12: ch = "FF";  break;
                        case 13: ch = "\\r"; break;
                        default:
                            ch = "#" + ((int) ch2);
                    }
                }
                msg = "<" + where.substring(0, 1) + "@" + index + ":" + ch + ">";
            }
        }
        return msg;
    }

    private static String finalHttpNormalization(String urlStr, URI uri) {

        try {

        // Find a non-default port
        String port = "";
        if(uri.getPort() != -1) {
            if(uri.getScheme().equals(HTTP)) {
                if(uri.getPort() != HTTP_PORT) {
                    port = ":" + uri.getPort();
                }
            } else if(uri.getScheme().equals(HTTPS)) {
                if(uri.getPort() != HTTPS_PORT) {
                    port = ":" + uri.getPort();
                }
            }
        }

        int a = urlStr.indexOf(uri.getAuthority());
        String suffix = urlStr.substring(a + uri.getAuthority().length());
        String sl = suffix.startsWith("/") ? "" : "/";

        int q = suffix.indexOf('?');
        if(q == -1) {
            q = suffix.length();
        }
        String path = suffix.substring(0, q);
        path = path.replaceAll("//+", "/");

        suffix = path + suffix.substring(q);

        return
            urlStr.substring(0, a) +
            uri.getHost().toLowerCase() + port +     // This also removes the USER/PASSWORD component.
            sl + suffix;

        // REMOVE THIS SOME DAY
        } catch(Exception e) {
            throw new HttpUrlFormatException("FIN;urlStr=" + urlStr + ",uri=" + renderUri(uri), e);
        }
    }

    private static void validateHttpScheme(String originalUrl, URI uri, boolean afterNorm) {
        String  afterStr = afterNorm ? " [after nmzn]" : "";

        // This is the same method that DefaultRequestDirector (HC) uses
        // to determine validity of a URI after a successful parse.
        if(!uri.getScheme().equals(HTTP) && !uri.getScheme().equals(HTTPS)) {
            throw new HttpUrlFormatException("Invalid protocol" + afterStr + ": " + originalUrl);
        }
    }

    private static void validateHttpHost(String urlStr, URI uri, boolean afterNorm) {
        String  afterStr = afterNorm ? " [after nmzn]" : "";

        // If we are restricted to http and https, there must be
        // a valid host.  Only if we are allowing other schemas
        // should we not care about the host field because the
        // "opaque" URI's may not have them, e.g. "mailto:x@y.com".
        // Unfortunately, this still will not allow host names
        // with _ in them.
        if(uri.getHost() == null) {
            throw new HttpUrlFormatException(
                "No host" + afterStr + ": " + urlStr);
        }
    }

    // Future idea on how to get around Java's URI restriction
    // on having _ in the host name.  Right now, any attempt to
    // "normalize" or "parse" the host name, may be in vain since
    // HttpClient merely uses the URI class as well and probably
    // does not have special parsing of the authority field itself.
//    private static String getHost(String urlStr, URI uri) {
//        String host = uri.getHost();
//        if(host == null) {
//            String auth = uri.getAuthority();
//            if(auth != null) {
//                int p = auth.lastIndexOf(':');
//                if(p == -1) {
//                    p = auth.length();
//                }
//                int u = auth.indexOf('@');
//                host = auth.substring(u + 1, p);
//            }
//        }
//        validateHttpHost(urlStr, host);
//        return host.toLowerCase();
//    }


    //////////
    // TEST //
    //////////

    public static void main(String[] args) throws Exception {
//        URI x = new URI("http://cnn.com/z/");
//        URI y = new URI("Category:html_asdf");
//        URI n = x.resolve(y);
//        System.out.println(n);
//        System.out.println(clean("Category:bees_and_honey", "http://nature.loves.us/", false));
//        if(true) {
//            return;
//        }
//        System.out.println(clean("/public/index.php", "http://www.cnn.com/a/b/c/d", true));
//        System.out.println(new URL("mailto:someone@example.com").toString());
//        System.out.println(new URI("mailto:someone@example.com").toString());
//        URL x = new URL("mailto:someone@example.com");
//        URI y = new URI("mailto:someone@example.com");
//        System.out.println(renderUrl(x));
//        System.out.println(renderUri(y));
//        ReflectionUtil.printValues(y);
//        if(true) {
//            return;
//        }
//        File f = new File("C:\\Users");
//        System.out.println(f.toURI().toURL());
//        String g = clean(f.toURI().toString());
//        System.out.println(g);
//        String url = "http://hi.com/tim%20jon es";
//        String enc = URIUtil.encodePath(url);
//        String dec = new URLCodec().decode(url);
//        System.out.println(enc);
//        System.out.println(dec);
//        String s = "http://cnn.com%5d%3B/%7E%r%41%a%3a";
//        System.out.println(s);
//        System.out.println(percentEncodingCleaning(s));
//        System.out.println(decodeProtocol("%68%74%74%70%3a//hi.com"));
//        URI u = new URI("http://www.example.com/%7Euser%20name///");
//        u = u.normalize();
//        System.out.println(u.toString());
//        System.out.println(clean());
        testUrls();
    }

    private static void testUrls() {
        String[] testUrls = {
//            null,
//            "",
//            "   ",
//            "hello!? url!?",
//            "http://www.cnn.com",
//            "http:// eifhwl!@%i309)&o2j",
//            "http://www.rightwingwatch.org/content/religious-right-author-claims-environmentalism-will-destroy-america-\r\nintroduce-fascist-tyrann",
//            "http://dlib.nyu.edu/undercover/search/apachesolr_search/?filters=sm_reporter_dlts_undercover:\"William%20Hard\"",
//            "%68%74%74%70%3A//www.cnn.com",
//            "%68%74%74%70%73%3A%2F%2Fwww.cnn.com",    // https://
//            "%68%74%74%70%73%3A%2Fwww.cnn.com",       // https:/
//            "%68%74%74%70%3A%2F%2Fwww.cnn.com",       // http://
//            "%68%74%74%70%3A%2Fwww.cnn.com",          // http:/
//            "%68%74%74%70%73%3Awww.cnn.com",          // https:
//            "%68%74%74%70%3Awww.cnn.com",             // http:
//            "%68%74%74%70%73www.cnn.com",             // https
//            "%68%74%74%70www.cnn.com",                // http
//            "https//hi.com#3",
//            "https:/hi.com#3",
//            "https:hi.com#3",
//            "httpshi.com#3",
//            "http//hi.com#3",
//            "http:/hi.com#3",
//            "http:hi.com#3",
//            "%68%74%74%70%3Ahi.com#3",
//            "dude",
//            "ftp://www.cnn.com",
//            "/tags",
//            "http:///x.com",
//            "Http://www.cnn.com",
//            "http://~/aslfk",
//            "Http://dtrumbo@www.cnn.com:7",
//            "hthttp://costarica.usembassy.gov/catsdogs.html",
//            "hhttp//cnn.com",
//            "derafde://dd.com",
//            "public/index.php",
//            "http://www.washingtonpost.com/how-can-i-opt-out-of-online-advertising-co\nokies/2011/11/18/gIQABECbiN_story.html"
//            "http://www.chelseagreen.com"
//            "http://www.theguardian.com%20%20"
//            "http://tonytonytony.com/sol3/papers.cfm?abstract_id=2188796##"
//            "http://www.facebook.com\"",
//            "http://%20www.acronym.org.uk/bwc",
//            "https://%20   %20 www.state.gov",
//            "http://attractbats.com/bat-houses-for-sale/\">bat",
//            "http://www.sweetness-light.com/<?php%20",
//            "http://thedailycoin.org/<a%20href="
//            "hTtp:Www.FUE.goOg-le.com.AR/xyz!?asdf",
//            "\"'\"http://cnn.com",
//            "http://wikipedia.org/wiki/Wikipedia:Yll%C3%A4pit%C3%A4j%C3%A4t/", (see reso dir for spec char)
//            "http://cnn.com",
//            "http://cnn.com/",
//            "http://cnn.com/?q=f",
//            "http://cnn.com?q=e",
//            "http://sourceforge.net/p/forge/documentation/Docs Home/",
//            "/",
//            "http://en.wikipedia.org/wiki/File:2-8_Field_Regt.jpg"
//            "htp://///cnn.com/video\"\"",
//            "htp://///cnn.com/video//////aaab///a?a=\"bc\"///bb'",
//            "http://gan.wikipedia.org/wiki/%E7%8B%97"
//            "http://av.wikipedia.org/wiki/%D0%93%D1%8C%D0%BE%D0%B9" // normalize these somehow? with i18n...
//            "  mailto:someone@example.com",
//            " ftp://xxx.asddf.sd.com/asdf",
//            "/index.shtml"
//            "http://http://www.cnn.com",
//            "http:/http://www.cnn2.com"
//            "http://x_y.blogspot.com"      // Still need to do this
//            "http://www.cnn.com%20",       // Still need to do this
//            "http://www.cnn,com",          // Still need to do this
//            "http://www.cnn..com",         // Still need to do this
//            "http://www..cnn.com"          // Still need to do this
//            "http://http//journals.lww.com/annalsofsurgery/Abstract/2011/04000/Mortality,_Mauling,_and_Maiming_by_Vicious_Dogs.23.aspx"
//            "http:// http://www.vetinfo.com/care-tips-mrsa.html",
//            "http://http/www.dogs4dogs.com/JR_Articles/dog-autoimmune-thyroiditis.htm"
//            "javascript:"   // [CLEAN FAILED] {HttpUrlFormatException} (UriSynEx) javascript: !! Expected scheme-specific part at index 11: javascript:
//            "http:www.cnn.com/dir/file|what|what?hi",
//            "http://www.cnn.com/dir/file?what=what^why|when",
//            "http://www.cnn.com/dir/file?what=what#why;when\u001Fhow"
//            "http://cnn.com/abc#asdfsafsadfsladfksjalfk"

//            "/working_with_sandia/procurement/current_suppliers/IT_Transformation_Services/index"
//            "high_consequence_automation/index.html"
            "../registration/__assets/documents/Sand2011_2034P_Managing_Your__iSupplier_Profile.pdf"
        };

        // after long crawl look for:
        // NullPointerException
        // (Unexpected)
        // FIN;
        // {R~}
        // HttpUrlFormatException
        // {L!}
        // No host
        // No exceptions other than the connection ones!

        //    Category:bees_and_honey   @  base= http://nature.loves.us/

        // NOTE: Need to solve the above encoding issue:
        //   http://stackoverflow.com/questions/23399865/java-ant-error-unmappable-character-for-encoding-cp1252
        //   http://stackoverflow.com/questions/4995057/unmappable-character-for-encoding-utf-8-error
        //   http://stackoverflow.com/questions/701882/what-is-ansi-format
        // It appears that Notepad++ detects this file as "ANSI", which apparently is more properly
        // referred to as "Windows-1252", but may not be perfectly the same as "CP1252", which is
        // how Eclipse is currently compiling and interpreting this file.  A team-wide switch to UTF8 for
        // both file encoding and compilation may be in order.  This would be tackled with some future
        // internationalization task.

        // todo: upper case ALL % codes, handle internationalization
//        String baseUri = "http://www.dogchannel.com/";
//        String baseUri = "http://www.sandia.gov/working_with_sandia/procurement/what_sandia_buys/stimulus_spending_arra/index.html";
//        String baseUri = "http://www.sandia.gov/research/robotics/";
        String baseUri = "http://www.sandia.gov/working_with_sandia/procurement/current_suppliers/index.html";

     // Foreign language characters, [ ] among others
     // # symbol in fragment 6,000 over 3 days 300K-800K pages

        // Uncleanable:
        // http://www.historycommons.org/href="http://www.guardian.co.uk/politics/2008/feb/26/iraq.whitehall"
        // This one is clearly a malformed link... two things jumbled together.  I suppose
        // you could search all malformed URL's for href="..." but that is very specific
        // to this particular site's mistake (whoever hosted this link).
        // "http://www.fanzzjerseys.com/nfl-jerseys/denver-broncos/peyton-manning-jersey-c-693_874_2415.html      Peyton"

        for(String testUrl : testUrls) {
//            System.out.println(testUrl);
//            System.out.println(" => " + cleanProtocol(decodeProtocol(testUrl)));
            testUrl(testUrl);
            if(testUrl != null) {
                try {
                    String cl = clean(testUrl, baseUri, false);
                    System.out.println("[CLEAN OK]");
                    if(!cl.equals(testUrl)) {
                        testUrl(cl);
                    } else {
                        System.out.println("[NO CHANGE]");
                    }
                } catch(Exception e) {
                    System.out.println("[CLEAN FAILED] {" + e.getClass().getSimpleName() + "} " + e.getMessage());
                }
            }
            System.out.println(StringUtil.replicateChar('=', 20));
        }
    }

    private static void testUrl(String urlStr) {
        Exception e1 = null;
        Exception e2 = null;
        URL u = null;
        URI v = null;

        try {
            u = new URL(urlStr);
        } catch(Exception e) {
            e1 = e;
        }
        try {
            v = new URI(urlStr);
        } catch(Exception e) {
            e2 = e;
        }

        System.out.println("=====<<" + urlStr + ">>=====");

        if(e1 == null && e2 == null) {
            System.out.println("[CTORs OK] " + urlStr);
            System.out.println("  URL=" + renderUrl(u));
            System.out.println("  URI=" + renderUri(v));

        } else {
            System.out.println("[BAD] " + urlStr);

            if(e1 == null) {
                System.out.println("  URL: OK (" + renderUrl(u) + ")");
            } else {
                String code = code(e1);
                System.out.println("  URL: BAD [" + code + "] " + e1.getMessage());
            }

            if(e2 == null) {
                System.out.println("  URI: OK (" + renderUri(v) + ")");
            } else {
                String code = code(e2);
                System.out.println("  URI: BAD [" + code + "] " + e2.getMessage());
            }
        }
    }

    private static String renderUrl(URL u) {
        return "(" + u + " @ s[" + u.getProtocol() + "] h[" + u.getHost() + "] a[" + u.getAuthority() + "] p[" + u.getPort() + "] pt[" + u.getPath() + "] q[" + u.getQuery() + "] fr[" + u.getRef() + "])";
    }
    private static String renderUri(URI u) {
        return "(" + u + " @ s[" + u.getScheme() + "] h[" + u.getHost() + "] a[" + u.getAuthority() + "] p[" + u.getPort() + "] pt[" + u.getPath() + "] q[" + u.getQuery() + "] fr[" + u.getFragment() + "] ssp[" + u.getSchemeSpecificPart() + "])";
    }

    private static String code(Exception e) {
        if(e instanceof IllegalArgumentException) {
            return "IAE";
        } else if(e instanceof URISyntaxException) {
            return "USE";
        } else if(e instanceof MalformedURLException) {
            return "MUE";
        }
        return e.getClass().getName();
    }

    // Need to check URL and URI and how they deal with these situations:
    //    www.cnn.com             +all i'm sure
    //    http:/www.cnn.com       +chrome
    //    http:www.cnn.com        +chrome
    //    http//www.cnn.com       -chrome
    //    htp://www.cnn.com       -chrome
    //    http:///////www.cnn.com +chrome
    //    http://www.cnn.com///////2014//////07/31/politics/congress-immigration/index.html  +chrome
    //    %68%74%74%70://www.cnn.com       -chrome
    //    %68%74%74%70%3A//www.cnn.com     +chrome
    //    %68%74%74%70%3A%2F%2Fwww.cnn.com -chrome

    // : seems OK after some point: http://en.wikipedia.org/wiki/File:2-8_Field_Regt.jpg

    // From createLocationURI
    //http://stackoverflow.com/questions/10786042/java-url-encoding
    // TODO: Why was there custom code here?  Why the need to encode and decode?
    // The base class just does new URI(location).normalize()
    // TODO: Learn what URIUtil.encodePath and URLCodec.decode do??
//      try {
//          location = URIUtil.encodePath(new URLCodec().decode(location));
//          return new URI(location);
//      } catch(Exception e) {    // DecoderException, URIException, URISyntaxException
//          logger.error("createLocationURI: ", e);
//          return null;
//      }

// Can any of these be cleaned now?
//    ProtocolException
//    - Nearly every single one just has bad characters in the URL: (263 of these)
//       Caused by: org.apache.http.ProtocolException: Invalid redirect URI: http://www.biblegateway.com/passage/?search=Gn 24:16;&version=ESV;
//       Caused by: org.apache.http.ProtocolException: Invalid redirect URI: http://muzipedia.net/artist.php?artist=Taylor Swift
//       Caused by: org.apache.http.ProtocolException: Invalid redirect URI: http://news.cincinnati.com/article/20110920/NEWS010702/109210332/Warshak-s-25-year-sentence-cut-10?odyssey=tab|topnews|text|News
//       Caused by: org.apache.http.ProtocolException: Invalid redirect URI: Hello There World   (<-- seriously this is the URL)
//       Also characters like [, ], <, >, and ((ahat))<U+0080><U+0099>
//    - One seen to be caused by:
//       Caused by: org.apache.http.ProtocolException: Received redirect response HTTP/1.1 301 Moved but no location header

// Internationalization concerns:
//    Caused by: org.apache.http.ProtocolException: Invalid redirect URI: http://www.asdfasfda.com/world/series/abc-of-xyz((ahat))<U+0080><U+0099>s-hotdog
//        at org.apache.http.impl.client.DefaultRedirectStrategy.createLocationURI(DefaultRedirectStrategy.java:189)
//        at org.apache.http.impl.client.DefaultRedirectStrategy.getLocationURI(DefaultRedirectStrategy.java:140)
//        at org.apache.http.impl.client.DefaultRedirectStrategy.getRedirect(DefaultRedirectStrategy.java:209)
//        at org.apache.http.impl.client.DefaultRequestDirector.handleResponse(DefaultRequestDirector.java:1068)
//        at org.apache.http.impl.client.DefaultRequestDirector.execute(DefaultRequestDirector.java:545)
//        at org.apache.http.impl.client.AbstractHttpClient.execute(AbstractHttpClient.java:906)
//        ... 7 more
//    Caused by: java.net.URISyntaxException: Illegal character in path at index 73: http://www.asdfasfda.com/world/series/abc-of-xyz((ahat))<U+0080><U+0099>s-hotdog
//        at java.net.URI$Parser.fail(URI.java:2829)
//        at java.net.URI$Parser.checkChars(URI.java:3002)
//        at java.net.URI$Parser.parseHierarchical(URI.java:3086)
//        at java.net.URI$Parser.parse(URI.java:3034)
//        at java.net.URI.<init>(URI.java:595)
}
