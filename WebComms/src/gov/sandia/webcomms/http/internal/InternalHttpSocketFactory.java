package gov.sandia.webcomms.http.internal;

import org.apache.http.conn.scheme.PlainSocketFactory;

import gov.sandia.webcomms.http.Http;

public class InternalHttpSocketFactory extends PlainSocketFactory {


    ////////////
    // FIELDS //
    ////////////

    private Http http;


    /////////////////
    // CONSTRUCTOR //
    /////////////////

    public InternalHttpSocketFactory(Http http) {
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
//            throws IOException, ConnectTimeoutException {
//        return super.connectSocket(socket, remoteAddr, localAddr, params);
//    }
}
