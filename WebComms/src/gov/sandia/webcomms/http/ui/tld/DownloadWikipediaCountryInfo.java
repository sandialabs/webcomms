package gov.sandia.webcomms.http.ui.tld;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.util.Map;
import java.util.TreeMap;

import gov.sandia.webcomms.http.Http;
import gov.sandia.webcomms.http.rsc.HttpResource;
import gov.sandia.webcomms.plugin.WebCommsPlugin;
import replete.plugins.PluginManager;
import replete.plugins.RepletePlugin;
import replete.text.StringUtil;
import replete.util.User;
import replete.xstream.VersioningMetadataXStream;

public class DownloadWikipediaCountryInfo {

    public static void main(String[] args) {
        PluginManager.initialize(
            RepletePlugin.class,
            WebCommsPlugin.class
        );

//        Http.getInstance().useSandiaProxy();
//        HttpResource tldHtml = Http.getInstance().doGet(
//            "https://en.wikipedia.org/wiki/List_of_Internet_top-level_domains"
//        );
//        if(tldHtml.isError()) {
//            System.err.println("Error!");
//            System.err.println(tldHtml);
//            return;
//        }
//        if(true) {
//            FileUtil.writeBytes(tldHtml.getContent(), User.getDesktop("tld.html"));
//            System.out.println("GOT IT!");
//            return;
//        }
        Map<String, CountryCode> codes = new TreeMap<>();

        try(BufferedReader reader = new BufferedReader(new FileReader(User.getDesktop("tld.html")))) {
//        try(BufferedReader reader = new BufferedReader(new StringReader(tldHtml.getContentAsString()))) {
            String line;
            boolean inIntlSection = false;
            while((line = reader.readLine()) != null) {
                if(line.contains("Explanation of the code when it is not self-evident from the English name of the country")) {
                    inIntlSection = true;
                    continue;
                }
                if(line.contains("id=\"Notes\">Notes</span>")) {
                    break;
                }
                if(!inIntlSection) {
                    continue;
                }

                // Two different format at this time:
                //     <td><span id="uk"></span><a href="/wiki/.uk" title=".uk">.uk</a></td>
                //     <td><a href="/wiki/.us" title=".us">.us</a></td>

                String[] parts = StringUtil.extractCaptures(line, "<td>(?:<span id=\".*\"></span>)?<a href=.*title=\"\\..*\">\\.(.*)</a></td>");
                if(parts != null) {
                    line = reader.readLine();
                    String tld = parts[0];
                    int a = line.indexOf("</a>");
                    int b = line.lastIndexOf('>', a);
                    String countryName = line.substring(b + 1, a);
                    String iconUrl = "http:" + StringUtil.extractPart(line, "src=\"", "\"");
                    CountryCode code = new CountryCode()
                        .setTld(tld)
                        .setCountryName(countryName)
                        .setIconRemoteUrl(iconUrl)
                        .setIconLocalFileName(tld + ".png")
                    ;
                    codes.put(tld, code);
                }
            }

            CountryCode usCode = codes.get("us");
            CountryCode govCode = new CountryCode()
                .setTld("gov")
                .setCountryName(usCode.getCountryName())
                .setIconRemoteUrl(usCode.getIconRemoteUrl())
                .setIconLocalFileName(usCode.getIconLocalFileName())
            ;
            codes.put("gov", govCode);
            CountryCode milCode = new CountryCode()
                .setTld("mil")
                .setCountryName(usCode.getCountryName())
                .setIconRemoteUrl(usCode.getIconRemoteUrl())
                .setIconLocalFileName(usCode.getIconLocalFileName())
            ;
            codes.put("mil", milCode);
            CountryCode eduCode = new CountryCode()
                .setTld("edu")
                .setCountryName(usCode.getCountryName())
                .setIconRemoteUrl(usCode.getIconRemoteUrl())
                .setIconLocalFileName(usCode.getIconLocalFileName())
            ;
            codes.put("edu", eduCode);

            for(String tld : codes.keySet()) {
                CountryCode code = codes.get(tld);
                System.out.println(code);
            }
            System.out.println(codes.size() + " Country Code Top-Level Domains Found");

            File tldDir = new File("C:\\Users\\dtrumbo\\work\\eclipse-main\\WebComms\\src\\gov\\sandia\\webcomms\\http\\ui\\tld");
            VersioningMetadataXStream xStream = new VersioningMetadataXStream();
            xStream.toXML(codes, new File(tldDir, "country-codes.xml"));

            File flagDir = new File("C:\\Users\\dtrumbo\\work\\eclipse-main\\WebComms\\src\\gov\\sandia\\webcomms\\http\\ui\\images\\flags");
            downloadIcons(flagDir, codes);

        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    private static void downloadIcons(File flagDir, Map<String, CountryCode> codes) throws Exception {
        for(CountryCode code : codes.values()) {
            System.out.println("Downloading " + code);
            HttpResource reso = Http.getInstance().doGet(code.getIconRemoteUrl());
            if(reso.isError()) {
                System.err.println("    ERROR! " + reso.getResponseCode() + " / " + reso.getException());
                continue;
            }
            File imgFile = new File(flagDir, code.getIconLocalFileName());
            try(FileOutputStream fos = new FileOutputStream(imgFile)) {
                fos.write(reso.getContent());
            } catch(Exception e) {
                e.printStackTrace();
            }
            System.out.println("    SUCCESS");
        }
    }
}
