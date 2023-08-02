package gov.sandia.webcomms.http.stats;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;

import replete.web.UrlUtil;

public class UrlStats implements Serializable {


    ////////////
    // FIELDS //
    ////////////

    private AggregatedUrlStats combinedStats = new AggregatedUrlStats();
    private Map<String, AggregatedUrlStats> indivDomainStats = new TreeMap<>();


    /////////
    // ADD //
    /////////

    public synchronized void add(String url) {
        String host = UrlUtil.getHostUrl(url);
        AggregatedUrlStats domainStats = indivDomainStats.get(host);
        if(domainStats == null) {
            domainStats = new AggregatedUrlStats();
            indivDomainStats.put(host, domainStats);
        }
        combinedStats.add(url);
        domainStats.add(url);
    }


    //////////////////////////
    // ACCESSORS / MUTATORS //
    //////////////////////////

    // Accessors (Computed)

    public synchronized UrlStats copy() {
        Map<String, AggregatedUrlStats> indivDomainStatsCopy = new TreeMap<>();
        for(String host : indivDomainStats.keySet()) {
            indivDomainStatsCopy.put(host, indivDomainStats.get(host).copy());
        }
        return new UrlStats()
            .setCombinedStats(combinedStats.copy())
            .setIndivDomainStats(indivDomainStatsCopy)
        ;
    }

    // Mutators

    public UrlStats setCombinedStats(AggregatedUrlStats combinedStats) {
        this.combinedStats = combinedStats;
        return this;
    }
    public UrlStats setIndivDomainStats(Map<String, AggregatedUrlStats> indivDomainStats) {
        this.indivDomainStats = indivDomainStats;
        return this;
    }


    //////////
    // MISC //
    //////////

    @Override
    public synchronized String toString() {
        StringBuilder buffer = new StringBuilder();
        buffer.append("COMBINED\n");
        buffer.append("========\n");
        buffer.append(combinedStats.toString(1));
        if(!indivDomainStats.isEmpty()) {
            buffer.append("\n");
            buffer.append("BY HOST\n");
            buffer.append("=======\n");
            for(String host : indivDomainStats.keySet()) {
                AggregatedUrlStats stats = indivDomainStats.get(host);
                buffer.append("  " + host + ":\n");
                buffer.append(stats.toString(2));
            }
        }
        return buffer.toString();
    }


    //////////
    // TEST //
    //////////

    private static final int max = 10000;
    public static void main(String[] args) {
        UrlStats stats = new UrlStats();
//        stats.add("http://derek:pw@cnn.com:4");
//        stats.print();
        Random R = new Random();
        List<String> urls = new ArrayList<>();
        for(int i = 0; i < max; i++) {
            int c = R.nextInt(3);
            int n = R.nextInt(10000);
            StringBuilder b = new StringBuilder();
            for(int a = 0; a < 1000; a++) {
                b.append((char) (65 + R.nextInt(26)));
                if((a + 1) % 26 == 0) {
                    b.append('/');
                }
            }
            String p = "";
            if(flip()) {
                p = ":" + R.nextInt(300);
            }
            String url = "http://cnn-" + c + ".com" + p + "/" + b;
            if(flip()) {
                int q = R.nextInt(3) + 1;
                url += "?";
//                for(int j = 0; j < q; j++) {
//                    if(!url.endsWith("?")) {
//                        url += "&";
//                    }
//                    url += "a=b";
//                }
            }
            if(flip()) {
                url += "#1";
            }
//            System.out.println(url);
            urls.add(url);
        }
        long T = System.currentTimeMillis();
        for(String url : urls) {
            stats.add(url);
        }
//        stats.print();
        T = System.currentTimeMillis() - T;
        System.out.println(T);
    }
    private static boolean flip() {
        return new Random().nextBoolean();
    }
}
