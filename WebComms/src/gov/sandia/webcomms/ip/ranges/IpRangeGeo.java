package gov.sandia.webcomms.ip.ranges;

import java.io.Serializable;

public abstract class IpRangeGeo implements Serializable {


    ////////////
    // FIELDS //
    ////////////

    protected String country;
    protected String region;      //Also: State, Province, Territory, District
    protected String city;


    //////////////////
    // CONSTRUCTORS //
    //////////////////

    public IpRangeGeo(String country, String region, String city) {
        this.country = country;
        this.region = region;
        this.city = city;
    }


    ///////////////
    // ACCESSORS //
    ///////////////

    public String getCountry() {
        return country;
    }
    public String getRegion() {
        return region;
    }
    public String getCity() {
        return city;
    }
}
