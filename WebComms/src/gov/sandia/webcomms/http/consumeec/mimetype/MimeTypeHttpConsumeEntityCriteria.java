package gov.sandia.webcomms.http.consumeec.mimetype;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;

import gov.sandia.webcomms.http.HttpRequestWrapper;
import gov.sandia.webcomms.http.consumeec.HttpConsumeEntityCriteria;
import replete.collections.Pair;
import replete.text.StringUtil;
import replete.text.patterns.PatternInterpretation;
import replete.text.patterns.PatternUtil;

public class MimeTypeHttpConsumeEntityCriteria
        extends HttpConsumeEntityCriteria<MimeTypeHttpConsumeEntityCriteriaParams> {


    /////////////////
    // CONSTRUCTOR //
    /////////////////

    public MimeTypeHttpConsumeEntityCriteria(MimeTypeHttpConsumeEntityCriteriaParams params) {
        super(params);
    }


    ////////////////
    // OVERRIDDEN //
    ////////////////

    // It's important to know that just like almost all of the fields
    // in HttpResource, the content type field and thus the computed
    // MIME type can be null.  Like other fields, this could be because
    // it was never even attempted to be populated due to an error
    // before the HTTP request was made or because there was no error
    // but that header was just not returned from the remote server
    // in the response.
    @Override
    public boolean doConsume(HttpRequestWrapper requestWrapper, HttpResponse response, HttpEntity entity) {
        System.out.println(requestWrapper.getResource());
        boolean matches = false;
        String mimeType = requestWrapper.getResource().getMimeTypeFromContentType();
        if(mimeType != null) {
            for(String patternWithTag : params.getPatterns()) {

                // Here the default is actually whatever the USER specified in the
                // panel, as it is obvious that it is meant to apply to all the patterns
                // in the list.  The user will probably never additionally add a
                // pattern interpretation tag to an item in the list, but at least it's
                // implemented anyway, just in case.
                PatternInterpretation defaultInterp = params.getPatternInterpretation();
                Pair<String, PatternInterpretation> result =
                    PatternUtil.parsePatternInterpretationTag(patternWithTag, defaultInterp);
                String patternAny = result.getValue1();
                PatternInterpretation interp = result.getValue2();
                String patternRegex = PatternUtil.convertToRegex(patternAny, interp);
                if(StringUtil.matches(mimeType, patternRegex, params.getPatternInterpretation())) {
                    matches = true;
                    break;
                }
            }
        }
        return (params.getPrivilege() == PatternPrivilege.DENY) ? !matches : matches;
    }
}
