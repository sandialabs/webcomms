package gov.sandia.webcomms.http;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import javax.swing.ImageIcon;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.junit.ClassRule;
import org.junit.Test;

import gov.sandia.avondale.lighthouse.agent.AttemptLighthouseTestsRule;
import gov.sandia.avondale.lighthouse.agent.LighthouseConstants;
import gov.sandia.avondale.lighthouse.agent.LighthouseUtil;
import gov.sandia.webcomms.http.consumeec.HttpConsumeEntityCriteria;
import gov.sandia.webcomms.http.consumeec.HttpConsumeEntityCriteriaGenerator;
import gov.sandia.webcomms.http.consumeec.HttpConsumeEntityCriteriaParams;
import gov.sandia.webcomms.http.consumeec.HttpConsumeEntityCriteriaParamsPanel;
import gov.sandia.webcomms.http.errors.HttpNoExtensionLoadedException;
import gov.sandia.webcomms.http.options.HttpRequestOptions;
import gov.sandia.webcomms.http.rsc.HttpResource;
import replete.plugins.PluginManager;
import replete.plugins.test.DiagnosticsPlugin;

// This class tests whether or not the logic that
// implements the consume entity criteria feature
// within the Http class is working.
public class HttpConsumeEntityCriteriaTest extends HttpTestBase {

    // This seemingly unused field instructs JUnit 4 to check
    // whether it should skip all the tests in this class or not.
    // Currently this is controlled by a single global field
    // instead of performing a connectivity check (because a
    // failed connect might be a valid failing test you want
    // to know about).
    @ClassRule
    public static AttemptLighthouseTestsRule attemptLighthouseTestsRule
        = new AttemptLighthouseTestsRule();

    @Test
    public void base() {
        String url = LighthouseUtil.getCurrent("/ping");
        HttpRequestOptions options = new HttpRequestOptions();
        HttpResource resource = Http.getInstance().doGet(url, options);
        baseAndAlwaysTest(resource);
    }

    @Test
    public void misconfigured() {
        String url = LighthouseUtil.getCurrent("/ping");
        HttpRequestOptions options = new HttpRequestOptions()
            .setConsumeEntityCriteriaParams(new NeverConsumeEntityCriteriaParams())
        ;
        HttpResource resource = Http.getInstance().doGet(url, options);
        assertEquals(200, resource.getResponseCode());
        assertFalse(resource.hasContent());        // TODO: HttpResource too intertwined with Mongo
        assertEquals(-1, resource.getReturnedSize());
        assertEquals(24, resource.getReportedSize());
        assertNotNull(resource.getException());
        assertEquals(HttpNoExtensionLoadedException.class, resource.getException().getClass());
        assertEquals("[Consume Entity Criteria] No extension loaded for class '" + NeverConsumeEntityCriteriaParams.class.getName() + "'", resource.getException().getMessage());
        assertTrue(resource.isError());
        assertFalse(resource.isContentDeclined());
    }

    @Test
    public void never() {
        PluginManager.reset();        // Because always() might have been called first
        PluginManager.initialize(
            new DiagnosticsPlugin(
                new NeverConsumeEntityCriteriaGenerator()
            )
        );
        String url = LighthouseUtil.getCurrent("/ping");
        HttpRequestOptions options = new HttpRequestOptions()
            .setConsumeEntityCriteriaParams(new NeverConsumeEntityCriteriaParams())
        ;
        HttpResource resource = Http.getInstance().doGet(url, options);
        assertEquals(200, resource.getResponseCode());
        assertFalse(resource.hasContent());        // TODO: HttpResource too intertwined with Mongo
        assertEquals(-1, resource.getReturnedSize());
        assertEquals(24, resource.getReportedSize());
        assertNull(resource.getException());
        assertFalse(resource.isError());
        assertTrue(resource.isContentDeclined());
    }

