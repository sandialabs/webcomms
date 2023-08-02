package gov.sandia.webcomms.http.examples;

import gov.sandia.webcomms.http.Http;
import gov.sandia.webcomms.http.HttpSummaryState;
import gov.sandia.webcomms.http.options.HttpRequestOptions;
import gov.sandia.webcomms.http.rsc.HttpResource;
import gov.sandia.webcomms.http.util.UriCleaner;

public class Examples {

    public static void main(String[] args) {
        demonstrateFetchingAResource();
        demonstrateCleaningAndNormalizingAUrl();
    }

    private static void demonstrateFetchingAResource() {

        Http.getInstance().useSandiaProxy();

        // Optional per-request options
        HttpRequestOptions options = new HttpRequestOptions()
            .setSaveContent(true)                  // Default value
            .setSaveResponseHeaders(true)          // Default value
            .setSaveRedirects(true)                // Default value
            .setSaveRedirectResponseHeaders(true)  // Default value
            .setSaveSecurity(true)                 // Default value
            .setSaveRequest(true)                  // Default value
            .setOverrideUserAgent("MyUserAgent")
            .setReplaceAjaxFragment(true)          // Default value
            .setAllowRedirectCriteriaParams(null)  // Default value
        ;

        // Shows multiple redirects
        String url = "http://entertainmentweekly.com";
        HttpResource resource = Http.getInstance().doGet(url, options);
        System.out.println(resource);

        // Shows security info attached
        // ** Reminder: if your JVM is fetching an HTTPS resource
        // from the SRN via the Sandia proxy, your JVM needs the
        // Sandia root certificate installed in that JVM in your
        // "cacerts" file using the "keytool" utility.  See
        // http://interception.sandia.gov for more information.
        String url2 = "https://www.lostdogrescue.org/";
        HttpResource resource2 = Http.getInstance().doGet(url2);
        System.out.println("\n" + resource2);

        // Shows a 404
        String url3 = "http://cnn.com/no~such~file";
        HttpResource resource3 = Http.getInstance().doGet(url3);
        System.out.println("\n" + resource3);

        // The Http object retains its own metrics.
        HttpSummaryState state = Http.getInstance().createSummaryState();
        System.out.println("\n" +
            "# Requests: " + state.getNumRequests() + "\n" +
            "# Errors: " + state.getNumErrors() + "\n" +
            "Status Codes: " + state.getStatusCodeHistogram() + "\n");
    }

    private static void demonstrateCleaningAndNormalizingAUrl() {

        // Malformed and un-normalized URL
        String url1 = "htp://university.edu:80/%7Ejsmith/my resume.html    ";
        String cleanedUrl1 = UriCleaner.clean(url1);
        System.out.println(url1 + "\n    [=>] " + cleanedUrl1);

        // A relative URL found on a web page.
        String url2 = "page/products.aspx";
        String base2 = "http://www.wigets.com/index.html";
        String cleanedUrl2 = UriCleaner.clean(url2, base2);
        System.out.println(url2 + " [from] " + base2 +
            "\n    [=>] " + cleanedUrl2);

        // An un-normalized non-http/https URL can be allowed (not
        // seen as an error) with the boolean parameter.
        String url3 = "%7Ejsmith/binaries/unsuspicious file.exe";
        String base3 = "ftp://filehost.org/main/";
        String cleanedUrl3 = UriCleaner.clean(url3, base3, false);
        System.out.println(url3 + " [from] " + base3 +
            "\n    [=>] " + cleanedUrl3);

    }

}
