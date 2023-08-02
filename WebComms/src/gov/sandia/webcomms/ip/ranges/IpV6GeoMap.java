package gov.sandia.webcomms.ip.ranges;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

import replete.web.IpUtil;

public class IpV6GeoMap extends IpGeoMap {


    ////////////
    // FIELDS //
    ////////////

    private TreeMap<Long, TreeMap<Long, List<IpV6RangeGeo>>> ranges = new TreeMap<>();
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
        for(Long octet0Key : ranges.keySet()) {
            TreeMap<Long, List<IpV6RangeGeo>> mapLvl1 = ranges.get(octet0Key);     // ranges is like mapLvl0
            if(mapLvl1 != null) {
                count++;
                for(Long octet1Key : mapLvl1.keySet()) {
                    List<IpV6RangeGeo> rangeList = mapLvl1.get(octet1Key);
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

    public void add(IpV6RangeGeo r) {
        long octet0 = r.ipLoUp >>> 32;
        TreeMap<Long, List<IpV6RangeGeo>> subMap = ranges.get(octet0);
        if(subMap == null) {
            subMap = new TreeMap<>();
            ranges.put(octet0, subMap);
        }

        long octet1 = r.ipLoUp & 0xFFFFFFFFL;
        List<IpV6RangeGeo> rangeList = subMap.get(octet1);
        if(rangeList == null) {
            rangeList = new ArrayList<>();    // Would be nice to read initial sizes in from a file
            subMap.put(octet1, rangeList);
        }

        rangeList.add(r);

        rangeCount++;
    }

    @Override
    public IpRangeGeo lookup(String ip) {
        long[] ipLongs = IpUtil.ipV6StrToLongs(ip);

        long octet0Cur = ipLongs[0] >>> 32;
        long octet0Pre = octet0Cur - 1;

        Long keyCur = octet0Cur;  // Might not exist in the map
        TreeMap<Long, List<IpV6RangeGeo>> mapLvl1Cur = ranges.get(keyCur);     // ranges is like mapLvl0
        if(mapLvl1Cur != null) {
            IpV6RangeGeo r = checkLvl1(mapLvl1Cur, ipLongs);
            if(r != null) {
                return r;
            }
        }

        Long keyPre = ranges.floorKey(octet0Pre);   // Could be null only near the very front an octet's value space.
        if(keyPre != null && !keyPre.equals(keyCur)) {
            TreeMap<Long, List<IpV6RangeGeo>> mapLvl1Pre = ranges.get(keyPre);    // Choose last list
            Long lastKey = mapLvl1Pre.lastKey();
            List<IpV6RangeGeo> lastRangeList = mapLvl1Pre.get(lastKey);
            for(IpV6RangeGeo r : lastRangeList) {
                if(r.contains(ipLongs)) {
                    return r;
                }
            }
        }

        return null;
    }

    private IpV6RangeGeo checkLvl1(TreeMap<Long, List<IpV6RangeGeo>> mapLvl1, long[] ipLongs) {
        long octet1Cur = ipLongs[0] & 0xFFFFFFFFL;
        long octet1Pre = octet1Cur - 1;

        Long keyCur = octet1Cur;  // Might not exist in the map
        List<IpV6RangeGeo> rangeListCur = mapLvl1.get(keyCur);
        if(rangeListCur != null) {
            for(IpV6RangeGeo r : rangeListCur) {
                if(r.contains(ipLongs)) {
                    return r;
                }
            }
        }

        Long keyPre = mapLvl1.floorKey(octet1Pre);   // Could be null only near the very front an octet's value space.
        if(keyPre != null && !keyPre.equals(keyCur)) {
            List<IpV6RangeGeo> rangeListPre = mapLvl1.get(keyPre);
            IpV6RangeGeo r = rangeListPre.get(rangeListPre.size() - 1);
            if(r.contains(ipLongs)) {
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
        for(Long octet0Key : ranges.keySet()) {
            TreeMap<Long, List<IpV6RangeGeo>> mapLvl1 = ranges.get(octet0Key);     // ranges is like mapLvl0
//            System.out.println(octet0Key + "...");
            totalSM += mapLvl1.size();
            countSM++;
            for(Long octet1Key : mapLvl1.keySet()) {
                List<IpV6RangeGeo> rangeList = mapLvl1.get(octet1Key);
//                System.out.printf("  %3d = %d%n", octet1Key, rangeList.size());
                total += rangeList.size();
                count++;
            }
        }
        System.out.println("V6:");
        System.out.println("  Lvl 0 Map Size: " + ranges.size());
        System.out.println("  Average Submap Size: " + ((double) totalSM / countSM));
        System.out.println("  Average Range List Length: " + ((double) total / count));
    }

    public void compress() {
        for(Long octet0Key : ranges.keySet()) {
            TreeMap<Long, List<IpV6RangeGeo>> mapLvl1 = ranges.get(octet0Key);     // ranges is like mapLvl0
            for(Long octet1Key : mapLvl1.keySet()) {
                List<IpV6RangeGeo> rangeList = mapLvl1.get(octet1Key);
                ((ArrayList) rangeList).trimToSize();
            }
        }
    }
}
