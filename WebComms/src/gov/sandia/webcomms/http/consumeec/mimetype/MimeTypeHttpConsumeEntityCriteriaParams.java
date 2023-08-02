package gov.sandia.webcomms.http.consumeec.mimetype;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import gov.sandia.webcomms.http.consumeec.HttpConsumeEntityCriteriaParams;
import replete.text.patterns.PatternInterpretation;
import replete.text.patterns.PatternInterpretationType;

public class MimeTypeHttpConsumeEntityCriteriaParams extends HttpConsumeEntityCriteriaParams {


    ////////////
    // FIELDS //
    ////////////

    // Defaults

    public static final PatternPrivilege      DEFAULT_PRIVILEGE              = PatternPrivilege.DENY;
    public static final List<String>          DEFAULT_PATTERNS               = Collections.unmodifiableList(new ArrayList<>());
    public static final PatternInterpretation DEFAULT_PATTERN_INTERPRETATION = new PatternInterpretation()
        .setType(PatternInterpretationType.WILDCARDS)
        .setCaseSensitive(false)
        .setWholeMatch(false)
    ;

    // Core

    private PatternPrivilege      privilege             = DEFAULT_PRIVILEGE;
    private List<String>          patterns              = DEFAULT_PATTERNS;
    private PatternInterpretation patternInterpretation = DEFAULT_PATTERN_INTERPRETATION;


    //////////////////////////
    // ACCESSORS / MUTATORS //
    //////////////////////////

    // Accessors

    public PatternPrivilege getPrivilege() {
        return privilege;
    }
    public List<String> getPatterns() {
        return patterns;
    }
    public PatternInterpretation getPatternInterpretation() {
        return patternInterpretation;
    }

    // Mutators

    public MimeTypeHttpConsumeEntityCriteriaParams setPrivilege(PatternPrivilege privilege) {
        this.privilege = privilege;
        return this;
    }
    public MimeTypeHttpConsumeEntityCriteriaParams setPatterns(List<String> patterns) {
        this.patterns = patterns;
        return this;
    }
    public MimeTypeHttpConsumeEntityCriteriaParams setPatternInterpretation(PatternInterpretation patternInterpretation) {
        this.patternInterpretation = patternInterpretation;
        return this;
    }


    ////////////////
    // OVERRIDDEN //
    ////////////////

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result +
            ((patternInterpretation == null) ? 0 : patternInterpretation.hashCode());
        result = prime * result + ((patterns == null) ? 0 : patterns.hashCode());
        result = prime * result + ((privilege == null) ? 0 : privilege.hashCode());
        return result;
    }
    @Override
    public boolean equals(Object obj) {
        if(this == obj) {
            return true;
        }
        if(!super.equals(obj)) {
            return false;
        }
        if(getClass() != obj.getClass()) {
            return false;
        }
        MimeTypeHttpConsumeEntityCriteriaParams other =
            (MimeTypeHttpConsumeEntityCriteriaParams) obj;
        if(patternInterpretation != other.patternInterpretation) {
            return false;
        }
        if(patterns == null) {
            if(other.patterns != null) {
                return false;
            }
        } else if(!patterns.equals(other.patterns)) {
            return false;
        }
        if(privilege != other.privilege) {
            return false;
        }
        return true;
    }
}
