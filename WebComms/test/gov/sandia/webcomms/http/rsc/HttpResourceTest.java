package gov.sandia.webcomms.http.rsc;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Random;

import org.junit.Test;

import gov.sandia.webcomms.http.Http;
import replete.io.FileUtil;
import replete.text.StringFilterer;
import replete.text.StringUtil;
import replete.util.CodeUtil;
import replete.xstream.MetadataXStream;
import replete.xstream.XStreamWrapper;

public class HttpResourceTest {

    @Test
    public void basicFields() {
        Random rand = new Random();
        String url = "http://www.sandia.gov";
        String contentType = "contentType";
        String contentEncoding = "contentEncoding";
        int responseCode = rand.nextInt();
        String responseMessage = "responseMessage";
        byte[] content = "my awesome content".getBytes();

        HttpResource resource = new HttpResource(url)
            .setCleanedUrl(Http.getInstance().clean(url))
            .setContentType(contentType)
            .setContentEncoding(contentEncoding)
            .setResponseCode(responseCode)
            .setResponseMessage(responseMessage)
            .setContent(content)
        ;

        assertEquals(url + "/", resource.getUrl());
        assertEquals(contentType, resource.getContentType());
        assertEquals(contentEncoding, resource.getContentEncoding());
        assertEquals(responseCode, resource.getResponseCode());
        assertEquals(responseMessage, resource.getResponseMessage());
        assertEquals("my awesome content", new String(resource.getContent()));
    }

    @Test
    public void content() {
        Random rand = new Random();
        String url = "http://www.sandia.gov";
        String contentType = "contentType";
        String contentEncoding = "contentEncoding";
        int responseCode = rand.nextInt();
        String responseMessage = "responseMessage";
        byte[] content = "my awesome content".getBytes();

        HttpResource instance = new HttpResource(url)
            .setCleanedUrl(Http.getInstance().clean(url))
            .setContentType(contentType)
            .setContentEncoding(contentEncoding)
            .setResponseCode(responseCode)
            .setResponseMessage(responseMessage)
            .setContent(content)
        ;

        instance.setContent("my new awesome content".getBytes());

        assertEquals("my new awesome content", new String(instance.getContent()));
    }

    @Test
    public void header() {
        Random rand = new Random();
        String url = "http://www.sandia.gov";
        String contentType = "contentType";
        String contentEncoding = "contentEncoding";
        int responseCode = rand.nextInt();
        String responseMessage = "responseMessage";
        byte[] content = "my awesome content".getBytes();

        HttpResource instance = new HttpResource(url)
            .setCleanedUrl(Http.getInstance().clean(url))
            .setContentType(contentType)
            .setContentEncoding(contentEncoding)
            .setResponseCode(responseCode)
            .setResponseMessage(responseMessage)
            .setContent(content)
        ;

        instance.addResponseHeader("name", "value");

        assertTrue(instance.hasResponseHeader("name"));
        assertSame("value", instance.getResponseHeader("name"));
    }

    @Test
    public void render() throws Exception {

//        rebuildExpectedFiles("cnn-err", "http://cnn.com");   // For this one go put an ExceptionUtil.toss() statement in Http.requestIntoResource
//        rebuildExpectedFiles("null", null);
//        rebuildExpectedFiles("time-sec", "https://time.org");
//        if(true) {
//            return;
//        }

        String[] names = {"cnn-all", "cnn-none", "cnn-err", "null", "time-sec"};
        for(String name : names) {
            InputStream rscIs = HttpResourceTest.class.getResourceAsStream("input/" + name + ".exp.rsc.xml");
            HttpResource rsc = new MetadataXStream().fromXMLExt(rscIs);
            long T = System.currentTimeMillis();
            String actual = rsc.toString();
            System.out.println(name + " duration: " + (System.currentTimeMillis() - T));
            actual = actual.trim();
            // TODO: one day need a sed-like replacer so we're not just removing lines wholesale.
//            StringReplacer replacer = new StringReplacer("MIME Type \\(Tika\\):   ([^\n]+)\n", (m, g) -> {
//                System.out.println("{" + m + ":" + Arrays.toString(g) + "}");
//                return "X";
//            });
//            actual = replacer.replace(actual);
            StringFilterer mimeFilterer = new StringFilterer("MIME Type \\(Tika\\):").setInverse(true);
            StringFilterer cntFilterer = new StringFilterer("Content:").setInverse(true);
            actual = mimeFilterer.filter(actual);
            actual = cntFilterer.filter(actual);
            InputStream expIs = HttpResourceTest.class.getResourceAsStream("input/" + name + ".exp.ts.txt");
            String expected = FileUtil.getTextContent(expIs).trim();
            expected = mimeFilterer.filter(expected);
            expected = cntFilterer.filter(expected);
            if(!actual.equals(expected)) {
                try {
                    File testDir = new File(CodeUtil.getCodeSourcePath().getParentFile(), "test");
                    File inputDir = new File(testDir, "gov/sandia/webcomms/http/rsc/input");
                    FileUtil.writeTextContent(new File(inputDir, name + ".act.ts.txt"), actual);
                } catch(Exception e) {
                    e.printStackTrace();
                }
                System.out.println(name);
                System.out.println(StringUtil.diff(expected, actual, 20));
            }
            assertEquals("Failure: " + name, expected, actual);
        }
    }

    private void rebuildExpectedFiles(String name, String url) throws IOException {

        // This code is used to rebuild *.rsc.xml and *.exp.txt files if
        // the code has changed and the expected values need to be updated.
        Http.getInstance().useSandiaProxy();
        HttpResource resource = Http.getInstance().doGet(url);
        System.out.println(resource.toString());

        File testDir  = new File(CodeUtil.getCodeSourcePath().getParentFile(), "test");
        File inputDir = new File(testDir, "gov/sandia/webcomms/http/rsc/input");
        File xmlFile   = new File(inputDir, name + ".exp.rsc-NEW.xml");
        File tsFile    = new File(inputDir, name + ".exp.ts-NEW.txt");

        XStreamWrapper.writeToFile(resource, xmlFile);
        FileUtil.writeTextContent(tsFile, resource.toString());
    }
}
