package gov.sandia.webcomms.http.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.function.Predicate;

import org.junit.Test;

import replete.collections.RArrayList;

public class UriCleanerTest {

    private static final String NO_HTTP_S     = "A. no http nor https";
    private static final String NO_HTTP_S_C   = "B. no http: nor https:";
    private static final String NO_HTTP_S_CS  = "C. no http:/ nor https:/";
    private static final String NO_HTTP_S_CSS = "D. no http:// nor https://";
    private static final String PROT_EXTRA_SL = "E. extra / http[s]:///";
    private static final String PROT_ONLY     = "F. protocol only http:///";
    private static final String INVALID_AUTH  = "G. invalid authority";
    private static final String GOOD          = "Z. GOOD (no checker angry)";

    // [%] -   29
    // [H] -   70
    // [R] -  224
    // [S] - 1705

    @Test
    public void stringPooling() {
        String willChange   = "http://cnn.com";
        String alreadyClean = "http://cnn.com/";

        String cleaned1 = UriCleaner.clean(willChange);
        String cleaned2 = UriCleaner.clean(alreadyClean);

        assertEquals(alreadyClean, cleaned1);
        assertEquals(alreadyClean, cleaned2);

        assertEquals(System.identityHashCode(alreadyClean), System.identityHashCode(cleaned2));
        assertNotEquals(System.identityHashCode(willChange), System.identityHashCode(cleaned1));
    }

    @Test
    public void readFile() throws IOException {
        List<String> mUrls = readMalformedUrlsFile();
        assertEquals(2028, mUrls.size());
        assertEquals("http://nukusoku.net/news/2011/03/14/%e3%80%90%e5%8e%9f%e7%99%ba%e5%95%8f%e9%a1%8c%e3%80%91%e7%87%83%e6%96%99%e6%a3%92%e3%81%ae%e9%9c%b2%e5%87%ba%e7%b6%9a%e3%81%8f%e3%80%80%e7%a6%8f%e5%b3%b6%e7%ac%ac%e",
            mUrls.get(0));
        assertEquals("http://www.theboombox.com/2010/07/15/author-claims-tupacs-murder-was-a-federal-operation/?icid=main|main|dl7|link6|http%3A%2F%2Fwww.theboombox.com%2F2010%2F07%2F15%2Fauthor-claims-tupacs-murder-was-a-federal-operation%2F",
            mUrls.get(mUrls.size() - 1));

        List<String> slashUrls = readMalformedUrlsFile(u -> u.startsWith("/"));
        assertEquals(88, slashUrls.size());
        assertEquals("//", slashUrls.get(0));
        assertEquals("/Personalisation/Login.aspx?BookMarkPage=/Personalisation/save-favourite-pages.aspx?PageTitle=David Weir, Paralympic superhuman - NHS Choices&BackUrl=/Livewell/fitness/Pages/david-weir-paralympics.aspx",
            slashUrls.get(slashUrls.size() - 1));

        List<String> httpsUrls = readMalformedUrlsFile(u -> u.startsWith("https"));
        assertEquals(12, httpsUrls.size());
        assertEquals("https://accounts.google.com/o/oauth2/auth?client_id=270346594798.apps.googleusercontent.com&redirect_uri=http%3A%2F%2Fwww.ultimate-guitar.com%2Fuser%2Fauth%2Flogin%3Fservice%3Dgoogle&scope= https://www.googleapis.com/auth/userinfo.email https://www.googleapis.com/auth/userinfo.profile&response_type=code",
            httpsUrls.get(0));
        assertEquals("https://subscribe.newscientist.com/myaccount/renewal.aspx?cmpid=DMC|NSNS|2012-GLOBAL-renew",
            httpsUrls.get(httpsUrls.size() - 1));
    }

    @Test
    public void passURI() throws IOException {
        List<String> mUrls = readMalformedUrlsFile();
        List<String> uriOk = new ArrayList<>();
        int good = 0;
        int bad = 0;
        for(String mUrl : mUrls) {
            try {
                new URI(mUrl);
                uriOk.add(mUrl);
//                System.out.println("PASS: " + mUrl);
                good++;
            } catch(Exception e) {
                bad++;
            }
        }
        assertEquals(2028, good + bad);
        assertEquals(173, good);
        assertEquals(1855, bad);
        assertEquals("http:/", uriOk.get(0));
        assertEquals("http://factfinder2.census.gov/faces/tableservices/jsf/pages/productview.xhtml?",
            uriOk.get(uriOk.size() - 1));
    }

    @Test
    public void sortIntoCategories() throws IOException {
        List<String> mUrls = readMalformedUrlsFile();
        Map<String, List<String>> cats = categorizeUrls(mUrls, false);
        List<String> L1 = cats.get(NO_HTTP_S);
        assertEquals(100, L1.size());
        List<String> L2 = cats.get(NO_HTTP_S_C);
        assertNull(L2);
        List<String> L3 = cats.get(NO_HTTP_S_CS);
        assertEquals(1, L3.size());
        assertEquals("http:http://1389blog.com/category/1389-blog/authors/jtf/", L3.get(0));
        List<String> L4 = cats.get(NO_HTTP_S_CSS);
        assertEquals(57, L4.size());
        assertEquals("http:/", L4.get(0));
        assertEquals("http:/www.wyastone.co.uk/nrl/pvoce/7808a.html", L4.get(L4.size() - 1));
        List<String> L5 = cats.get(PROT_EXTRA_SL);
        assertEquals(5, L5.size());
        assertEquals("http:///", L5.get(0));
        assertEquals("http:///www.bostonglobe.com/magazine", L5.get(L5.size() - 1));

        int c = 0;
        for(List<String> list : cats.values()) {
            c += list.size();
        }
        assertEquals(2028, c);
//        printCatCounts(cats);
    }

//    private void printCatCounts(Map<String, List<String>> cats) {
//        int n = StringUtil.maxLength(cats.keySet());
//        for(String reason : cats.keySet()) {
//            List<String> list = cats.get(reason);
//            System.out.printf("[%-" + n + "s]: %d\n", reason, list.size());
//        }
//    }

