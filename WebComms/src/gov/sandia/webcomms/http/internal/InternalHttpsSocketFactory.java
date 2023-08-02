package gov.sandia.webcomms.http.internal;

import javax.net.ssl.SSLContext;

import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.conn.ssl.X509HostnameVerifier;

import gov.sandia.webcomms.http.Http;

public class InternalHttpsSocketFactory extends SSLSocketFactory {


    ////////////
    // FIELDS //
    ////////////

    private Http http;


    //////////////////
    // CONSTRUCTORS //
    //////////////////

    public InternalHttpsSocketFactory(SSLContext sslContext, X509HostnameVerifier hostnameVerifier, Http http) {
        super(sslContext, hostnameVerifier);
        this.http = http;
    }


    ////////////////
    // OVERRIDDEN //
    ////////////////

    // This used to be used to track the IP/port we connect to.  Maybe
    // will use in future for something?
//    @Override
//    public Socket connectSocket(Socket socket, InetSocketAddress remoteAddr,
//                                InetSocketAddress localAddr, HttpParams params)
//            throws IOException, UnknownHostException,ConnectTimeoutException {
//        RequestTrace trace = http.getRequestTrace();
//        String ipPort = remoteAddr.getAddress().getHostAddress() + ":" + remoteAddr.getPort();
//        trace.setProvidingIpPort(ipPort);
//        return super.connectSocket(socket, remoteAddr, localAddr, params);
//    }
}
