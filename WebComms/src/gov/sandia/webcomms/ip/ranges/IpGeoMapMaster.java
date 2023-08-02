package gov.sandia.webcomms.ip.ranges;

import java.io.Serializable;
import java.util.Map;
import java.util.TreeMap;

import gov.sandia.webcomms.http.ui.tld.CountryCode;
import replete.web.IpUtil;

public class IpGeoMapMaster implements Serializable {


    ////////////
    // FIELDS //
    ////////////

    public IpV4GeoMap ipV4Map = new IpV4GeoMap();
    public IpV6GeoMap ipV6Map = new IpV6GeoMap();
    private Map<String, Map<String, Map<String, Integer>>> countries = new TreeMap<>();  // Not used beyond debugging yet


    ////////////
    // LOOKUP //
    ////////////

    public IpRangeGeo lookup(String ip) {
        if(ip.contains(":")) {
            return ipV6Map.lookup(ip);
        }
        return ipV4Map.lookup(ip);
    }


    /////////
    // ADD //
    /////////

    public boolean add(String ipLo, String ipHi, String country, String region, String city) {
        addRegion(country, region, city);

        if(ipLo.contains(".")) {
            IpV4RangeGeo r = new IpV4RangeGeo(
                IpUtil.ipV4StrToInt(ipLo),
                IpUtil.ipV4StrToInt(ipHi),
                country,
                region,
                city
            );
            ipV4Map.add(r);

        } else if(ipLo.contains(":")) {
            long[] ipLoLongs = IpUtil.ipV6StrToLongs(ipLo);
            long[] ipHiLongs = IpUtil.ipV6StrToLongs(ipHi);
            IpV6RangeGeo r = new IpV6RangeGeo(
                ipLoLongs[0],
                ipLoLongs[1],
                ipHiLongs[0],
                ipHiLongs[1],
                country,
                region,
                city
            );
            ipV6Map.add(r);

        } else {
            return false;
        }

        return true;
    }

    private void addRegion(String country, String region, String city) {
        Map<String, Map<String, Integer>> regions = countries.get(country);
        if(regions == null) {
            regions = new TreeMap<>();
            countries.put(country, regions);
        }
        Map<String, Integer> cities = regions.get(region);
        if(cities == null) {
            cities = new TreeMap<>();
            regions.put(region, cities);
        }
        Integer blockCount = cities.get(city);
        if(blockCount == null) {
            blockCount = 0;
        }
        cities.put(city, blockCount + 1);
    }


    //////////
    // MISC //
    //////////

    public void printDist() {
        ipV4Map.printDist();
        ipV6Map.printDist();
        printCountries();
    }

    public void compress() {
        ipV4Map.compress();
        ipV6Map.compress();
    }

    public void printCountries() {
        for(String country : countries.keySet()) {
            CountryCode cc = CountryCode.getByCountryCode(country);
            System.out.println(country + " => " + cc);
            Map<String, Map<String, Integer>> regions = countries.get(country);
            for(String region : regions.keySet()) {
                Map<String, Integer> cities = regions.get(region);
                for(String city : cities.keySet()) {
                    Integer blockCount = cities.get(city);
                    System.out.println(country + "/" + region + "/" + city + " => " + blockCount);
                }
            }
        }
    }
}
