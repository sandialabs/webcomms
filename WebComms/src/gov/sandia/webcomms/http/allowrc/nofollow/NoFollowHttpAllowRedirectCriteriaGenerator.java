package gov.sandia.webcomms.http.allowrc.nofollow;

import javax.swing.ImageIcon;

import gov.sandia.webcomms.http.allowrc.HttpAllowRedirectCriteriaGenerator;
import gov.sandia.webcomms.http.allowrc.HttpAllowRedirectCriteriaParamsPanel;
import gov.sandia.webcomms.http.allowrc.NoParametersHttpAllowRedirectCriteriaParamsPanel;
import replete.text.StringUtil;
import replete.ui.images.RepleteImageModel;
import replete.ui.images.concepts.ImageLib;

// This extension extends the functionality defined in the WebComms
// plug-in by the HttpRequestPreemptorGenerator extension point.
public class NoFollowHttpAllowRedirectCriteriaGenerator
        extends HttpAllowRedirectCriteriaGenerator<NoFollowHttpAllowRedirectCriteriaParams> {


    ////////////////
    // OVERRIDDEN //
    ////////////////

    @Override
    public String getName() {
        return "No Follow";
    }

    @Override
    public String getDescription() {
        return StringUtil.createMissingText("Description");
    }

    @Override
    public ImageIcon getIcon() {
        return ImageLib.get(RepleteImageModel.NOFIRE);
    }

    @Override
    public Class<?>[] getCoordinatedClasses() {
        return new Class[] {
            NoFollowHttpAllowRedirectCriteriaParams.class,           // Empty
            //NoFollowHttpAllowRedirectCriteriaParamsPanel.class,    // Not needed yet
            NoFollowHttpAllowRedirectCriteria.class,
        };
    }

    @Override
    public NoFollowHttpAllowRedirectCriteriaParams createParams() {
        return new NoFollowHttpAllowRedirectCriteriaParams();
    }

    @Override
    public HttpAllowRedirectCriteriaParamsPanel createParamsPanel(Object... args) {
        return new NoParametersHttpAllowRedirectCriteriaParamsPanel(this);
    }

    @Override
    public NoFollowHttpAllowRedirectCriteria createCriteria(
            NoFollowHttpAllowRedirectCriteriaParams params) {
        return new NoFollowHttpAllowRedirectCriteria(params);
    }
}
