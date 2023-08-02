package gov.sandia.webcomms.http;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// This class exists because neither Java nor Apache seems to 
// have such a complete list of status codes in one place
// (and definitely do not have data structures that combine
// the reason phrases with the numeric status code).

// Lists are used instead of maps just due to the historically
// not so perfectly strict (1:1) nature of numeric codes to their
// semantic meanings (and thus textual names).  Some vendors over
// the years have used codes not perfectly in accordance with
// the W3C's recommendations and thus this implementation allows
// for a slightly more flexible idea of what a response code is.

// https://www.w3.org/Protocols/rfc2616/rfc2616-sec10.html
// https://en.wikipedia.org/wiki/List_of_HTTP_status_codes
// https://httpstatuses.com/
// https://www.w3.org/Protocols/rfc2616/rfc2616-sec6.html

public class StandardHttpResponseCodes {


    ////////////
    // FIELDS //
    ////////////

    private static List<HttpResponseCode> allResponseCodes = new ArrayList<>();
    private static Map<Integer, List<HttpResponseCode>> previousLookups = new HashMap<>();

    // 1xx Informational
    public static final HttpResponseCode RC_100 = addCode(100, "Continue");
    public static final HttpResponseCode RC_101 = addCode(101, "Switching Protocols");
    public static final HttpResponseCode RC_102 = addCode(102, "Processing", "WebDAV; RFC 2518");
    public static final HttpResponseCode RC_103 = addCode(103, "Early Hints", "RFC 8297");

    // 2xx Successful
    public static final HttpResponseCode RC_200 = addCode(200, "OK");
    public static final HttpResponseCode RC_201 = addCode(201, "Created");
    public static final HttpResponseCode RC_202 = addCode(202, "Accepted");
    public static final HttpResponseCode RC_203 = addCode(203, "Non-Authoritative Information", "since HTTP/1.1");
    public static final HttpResponseCode RC_204 = addCode(204, "No Content");
    public static final HttpResponseCode RC_205 = addCode(205, "Reset Content");
    public static final HttpResponseCode RC_206 = addCode(206, "Partial Content", "RFC 7233");
    public static final HttpResponseCode RC_207 = addCode(207, "Multi-Status", "WebDAV; RFC 4918");
    public static final HttpResponseCode RC_208 = addCode(208, "Already Reported", "WebDAV; RFC 5842");
    public static final HttpResponseCode RC_226 = addCode(226, "IM Used", "RFC 3229");

    // 3xx Redirection
    public static final HttpResponseCode RC_300 = addCode(300, "Multiple Choices");
    public static final HttpResponseCode RC_301 = addCode(301, "Moved Permanently");
    public static final HttpResponseCode RC_302 = addCode(302, "Found");
    public static final HttpResponseCode RC_303 = addCode(303, "See Other", "since HTTP/1.1");
    public static final HttpResponseCode RC_304 = addCode(304, "Not Modified", "RFC 7232");
    public static final HttpResponseCode RC_305 = addCode(305, "Use Proxy", "since HTTP/1.1");
    public static final HttpResponseCode RC_306 = addCode(306, "Switch Proxy");
    public static final HttpResponseCode RC_307 = addCode(307, "Temporary Redirect", "since HTTP/1.1");
    public static final HttpResponseCode RC_308 = addCode(308, "Permanent Redirect", "RFC 7538");

