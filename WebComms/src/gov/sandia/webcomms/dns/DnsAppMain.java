
package gov.sandia.webcomms.dns;

import java.net.InetAddress;
import java.net.UnknownHostException;

import gov.sandia.webcomms.SoftwareVersion;
import replete.cli.CommandLineParser;
import replete.cli.errors.CommandLineParseException;
import replete.cli.errors.UserRequestedHelpException;
import replete.cli.options.Option;
import replete.util.AppMain;

// http://www.myhowto.org/java/42-understanding-host-name-resolution-and-dns-behavior-in-java/
// http://www.rgagnon.com/javadetails/java-0445.html
// https://docs.oracle.com/javase/7/docs/technotes/guides/net/properties.html
// https://www.ibm.com/developerworks/community/blogs/738b7897-cd38-4f24-9f05-48dd69116837/entry/dns_in_java46?lang=en
// https://docs.oracle.com/javase/7/docs/technotes/guides/security/PolicyFiles.html
// https://stackoverflow.com/questions/17420620/clean-dns-server-in-jvm

// successful hit TTL: networkaddress.cache.ttl
// failure hit TTL:    networkaddress.cache.negative.ttl
// (java.security file)

//#    -Djava.net.preferIPv4Stack=true \
//System.setProperty(Context.DNS_URL, "dns://127.0.0.1:5555");
//System.setProperty(Context.PROVIDER_URL, "dns://127.0.0.1:5555");

public class DnsAppMain extends AppMain {


    ////////////
    // FIELDS //
    ////////////

    // Constants

    private static final String SUN_NET_NS      = "sun.net.spi.nameservice.nameservers";
    private static final String SUN_NET_PROV    = "sun.net.spi.nameservice.provider.1";
    private static final String SUN_NET_TTL     = "sun.net.inetaddr.ttl";
    private static final String SUN_NET_NEG_TTL = "sun.net.inetaddr.negative.ttl";


    //////////
    // MAIN //
    //////////

    private static String[] replaceWithTestArgs(String[] args) {
//        args = new String[] {
//            "--ttl=77",
//            "--negttl=22",
//            "-?",
//            "cnn.com",
//            "time.org",
//            "8.8.8.8"
//        };
        return args;
    }

    public static void main(String[] args) {
        if(SoftwareVersion.get().isDevelopment()) {
            args = replaceWithTestArgs(args);
        }

        String addl =
            "xx\n" +
            "xx\n" +
            "xx"
        ;

        CommandLineParser parser = new CommandLineParser();
        parser.setCommandName("web-resolve");
        parser.setMiniumNonOptionArguments(1);
        parser.setPrintParamDelimiters(true);
        parser.setDashDashCommentEnabled(true);
//        parser.setCustomUsageLine("[-?|--help] DOMAIN_NAME [DOMAIN_NAME ...]");
        parser.setAddlUsageMessage(addl, 2);
        parser.addDefaultHelpOption();
        parser.setNonOptionNames("HOST");

        Option optNs     = parser.addStringOption("ns");
        Option optPl     = parser.addStringOption("pl");
        Option optTtl    = parser.addStringOption("ttl");
        Option optNegTtl = parser.addStringOption("negttl");

        optNs.setHelpDescription("xx");
        optNs.setHelpParamName("NAMESERVERS");

        optPl.setHelpDescription("(e.g. \"dns,dnsjava\", \"dns,sun\")");
        optPl.setHelpParamName("PROVIDER-LIST");

        optTtl.setHelpDescription("xx");
        optTtl.setHelpParamName("TTL");

        optNegTtl.setHelpDescription("xx");
        optNegTtl.setHelpParamName("TTL");

        try {
            parser.parse(args);
        } catch(CommandLineParseException e) {
            return;
        } catch(UserRequestedHelpException e) {
            return;
        }

        String ns     = parser.getOptionValue(optNs,     null);
        String pl     = parser.getOptionValue(optPl,     null);
        String ttl    = parser.getOptionValue(optTtl,    null);
        String negTtl = parser.getOptionValue(optNegTtl, null);

        if(ns != null) {
            System.setProperty(SUN_NET_NS, ns);
        }
        if(pl != null) {
            System.setProperty(SUN_NET_PROV, pl);
        }
        if(ttl != null) {
            System.setProperty(SUN_NET_TTL, ttl);
        }
        if(negTtl != null) {                              // Has no effect, because apparently the
            System.setProperty(SUN_NET_NEG_TTL, negTtl);  // "negative" TTL is already set before main
        }                                                 // is called! (OS/JVM impl specific?)

//        System.out.println(sun.net.InetAddressCachePolicy.get());          // Interesting backdoor API
//        System.out.println(sun.net.InetAddressCachePolicy.getNegative());  // Interesting backdoor API

        for(String dn : parser.getNonOptionArguments()) {
            System.out.println("Resolving host '" + dn + "'");
            try {
                InetAddress[] addrs = InetAddress.getAllByName(dn);
                for(InetAddress addr : addrs) {
                    System.out.println("    IP: " + addr.toString());
                }
            } catch(UnknownHostException e) {
                System.out.println("    <Unknown>");
            } catch(Exception e) {
                System.out.println("    Error: " + e.getClass().getName() + ": " + e.getMessage());
                e.printStackTrace();
            }
        }
    }
}
