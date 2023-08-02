package gov.sandia.webcomms.plugin;

import gov.sandia.webcomms.SoftwareVersion;
import replete.bc.ClassCompareSoftwareVersionLookup;

public class WebCommsSoftwareVersionLookup extends ClassCompareSoftwareVersionLookup {

    // The SoftwareVersion class is by convention always in a versioned project's
    // top-level source package and thus it serves as a good class to compare
    // other classes' packages to.
    @Override
    protected Class getCompareClass() {
        return SoftwareVersion.class;
    }

    @Override
    protected String getSoftwareVersion() {
        return SoftwareVersion.get().getShortVersionString();
    }

}