    @Test
    public void always() {
        PluginManager.reset();        // Because never() might have been called first
        PluginManager.initialize(
            new DiagnosticsPlugin(
                new AlwaysConsumeEntityCriteriaGenerator()
            )
        );
        String url = LighthouseUtil.getCurrent("/ping");
        HttpRequestOptions options = new HttpRequestOptions()
            .setConsumeEntityCriteriaParams(new AlwaysConsumeEntityCriteriaParams())
        ;
        HttpResource resource = Http.getInstance().doGet(url, options);
        baseAndAlwaysTest(resource);
    }

    public void baseAndAlwaysTest(HttpResource resource) {
        assertEquals(200, resource.getResponseCode());
        assertTrue(resource.hasContent());
        assertEquals(24, resource.getContent().length);
        assertEquals(24, resource.getReturnedSize());
        assertEquals(24, resource.getReportedSize());
        assertEquals(LighthouseConstants.PING_KNOWN_STRING, resource.getContentAsString());
        assertNull(resource.getException());
        assertFalse(resource.isError());
        assertFalse(resource.isContentDeclined());
    }


    ///////////////////
    // INNER CLASSES //
    ///////////////////

    private class NeverConsumeEntityCriteriaParams extends HttpConsumeEntityCriteriaParams {
        // Empty
    }
    private class NeverConsumeEntityCriteria extends HttpConsumeEntityCriteria {
        public NeverConsumeEntityCriteria(HttpConsumeEntityCriteriaParams params) {
            super(params);
        }
        @Override
        public boolean doConsume(HttpRequestWrapper requestWrapper, HttpResponse response,
                                 HttpEntity entity) {
            return false;
        }
    }
    private class NeverConsumeEntityCriteriaGenerator
            extends HttpConsumeEntityCriteriaGenerator<NeverConsumeEntityCriteriaParams> {
        @Override
        public String getName() {
            return null;
        }
        @Override
        public String getDescription() {
            return null;
        }
        @Override
        public ImageIcon getIcon() {
            return null;
        }
        @Override
        public Class<?>[] getCoordinatedClasses() {
            return new Class[] {
                NeverConsumeEntityCriteriaParams.class,
                NeverConsumeEntityCriteria.class
            };
        }
        @Override
        public NeverConsumeEntityCriteriaParams createParams() {
            return new NeverConsumeEntityCriteriaParams();
        }
        @Override
        public HttpConsumeEntityCriteriaParamsPanel createParamsPanel(Object... args) {
            return null;
        }
        @Override
        public NeverConsumeEntityCriteria createCriteria(NeverConsumeEntityCriteriaParams params) {
            return new NeverConsumeEntityCriteria(params);
        }
    }
    private class AlwaysConsumeEntityCriteriaParams extends HttpConsumeEntityCriteriaParams {
        // Empty
    }
    private class AlwaysConsumeEntityCriteria extends HttpConsumeEntityCriteria {
        public AlwaysConsumeEntityCriteria(HttpConsumeEntityCriteriaParams params) {
            super(params);
        }
        @Override
        public boolean doConsume(HttpRequestWrapper requestWrapper, HttpResponse response,
                                 HttpEntity entity) {
            return true;
        }
    }
    private class AlwaysConsumeEntityCriteriaGenerator
            extends HttpConsumeEntityCriteriaGenerator<AlwaysConsumeEntityCriteriaParams> {
        @Override
        public String getName() {
            return null;
        }
        @Override
        public String getDescription() {
            return null;
        }
        @Override
        public ImageIcon getIcon() {
            return null;
        }
        @Override
        public Class<?>[] getCoordinatedClasses() {
            return new Class[] {
                AlwaysConsumeEntityCriteriaParams.class,
                AlwaysConsumeEntityCriteria.class
            };
        }
        @Override
        public AlwaysConsumeEntityCriteriaParams createParams() {
            return new AlwaysConsumeEntityCriteriaParams();
        }
        @Override
        public HttpConsumeEntityCriteriaParamsPanel createParamsPanel(Object... args) {
            return null;
        }
        @Override
        public AlwaysConsumeEntityCriteria createCriteria(AlwaysConsumeEntityCriteriaParams params) {
            return new AlwaysConsumeEntityCriteria(params);
        }
    }
}