    @Test
    public void httpPrefixUnchanged() {
        String cleanedAbsoluteUrl = UriCleaner.clean(
            "httpcomponents-client-4.3-beta1-src.tar.gz",
            "http://archive.apache.org/dist/httpcomponents/httpclient/source/",
            false
        );
        assertEquals(
            "http://archive.apache.org/dist/httpcomponents/httpclient/source/httpcomponents-client-4.3-beta1-src.tar.gz",
            cleanedAbsoluteUrl
        );
    }


    //////////
    // MISC //
    //////////

    private List<String> readMalformedUrlsFile() throws IOException {
        return readMalformedUrlsFile(null);
    }
    private List<String> readMalformedUrlsFile(Predicate<String> filter) throws IOException {
        List<String> mUrls = new ArrayList<>();
        try(BufferedReader reader = new BufferedReader(
                new InputStreamReader(
                    UriCleanerTest.class.getResourceAsStream("malformed-urls.txt"),
                    "utf-8"))) {
            String line;
            while((line = reader.readLine()) != null) {
                String url;
                if(line.startsWith("[%]") || line.startsWith("[S]")) {
                    url = line.substring(line.indexOf(":") + 1).trim();
                } else if(line.startsWith("[H]") || line.startsWith("[R]")) {
                    url = line.substring(3).trim();
                } else {
                    throw new RuntimeException("unrecognized line type");
                }
                if(filter == null || filter.test(url)) {
                    mUrls.add(url);
                }
            }
        }
        return mUrls;
    }

    private Map<String, List<String>> categorizeUrls(List<String> mUrls, boolean clean) {
        Map<String, List<String>> mUrlCategories = new TreeMap<>();
        for(String mUrl : mUrls) {
            String cleaned = null;
            if(clean) {
                try {
                    cleaned = UriCleaner.clean(mUrl, null, true);
                } catch(Exception e) {}
                if(cleaned == null) {
                    cleaned = mUrl;
                }
            } else {
                cleaned = mUrl;
            }

            boolean foundBad = false;
            for(Function<String, String> checker : checkers) {
                String reason = checker.apply(cleaned);
                if(reason != null) {
                    addToList(mUrlCategories, cleaned, reason);
                    foundBad = true;
                    break;
                }
            }
            if(!foundBad) {
                addToList(mUrlCategories, cleaned, GOOD);
            }
        }

        return mUrlCategories;
    }

//    private void printCats(Map<String, List<String>> mUrlCategories) {
//        for(String reason : mUrlCategories.keySet()) {
//            System.out.println(reason);
//            System.out.println(StringUtil.replicateChar('=', reason.length()));
//            List<String> list = mUrlCategories.get(reason);
//            for(String url : list) {
//                System.out.println("    " + url);
//            }
//        }
//    }

    private void addToList(Map<String, List<String>> badLists, String url, String reason) {
        List<String> bad = badLists.get(reason);
        if(bad == null) {
            bad = new ArrayList<>();
            badLists.put(reason, bad);
        }
        bad.add(url);
    }


    //////////////
    // CHECKERS //
    //////////////

    private List<Function<String, String>> checkers = new RArrayList<Function<String, String>>().add(
        url -> url.startsWith("http")     || url.startsWith("https")     ? null : NO_HTTP_S,
        url -> url.startsWith("http:")    || url.startsWith("https:")    ? null : NO_HTTP_S_C,
        url -> url.startsWith("http:/")   || url.startsWith("https:/")   ? null : NO_HTTP_S_CS,
        url -> url.startsWith("http://")  || url.startsWith("https://")  ? null : NO_HTTP_S_CSS,
        url -> url.startsWith("http:///") || url.startsWith("https:///") ? PROT_EXTRA_SL : null,
        url -> url.matches("^https?:/+$") ? PROT_ONLY : null,
        url -> {
            // TODO: Someday this check could include international
            // characters.
            int start;
            if(url.startsWith("https")) {
                start = UriCleaner.HTTPS_CSS.length();
            } else {
                start = UriCleaner.HTTP_CSS.length();
            }
            int sl = url.indexOf('/', start);
            if(sl == -1) {
                sl = url.indexOf('?', start);
                if(sl == -1) {
                    sl = url.length();
                }
            }
            String firstSegment = url.substring(start, sl);
            int dot = firstSegment.indexOf('.');
            if(dot <= 0) {
        //                    System.out.println("II: " + url);
                return INVALID_AUTH;
            }
            String[] parts = firstSegment.split("\\.");
            for(String part : parts) {
                if(!part.matches(UriCleaner.HOST_PART_TLD_PATTERN)) {
        //                        System.out.println("II: " + url);
                    return INVALID_AUTH;
                }
            }
            return null;
        }
    );
}
