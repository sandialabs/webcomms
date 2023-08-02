package gov.sandia.webcomms.http.rsc;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.mongodb.morphia.annotations.Embedded;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;

// ALSO: Due to how many URLs will probably have the same exact
// security info, these options will most likely be stored in
// some centralized data store, and the Resource objects will
// merely point to the one that was theirs.

// OTHER OPTION: If there truly is only one possible SecurityInfo
// object per DOMAIN NAME (www.cnn.com or cnn.com?), then you
// don't even need the pointer and you can always look up a
// Resource's SecInfo by using it's providing URL's domain name.

@Entity(noClassnameStored=true)
public class SecurityInfo implements Serializable {


    ////////////
    // FIELDS //
    ////////////

    @Id private String hostName;
    private String authType;
    private int numCerts;
    private String trustManagerExceptionText;
    private String hostnameVerifierExceptionText;
    @Embedded private List<CertificateInfo> certs = new ArrayList<>();


    //////////////////////////
    // ACCESSORS / MUTATORS //
    //////////////////////////

    // Accessors

    public String getHostName() {
        return hostName;
    }
    public String getAuthType() {
        return authType;
    }
    public int getNumCerts() {
        return numCerts;
    }
    public String getTrustManagerExceptionText() {
        return trustManagerExceptionText;
    }
    public String getHostnameVerifierExceptionText() {
        return hostnameVerifierExceptionText;
    }
    public List<CertificateInfo> getCerts() {
        return certs;
    }

    // Mutators

    public SecurityInfo setHostName(String hostName) {
        this.hostName = hostName;
        return this;
    }
    public SecurityInfo setAuthType(String authType) {
        this.authType = authType;
        return this;
    }
    public SecurityInfo setNumCerts(int numCerts) {
        this.numCerts = numCerts;
        return this;
    }
    public SecurityInfo setTrustManagerExceptionText(String trustManagerExceptionText) {
        this.trustManagerExceptionText = trustManagerExceptionText;
        return this;
    }
    public SecurityInfo setHostnameVerifierExceptionText(String hostnameVerifierExceptionText) {
        this.hostnameVerifierExceptionText = hostnameVerifierExceptionText;
        return this;
    }


    ////////////////
    // OVERRIDDEN //
    ////////////////

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("[hostName=");
        builder.append(hostName);
        builder.append(", authType=");
        builder.append(authType);
        builder.append(", numCerts=");
        builder.append(numCerts);
        builder.append(", trustManagerExceptionText=");
        builder.append(trustManagerExceptionText != null);
        builder.append(", hostnameVerifierExceptionText=");
        builder.append(hostnameVerifierExceptionText != null);
        builder.append("]");
        return builder.toString();
    }
}
