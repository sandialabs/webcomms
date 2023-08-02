package gov.sandia.webcomms.http.allowrc.nofollow;

import gov.sandia.webcomms.http.allowrc.HttpAllowRedirectCriteria;

public class NoFollowHttpAllowRedirectCriteria
        extends HttpAllowRedirectCriteria<NoFollowHttpAllowRedirectCriteriaParams> {


    /////////////////
    // CONSTRUCTOR //
    /////////////////

    public NoFollowHttpAllowRedirectCriteria(NoFollowHttpAllowRedirectCriteriaParams params) {
        super(params);
    }


    ////////////////
    // OVERRIDDEN //
    ////////////////

    @Override
    public boolean doRedirect(String location, String cleanedLocation) {
        return false;
    }
}
