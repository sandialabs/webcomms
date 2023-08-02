package gov.sandia.webcomms.http;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.regex.Pattern;

import org.apache.http.pool.PoolStats;

import gov.sandia.cortext.plugin.CortextPlugin;
import gov.sandia.webcomms.SoftwareVersion;
import gov.sandia.webcomms.http.allowrc.HttpAllowRedirectCriteriaParams;
import gov.sandia.webcomms.http.options.HttpRequestOptions;
import gov.sandia.webcomms.http.preemptors.HttpRequestPreemptorParams;
import gov.sandia.webcomms.http.rsc.HttpResource;
import gov.sandia.webcomms.logging.LogUtil;
import gov.sandia.webcomms.plugin.WebCommsPlugin;
import replete.cli.CommandLineParser;
import replete.cli.errors.CommandLineParseException;
import replete.cli.errors.UserRequestedHelpException;
import replete.cli.options.Option;
import replete.cli.validator.OptionValueValidator;
import replete.collections.Pair;
import replete.errors.ExceptionUtil;
import replete.io.FileUtil;
import replete.logging.LoggingInitializer;
import replete.numbers.NumUtil;
import replete.plugins.PluginManager;
import replete.plugins.RepletePlugin;
import replete.text.StringUtil;
import replete.util.AppMain;
import replete.util.ReflectionUtil;
import replete.web.UrlUtil;
import replete.xstream.ExtensibleMetadataXStream;

// TODO: When parsing groups, ignore groups that don't actually get any URLs so
// the group ID's are a little more understandable.
//http://stackoverflow.com/questions/4775618/httpclient-4-0-1-how-to-release-connection
public class HttpAppMain extends AppMain {

    // TODO: implement group.abortTimeout...
    // TODO: check on if( response.getEntity() != null ) {
//    response.getEntity().consumeContent();
// } or
//    EntityUtils.consume(HttpEntity)  are we using these already?


    ////////////
    // FIELDS //
    ////////////

    private static final String SANDIA_PROXY_TAG = "sandia";                            // User convenience
    private static final String SANDIA_SRN_PROXY = "wwwproxy.sandia.gov:80";
    private static final String LH_LOCAL         = "http://localhost:8080/Lighthouse";  // Developer convenience
    private static final String LH_SRN           = "http://av-lighthouse.sandia.gov";   // Developer convenience

    private static ExtensibleMetadataXStream xStream;
    private static Map<String, BooleanGroupOption> booleanGroupOptions = new LinkedHashMap<>();
    private static Map<String, ValuedGroupOption> valuedGroupOptions = new LinkedHashMap<>();
    private static Object printLock = new Object();


    ////////////////////
    // INITIALIZATION //
    ////////////////////

