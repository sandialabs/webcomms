package gov.sandia.webcomms.ip.ranges;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

import replete.web.IpUtil;

public class IpV4GeoMap extends IpGeoMap {


    ////////////
    // FIELDS //
    ////////////

    private TreeMap<Integer, TreeMap<Integer, List<IpV4RangeGeo>>> ranges = new TreeMap<>();
    private int rangeCount;

    @Override
    public int getRangeCount() {
        return rangeCount;
    }

    @Override
    public Number[] getLoadedMapCount() {
        int count = 1;
        int leafListCount = 0;
        int leafListSize = 0;
        for(Integer octet0Key : ranges.keySet()) {
            TreeMap<Integer, List<IpV4RangeGeo>> mapLvl1 = ranges.get(octet0Key);     // ranges is like mapLvl0
            if(mapLvl1 != null) {
                count++;
                for(Integer octet1Key : mapLvl1.keySet()) {
                    List<IpV4RangeGeo> rangeList = mapLvl1.get(octet1Key);
                    if(rangeList != null) {
                        count++;
                        leafListCount++;
                        leafListSize += rangeList.size();
                    }
                }
            }
        }
        return new Number[] {count, (double) leafListSize / leafListCount};
    }

    public void add(IpV4RangeGeo r) {
        int octet0 = r.ipLo >>> 24;
        TreeMap<Integer, List<IpV4RangeGeo>> subMap = ranges.get(octet0);
        if(subMap == null) {
            subMap = new TreeMap<>();
            ranges.put(octet0, subMap);
        }

        int octet1 = (r.ipLo >>> 16) & 0xFF;
        List<IpV4RangeGeo> rangeList = subMap.get(octet1);
        if(rangeList == null) {
            rangeList = new ArrayList<>();    // Would be nice to read initial sizes in from a file
            subMap.put(octet1, rangeList);
        }

        rangeList.add(r);

        rangeCount++;
    }

    @Override
    public IpRangeGeo lookup(String ip) {
        int ipInt = IpUtil.ipV4StrToInt(ip);

        int octet0Cur = ipInt >>> 24;
        int octet0Pre = octet0Cur - 1;

        Integer keyCur = octet0Cur;  // Might not exist in the map
        TreeMap<Integer, List<IpV4RangeGeo>> mapLvl1Cur = ranges.get(keyCur);     // ranges is like mapLvl0
        if(mapLvl1Cur != null) {
            IpV4RangeGeo r = checkLvl1(mapLvl1Cur, ipInt);
            if(r != null) {
                return r;
            }
        }

        Integer keyPre = ranges.floorKey(octet0Pre);   // Could be null only near the very front an octet's value space.
        if(keyPre != null && !keyPre.equals(keyCur)) {
            TreeMap<Integer, List<IpV4RangeGeo>> mapLvl1Pre = ranges.get(keyPre);    // Choose last list
            Integer lastKey = mapLvl1Pre.lastKey();
            List<IpV4RangeGeo> lastRangeList = mapLvl1Pre.get(lastKey);
            for(IpV4RangeGeo r : lastRangeList) {
                if(r.contains(ipInt)) {
                    return r;
                }
            }
        }


        return null;
    }

    private IpV4RangeGeo checkLvl1(TreeMap<Integer, List<IpV4RangeGeo>> mapLvl1, int ipInt) {
        int octet1Cur = (ipInt >>> 16) & 0xFF;
        int octet1Pre = octet1Cur - 1;

        Integer keyCur = octet1Cur;  // Might not exist in the map
        List<IpV4RangeGeo> rangeListCur = mapLvl1.get(keyCur);
        if(rangeListCur != null) {
            for(IpV4RangeGeo r : rangeListCur) {
                if(r.contains(ipInt)) {
                    return r;
                }
            }
        }

        Integer keyPre = mapLvl1.floorKey(octet1Pre);   // Could be null only near the very front an octet's value space.
        if(keyPre != null && !keyPre.equals(keyCur)) {
            List<IpV4RangeGeo> rangeListPre = mapLvl1.get(keyPre);
            IpV4RangeGeo r = rangeListPre.get(rangeListPre.size() - 1);
            if(r.contains(ipInt)) {
                return r;
            }
        }

        return null;
    }

    public void printDist() {
        long count = 0;
        long total = 0;
        long countSM = 0;
        long totalSM = 0;
        for(Integer octet0Key : ranges.keySet()) {
            TreeMap<Integer, List<IpV4RangeGeo>> mapLvl1 = ranges.get(octet0Key);     // ranges is like mapLvl0
//            System.out.println(octet0Key + "...");
            totalSM += mapLvl1.size();
            countSM++;
            for(Integer octet1Key : mapLvl1.keySet()) {
                List<IpV4RangeGeo> rangeList = mapLvl1.get(octet1Key);
//                System.out.printf("  %3d = %d%n", octet1Key, rangeList.size());
                total += rangeList.size();
                count++;
            }
        }
        System.out.println("V4:");
        System.out.println("  Lvl 0 Map Size: " + ranges.size());
        System.out.println("  Average Submap Size: " + ((double) totalSM / countSM));
        System.out.println("  Average Range List Length: " + ((double) total / count));
    }

    public void compress() {
        for(Integer octet0Key : ranges.keySet()) {
            TreeMap<Integer, List<IpV4RangeGeo>> mapLvl1 = ranges.get(octet0Key);     // ranges is like mapLvl0
            for(Integer octet1Key : mapLvl1.keySet()) {
                List<IpV4RangeGeo> rangeList = mapLvl1.get(octet1Key);
                ((ArrayList) rangeList).trimToSize();
            }
        }
    }
}
