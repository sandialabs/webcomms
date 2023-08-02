package gov.sandia.webcomms.http.stats;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

import replete.collections.RHashMap;
import replete.numbers.RandomUtil;

public class RandomUrlGenerator {
    private static String[][] SANDIA_HOST_OPTIONS = {
        {"gov", "cn"},
        {"sandia"},
        {"info", "hrss", "rafts2", "webprod", "eims", "mysite", "sharepoint"},
        {"sub", "int", "w3", "", "", ""}
    };
    private static String[][] GOV_HOST_OPTIONS = {
        {"gov", "mil", "edu"},
        {"energy", "irs", "whitehouse", "treasury", "justice", "fda", "cdc"},
        {"vote", "public", "media", "internal"},
        {"aws", "g9", "", "", ""}
    };
    private static String[][] DOM_HOST_OPTIONS = {
        {"com", "org", "net", "tv", "biz"},
        {"cnn", "google", "wikipedia", "youtube", "toysrus"},
        {"buy", "watch", "videos", "social"}
    };
    private static String[][] FOR_HOST_OPTIONS = {
        {"au", "br", "fr", "de", "uk", "us"},
        {"co", "books", "libros", "finance", "google"},
        {"vacation", "hola", "popular", "trends"}
    };

    public List<String> genSourceUrls(int urlCount) {
        List<String> sourceUrls = new ArrayList<>();
        for(int i = 0; i < urlCount; i++) {
            String randUrl = genRandomSandiaUrl();
            sourceUrls.add(randUrl);
            System.out.println(randUrl);
        }
        return sourceUrls;
    }

    private String genRandomUrl(String[][] hostOptions) {
        Random R = new Random();
        int segs = RandomUtil.getRandomWithinRange(2, hostOptions.length + 1);  // Random between 2-4 segments (sandia.gov - x.y.sandia.gov)
        String host = "";
        for(int seg = 0; seg < segs; seg++) {
            String[] segOptions = hostOptions[seg];
            String nextSeg = segOptions[R.nextInt(segOptions.length)];
            if(!nextSeg.isEmpty()) {
                if(!host.isEmpty()) {
                    host = "." + host;
                }
                host = nextSeg + host;
            }
        }
        return "http://" + host + "/" + R.nextInt();
    }

    private static Map<String, Integer> DEST_GROUPS = new RHashMap<>(
        "*.sandia.gov", 35, "*.gov", 35, "Other Domestic", 20, "Foreign", 10
    );


    public List<String> genOutgoingUrls(int out) {
        List<String> outgoingUrls = new ArrayList<>();
        for(int o = 0; o < out; o++) {
            String randUrl = genRandomOutgoingUrl();
            outgoingUrls.add(randUrl);
        }
        return outgoingUrls;
    }

    private String genRandomOutgoingUrl() {
        String group = RandomUtil.chooseFromDistribution(DEST_GROUPS);
        if(group.equals("*.sandia.gov")) {
            return genRandomSandiaUrl();
        } else if(group.equals("*.gov")) {
            return genRandomGovUrl();
        } else if(group.equals("Other Domestic")) {
            return genRandomDomUrl();
        }
        return genRandomForeignUrl();
    }

    private String genRandomSandiaUrl() {
        return genRandomUrl(SANDIA_HOST_OPTIONS);
    }
    private String genRandomGovUrl() {
        return genRandomUrl(GOV_HOST_OPTIONS);
    }
    private String genRandomDomUrl() {
        return genRandomUrl(DOM_HOST_OPTIONS);
    }
    private String genRandomForeignUrl() {
        return genRandomUrl(FOR_HOST_OPTIONS);
    }


    //////////
    // TEST //
    //////////

    public static void mainx(String[] args) {
        RandomUrlGenerator gen = new RandomUrlGenerator();
        List<String> urls = gen.genSourceUrls(7);
        System.out.println("\nprint:");
        urls.stream().forEach(System.out::println);

        System.out.println("x");
        List<String> outg = gen.genOutgoingUrls(10);
        outg.stream().forEach(System.out::println);
    }
}
