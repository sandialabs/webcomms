package gov.sandia.webcomms;

public class SoftwareVersion extends replete.version.SoftwareVersion {


    ///////////////
    // CONSTANTS //
    ///////////////

    // ** DO NOT EDIT THESE CONSTANTS **

    // The following variables are set automatically by the Ant build
    // script.  If you need to change the version of the software, you
    // can do so by editing the version numbers in the build script.
    // Only if you change MAJOR (sw_version_major), MINOR (sw_version_minor),
    // or SERVICE (sw_version_service) will you need to commit this file
    // to the source code repository after executing the build script.

    public static final String MAJOR   = "1";
    public static final String MINOR   = "0";
    public static final String SERVICE = "0";
    public static final String BUILD   = null;


    ///////////////
    // SINGLETON //
    ///////////////

    private static SoftwareVersion instance = new SoftwareVersion(
        MAJOR, MINOR, SERVICE, BUILD
    );

    public static SoftwareVersion get() {
        return instance;
    }


    /////////////////
    // CONSTRUCTOR //
    /////////////////

    public SoftwareVersion(String major, String minor, String service, String build) {
        super(major, minor, service, build);
    }
}
