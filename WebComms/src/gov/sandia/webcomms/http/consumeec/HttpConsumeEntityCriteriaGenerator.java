package gov.sandia.webcomms.http.consumeec;

import replete.plugins.ExtensionPoint;
import replete.plugins.ParamsAndPanelUiGenerator;

public abstract class HttpConsumeEntityCriteriaGenerator
        <P extends HttpConsumeEntityCriteriaParams>
            extends ParamsAndPanelUiGenerator<P> implements ExtensionPoint {


    //////////////
    // ABSTRACT //
    //////////////

    public abstract <M extends HttpConsumeEntityCriteria> M createCriteria(P params);


    ////////////////
    // OVERRIDDEN //
    ////////////////

    // Generics Nuance: Since this class has placed further restrictions on
    // the generic parameter, these overrides propagate that change to these
    // methods' return type, eliminating need for some casts in client code.
    @Override
    public abstract HttpConsumeEntityCriteriaParamsPanel<P> createParamsPanel(Object... args);
    @Override
    public abstract P createParams();
}