    static {
        addBooleanOption("sc", "Save Content (on by default)",
            (options, group, enabled) -> options.setSaveContent(enabled)
        );
        addBooleanOption("srh", "Save Response Headers(on by default)",
            (options, group, enabled) -> options.setSaveResponseHeaders(enabled)
        );
        addBooleanOption("srd", "Save Redirects (on by default)",
            (options, group, enabled) -> options.setSaveRedirects(enabled)
        );
        addBooleanOption("srdrh", "Save Redirect Response Headers (on by default)",
            (options, group, enabled) -> options.setSaveRedirectResponseHeaders(enabled)
        );
        addBooleanOption("srdrc", "Save Redirect Response Content (on by default)",
            (options, group, enabled) -> options.setSaveRedirectResponseContent(enabled)
        );
        addBooleanOption("ss", "Save Security (on by default)",
            (options, group, enabled) -> options.setSaveSecurity(enabled)
        );
        addBooleanOption("srq", "Save Request (on by default)",
            (options, group, enabled) -> options.setSaveRequest(enabled)
        );
        addBooleanOption("cu", "Clean URLs (on by default)",
            (options, group, enabled) -> options.setCleanUrls(enabled)
        );
        addBooleanOption("igssl", "Ignore SSL Errors (off by default)",
            (options, group, enabled) -> options.setIgnoreSslProblems(enabled)
        );
        addBooleanOption("pet", "Print Execution Trace (for Debugging) (off by default)",
            (options, group, enabled) -> options.setPrintExecutionTrace(enabled)
        );
        // Group / non-HttpRequestOptions Options
        addBooleanOption("pr", "Print Resources after Fetch (on by default)",
            (options, group, enabled) -> group.print = enabled
        );
        addBooleanOption("svr", "Save resource (as XML) to the save directory (off by default)",
            (options, group, enabled) -> group.saveResourceNoContent = enabled
        );
        addBooleanOption("svrc", "Save resource content (as binary) to the save directory (off by default)",
            (options, group, enabled) -> group.saveResourceContent = enabled
        );
        addBooleanOption("svrt", "Save resource textual representation to the save directory (off by default)",
            (options, group, enabled) -> group.saveResourceToString = enabled
        );
        addBooleanOption("ufp", "Execute URL fetches in parallel (off by default)",
            (options, group, enabled) -> group.urlsParallel = enabled
        );

        // TODO: Not yet sure how to provide arbitrary UA strings while current
        // command line parsing strategy in place.  Example:
        //   Mozilla/5.0 (Windows NT 6.1; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/63.0.3239.132 Safari/537.36
        // probably doesn't parse well at least due to the comma within.
        addValuedOption("ua", "User-Agent",
            (options, group, value) -> options.setOverrideUserAgent(value),
            (value) -> {return null;}
        );
        addValuedOption("mcl", "Max Content Length (in bytes)",
            (options, group, value) -> options.setMaxContentLength(Long.parseLong(value)),
            (value) -> {return !NumUtil.isLong(value) ? "max content length must be a long" : null;}
        );
        addValuedOption("to", "Timeout (in milliseconds)",
            (options, group, value) -> options.setTimeout(Integer.parseInt(value)),
            (value) -> {return !NumUtil.isInt(value) ? "timeout must be an integer" : null;}
        );
        addValuedOption("prx", "Proxy (host:port format)",    // make a 'prxex' option?
            (options, group, value) -> {
                if(value.equalsIgnoreCase(SANDIA_PROXY_TAG)) {
                    value = SANDIA_SRN_PROXY;
                }
                options.setProxy(value);
            },
            (value) -> {
                if(value.equalsIgnoreCase(SANDIA_PROXY_TAG)) {
                    return null;
                }
                String valMsg = UrlUtil.validateHostPort(value);
                if(valMsg != null) {
                    return "invalid proxy server: " + valMsg;
                }
                return null;
            }
        );
        addValuedOption("arc", "Allow Redirect Criteria (not used yet)",
            (options, group, value) -> {
                HttpAllowRedirectCriteriaParams params = ReflectionUtil.create(value);
                options.setAllowRedirectCriteriaParams(params);
            },
            (value) -> {
                try {
                    ReflectionUtil.create(value);
                    return null;
                } catch(Exception e) {
                    return "cannot instantiate params class";
                }
            }
        );
        addValuedOption("pre", "Request Preemptor (not used yet)",
            (options, group, value) -> {
                HttpRequestPreemptorParams params = ReflectionUtil.create(value);
                options.setRequestPreemptorParams(params);
            },
            (value) -> {
                try {
                    ReflectionUtil.create(value);
                    return null;
                } catch(Exception e) {
                    return "cannot instantiate params class";
                }
            }
        );
        // Group / non-HttpRequestOptions Options
        addValuedOption("m", "HTTP Method (e.g. GET, POST)",
            (options, group, value) -> group.setMethod(value.toUpperCase()),
            (value) -> {
                try {
                    RequestMethod.valueOf(value.toUpperCase());
                    return null;
                } catch(Exception e) {
                    return "method must be one of " + Arrays.toString(RequestMethod.values());
                }
            }
        );
        addValuedOption("ato", "Abort Timeout (in milliseconds)",
            (options, group, value) -> group.setAbortTimeout(Integer.parseInt(value)),
            (value) -> {return !NumUtil.isInt(value) ? "abort timeout must be an integer" : null;}
        );
    }

    private static void addBooleanOption(String name, String desc, BooleanGroupOptionAction action) {
        booleanGroupOptions.put(name, new BooleanGroupOption(name, desc, action));
    }
    private static void addValuedOption(String name, String desc,
                                        ValuedGroupOptionAction action, ValuedGroupOptionValidator validator) {
        valuedGroupOptions.put(name, new ValuedGroupOption(name, desc, action, validator));
    }


    //////////
    // MAIN //
    //////////

