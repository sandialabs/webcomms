package gov.sandia.webcomms.http.ui.tld;

import java.util.Map;

import gov.sandia.webcomms.plugin.WebCommsPlugin;
import replete.collections.ArrayUtil;
import replete.plugins.PluginManager;
import replete.plugins.RepletePlugin;
import replete.web.UrlUtil;
import replete.xstream.VersioningMetadataXStream;

public class CountryCode {


    ////////////
    // FIELDS //
    ////////////

    // Static

    private static Map<String, CountryCode> codes;

    // Core

    private String tld;
    private String[] tldAlts;
    private String countryName;
    private String iconRemoteUrl;
    private String iconLocalFileName;


    //////////////////////////
    // ACCESSORS / MUTATORS //
    //////////////////////////

    // Accessors (Static)

    public synchronized static Map<String, CountryCode> getAll() {
        if(codes == null) {
            VersioningMetadataXStream xStream = new VersioningMetadataXStream();
            codes = xStream.fromXMLExt(CountryCode.class.getResourceAsStream("country-codes.xml"));
        }
        return codes;
    }

    // Accessors

    public String getTld() {
        return tld;
    }
    public String[] getTldAlts() {
        return tldAlts;
    }
    public String getCountryName() {
        return countryName;
    }
    public String getIconRemoteUrl() {
        return iconRemoteUrl;
    }
    public String getIconLocalFileName() {
        return iconLocalFileName;
    }

    // Mutators

    public CountryCode setTld(String tld) {
        this.tld = tld;
        return this;
    }
    public CountryCode setTldAlts(String[] tldAlts) {
        this.tldAlts = tldAlts;
        return this;
    }
    public CountryCode setCountryName(String countryName) {
        this.countryName = countryName;
        return this;
    }
    public CountryCode setIconRemoteUrl(String iconRemoteUrl) {
        this.iconRemoteUrl = iconRemoteUrl;
        return this;
    }
    public CountryCode setIconLocalFileName(String iconLocalFileName) {
        this.iconLocalFileName = iconLocalFileName;
        return this;
    }


    //////////
    // MISC //
    //////////

    public static boolean isUsGovt(String tld) {
        return ArrayUtil.contains(new String[] {"gov", "mil"}, tld);
    }
    public static boolean isUsOther(String tld) {
        return ArrayUtil.contains(new String[] {"us", "edu"}, tld);
    }

    public static CountryCode getByUrl(String url) {
        try {
            String hostName = UrlUtil.getHostStringOnly(url);
            return getByHostName(hostName);
        } catch(Exception e) {
            return null;     // Soft fail on all errors, null or URL format
        }
    }
    public static CountryCode getByHostName(String hostName) {
        try {
            int dot = hostName.lastIndexOf('.');
            String tld = hostName.substring(dot + 1);
            return getByCountryCode(tld);
        } catch(Exception e) {
            return null;     // Soft fail on all errors, null or host name format
        }
    }
    public static CountryCode getByCountryCode(String code) {
        try {
            code = code.toLowerCase();
            return getAll().get(code);
        } catch(Exception e) {
            return null;     // Soft fail on all errors, null or otherwise
        }
    }


    ////////////////
    // OVERRIDDEN //
    ////////////////

    @Override
    public String toString() {
        return tld + "|" + countryName + "|" + iconRemoteUrl + "|" + iconLocalFileName;
    }


    //////////
    // TEST //
    //////////

    public static void main(String[] args) {
        PluginManager.initialize(
            RepletePlugin.class,
            WebCommsPlugin.class
        );

        Map<String, CountryCode> codes = CountryCode.getAll();
        for(String tld : codes.keySet()) {
            CountryCode code = codes.get(tld);
            System.out.println(tld + " => " + code);
        }
        System.out.println(codes.size());
    }
}
