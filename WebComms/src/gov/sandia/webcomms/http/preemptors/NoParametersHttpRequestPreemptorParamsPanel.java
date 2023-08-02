package gov.sandia.webcomms.http.preemptors;

import replete.ui.lay.Lay;

public class NoParametersHttpRequestPreemptorParamsPanel<P extends HttpRequestPreemptorParams>
        extends HttpRequestPreemptorParamsPanel<P> {


    ///////////
    // FIELD //
    ///////////

    public static final String NO_PARAMS = "<html><i>(No Parameters)</i></html>";
    private HttpRequestPreemptorGenerator<P> generator;


    /////////////////
    // CONSTRUCTOR //
    /////////////////

    public NoParametersHttpRequestPreemptorParamsPanel(HttpRequestPreemptorGenerator<P> generator) {
        this.generator = generator;

        Lay.GBLtg(this,
            Lay.lb(NO_PARAMS)
        );
    }


    //////////////////////////
    // ACCESSORS / MUTATORS //
    //////////////////////////

    // Accessors (Computed)

    @Override
    public P get() {
        return generator.createParams();
    }

    // Mutators

    @Override
    public void set(P params) {
        super.set(params);
    }
}