    private static String[] replaceWithTestArgs(String[] args) {
//        args = new String[] {
//            "--dprx=sandia", "--dprxex=localhost|127\\.0\\.0\\.1",
//            "--savedir=C:\\Users\\dtrumbo\\work\\eclipse-main\\WebComms\\test\\gov\\sandia\\webcomms",
//            "--optset=svr,svrc,svrt",
//            "http://cnn.com",
//            "--optset=svr,svrc,svrt",
//            "http://cnn.com",
//        };
//        args = new String[] {
//            "--poolprint", "1000", "--poolsize", "100",
//            LH_LOCAL + "/ping?delay=10000",
//            "--optset=+sc",
//            LH_LOCAL + "/ping?delay=10000",
//        };
//
//        args = new String[] {
//            "--poolprint", "1000", "--poolsize", "1000",
//            "--optset=ufp,ato=5000",
//            LH_LOCAL + "/ping?delay=1000000",
//            LH_LOCAL + "/ping?delay=1000000",
//            LH_LOCAL + "/ping?delay=1000000",
//            LH_LOCAL + "/ping?delay=1000000",
//            "--optset=ufp",
//            LH_LOCAL + "/ping?delay=10000",
//            LH_LOCAL + "/ping?delay=10000",
//        };
//
//        args = new String[] {
//            "--dprx=wwwproxy.sandia.gov:80", "--optset=sv,m=options", "http://google.com/",
//            "--savedir=C:\\Users\\dtrumbo\\Desktop"
//        };
//
//        args = "--dprx=sandia --optset -sc,-srh,-srd,-srdrh,-ss,-srq,-cu http://time.org --optchg +cu htp://time.org".split(" ");
//
//        args = new String[] {
//            "-?",
//            "--debug",
//            "--dprx=sandia",
//            "--optset=sc,ss,mcl=10,m=get",
//            "--optset", "-sc",
//            "http://cnn.com",
//            "--optchg=m=POST,prx=a:1",
//            "URL2",
//            "--urlfile", "C:\\Users\\dtrumbo\\work\\eclipse-main\\WebComms\\src\\gov\\sandia\\webcomms\\http\\test.txt",
//            "URL33"
//        };
//
//        args = new String[] {
//            "--debug",
//            "--dprx=sandia",
//            "--gfp",
//            "--optset=-srh,+ufp,-pr",
//            "http://time.org/unknown.jsp",
//            "--optset=+ufp,prx=aa:2,-pr",
//            "http://cnn.com"
//        };
//
//        args = new String[] {
//            "--savedir=C:\\Users\\dtrumbo\\Desktop\\wctest",
//            "--dprx=wwwproxy.sandia.gov:80",
//            "--optset=+sv",
//            "http://cnn.com",
//            "http://msnbc.com"
//        };
//
//        args = new String[] {
//            "-?"
//        };

        args = new String[] {
//            "--dprx=wwwproxy.sandia.gov:80",
//            "--dprxex=localhost",
//            "--dprx=example.com:80",
//            "--gfp",
//            "--savedir=C:\\Users\\dtrumbo\\Desktop\\wctest",
//            "--optset=+igssl,+pet,+ufp,+pr,+svr,+svrc,+svrt",
            "--fse",
            "--optset=-igssl,+pet,-pr,+ufp",
            "http://localhost:8080/Lighthouse",
//            "http://localhost:8080/Lighthouse/redirect?to=https://localhost:8080/Lighthouse/ping",   // Truly invalid
//            "http://localhost:8080/Lighthouse/redirect?to=https://localhost:8443/Lighthouse/ping",
//            "http://localhost:8080/Lighthouse/redirect?to=https://localhost:8443/Lighthouse/ping",
            "http://localhost:8080/Lighthouse/ping",
//            "https://localhost:8080/Lighthouse/ping",     // Truly invalid
            "https://localhost:8443/Lighthouse/ping",
//            "--optset=-igssl",
//            "https://localhost:8443/Lighthouse/ping",   // <-- SSL handshake not performed again
//            "--optset=prx=wwwproxy.sandia.gov:80",
//            "https://google.com/",
//            "https://capitalone.com",
//            "https://localhost:8443/Lighthouse/empty",  // <-- SSL handshake not performed again
//            "http://www.animalplanet.com/robots.txt"      // Forwards to https, no sec info??
//            "https://www.vgtv.no/robots.txt",            // Invalid cert (Issuer: "Let's Encrypt")
//            "https://www.bt.dk/robots.txt",              // Invalid cert
//            "https://www.mcneeslaw.com/robots.txt"       // Invalid cert (Certs OK but doesn't correspond to host)
        };

        // https://www.dog.com returns RC 456 when "User-Agent: Apache-HttpClient/4.2.1 (java 1.5)" is used
        // I could only find this code mentioned here: https://msdn.microsoft.com/en-us/library/mt563432(v=exchg.80).aspx
        args = new String[] {
            "--dprx=wwwproxy.sandia.gov:80",
            //"--savedir=C:\\Users\\dtrumbo\\work\\eclipse-main\\WebComms\\src\\gov\\sandia\\webcomms\\http",
            //"--optset=+svr,+svrc,+svrt",
            "https://www.dog.com/",
        };

        args = new String[] {
            //"--savedir=C:\\Users\\dtrumbo\\work\\eclipse-main\\WebComms\\src\\gov\\sandia\\webcomms\\http",
            //"--optset=+svr,+svrc,+svrt",
            "http://localhost:8080/Lighthouse/ping?rc=541",
        };

        return args;
    }

