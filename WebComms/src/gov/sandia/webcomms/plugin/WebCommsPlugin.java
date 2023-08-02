package gov.sandia.webcomms.plugin;

import javax.swing.ImageIcon;

import gov.sandia.webcomms.SoftwareVersion;
import gov.sandia.webcomms.http.allowrc.HttpAllowRedirectCriteriaGenerator;
import gov.sandia.webcomms.http.allowrc.nofollow.NoFollowHttpAllowRedirectCriteriaGenerator;
import gov.sandia.webcomms.http.consumeec.HttpConsumeEntityCriteriaGenerator;
import gov.sandia.webcomms.http.consumeec.mimetype.MimeTypeHttpConsumeEntityCriteriaGenerator;
import gov.sandia.webcomms.http.preemptors.HttpRequestPreemptorGenerator;
import gov.sandia.webcomms.http.preemptors.blank.BlankHttpRequestPreemptorGenerator;
import replete.plugins.ExtensionPoint;
import replete.plugins.Plugin;

public class WebCommsPlugin implements Plugin {

    @Override
    public String getName() {
        return "WebComms";
    }

    @Override
    public String getVersion() {
        return SoftwareVersion.get().getFullVersionString();
    }

    @Override
    public String getProvider() {
        return "Sandia National Laboratories";
    }

    @Override
    public ImageIcon getIcon() {
        return null;
    }

    @Override
    public String getDescription() {
        return "This plug-in provides the base WebComms platform extension points and basic default extensions.";
    }

    @Override
    public Class<? extends ExtensionPoint>[] getExtensionPoints() {
        return new Class[] {
            HttpAllowRedirectCriteriaGenerator.class,
            HttpConsumeEntityCriteriaGenerator.class,
            HttpRequestPreemptorGenerator.class
        };
    }

    @Override
    public ExtensionPoint[] getExtensions() {
        return new ExtensionPoint[] {

            // [Replete] ClassNameSimplifier
            new WebCommsClassNameSimplifier(),

            // [Replete] SoftwareVersionLookup
            new WebCommsSoftwareVersionLookup(),

            // HttpAllowRedirectCriteriaGenerator
            new NoFollowHttpAllowRedirectCriteriaGenerator(),

            // HttpConsumeEntityCriteriaGenerator
            new MimeTypeHttpConsumeEntityCriteriaGenerator(),

            // HttpRequestPreemptorGenerator
            new BlankHttpRequestPreemptorGenerator()
        };
    }

    @Override
    public void start() {
    }
}
