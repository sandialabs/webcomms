package gov.sandia.webcomms.http.allowrc;

import replete.plugins.StatelessProcess;

public abstract class HttpAllowRedirectCriteria<P extends HttpAllowRedirectCriteriaParams> extends StatelessProcess<P> {


    /////////////////
    // CONSTRUCTOR //
    /////////////////

    public HttpAllowRedirectCriteria(P params) {
        super(params);
    }


    //////////////
    // ABSTRACT //
    //////////////

    public abstract boolean doRedirect(String location, String cleanedLocation);
}