    public static void main(String[] args) {
        if(SoftwareVersion.get().isDevelopment()) {
            args = replaceWithTestArgs(args);
        }

        PluginManager.initialize(
            RepletePlugin.class,
            CortextPlugin.class,
            WebCommsPlugin.class
        );

        LoggingInitializer.init(LogUtil.INTERNAL_PROPERTIES_PATH);
        xStream = new ExtensibleMetadataXStream();

        String addlMessage =
            "The command line arguments allow for the creation of 'URL Groups', each with their\n" +
            "distinct options.  A group with system default HTTP request options is conceptually\n" +
            "open at the beginning of the command line and any URLs (either inline or via --urlfile)\n" +
            "provided before --optset or --optchg will have these options.\n" +
            "\nSyntax for OPTIONS used by --optset and --optchg:\n" +
            "  Format: comma-separated list of '[+|-]key|key=value' options, no spaces\n" +
            "  Boolean Options:\n";
        for(String name : booleanGroupOptions.keySet()) {
            BooleanGroupOption option = booleanGroupOptions.get(name);
            addlMessage += String.format("      %-5s - %s%n", name, option.desc);
        }
        addlMessage += "  Valued Options:\n";
        for(String name : valuedGroupOptions.keySet()) {
            ValuedGroupOption option = valuedGroupOptions.get(name);
            addlMessage += String.format("      %-5s - %s%n", name, option.desc);
        }
        addlMessage +=
            "\nSystem Default Options:\n" +
            "  +sc,+srh,+srd,+srdrh,+srdrc,+ss,+srq,+cu,+pr" +
                    ",mcl=" + HttpRequestOptions.DEFAULT_MAX_CONTENT_LENGTH +
                    ",to=" + HttpRequestOptions.DEFAULT_TIMEOUT + "\n" +
            "  In words: save all content, clean URLs, and print results.\n" +
            "\nExample Command Lines (can use spaces or = for top level argument values):\n" +
            "  web-fetch http://time.org\n" +
            "  web-fetch --urlfile=/home/me/urllist.txt\n" +
            "  web-fetch --dprx=proxy.example.com:80 --urlfile=/home/me/urllist.txt\n" +
            "  web-fetch --optset=-sc http://time.org\n" +
            "  web-fetch --optset=-sc,-srh,-srd,-srdrh,-ss,-srq,-cu http://time.org ...\n" +
            "            --optchg=+cu htp://time.org\n" +
            "  web-fetch --optset -ss http://time.org --urlfile myurls.txt ...\n" +
            "            --optset -srq --urlfile a.txt --urlfile b.txt\n" +
            "  web-fetch --dprx proxy1:80 http://time.org ...\n" +
            "            --optset prx=proxy2:80 http://time2.org ...\n" +
            "            --optset prx=proxy3:80 http://time3.org ...\n" +
            "            --optset +cu http://time4.org\n" +
            "  web-fetch --savedir=C:\\Users\\TomServo\\Desktop ...\n" +
            "            --optset=svr,svrc http://time.org\n" +
            "  web-fetch --dprx=proxy.example.com:80 --dprxex=localhost|127\\.0\\.0\\.1 ...\n" +
            "            http://localhost:8080/MyServer/page http://time.org"
        ;

        CommandLineParser parser = new CommandLineParser();
        parser.setCommandName("web-fetch");
        parser.setMiniumNonOptionArguments(1);
        parser.setPrintParamDelimiters(true);
        parser.setDashDashCommentEnabled(true);
        parser.setCustomUsageLine("[-?|--help] [--debug] [--gfp] [--savedir=<DIR>] [--dprx=<PROXY>] ([--optset=<OPTIONS>|--optchg=<OPTIONS>] (<URL>|--urlfile <FILE>)+)+");
        parser.setAddlUsageMessage(addlMessage, 2);

        parser.addDefaultHelpOption();
        Option optDebug     = parser.addBooleanOption("debug").setDefaultLabel("Off");
        Option optSet       = parser.addStringOption("optset");
        Option optChg       = parser.addStringOption("optchg");
        Option optDefPrx    = parser.addStringOption("dprx");
        Option optDefPrxEx  = parser.addStringOption("dprxex");      // Exception regular expression e.g. "localhost|127\.0\.0\.1"
        Option optUrlFile   = parser.addPathOption("urlfile").setMustBeFile(true);
        Option optSaveDir   = parser.addPathOption("savedir").setMustBeDirectory(true);
        Option optGfp       = parser.addBooleanOption("gfp").setDefaultLabel("Off");
        Option optPoolSize  = parser.addIntegerOption("poolsize");
        Option optPoolPrint = parser.addIntegerOption("poolprint");
        Option optFse       = parser.addBooleanOption("fse");

        optDebug.setHelpDescription("Whether or not to enable debug mode, which does not actually perform the fetching of the resources.");
        optDebug.setHelpParamName("ENABLED");

        optSet.setHelpDescription("The HTTP request options to set on top of the system default options and apply to the URLs that follow until the next --optset or --optchg.  OPTIONS described below.  A group with the system default HTTP request options is applies at the start of the command line without having to first call --optset.");
        optSet.setHelpParamName("OPTIONS");
        optSet.addValidator((OptionValueValidator<String>) (option, value) -> {
            return validateAndApplyOptions(value, null);
        });

        optChg.setHelpDescription("The HTTP request options to change from the previous URL group's options and apply to the URLs that follow until the next --optset or --optchg.  OPTIONS described below.");
        optChg.setHelpParamName("OPTIONS");
        optChg.addValidator((OptionValueValidator<String>) (option, value) -> {
            return validateAndApplyOptions(value, null);
        });

        optDefPrx.setHelpDescription("The default proxy for those groups who do not define their own.  Format: 'host:port'.  The value 'sandia' is also valid and provides a default proxy for Sandia.");
        optDefPrx.setHelpParamName("PROXY");
        optDefPrx.setAllowMulti(false);
        optDefPrx.addValidator((OptionValueValidator<String>) (option, value) -> {
            if(value.equalsIgnoreCase(SANDIA_PROXY_TAG)) {
                return null;
            }
            String valMsg = UrlUtil.validateHostPort(value);
            if(valMsg != null) {
                return "invalid default proxy server: " + valMsg;
            }
            return null;
        });

        optDefPrxEx.setHelpDescription("A regular expression that describes the hosts of which URLs will not use the default proxy (e.g. \"localhost|127\\.0\\.0\\.1\").");
        optDefPrxEx.setHelpParamName("EXPR");
        optDefPrxEx.setAllowMulti(false);
        optDefPrxEx.addValidator((OptionValueValidator<String>) (option, value) -> {
            try {
                Pattern.compile(value);
                return null;
            } catch(Exception e) {
                return "invalid default proxy exception regular expression: " + e.getMessage();
            }
        });

        optUrlFile.setHelpDescription("A file from which a list of URLs can be read for the current group (one URL per line).");
        optUrlFile.setHelpParamName("FILE");

        optSaveDir.setHelpDescription("A directory where resource content will be saved for a group whose save flag is enabled.");
        optSaveDir.setAllowMulti(false);
        optSaveDir.setHelpParamName("DIR");

        optGfp.setHelpDescription("Whether or not groups are to be executed in parallel.");
        optGfp.setHelpParamName("ENABLED");

        optPoolSize.setHelpDescription("The maximum size of the HTTP connection pool (must be >= 1).");
        optPoolSize.setHelpParamName("SIZE");

        optPoolPrint.setHelpDescription("An interval in milliseconds on which to print connection pool statistics (must be >= 1).");
        optPoolPrint.setHelpParamName("INTERVAL");

        optFse.setHelpDescription("Whether to ensure all requests are executed serially.");
        optFse.setHelpParamName("ENABLED");

        try {
            parser.parse(args);
        } catch(CommandLineParseException e) {
            return;
        } catch(UserRequestedHelpException e) {
            return;
        }

        boolean debug          = parser.getOptionValue(optDebug, false);
        String defProxy        = parser.getOptionValue(optDefPrx);
        String defProxyExRe    = parser.getOptionValue(optDefPrxEx);
        boolean groupsParallel = parser.getOptionValue(optGfp, false);
        String saveDirStr      = parser.getOptionValue(optSaveDir, null);
        File saveDir = saveDirStr == null ? null : new File(saveDirStr);
        int poolSize           = parser.getOptionValue(optPoolSize, 0);
        int poolPrintInterval  = parser.getOptionValue(optPoolPrint, 0);
        boolean fse            = parser.getOptionValue(optFse, false);

        Http.getInstance().setForceSerialExecution(fse);

        initPool(poolSize, poolPrintInterval);
        List<ActionGroup> groups = parseArgsIntoGroups(parser, optSet, optChg, optUrlFile);

        GlobalConfig config = new GlobalConfig()
            .setDebug(debug)
            .setGroupsParallel(groupsParallel)
            .setDefaultProxy(defProxy)
            .setDefaultProxyExRe(defProxyExRe)
            .setSaveDirectory(saveDir)
        ;
        processGroups(config, groups);
    }


