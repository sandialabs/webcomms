package gov.sandia.webcomms.http.consumeec;

import replete.ui.BeanPanel;

public abstract class HttpConsumeEntityCriteriaParamsPanel
        <P extends  HttpConsumeEntityCriteriaParams>
            extends BeanPanel<P> {


    /////////////////
    // CONSTRUCTOR //
    /////////////////

    public HttpConsumeEntityCriteriaParamsPanel() {
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
