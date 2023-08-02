package gov.sandia.webcomms.http.internal;

import java.io.IOException;
import java.security.cert.X509Certificate;

import javax.net.ssl.SSLException;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;

import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.conn.ssl.X509HostnameVerifier;

import gov.sandia.webcomms.http.Http;
import gov.sandia.webcomms.http.rsc.SecurityInfo;
import replete.errors.ExceptionUtil;

public class InternalHostnameVerifier implements X509HostnameVerifier {


    ////////////
    // FIELDS //
    ////////////

    private Http http;

    // Default verifier HTTP client uses without re-registration of a
    // new HTTPS Scheme object.  Can't use inheritance due to some
    // "final" method action...
    private X509HostnameVerifier defaultVerifier =
        SSLSocketFactory.BROWSER_COMPATIBLE_HOSTNAME_VERIFIER;  // Also possible: All, Strict


    //////////////////
    // CONSTRUCTORS //
    //////////////////

    public InternalHostnameVerifier(Http http) {
        this.http = http;
    }


    ////////////////
    // OVERRIDDEN //
    ////////////////

    // Only one being called apparently.
    @Override
    public void verify(String hostName, SSLSocket socket) throws IOException {
        RequestTrace trace = http.getRequestTrace();
        trace.debugBlockOpen("HOSTNAME CHECK: " + hostName + ":R" + socket.getPort() + "|L" + socket.getLocalPort());

        try {  // Debug try-finally block (w/ indent)
            if(trace.getOptions().isSaveSecurity()) {
                SecurityInfo secInfo = trace.getResource().getSecurityInfo();

                String newExisting = (secInfo == null ? "!New => " : "~Existing => ");

                if(secInfo == null) {
                    secInfo = new SecurityInfo();
                    trace.getResource().setSecurityInfo(secInfo);
                }
                secInfo.setHostName(hostName);

                trace.debugStep("SAVE SEC " + newExisting + secInfo);
            }

            try {
                trace.getResource().setIgnoredSslProblems(false);   // Whether this should be reset is an outstanding question, related to {artf194123}.
                trace.setTrustManagerException(null);
                defaultVerifier.verify(hostName, socket);
                trace.debugStep("Perform Check: Pass");

            // Under normal, non-customized circumstances, if the
            // hostname verifier receives an exception from calling the
            // trust manager, it will replace that exception with a more
            // general exception:
            //   javax.net.ssl.SSLPeerUnverifiedException: peer not authenticated
            // and throw that to the calling code.  But once we customized
            // the trust manager and started not throwing exceptions at
            // that layer then other exceptions started appearing (more
            // on that below).
            //
            // Even if we've chosen not to throw an exception from the
            // InternalTrustManager because we are ignoring SSL
            // problems, the code that comes after that, but still within
            // the above verify() method can ALSO trip up on SSL issues.
            // This is because just because we've ignored core certificate
            // issues doesn't mean there aren't other obvious SSL problems
            // related to the host name that the hostname verifier can
            // figure out.  Thus, if this catch block is entered AND
            // there was an exception from the trust manager, we'll ASSUME
            // that any other SSL exception that followed is also related
            // to trusting or not trusting the host.  This can be seen with
            // the standard localhost:8443/self-signed certificate test.
            // If we ignore the TM exception, then hostname verifier will
            // itself throw an exception related to the same invalid
            // certificate problem. From examining the source, the other
            // two likely exceptions to occur in this situation are:
            //   SSLException("Certificate for <$host> doesn't contain CN or DNS subjectAlt")
            // and
            //   SSLException("hostname in certificate didn't match: <$myhost> != <$certhost>")
            //
            // HOWEVER, if there are exceptions that happen in verify()
            // BEFORE the TM has even been checked, then those probably
            // aren't related to trust and we should just respect those
            // exceptions as the are by rethrowing them.  This happens
            // for example when you try to access https://localhost:8080.
            // In fact this exception is also a
            //   javax.net.ssl.SSLPeerUnverifiedException: peer not authenticated
            // The hostname verifier will fail on this URL before needing
            // to check the TM, which shouldn't have any impact on whether
            // that URL can or should be accessed.
            } catch(IOException e) {

                // If we are saving the security information, then copy the full text of
                // the exception.  This may be a copy of the exception thrown to the Http
                // class or it might be a unique exception only witnessed when the TM
                // exception is ignored but the hostname verifier still identifies
                // problems on its own.  In the past I have tried to only save this info
                // when it wasn't going to be the same exception as the one recorded
                // in the HttpResource, but this feels like a premature optimization.
                if(trace.getOptions().isSaveSecurity()) {
                    String exceptionText = ExceptionUtil.toCompleteString(e, 4);
                    SecurityInfo secInfo = trace.getResource().getSecurityInfo();
                    secInfo.setHostnameVerifierExceptionText(exceptionText);
                }

                // If there was no recorded exception within the trust manager, then
                // the exception occurred with code in the verify method that was
                // executed either before or after the trust manager code.  Currently,
                // we're not concerned about those kinds of errors.
                if(trace.getTrustManagerException() == null) {

                    boolean nonTmSslCertIssue =
                        e instanceof SSLException && e.getMessage().endsWith("doesn't contain CN or DNS subjectAlt") ||
                        e instanceof SSLException && e.getMessage().startsWith("hostname in certificate didn't match");

                    // In some cases, the certificates are valid but the hostname
                    // verifier finds an additional problem with the SSL connection.
                    if(nonTmSslCertIssue) {
                        if(trace.getOptions().isIgnoreSslProblems()) {
                            trace.getResource().setIgnoredSslProblems(true);
                            trace.debugStep("Perform Check: HV Fail/Ignore");
                            return;
                        }
                    }

                    trace.debugStep("Perform Check: HV Fail/Throw");
                    throw e;
                }

                // Because the verify() method above has its own checks
                // related to the SSL connection AFTER the TM has had
                // its chance to check the certificates, but which are
                // still related to the overall SSL problems, we must
                // check again whether to throw those upwards.
                if(!trace.getOptions().isIgnoreSslProblems()) {
                    trace.debugStep("Perform Check: TM Fail/Throw");
                    throw e;
                }

                trace.getResource().setIgnoredSslProblems(true);
                trace.debugStep("Perform Check: TM Fail/Ignore");

                // The hostname verifier's local exception at this
                // point could be of the types
                //   SSLException("Certificate for <$host> doesn't contain CN or DNS subjectAlt")
                // or
                //   SSLException("hostname in certificate didn't match: <$myhost> != <$certhost>")
            }

        } finally {  // Debug try-finally block (w/ indent)
            trace.debugBlockClose();
        }
    }