    //////////
    // INIT //
    //////////

    private static void initPool(int poolSize, int poolPrintInterval) {
        if(poolSize > 0) {
            Http.getInstance().setConnCacheSize(poolSize);
        }
        if(poolPrintInterval > 0) {
            Timer timer = new Timer(true);
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    Http http = Http.getInstance();
                    PoolStats stats = http.getPoolStats();
                    System.out.println("Pool Stats: " + stats);
                }
            }, poolPrintInterval, poolPrintInterval);
        }
    }

    private static List<ActionGroup> parseArgsIntoGroups(CommandLineParser parser, Option optSet,
                                                         Option optChg, Option optUrlFile) {
        ActionGroup curGroup;
        List<ActionGroup> groups = new ArrayList<>();
        groups.add(curGroup = new ActionGroup());

        for(Pair<Option<?>, Object> parsedArg : parser.getOrderedParsedArguments()) {
            Option option = parsedArg.getValue1();
            if(option != optSet && option != optChg && option != optUrlFile && option != null) {
                continue;
            }
            String value = (String) parsedArg.getValue2();        // All options are string options

            // Just add the URL to the current group.
            if(option == null) {
                curGroup.urls.add(value);

            } else if(option == optUrlFile) {
                File f = new File(value);
                String content = FileUtil.getTextContent(f, false).trim();
                String[] lines = content.split("\n");
                for(String line : lines) {
                    curGroup.urls.add(line);
                }

            } else if(option == optSet) {
                groups.add(curGroup = new ActionGroup());
                validateAndApplyOptions(value, curGroup);

            } else {
                groups.add(curGroup = new ActionGroup(curGroup));
                validateAndApplyOptions(value, curGroup);
            }
        }
        return groups;
    }


    /////////////
    // EXECUTE //
    /////////////

    private static void processGroups(GlobalConfig config, List<ActionGroup> groups) {
        if(!config.groupsParallel) {
            int g = 0;
            for(ActionGroup group : groups) {
                processGroup(config, g, group);
                g++;
            }
        } else {
            List<Thread> threads = new ArrayList<>();
            int g = 0;
            for(ActionGroup group : groups) {
                int gfin = g;
                threads.add(new Thread() {
                    @Override
                    public void run() {
                        processGroup(config, gfin, group);
                    }
                });
                g++;
            }

            for(Thread t : threads) {
                t.start();
            }
            for(Thread t : threads) {
                try {
                    t.join();
                } catch(InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    private static void processGroup(GlobalConfig config, int g, ActionGroup group) {
        if(group.urls.isEmpty()) {
            return;
        }
        if(!group.urlsParallel) {
            int i = 0;
            for(String url : group.urls) {
                processUrl(config, g, group, i, url);
                i++;
            }
        } else {
            int gfin = g;
            List<Thread> threads = new ArrayList<>();
            int i = 0;
            for(String url : group.urls) {
                int ifin = i;
                threads.add(new Thread() {
                    @Override
                    public void run() {
                        processUrl(config, gfin, group, ifin, url);
                    }
                });
                i++;
            }

            for(Thread t : threads) {
                t.start();
            }
            for(Thread t : threads) {
                try {
                    t.join();
                } catch(InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    private static void processUrl(GlobalConfig config, int g, ActionGroup group, int i, String url) {
        String fMsg = createFetchMessage(config.groupsParallel, g, group, i, url);
        System.out.println(fMsg);
        if(config.debug) {
            String rMsg = createResultsMessage(g, i, url, true, -1, null);
            synchronized(printLock) {           // Will ensure output is easier to read
                System.out.println(rMsg);
            }
        } else {
            HttpResource resource = fetch(config, group, url);
            String rMsg = createResultsMessage(g, i, url, false, resource.getResponseCode(), resource.getException());
            String rMsg2 = group.print ? resource.toString() : null;
            synchronized(printLock) {           // Will ensure output is easier to read
                System.out.println(rMsg);
                if(group.print) {
                    System.out.println(rMsg2);
                }
            }
            if(config.saveDirectory != null) {
                String urlCleaned = FileUtil.cleanForFileName(url);
                String baseName = "g" + g + "-u" + i + "-" + StringUtil.max(urlCleaned, 40, false);
                if(group.saveResourceNoContent) {
                    String name = baseName + ".xml";
                    File file = new File(config.saveDirectory, name);
                    try {
                        xStream.toXML(resource, file);
                    } catch(Exception e) {
                        String errMsg = "<Error saving resource to file: " + file + ">";
                        System.out.println(errMsg);
                        System.out.println(ExceptionUtil.toCompleteString(e).trim());
                    }
                }
                if(group.saveResourceContent && resource.hasContent()) {
                    String name = baseName + ".bin";
                    File file = new File(config.saveDirectory, name);
                    try {
                        FileUtil.writeBytes(resource.getContent(), file);
                    } catch(Exception e) {
                        String errMsg = "<Error saving resource content to file: " + file + ">";
                        System.out.println(errMsg);
                        System.out.println(ExceptionUtil.toCompleteString(e).trim());
                    }
                }
                if(group.saveResourceToString) {
                    String name = baseName + ".txt";
                    File file = new File(config.saveDirectory, name);
                    try {
                        String text = resource.toString();
                        FileUtil.writeTextContent(file, text);
                    } catch(Exception e) {
                        String errMsg = "<Error saving resource textual representation to file: " + file + ">";
                        System.out.println(errMsg);
                        System.out.println(ExceptionUtil.toCompleteString(e).trim());
                    }
                }
            }
        }
    }

    private static String createFetchMessage(boolean groupsParallel, int g,
                                             ActionGroup group, int i, String url) {
        return "[Fetching: #" + g + "/" + i + " '" + url + "' using '" + (groupsParallel?"+":"-") + "GFP,"+ group + "']";
    }

    private static String createResultsMessage(int g, int i, String url, boolean debug, int rc, Throwable error) {
        String extra = "";
        if(debug) {
            extra = " (Not Fetched: Debug Enabled)";
        } else {
            extra = " (RC: " + rc + (error != null ? ", Error: " + error.getClass().getSimpleName() : "") + ")";
        }
        return "[Results: #" + g + "/" + i + " '" + url + "'" + extra + "]";
    }

    private static HttpResource fetch(GlobalConfig config, ActionGroup group, String url) {
        Http http = Http.getInstance();

        HttpRequestOptions options = transform(config, group, url);

        // Manual/Abortable Request
        if(group.abortTimeout >= 0) {

            // Create
            HttpRequestWrapper requestWrapper =
                Http.getInstance().create(group.method, url, options);

            // Immediate Abort
            if(group.abortTimeout == 0) {
                requestWrapper.abort();

            // Delayed Abort
            } else {
                Timer timer = new Timer(true);
                timer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        requestWrapper.abort();    // TODO: check if this happens before && after request happening
                    }
                }, group.abortTimeout);
            }

            // Request
            return Http.getInstance().request(requestWrapper);
        }

        // Convenience Request
        switch(group.method) {
            case DELETE:  return http.doDelete(url, options);
            case GET:     return http.doGet(url, options);
            case HEAD:    return http.doHead(url, options);
            case POST:    return http.doPost(url, options);
            case PUT:     return http.doPut(url, options);
            case OPTIONS: return http.doOptions(url, options);
            case TRACE:   return http.doTrace(url, options);
        }
        throw new IllegalStateException();
    }

    // If the options don't already have their proxy set
    // from the group then apply the default proxy to the
    // options.  This gives us much more flexibility than
    // setting the default proxy directly onto
    // Http.getInstance().
    private static HttpRequestOptions transform(GlobalConfig config, ActionGroup group, String url) {
        HttpRequestOptions newOptions = new HttpRequestOptions(group.options);
        if(!group.options.hasProxy()) {
            if(config.defaultProxy != null) {
                boolean exceptionMatch;
                if(config.defaultProxyExRe != null) {
                    String host = UrlUtil.getHostStringOnly(url);
                    exceptionMatch = StringUtil.matches(host, config.defaultProxyExRe, true);
                } else {
                    exceptionMatch = false;
                }
                if(!exceptionMatch) {
                    if(config.defaultProxy.equalsIgnoreCase(SANDIA_PROXY_TAG)) {
                        newOptions.setProxy(SANDIA_SRN_PROXY);
                    } else {
                        newOptions.setProxy(config.defaultProxy);
                    }
                }
            }
        }
        return newOptions;
    }


    //////////////////////
    // VALIDATE & APPLY //
    //////////////////////

    private static String validateAndApplyOptions(String value, ActionGroup curGroup) {
        String kvPat = "([-+]?[:a-z]+)(?:=([:a-z0-9_\\.]+))?";
        String pattern = kvPat + "(?:," + kvPat + ")*";
        if(!StringUtil.matchesIgnoreCase(value, pattern)) {
            return "invalid options format";
        }
        String[] parts = value.split(",");
        for(String part : parts) {
            String[] kv = part.split("=");
            String k = kv[0].toLowerCase();
            String v = kv.length == 1 ? null : kv[1];
            String del = null;
            if(k.startsWith("+") || k.startsWith("-")) {
                del = k.substring(0, 1);
                k = k.substring(1);
            }
            if(booleanGroupOptions.containsKey(k)) {
                BooleanGroupOption option = booleanGroupOptions.get(k);
                boolean enabled;
                if(v != null) {
                    if(!v.equalsIgnoreCase("true") && !v.equalsIgnoreCase("false")) {
                        return "boolean option '" + kv[0] + "' must only have values 'true' or 'false' if supplied";
                    }
                    Boolean b = Boolean.parseBoolean(v);
                    if(del != null) {
                        boolean d = del.equals("+");
                        if(b != d) {
                            return "boolean option '" + kv[0] + "' has inconsistent values";
                        }
                    }
                    enabled = b;
                } else {
                    if(del != null) {
                        enabled = del.equals("+");
                    } else {
                        enabled = true;
                    }
                }

                if(curGroup != null) {
                    HttpRequestOptions options = curGroup.options;
                    option.action.perform(options, curGroup, enabled);
                }

            } else if(valuedGroupOptions.containsKey(k)) {
                ValuedGroupOption option = valuedGroupOptions.get(k);
                if(del != null) {
                    return "non-boolean option '" + kv[0] + "' can't have +/-";
                }
                if(v == null) {
                    return "non-boolean option '" + kv[0] + "' requires value";
                }

                String valMsg = option.validator.validate(v);
                if(valMsg != null) {
                    return valMsg;
                }

                if(curGroup != null) {
                    HttpRequestOptions options = curGroup.options;
                    option.action.perform(options, curGroup, v);
                }

            } else {
                return "invalid option name '" + kv[0] + "'";
            }
        }
        return null;
    }


    ///////////////////
    // INNER CLASSES //
    ///////////////////

    private static interface BooleanGroupOptionAction {
        void perform(HttpRequestOptions options, ActionGroup group, boolean enabled);
    }
    private static interface ValuedGroupOptionAction {
        void perform(HttpRequestOptions options, ActionGroup group, String value);
    }
    private static interface ValuedGroupOptionValidator {
        String validate(String value);
    }

    private static class BooleanGroupOption {
        private String name;
        private String desc;
        private BooleanGroupOptionAction action;

        public BooleanGroupOption(String name, String desc,
                                  BooleanGroupOptionAction action) {
            this.name = name;
            this.desc = desc;
            this.action = action;
        }
    }

    private static class ValuedGroupOption {
        private String name;
        private String desc;
        private ValuedGroupOptionAction action;
        private ValuedGroupOptionValidator validator;

        public ValuedGroupOption(String name, String desc,
                                 ValuedGroupOptionAction action, ValuedGroupOptionValidator validator) {
            this.name = name;
            this.desc = desc;
            this.action = action;
            this.validator = validator;
        }
    }

    private static class GlobalConfig {
        boolean debug;
        boolean groupsParallel;
        String defaultProxy;
        String defaultProxyExRe;
        File saveDirectory;

        public GlobalConfig setDebug(boolean debug) {
            this.debug = debug;
            return this;
        }
        public GlobalConfig setGroupsParallel(boolean groupsParallel) {
            this.groupsParallel = groupsParallel;
            return this;
        }
        public GlobalConfig setDefaultProxy(String defaultProxy) {
            this.defaultProxy = defaultProxy;
            return this;
        }
        public GlobalConfig setDefaultProxyExRe(String defaultProxyExRe) {
            this.defaultProxyExRe = defaultProxyExRe;
            return this;
        }
        public GlobalConfig setSaveDirectory(File saveDirectory) {
            this.saveDirectory = saveDirectory;
            return this;
        }
    }

    private static class ActionGroup {


        ////////////
        // FIELDS //
        ////////////

        HttpRequestOptions options;
        RequestMethod method;
        boolean urlsParallel;
        boolean print;
        boolean saveResourceNoContent;
        boolean saveResourceContent;
        boolean saveResourceToString;
        int abortTimeout = -1;             // -1 is N/A value, whereas 0 is a real value that means abort before you even start the request
        List<String> urls = new ArrayList<>();


        //////////////////
        // CONSTRUCTORS //
        //////////////////

        private ActionGroup() {
            options        = new HttpRequestOptions();
            method         = RequestMethod.GET;
            print          = true;
            urlsParallel   = false;
        }
        private ActionGroup(ActionGroup other) {
            options        = new HttpRequestOptions(other.options);
            method         = other.method;
            print          = other.print;
            urlsParallel   = other.urlsParallel;
        }


        //////////
        // MISC //
        //////////

        public void setMethod(String methodStr) {
            method = RequestMethod.valueOf(methodStr);    // Must be UPPER CASE
        }
        public void setAbortTimeout(int abortTimeout) {
            this.abortTimeout = abortTimeout;
        }

        ////////////////
        // OVERRIDDEN //
        ////////////////

        @Override
        public String toString() {
            String o = options.toString();
            if(o.equals("(Default)")) {
                o = " [Default Request Options]";
            }
            return "M=" + method + ",ATO=" + abortTimeout + "," +
                (print?"+":"-") + "PR" +
                (urlsParallel?"+":"-") + "UFP" +
                (saveResourceNoContent ? "+" : "-") + "SVR" +
                (saveResourceContent   ? "+" : "-") + "SVRC" +
                (saveResourceToString  ? "+" : "-") + "SVRT" + o;
        }
    }
}