    // 4xx Client Error
    public static final HttpResponseCode RC_400 = addCode(400, "Bad Request");
    public static final HttpResponseCode RC_401 = addCode(401, "Unauthorized", "RFC 7235");
    public static final HttpResponseCode RC_402 = addCode(402, "Payment Required");
    public static final HttpResponseCode RC_403 = addCode(403, "Forbidden");
    public static final HttpResponseCode RC_404 = addCode(404, "Not Found");
    public static final HttpResponseCode RC_405 = addCode(405, "Method Not Allowed");
    public static final HttpResponseCode RC_406 = addCode(406, "Not Acceptable");
    public static final HttpResponseCode RC_407 = addCode(407, "Proxy Authentication Required", "RFC 7235");
    public static final HttpResponseCode RC_408 = addCode(408, "Request Timeout");
    public static final HttpResponseCode RC_409 = addCode(409, "Conflict");
    public static final HttpResponseCode RC_410 = addCode(410, "Gone");
    public static final HttpResponseCode RC_411 = addCode(411, "Length Required");
    public static final HttpResponseCode RC_412 = addCode(412, "Precondition Failed", "RFC 7232");
    public static final HttpResponseCode RC_413 = addCode(413, "Payload Too Large", "RFC 7231");
    public static final HttpResponseCode RC_414 = addCode(414, "URI Too Long", "RFC 7231; Also: Request-URI Too Long");
    public static final HttpResponseCode RC_415 = addCode(415, "Unsupported Media Type");
    public static final HttpResponseCode RC_416 = addCode(416, "Range Not Satisfiable", "RFC 7233; Also: Requested Range Not Satisfiable");
    public static final HttpResponseCode RC_417 = addCode(417, "Expectation Failed");
    public static final HttpResponseCode RC_418 = addCode(418, "I'm a teapot", "RFC 2324");
    public static final HttpResponseCode RC_421 = addCode(421, "Misdirected Request", "RFC 7540");
    public static final HttpResponseCode RC_422 = addCode(422, "Unprocessable Entity", "WebDAV; RFC 4918");
    public static final HttpResponseCode RC_423 = addCode(423, "Locked", "WebDAV; RFC 4918");
    public static final HttpResponseCode RC_424 = addCode(424, "Failed Dependency", "WebDAV; RFC 4918");
    public static final HttpResponseCode RC_426 = addCode(426, "Upgrade Required");
    public static final HttpResponseCode RC_428 = addCode(428, "Precondition Required", "RFC 6585");
    public static final HttpResponseCode RC_429 = addCode(429, "Too Many Requests", "RFC 6585");
    public static final HttpResponseCode RC_431 = addCode(431, "Request Header Fields Too Large", "RFC 6585");
    public static final HttpResponseCode RC_451 = addCode(451, "Unavailable For Legal Reasons", "RFC 7725");

    // 5xx Server Error
    public static final HttpResponseCode RC_500 = addCode(500, "Internal Server Error");
    public static final HttpResponseCode RC_501 = addCode(501, "Not Implemented");
    public static final HttpResponseCode RC_502 = addCode(502, "Bad Gateway");
    public static final HttpResponseCode RC_503 = addCode(503, "Service Unavailable");
    public static final HttpResponseCode RC_504 = addCode(504, "Gateway Timeout");
    public static final HttpResponseCode RC_505 = addCode(505, "HTTP Version Not Supported");
    public static final HttpResponseCode RC_506 = addCode(506, "Variant Also Negotiates", "RFC 2295");
    public static final HttpResponseCode RC_507 = addCode(507, "Insufficient Storage", "WebDAV; RFC 4918");
    public static final HttpResponseCode RC_508 = addCode(508, "Loop Detected", "WebDAV; RFC 5842");
    public static final HttpResponseCode RC_510 = addCode(510, "Not Extended", "RFC 2774");
    public static final HttpResponseCode RC_511 = addCode(511, "Network Authentication Required", "RFC 6585");


    //////////
    // MISC //
    //////////

    private static HttpResponseCode addCode(int statusCode, String reasonPhrase) {
        return addCode(statusCode, reasonPhrase, null);
    }
    private static HttpResponseCode addCode(int statusCode, String reasonPhrase, String description) {
        HttpResponseCode responseCode = new HttpResponseCode(statusCode, reasonPhrase, description);
        allResponseCodes.add(responseCode);
        return responseCode;
    }


    ////////////
    // LOOKUP //
    ////////////

    public synchronized static List<HttpResponseCode> getAllByCode(int statusCode) {
        List<HttpResponseCode> responseCodes = previousLookups.get(statusCode);
        if(responseCodes != null) {
            return responseCodes;
        }
        responseCodes = new ArrayList<>();
        for(HttpResponseCode responseCode : allResponseCodes) {
            if(responseCode.getStatusCode() == statusCode) {
                responseCodes.add(responseCode);
            }
        }
        previousLookups.put(statusCode, responseCodes);
        return responseCodes;
    }

    public static HttpResponseCode getByCode(int statusCode) {
        List<HttpResponseCode> responseCodes = getAllByCode(statusCode);
        return responseCodes.isEmpty() ? null : responseCodes.get(0);
    }


    //////////
    // TEST //
    //////////

    public static void main(String[] args) {
        HttpResponseCode code = getByCode(-1);
        System.out.println(code);
        HttpResponseCode code2 = getByCode(511);
        System.out.println(code2);
        List<HttpResponseCode> code3 = getAllByCode(201);
        System.out.println(code3);
    }
}
