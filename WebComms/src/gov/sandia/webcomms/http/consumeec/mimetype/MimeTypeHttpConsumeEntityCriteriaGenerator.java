package gov.sandia.webcomms.http.consumeec.mimetype;

import javax.swing.ImageIcon;

import gov.sandia.webcomms.http.consumeec.HttpConsumeEntityCriteriaGenerator;
import gov.sandia.webcomms.http.ui.images.WebCommsImageModel;
import replete.text.StringUtil;
import replete.ui.images.concepts.ImageLib;

// This extension extends the functionality defined in the WebComms
// plug-in by the HttpRequestPreemptorGenerator extension point.
public class MimeTypeHttpConsumeEntityCriteriaGenerator
        extends HttpConsumeEntityCriteriaGenerator<MimeTypeHttpConsumeEntityCriteriaParams> {


    ////////////////
    // OVERRIDDEN //
    ////////////////

    @Override
    public String getName() {
        return "Specific MIME Types";
    }

    @Override
    public String getDescription() {
        return StringUtil.createMissingText("Description");
    }

    @Override
    public ImageIcon getIcon() {
        return ImageLib.get(WebCommsImageModel.MIME_TYPE);
    }

    @Override
    public Class<?>[] getCoordinatedClasses() {
        return new Class[] {
            MimeTypeHttpConsumeEntityCriteriaParams.class,
            MimeTypeHttpConsumeEntityCriteriaParamsPanel.class,
            MimeTypeHttpConsumeEntityCriteria.class,
        };
    }

    @Override
    public MimeTypeHttpConsumeEntityCriteriaParams createParams() {
        return new MimeTypeHttpConsumeEntityCriteriaParams();
    }

    @Override
    public MimeTypeHttpConsumeEntityCriteriaParamsPanel createParamsPanel(Object... args) {
        return new MimeTypeHttpConsumeEntityCriteriaParamsPanel();
    }

    @Override
    public MimeTypeHttpConsumeEntityCriteria createCriteria(
            MimeTypeHttpConsumeEntityCriteriaParams params) {
        return new MimeTypeHttpConsumeEntityCriteria(params);
    }
}