    // This is not being called because we're delegating the primary
    // call above to the default implementation, which then in turn
    // calls these methods.  How they designed these interfaces is
    // a little confusing...
    @Override
    public void verify(String hostName, X509Certificate cert) throws SSLException {
        // Capture anything here in resource's security information?
        //if(performCheck) {
        try {
            defaultVerifier.verify(hostName, cert);
        } catch(SSLException e) {
            throw e;             // We can record this and rethrow!
        }
        //}
    }

    // This is not being called because we're delegating the primary
    // call above to the default implementation, which then in turn
    // calls these methods.  How they designed these interfaces is
    // a little confusing...
    @Override
    public void verify(String hostName, String[] cns, String[] alts) throws SSLException {
        // Capture anything here in resource's security information?
        //if(performCheck) {
        try {
            defaultVerifier.verify(hostName, cns, alts);
        } catch(SSLException e) {
            throw e;             // We can record this and rethrow!
        }
        //}
    }

    // This is not being called because we're delegating the primary
    // call above to the default implementation, which then in turn
    // calls these methods.  How they designed these interfaces is
    // a little confusing...
    @Override
    public boolean verify(String hostName, SSLSession session) {
        // Capture anything here in resource's security information?
        //if(performCheck) {
        return defaultVerifier.verify(hostName, session);
        //} else return true;
    }
}
