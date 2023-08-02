package gov.sandia.webcomms.http.allowrc;

import replete.ui.lay.Lay;

public class NoParametersHttpAllowRedirectCriteriaParamsPanel<P extends HttpAllowRedirectCriteriaParams>
        extends HttpAllowRedirectCriteriaParamsPanel<P> {


    ///////////
    // FIELD //
    ///////////

    public static final String NO_PARAMS = "<html><i>(No Parameters)</i></html>";
    private HttpAllowRedirectCriteriaGenerator<P> generator;


    /////////////////
    // CONSTRUCTOR //
    /////////////////

    public NoParametersHttpAllowRedirectCriteriaParamsPanel(HttpAllowRedirectCriteriaGenerator<P> generator) {
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
