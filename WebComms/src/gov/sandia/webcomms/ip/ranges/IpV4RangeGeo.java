package gov.sandia.webcomms.ip.ranges;

import replete.web.IpUtil;

public class IpV4RangeGeo extends IpRangeGeo {


    ////////////
    // FIELDS //
    ////////////

    protected int ipLo;   // Used by other classes in package
    protected int ipHi;


    //////////////////
    // CONSTRUCTORS //
    //////////////////

    public IpV4RangeGeo(int ipLo, int ipHi, String country, String region, String city) {
        super(country, region, city);
        this.ipLo = ipLo;
        this.ipHi = ipHi;
    }


    //////////
    // MISC //
    //////////

    public boolean contains(int ip) {
        int loCmp = Integer.compareUnsigned(ip, ipLo);
        if(loCmp < 0) {
            return false;
        }

        int hiCmp = Integer.compareUnsigned(ip, ipHi);
        if(hiCmp > 0) {
            return false;
        }

        return true;
    }


    ////////////////
    // OVERRIDDEN //
    ////////////////

    @Override
    public String toString() {
        String lo = IpUtil.ipV4IntToStr(ipLo);
        String hi = IpUtil.ipV4IntToStr(ipHi);
        return "[" + lo + "-" + hi + "] " + country + "/" + region + "/" + city;
    }
}
