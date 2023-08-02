package gov.sandia.webcomms.http;

import org.junit.Before;

import gov.sandia.webcomms.logging.LogUtil;
import replete.logging.LoggingInitializer;

public class HttpTestBase {   // Cannot end in word "Test" or JUnit will process it differently.
    @Before
    public void setup() {

        // This prevents spurious console output during unit tests.
        LoggingInitializer.init(LogUtil.INTERNAL_PROPERTIES_PATH);
    }
}
