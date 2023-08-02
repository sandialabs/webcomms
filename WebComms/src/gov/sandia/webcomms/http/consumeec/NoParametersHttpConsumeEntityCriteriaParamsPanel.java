package gov.sandia.webcomms.http.consumeec;

import replete.ui.lay.Lay;

public class NoParametersHttpConsumeEntityCriteriaParamsPanel<P extends HttpConsumeEntityCriteriaParams>
        extends HttpConsumeEntityCriteriaParamsPanel<P> {


    ///////////
    // FIELD //
    ///////////

    public static final String NO_PARAMS = "<html><i>(No Parameters)</i></html>";
    private HttpConsumeEntityCriteriaGenerator<P> generator;


    /////////////////
    // CONSTRUCTOR //
    /////////////////

    public NoParametersHttpConsumeEntityCriteriaParamsPanel(HttpConsumeEntityCriteriaGenerator<P> generator) {
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
