package gov.sandia.webcomms.http.internal;

import java.io.IOException;

import org.apache.http.impl.client.DefaultHttpRequestRetryHandler;
import org.apache.http.protocol.HttpContext;

import gov.sandia.webcomms.http.Http;
import replete.text.StringUtil;

public class InternalHttpRequestRetryHandler extends DefaultHttpRequestRetryHandler {


    ////////////
    // FIELDS //
    ////////////

    private Http http;


    //////////////////
    // CONSTRUCTORS //
    //////////////////

    public InternalHttpRequestRetryHandler(Http http) {
        this.http = http;
    }


    ////////////////
    // OVERRIDDEN //
    ////////////////

    @Override
    public boolean retryRequest(IOException exception, int executionCount, HttpContext context) {
        boolean retry = super.retryRequest(exception, executionCount, context);

        RequestTrace trace = http.getRequestTrace();
        trace.debugStep("RETRY ALLOWED: " + StringUtil.yesNo(retry));

        return retry;
    }
}
