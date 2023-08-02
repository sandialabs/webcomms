package gov.sandia.webcomms.http.util;

import replete.equality.EqualsUtil;

public class EncodingUtil {

    // Default charset in HTTP 1.1 if there is no charset provided.
    public static final String DEFAULT_CHARSET_HTTP_1_1 = "ISO-8859-1";
    private static final String tSpecials = "()<>@,;:\\\"/[]?.=";

    // http://www.freeformatter.com/mime-types-list.html
    // http://www.w3.org/Protocols/rfc1341/4_Content-Type.html
    // TODO: This simplistic parsing algorithm still needs a couple
    // of conditions implemented:
    //    "text/html; zzcharset=utf-8; charset=utf-8"   // returns default instead of utf-8
    //    "text/html; prop="charset=utf-8""             // would find charset= inside a quoted string
    public static String findCharSet(String contentType) {
        if(contentType == null) {
            return null;
        }
        String paramKey = "charset=";
        int c = contentType.indexOf(paramKey);
        if(c == -1) {
            return DEFAULT_CHARSET_HTTP_1_1;
        }
        int start = c + paramKey.length();
        if(start == contentType.length()) {
            return DEFAULT_CHARSET_HTTP_1_1;
        }
        if(c != 0) {
            char preCh = contentType.charAt(c - 1);
            if(preCh > 32 && tSpecials.indexOf(preCh) == -1) {
                return DEFAULT_CHARSET_HTTP_1_1;
            }
        }
        int afterLast;
        if(contentType.charAt(start) == '"') {
            start++;
            if(start == contentType.length()) {
                return DEFAULT_CHARSET_HTTP_1_1;
            }
            afterLast = contentType.indexOf('"', start);
            if(afterLast == -1) {
                afterLast = contentType.length();
            }
        } else {
            for(afterLast = start + 1; afterLast < contentType.length(); afterLast++) {
                char ch = contentType.charAt(afterLast);
                if(ch <= 32 || tSpecials.indexOf(ch) != -1) {
                    break;
                }
            }
        }
        String ret = contentType.substring(start, afterLast).trim();
        if(ret.equals("")) {
            ret = DEFAULT_CHARSET_HTTP_1_1;
        }
        return ret;
    }


    //////////
    // TEST //
    //////////

    private static int errors = 0;
    public static void main(String[] args) {
        String D = DEFAULT_CHARSET_HTTP_1_1;
        test(null, null);
        test("", D);
        test("a", D);
        test("text/plain", D);
        test("text/plain; ", D);
        test("text/plain; abc", D);
        test("text/plain; charset", D);
        test("text/plain; charset=", D);
        test("text/plain; charset= ", D);
        test("text/plain; charset =", D);
        test("text/plain; charset=\"", D);
        test("text/plain; charset=\"\"  ", D);
        test("text/plain; charset=\" \"", D);
        test("text/plain; charset=dog", "dog");
        test("text/plain; charset=\"dog\"", "dog");
        test("text/plain; charset= \"dog\"", D);
        test("text/plain; charset=dog cat", "dog");
        test("text/plain; charset=dog-cat", "dog-cat");
        test("text/plain; charset=\"dog<>cat\"", "dog<>cat");
        test("text/plain; charset=dog-2?cat", "dog-2");
        test("text/plain; charset=\"   dog cat", "dog cat");
        test("text/plain; charset=\"   dog cat   \"", "dog cat");
        test("text/plain; charset=\"a;b;c.-ef<?A>Dfcne\", something=else  ", "a;b;c.-ef<?A>Dfcne");
        test("text/plain; charset=a  ;b;c", "a");
        test("text/plain; charsets=dog cat", D);
        test("text/plain; charset=dog\ncat", "dog");
        test("text/plain; charset=\"dog\ncat\"", "dog\ncat");
        test("text/plain; charset=dog_cat", "dog_cat");
        test("text/plain; mcharset=dog_cat", D);
        test("charset=dog0cat", "dog0cat");
        test("mcharset=dog0cat", D);
        test("text/plain; zzcharset=utf-8; charset=utf-8", "utf-8");   // FAILS
        test("text/html; prop=\"charset=utf-99\"", D);                 // FAILS
        if(errors == 0) {
            System.out.println("All Good!");
        }
    }

    private static void test(String type, String expected) {
        String actual = findCharSet(type);
        if(!EqualsUtil.equals(actual, expected)) {
            System.out.println("Error with: " + type);
            System.out.println("   Should be: " + expected);
            errors++;
        }
    }
}
