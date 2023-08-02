package gov.sandia.webcomms.http.internal;

import java.security.KeyStore;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.List;

import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import gov.sandia.webcomms.http.Http;
import gov.sandia.webcomms.http.rsc.CertificateInfo;
import gov.sandia.webcomms.http.rsc.SecurityInfo;
import replete.errors.ExceptionUtil;

// The trust manager's job I believe is to just check the validity
// of the certificates themselves - an internal check if you will.

// It's possible that the certificates themselves are valid but
// do not correspond to the website (since certificates have to
// have acceptable names contained within).  In that case they
// would pass the trust manager but additional checks in the
// hostname verifier should catch these issues.

// https://docs.oracle.com/javase/7/docs/api/java/security/cert/X509Certificate.html#getSubjectAlternativeNames()
// https://www.rapid7.com/db/vulnerabilities/certificate-common-name-mismatch

public class InternalTrustManager implements X509TrustManager {


    ////////////
    // FIELDS //
    ////////////

    private static final int DNS_NAME_TYPE = 2;

    private Http http;
    private X509TrustManager defaultTrustManager;  // Default Java X.509 trust manager


    //////////////////
    // CONSTRUCTORS //
    //////////////////

    public InternalTrustManager(Http http) {
        this.http = http;
        try {
            TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(
                TrustManagerFactory.getDefaultAlgorithm());
            trustManagerFactory.init((KeyStore) null);
            for(TrustManager trustManager : trustManagerFactory.getTrustManagers()) {
                if(trustManager instanceof X509TrustManager) {
                    defaultTrustManager = (X509TrustManager) trustManager;
                }
            }
            if(defaultTrustManager == null) {
                throw new RuntimeException();
            }
        } catch(Exception e) {
            throw new RuntimeException("Could not initialize default X.509 trust manager");
        }
    }


    ////////////////
    // OVERRIDDEN //
    ////////////////

    // Used to check if a certificate chain for a domain name is valid.
    // This will be called multiple times if a resource's redirect
    // chain contains multiple requests from same host.
    @Override
    public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
        RequestTrace trace = http.getRequestTrace();
        trace.debugBlockOpen("TRUST MANAGER CHECK: " + authType);

        try {  // Debug try-finally block (w/ indent)

            if(trace.getOptions().isSaveSecurity()) {
                SecurityInfo secInfo = trace.getResource().getSecurityInfo();
                String newExisting = (secInfo == null ? "!New => " : "~Existing => ");
                if(secInfo == null) {
                    secInfo = new SecurityInfo();
                    trace.getResource().setSecurityInfo(secInfo);
                }
                secInfo.setAuthType(authType);
                secInfo.setNumCerts(chain.length);
                for(X509Certificate cert : chain) {

                    // Can capture more things like subject public key,
                    // certificate signature info, and extensions.
                    CertificateInfo cInfo = new CertificateInfo()
                        .setSerialNumber("" + cert.getSerialNumber())
                        .setVersion(cert.getVersion())
                        .setIssuerDn(cert.getIssuerX500Principal().getName())
                        .setSubjectDn(cert.getSubjectDN().getName())
                        .setNotBefore(cert.getNotBefore())
                        .setNotAfter(cert.getNotAfter())
                    ;
                    if(cert.getSubjectAlternativeNames() != null) {
                        for(List<?> altNameEntry : cert.getSubjectAlternativeNames()) {
                            if(altNameEntry.get(0).equals(DNS_NAME_TYPE)) {
                                String dnsName = (String) altNameEntry.get(1);
                                cInfo.getSubjectAltDnsNames().add(dnsName);
                            }
                        }
                    }

                    // Right now might appear like adding duplicates, since same
                    // domain name inspected for original and redirects.  Need
                    // to add security info to redirects as well.  As far as I
                    // can see redirect's certs have same hash codes as the
                    // original's, implying that a centralized data store
                    // might be useful.  This could be helped with these:
                    // {artf194123} and {artf194149}.
                    secInfo.getCerts().add(cInfo);
                }

                trace.debugStep("SAVE SEC " + newExisting + secInfo);
            }

            try {
                defaultTrustManager.checkServerTrusted(chain, authType);
                trace.debugStep("Perform Check: Pass");

            } catch(CertificateException e) {

                // There are various possibilities for the exceptions that could be caught
                // here.  Presumably the "self-signed certificate issue" is largely
                // represented here with this exception:
                //
                //     sun.security.validator.ValidatorException: PKIX path building failed: ...
                //         sun.security.provider.certpath.SunCertPathBuilderException: unable to ...
                //         find valid certification path to requested target
                //
                // However, problems with 1) certificate expiration, 2) invalid/insecure/
                // old/unsupported ciphers, or 3) other certificate or server SSL
                // misconfiguration issues should also be manifested here with appropriate
                // exceptions.

                // Record the exception that occurred on account of the trust manager.
                trace.setTrustManagerException(e);

                // If we are saving the security information, then copy the full text of
                // the exception.  We allow this because the exception would otherwise
                // be lost/ignored by the normal execution paths.
                if(trace.getOptions().isSaveSecurity()) {
                    SecurityInfo secInfo = trace.getResource().getSecurityInfo();
                    String exceptionText = ExceptionUtil.toCompleteString(e, 4);
                    secInfo.setTrustManagerExceptionText(exceptionText);
                }

                // If we're not ignoring SSL problems, then we need to throw the
                // exception as would normally happen without all this supplemental
                // code.
                if(!trace.getOptions().isIgnoreSslProblems()) {
                    trace.debugStep("Perform Check: Fail/Throw");
                    throw e;
                }

                trace.getResource().setIgnoredSslProblems(true);
                trace.debugStep("Perform Check: Fail/Ignore");
            }

        } finally {  // Debug try-finally block (w/ indent)
            trace.debugBlockClose();
        }
    }

    // No evidence being called yet, not sure if/when this is called.
    // If this gets called at some point, we can figure out if we
    // need to instrument it.
    @Override
    public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
        //if(performCheck) {
        try {
            defaultTrustManager.checkClientTrusted(chain, authType);
        } catch(CertificateException e) {
            // Can instrument here like checkServerTrusted if needed.
            throw e;
        }
        //}
    }

    // Called by Java/HttpClient once each time an HTTPS URL is
    // accessed, either an original or a redirect.
    @Override
    public X509Certificate[] getAcceptedIssuers() {
        return defaultTrustManager.getAcceptedIssuers();   // Simply pass-through to default.
    }
}
