package gov.sandia.webcomms.http.allowrc;

import replete.ui.BeanPanel;

public abstract class HttpAllowRedirectCriteriaParamsPanel
        <P extends  HttpAllowRedirectCriteriaParams>
            extends BeanPanel<P> {


    /////////////////
    // CONSTRUCTOR //
    /////////////////

    public HttpAllowRedirectCriteriaParamsPanel() {
        super();
    }


    ////////////////
    // OVERRIDDEN //
    ////////////////

    // Generics Nuance: Since this class has placed further restrictions on
    // the generic parameter, these overrides propagate that change to these
    // methods' return type, eliminating need for some casts in client code.
    @Override
    public P get() {
        return super.get();
    }
}
