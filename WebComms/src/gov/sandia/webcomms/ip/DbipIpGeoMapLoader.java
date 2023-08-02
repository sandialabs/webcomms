package gov.sandia.webcomms.ip;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

import gov.sandia.webcomms.ip.ranges.IpGeoMapMaster;
import gov.sandia.webcomms.ip.ranges.IpRangeGeo;
import replete.errors.ExceptionUtil;
import replete.numbers.NumUtil;
import replete.profiler.RProfiler;
import replete.progress.FractionProgressMessage;
import replete.text.FieldListParser;
import replete.text.StringPool;
import replete.ttc.TransparentTaskContext;
import replete.web.IpUtil;

public class DbipIpGeoMapLoader {

    private static void testRangeConsistency(File f, IpGeoMapMaster master) throws IOException {
        readDbipFile(f, (n, line, ipLo, ipHi, country, region, city) -> {
            if(false && ipLo.contains(".")) {
//                String a = ipLo.substring(0, ipLo.indexOf('.'));
                if(true) {        //Arrays.asList(new String[] {"126", "127", "128"}).contains(a)) {
                    int ipLoInt = IpUtil.ipV4StrToInt(ipLo);
                    int ipHiInt = IpUtil.ipV4StrToInt(ipHi);

                    long ipLoLong = ipLoInt & 0xFFFFFFFFL;
                    long ipHiLong = ipHiInt & 0xFFFFFFFFL;

                    long howMany = (ipHiLong - ipLoLong + 1);

                    for(long i = ipLoLong; i <= ipHiLong; i++) {
                        String ipStr = IpUtil.ipV4IntToStr((int) i);
                        IpRangeGeo r = master.lookup(ipStr);
                        if(r == null) {
                            System.err.println("Error: " + n + ", " + ipLo + ", " + ipHi + ", " + ipLoInt + ", " + ipHiInt + ", " + i + ", " + ((int)i));
                            ExceptionUtil.toss();
                        }
                    }

                    if(n % 10_000 == 0) {
                        System.out.println("Test: " + NumUtil.pct(n, 8876231));
                        System.out.println(n + " [" + ipLo + " - " + ipHi + "] " + country + "/" + region + "/" + city + " #" + howMany);
                        System.out.print("  Range: #" + howMany + " ["); // NumUtil.pct(m, howMany));
                    }
                }

            } else if(ipLo.contains(":")) {
                if(true) { //ipLo.startsWith("2001:371::")) {        //Arrays.asList(new String[] {"126", "127", "128"}).contains(a)) {
                    long[] ipLoLongs = IpUtil.ipV6StrToLongs(ipLo);
                    long[] ipHiLongs = IpUtil.ipV6StrToLongs(ipHi);

//                    System.out.println(BitUtil.markupBinaryString(BitUtil.toBinaryString(ipLoLongs[0]) + BitUtil.toBinaryString(ipLoLongs[1])));
//                    System.out.println(BitUtil.markupBinaryString(BitUtil.toBinaryString(ipHiLongs[0]) + BitUtil.toBinaryString(ipHiLongs[1])));

                    long[][] tests = new long[2][];
                    tests[0] = ipLoLongs;
                    tests[1] = ipHiLongs;

                    for(long[] test : tests) {
                        String ipStr = IpUtil.ipV6LongsToStr(test);
                        IpRangeGeo r = master.lookup(ipStr);
                        if(r == null) {
                            System.err.println("Error: " + n + ", " + ipLo + ", " + ipHi + ", " +
                                Arrays.toString(ipLoLongs) + ", " + Arrays.toString(ipHiLongs) + ", " +
                                Arrays.toString(test)
                            );
                            ExceptionUtil.toss();
                        }
                    }

//                    System.out.println("Test: " + NumUtil.pct(n, 8876231));
//                    System.out.println(n + " [" + ipLo + " - " + ipHi + "] " + country + "/" + region + "/" + city);
                }
            }

            if(n % 100_000 == 0) {
                System.out.println(NumUtil.pct(n, 8876231));
            }
        }, null);
    }

