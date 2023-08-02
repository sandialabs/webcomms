package gov.sandia.webcomms.ip.ranges;

import replete.web.IpUtil;

public class IpV6RangeGeo extends IpRangeGeo {


    ////////////
    // FIELDS //
    ////////////

    protected long ipLoUp;
    protected long ipLoLo;
    protected long ipHiUp;
    protected long ipHiLo;


    //////////////////
    // CONSTRUCTORS //
    //////////////////

    public IpV6RangeGeo(long ipLoUp, long ipLoLo, long ipHiUp, long ipHiLo, String country, String region, String city) {
        super(country, region, city);
        this.ipLoUp = ipLoUp;
        this.ipLoLo = ipLoLo;
        this.ipHiUp = ipHiUp;
        this.ipHiLo = ipHiLo;
    }


    //////////
    // MISC //
    //////////

    public boolean contains(long[] ip) {
        int loUpCmp = Long.compareUnsigned(ip[0], ipLoUp);
        if(loUpCmp < 0) {
            return false;
        } else if(loUpCmp == 0) {
            int loLoCmp = Long.compareUnsigned(ip[1], ipLoLo);
            if(loLoCmp < 0) {
                return false;
            }
        }

        int hiUpCmp = Long.compareUnsigned(ip[0], ipHiUp);
        if(hiUpCmp > 0) {
            return false;
        } else if(hiUpCmp == 0) {
            int hiLoCmp = Long.compareUnsigned(ip[1], ipHiLo);
            if(hiLoCmp > 0) {
                return false;
            }
        }

        return true;
    }


    ////////////////
    // OVERRIDDEN //
    ////////////////

    @Override
    public String toString() {
        String lo = IpUtil.ipV6LongsToStr(new long[] {ipLoUp, ipLoLo});
        String hi = IpUtil.ipV6LongsToStr(new long[] {ipHiUp, ipHiLo});
        return "[" + lo + "-" + hi + "] " + country + "/" + region + "/" + city;
    }
}
