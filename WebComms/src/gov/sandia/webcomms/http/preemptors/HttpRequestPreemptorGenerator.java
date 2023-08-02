package gov.sandia.webcomms.http.preemptors;

import replete.plugins.ExtensionPoint;
import replete.plugins.ParamsAndPanelUiGenerator;

public abstract class HttpRequestPreemptorGenerator
        <P extends HttpRequestPreemptorParams>
            extends ParamsAndPanelUiGenerator<P> implements ExtensionPoint {


    //////////////
    // ABSTRACT //
    //////////////

    public abstract <M extends HttpRequestPreemptor> M createPreemptor(P params);


    ////////////////
    // OVERRIDDEN //
    ////////////////

    // Generics Nuance: Since this class has placed further restrictions on
    // the generic parameter, these overrides propagate that change to these
    // methods' return type, eliminating need for some casts in client code.
    @Override
    public abstract HttpRequestPreemptorParamsPanel<P> createParamsPanel(Object... args);
    @Override
    public abstract P createParams();
}
