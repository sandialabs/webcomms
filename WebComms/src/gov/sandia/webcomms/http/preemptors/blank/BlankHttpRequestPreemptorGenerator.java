package gov.sandia.webcomms.http.preemptors.blank;

import javax.swing.ImageIcon;

import gov.sandia.webcomms.http.preemptors.HttpRequestPreemptorGenerator;
import gov.sandia.webcomms.http.preemptors.HttpRequestPreemptorParamsPanel;
import gov.sandia.webcomms.http.preemptors.NoParametersHttpRequestPreemptorParamsPanel;
import gov.sandia.webcomms.http.ui.images.WebCommsImageModel;
import replete.text.StringUtil;
import replete.ui.images.concepts.ImageLib;

// This extension extends the functionality defined in the WebComms
// plug-in by the HttpRequestPreemptorGenerator extension point.
public class BlankHttpRequestPreemptorGenerator
        extends HttpRequestPreemptorGenerator<BlankHttpRequestPreemptorParams> {


    ////////////////
    // OVERRIDDEN //
    ////////////////

    @Override
    public String getName() {
        return "Blank (Trivial)";
    }

    @Override
    public String getDescription() {
        return StringUtil.createMissingText("Description");
    }

    @Override
    public ImageIcon getIcon() {
        return ImageLib.get(WebCommsImageModel.BLANK_LOGO);
    }

    @Override
    public Class<?>[] getCoordinatedClasses() {
        return new Class[] {
            BlankHttpRequestPreemptorParams.class,
            BlankHttpRequestPreemptorParamsPanel.class,
            BlankHttpRequestPreemptor.class,
        };
    }

    @Override
    public BlankHttpRequestPreemptorParams createParams() {
        return new BlankHttpRequestPreemptorParams();
    }

    @Override
    public HttpRequestPreemptorParamsPanel createParamsPanel(Object... args) {
        return new NoParametersHttpRequestPreemptorParamsPanel(this);
    }

    @Override
    public BlankHttpRequestPreemptor createPreemptor(BlankHttpRequestPreemptorParams params) {
        return new BlankHttpRequestPreemptor(params);
    }
}
