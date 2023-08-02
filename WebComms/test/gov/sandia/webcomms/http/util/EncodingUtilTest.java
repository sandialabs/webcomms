package gov.sandia.webcomms.http.util;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import replete.equality.EqualsUtil;

public class EncodingUtilTest {


    //////////
    // TEST //
    //////////

    @Test
    public void all() {
        String D = EncodingUtil.DEFAULT_CHARSET_HTTP_1_1;
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
        testF("text/plain; zzcharset=utf-8; charset=utf-8", "utf-8");   // FAILS
        testF("text/html; prop=\"charset=utf-99\"", D);                 // FAILS
    }

    private void test(String type, String expected) {
        String actual = EncodingUtil.findCharSet(type);
        assertTrue(EqualsUtil.equals(actual, expected));
    }
    private void testF(String type, String expected) {
        String actual = EncodingUtil.findCharSet(type);
        assertFalse(EqualsUtil.equals(actual, expected));
    }
}
