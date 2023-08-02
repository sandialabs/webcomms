package gov.sandia.webcomms.http.preemptors.blank;

import gov.sandia.webcomms.http.Http;
import gov.sandia.webcomms.http.HttpRequestWrapper;
import gov.sandia.webcomms.http.RequestMethod;
import gov.sandia.webcomms.http.errors.PreemptionStoppedException;
import gov.sandia.webcomms.http.preemptors.HttpRequestPreemptor;
import gov.sandia.webcomms.http.rsc.HttpResource;
import replete.collections.RHashMap;
import replete.threads.ThreadUtil;

public class BlankHttpRequestPreemptor extends HttpRequestPreemptor<BlankHttpRequestPreemptorParams> {


    //////////////////
    // CONSTRUCTORS //
    //////////////////

    public BlankHttpRequestPreemptor(BlankHttpRequestPreemptorParams params) {
        super(params);
    }


    ////////////////
    // OVERRIDDEN //
    ////////////////

    @Override
    public HttpResource premptRequest(HttpRequestWrapper requestWrapper) throws PreemptionStoppedException {
        HttpResource resource = requestWrapper.getResource();

        long now = System.currentTimeMillis();
        byte[] content = new byte[0];

        // No id?, req info, no sec info, no exception, no redirects, no providingIpPort
        resource
            .setContentType("text/html; charset=utf-8")
            .setResponseCode(200)
            .setResponseMessage("OK")
            .setResponseHeaders(new RHashMap<String, String>("A", "B", "C", "D", "E", "F"))
            .setContent(content)
            .setReportedSize(content.length)
            .setStartDownload(now)
            .setEndDownload(now)
        ;

        // Sleep to ensure that it's not possible to accidentally
        // allow for unexpected and insanely fast crawls.  Shouldn't
        // happen if all URLs handled by this class are from the
        // same domain and there is a reasonable domain delay,
        // but don't want to take the chance in case something
        // is not configured correctly and this class is used.
        ThreadUtil.sleep(2000);

        return resource;
    }


    //////////
    // TEST //
    //////////

    public static void main(String[] args) {
        HttpRequestWrapper requestWrapper =
            Http.getInstance().create(RequestMethod.GET, "http://test.com");
        BlankHttpRequestPreemptorParams params = new BlankHttpRequestPreemptorParams();
        BlankHttpRequestPreemptor p = new BlankHttpRequestPreemptor(params);
        p.premptRequest(requestWrapper);
    }
}
