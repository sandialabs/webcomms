package gov.sandia.webcomms.http;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;

import org.junit.Test;

import gov.sandia.webcomms.http.consumeec.mimetype.MimeTypeHttpConsumeEntityCriteria;
import gov.sandia.webcomms.http.consumeec.mimetype.MimeTypeHttpConsumeEntityCriteriaParams;
import gov.sandia.webcomms.http.consumeec.mimetype.PatternPrivilege;
import gov.sandia.webcomms.http.rsc.HttpResource;
import replete.text.patterns.PatternInterpretation;
import replete.text.patterns.PatternInterpretationType;

public class MimeTypeHttpConsumeEntityCriteriaTest {

    public static final PatternInterpretation LITERAL = new PatternInterpretation()
        .setType(PatternInterpretationType.LITERAL)
        .setCaseSensitive(false)
        .setWholeMatch(true)
    ;
    public static final PatternInterpretation WILDCARDS = new PatternInterpretation()
        .setType(PatternInterpretationType.WILDCARDS)
        .setCaseSensitive(false)
        .setWholeMatch(false)
    ;


    @Test
    public void allowLiteral() {
        MimeTypeHttpConsumeEntityCriteriaParams params = new MimeTypeHttpConsumeEntityCriteriaParams()
            .setPrivilege(PatternPrivilege.ALLOW)
        ;

        params.setPatternInterpretation(LITERAL);
        test(params, "text/html", false);

        params
            .setPatternInterpretation(LITERAL)
            .setPatterns(Arrays.asList("text/htmlx"))
        ;
        test(params, "text/html", false);

        params
            .setPatternInterpretation(LITERAL)
            .setPatterns(Arrays.asList("text/html "))
        ;
        test(params, "text/html", false);

        params
            .setPatternInterpretation(LITERAL)
            .setPatterns(Arrays.asList("text/html"))
        ;
        test(params, "text/html", true);

        params
            .setPatternInterpretation(LITERAL)
            .setPatterns(Arrays.asList("TEXT/HTML"))
        ;
        test(params, "text/html", true);

        params
            .setPatternInterpretation(LITERAL)
            .setPatterns(Arrays.asList("text/html", "image/png"))
        ;
        test(params, "image/png", true);
    }

    @Test
    public void allowLooseWildcards() {
        MimeTypeHttpConsumeEntityCriteriaParams params = new MimeTypeHttpConsumeEntityCriteriaParams()
            .setPrivilege(PatternPrivilege.ALLOW)
        ;

        params
            .setPatternInterpretation(WILDCARDS)
            .setPatterns(Arrays.asList("TEXT/*"))
        ;
        test(params, "text/html", true);

        params
            .setPatternInterpretation(WILDCARDS)
            .setPatterns(Arrays.asList("text/html", "/png"))
        ;
        test(params, "image/png", true);

        params
            .setPatternInterpretation(WILDCARDS)
            .setPatterns(Arrays.asList("a", "/png"))
        ;
        test(params, "tad", true);
    }

    @Test
    public void disallowLiteral() {
        MimeTypeHttpConsumeEntityCriteriaParams params = new MimeTypeHttpConsumeEntityCriteriaParams()
            .setPrivilege(PatternPrivilege.DENY)
        ;

        params.setPatternInterpretation(LITERAL);
        test(params, "text/html", true);

        params
            .setPatternInterpretation(LITERAL)
            .setPatterns(Arrays.asList("text/htmlx"))
        ;
        test(params, "text/html", true);

        params
            .setPatternInterpretation(LITERAL)
            .setPatterns(Arrays.asList("text/html "))
        ;
        test(params, "text/html", true);

        params
            .setPatternInterpretation(LITERAL)
            .setPatterns(Arrays.asList("text/html"))
        ;
        test(params, "text/html", false);

        params
            .setPatternInterpretation(LITERAL)
            .setPatterns(Arrays.asList("TEXT/HTML"))
        ;
        test(params, "text/html", false);

        params
            .setPatternInterpretation(LITERAL)
            .setPatterns(Arrays.asList("text/html", "image/png"))
        ;
        test(params, "image/png", false);
    }

    @Test
    public void denyLooseWildcards() {
        MimeTypeHttpConsumeEntityCriteriaParams params = new MimeTypeHttpConsumeEntityCriteriaParams()
            .setPrivilege(PatternPrivilege.DENY)
        ;

        params
            .setPatternInterpretation(WILDCARDS)
            .setPatterns(Arrays.asList("TEXT/*"))
        ;
        test(params, "text/html", false);

        params
            .setPatternInterpretation(WILDCARDS)
            .setPatterns(Arrays.asList("text/html", "/png"))
        ;
        test(params, "image/png", false);

        params
            .setPatternInterpretation(WILDCARDS)
            .setPatterns(Arrays.asList("a", "/png"))
        ;
        test(params, "tad", false);
    }

    private void test(MimeTypeHttpConsumeEntityCriteriaParams params, String mimeType, boolean expected) {
        MimeTypeHttpConsumeEntityCriteria criteria = new MimeTypeHttpConsumeEntityCriteria(params);
        boolean actual = criteria.doConsume(wrap(mimeType), null, null);
        assertEquals(expected, actual);
    }

    private HttpRequestWrapper wrap(String mimeType) {
        HttpResource resource = new HttpResource("http://example.com")
            .setContentType(mimeType)
        ;
        return new HttpRequestWrapper(null, resource, null);
    }

}
