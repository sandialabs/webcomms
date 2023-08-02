package gov.sandia.webcomms.http.rsc;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.mongodb.morphia.annotations.Embedded;

// This is a subset of the larger Java X509Certificate class.
// The idea is that we only need so much of the information
// stored the platform's class, so we'll maintain our own
// skinnier class.  But this may ultimately be a premature
// optimization.  Anyone doing security research might
// need the full certificate info and it might not be such
// a big deal disk-wise to just save the original certificate
// object in its entirety.  One day this can be revisited if
// it becomes an issue.

// http://en.wikipedia.org/wiki/X.509
// https://en.wikipedia.org/wiki/Subject_Alternative_Name

@Embedded
public class CertificateInfo implements Serializable {

    // Can capture more things like subject public key,
    // certificate signature info, and extensions.


    ////////////
    // FIELDS //
    ////////////

    private String serialNumber;
    private int version;
    private String issuerDn;
    private String subjectDn;
    private List<String> subjectAltDnsNames = new ArrayList<>();
    private Date notBefore;
    private Date notAfter;


    //////////////////////////
    // ACCESSORS / MUTATORS //
    //////////////////////////

    // Accessors

    public String getSerialNumber() {
        return serialNumber;
    }
    public int getVersion() {
        return version;
    }
    public String getIssuerDn() {
        return issuerDn;
    }
    public String getSubjectDn() {
        return subjectDn;
    }
    public List<String> getSubjectAltDnsNames() {
        return subjectAltDnsNames;
    }
    public Date getNotBefore() {
        return notBefore;
    }
    public Date getNotAfter() {
        return notAfter;
    }

    // Mutators (Builder)

    public CertificateInfo setSerialNumber(String serialNumber) {
        this.serialNumber = serialNumber;
        return this;
    }
    public CertificateInfo setVersion(int version) {
        this.version = version;
        return this;
    }
    public CertificateInfo setIssuerDn(String issuerDn) {
        this.issuerDn = issuerDn;
        return this;
    }
    public CertificateInfo setSubjectDn(String subjectDn) {
        this.subjectDn = subjectDn;
        return this;
    }
    public CertificateInfo setNotBefore(Date notBefore) {
        this.notBefore = notBefore;
        return this;
    }
    public CertificateInfo setNotAfter(Date notAfter) {
        this.notAfter = notAfter;
        return this;
    }


    ////////////////
    // OVERRIDDEN //
    ////////////////

    @Override
    public String toString() {
        return "SN=" + serialNumber + " (v" + version + ")";
    }
}
