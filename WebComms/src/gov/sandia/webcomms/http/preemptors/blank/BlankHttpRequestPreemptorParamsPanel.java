package gov.sandia.webcomms.http.preemptors.blank;

import gov.sandia.webcomms.http.preemptors.HttpRequestPreemptorParamsPanel;

public class BlankHttpRequestPreemptorParamsPanel
        extends HttpRequestPreemptorParamsPanel<BlankHttpRequestPreemptorParams> {


    /////////////////
    // CONSTRUCTOR //
    /////////////////

    public BlankHttpRequestPreemptorParamsPanel() {
        super();
    }


    ////////////////
    // OVERRIDDEN //
    ////////////////

    @Override
    public BlankHttpRequestPreemptorParams get() {
        return new BlankHttpRequestPreemptorParams()
        ;
    }
    @Override
    public void set(BlankHttpRequestPreemptorParams params) {
    }
}