    private static void checkSome(RProfiler P, IpGeoMapMaster master) {
        String[] check = new String[] {
            "10.3.2.104",                                // US/California/Los Angeles
            "29.3.2.104",                                // US/Ohio/Columbus
            "221.3.2.104",                               // CN/Shandong/Jinan
            "127.0.0.1",                                 // US/California/Los Angeles
            "255.255.255.255",                           // CH/Canton of Fribourg/Düdingen
            "2001:250:3ff:ffff:ffff:ffff:ffff:ffff",     // CN/Beijing/Beijing
            "2001:250:3ff:ffff:ffff:ffff:ffff:fffe",     // CN/Beijing/Beijing
            "2001:250:3ff::",                            // CN/Beijing/Beijing
            "2001:250:20f::",                            // CN/Beijing/Beijing
            "2c10::",                                    // <Not Found>
            "30B1::",                                    // <Not Found>
            "2001:238:ffff:feff:ffff:ffff:ffff:ffff",    // TW/Taoyuan County/Zhongli City
            "2001:218:3003::1",                          // NL/North Holland/Amsterdam
            "2001:0db8:85a3:0000:0000:8a2e:0370:7334",   // AU/Queensland/Brisbane
            "2607:f8b0:400f:803::200e",                  // Google = US/California/San Jose
            "2001:4998:58:c02::a9",                      // Yahoo = US/California/San Jose
            "2001:4801:1221:101:1c10:0:f5:116"           // Rackspace = Dallas
        };
        for(String ch : check) {
            P.block("IP/" + ch);
            try {
                IpRangeGeo r = master.lookup(ch);
                System.out.print("Checking " + ch + "...");
                if(r == null) {
                    System.out.println("<Not Found>");
                } else {
                    System.out.println("Found " + r);
                }
            } finally {
                P.end();
            }
        }
    }

    public static IpGeoMapMaster parseMasterFromCsvFile(File f, TransparentTaskContext ttc) throws IOException {
        IpGeoMapMaster master = new IpGeoMapMaster();
        readDbipFile(f, (n, line, ipLo, ipHi, country, region, city) -> {
            if(!master.add(ipLo, ipHi, country, region, city)) {
                throw new RuntimeException("IP Range Tree Addition Failure [Line #" + n + "] (" + line + ")");
            }
        }, ttc);
        master.compress();
        return master;
    }

    private interface Action {
        public void process(int n, String line, String ipLo, String ipHi, String country, String region, String city);
    }

    private static void readDbipFile(File f, Action action, TransparentTaskContext ttc) throws IOException {
        BufferedReader reader = null;

        try {
            reader = new BufferedReader(
                new InputStreamReader(new FileInputStream(f), StandardCharsets.UTF_8));

            StringPool pool = new StringPool();

            String line;
            int n = 0;
            final int MAX_LINES = 8876230; // TEMP CONSTANT FOR UI
            if(ttc != null) {
                ttc.publishProgress(new FractionProgressMessage("Reading lines", n, MAX_LINES));
            }
            while((line = reader.readLine()) != null) {
                if(ttc != null) {
                    ttc.checkPauseAndStop();
                }

                List<String> fields = FieldListParser.parseLine(line, ',', '"');
                if(fields.size() != 5) {
                    throw new RuntimeException("DBIP Format Problem [Line #" + n + "] (" + line + ")");
                }

                String ipLo = fields.get(0);        // Pool would not help these
                String ipHi = fields.get(1);

                if(ipHi.equalsIgnoreCase("ffff:ffff:ffff:ffff:ffff:ffff:ffff:ffff")) {
                    continue;
                }
//
                String country = pool.resolve(fields.get(2));
                String region  = pool.resolve(fields.get(3));
                String city    = pool.resolve(fields.get(4));

                action.process(
                    n,
                    line,
                    ipLo,
                    ipHi,
                    country,
                    region,
                    city
                );

                n++;
                if(ttc != null) {
                    ttc.publishProgress(new FractionProgressMessage("Reading lines", n, MAX_LINES));
                }
            }

        } finally {
            if(reader != null) {
                try {
                    reader.close();
                } catch(Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }


    //////////
    // TEST //
    //////////

    public static void main(String[] args) {

        File f = new File("C:\\Users\\dtrumbo\\work\\eclipse-main\\WebComms\\dbip-city-2016-10.csv");

        RProfiler P = RProfiler.get();
        P.block("read");

        IpGeoMapMaster master = null;
        try {
            master = parseMasterFromCsvFile(f, null);
        } catch(IOException e) {
            e.printStackTrace();
        }
        P.end();

//        File cachedMaster = User.getDesktop("geo-test.bin");
//        FileUtil.writeObjectContent(master, cachedMaster);
//        IpGeoMapMaster master = (IpGeoMapMaster) FileUtil.getObjectContent(cachedMaster);   // Takes forever to load for some reason

        System.out.println("IPv4: " + master.ipV4Map.getRangeCount() + ", IPv6: " + master.ipV6Map.getRangeCount());
        checkSome(P, master);
        P.printAll();
        master.printDist();

//        testRangeConsistency(f, master);
    }
}
