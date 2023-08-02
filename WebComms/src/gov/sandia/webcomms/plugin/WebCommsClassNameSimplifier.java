package gov.sandia.webcomms.plugin;

import replete.text.StringUtil;
import replete.ui.ClassNameSimplifier;

public class WebCommsClassNameSimplifier implements ClassNameSimplifier {
    private static final String prefix = "gov.sandia.webcomms";

    @Override
    public boolean appliesTo(String className) {
        return className.startsWith(prefix);
    }

    @Override
    public String simplifyAndMarkupClassName(String className) {
        String rest = StringUtil.removeStart(className, prefix);
        return "<i>g</i>.<i>s</i>.<b>W</b>" + rest;
    }

}
