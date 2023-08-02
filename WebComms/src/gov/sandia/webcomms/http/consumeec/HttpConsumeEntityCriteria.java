package gov.sandia.webcomms.http.consumeec;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;

import gov.sandia.webcomms.http.HttpRequestWrapper;
import replete.plugins.StatelessProcess;

public abstract class HttpConsumeEntityCriteria<P extends HttpConsumeEntityCriteriaParams> extends StatelessProcess<P> {


    /////////////////
    // CONSTRUCTOR //
    /////////////////

    public HttpConsumeEntityCriteria(P params) {
        super(params);
    }


    //////////////
    // ABSTRACT //
    //////////////

    public abstract boolean doConsume(
        HttpRequestWrapper requestWrapper, HttpResponse response, HttpEntity entity);
}
