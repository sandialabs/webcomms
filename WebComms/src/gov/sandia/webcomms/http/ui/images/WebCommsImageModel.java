package gov.sandia.webcomms.http.ui.images;

import replete.ui.images.concepts.ImageModel;
import replete.ui.images.concepts.ImageModelConcept;
import replete.ui.images.shared.SharedImage;


public class WebCommsImageModel extends ImageModel {
    public static final ImageModelConcept MIME_TYPE  = conceptShared(SharedImage.FILE_ONES_ZEROS);
    public static final ImageModelConcept BLANK_LOGO = conceptShared(SharedImage.PAGE_BLANK);
    public static final ImageModelConcept TRACES     = conceptLocal("traces.png");
}
