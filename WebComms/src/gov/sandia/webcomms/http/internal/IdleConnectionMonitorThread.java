package gov.sandia.webcomms.http.internal;

import java.util.concurrent.TimeUnit;

import org.apache.http.conn.ClientConnectionManager;

// http://hc.apache.org/httpcomponents-client-ga/tutorial/html/connmgmt.html
public class IdleConnectionMonitorThread extends Thread {


    ////////////
    // FIELDS //
    ////////////

    private final ClientConnectionManager connMgr;
    private volatile boolean shutdown;


    /////////////////
    // CONSTRUCTOR //
    /////////////////

    public IdleConnectionMonitorThread(ClientConnectionManager connMgr) {
        super("Http Idle Cxn Monitor");
        setDaemon(true);
        this.connMgr = connMgr;
    }


    ////////////////
    // OVERRIDDEN //
    ////////////////

    @Override
    public void run() {
        try {
            while(!shutdown) {
                synchronized (this) {
                    wait(5000);

                    // If need be, we could technically inspect
                    // the connection manager's pool to see what
                    // it holds at any given time.
                    // http://grepcode.com/file/repo1.maven.org/maven2/org.apache.httpcomponents/httpcore/4.2-beta1/org/apache/http/pool/AbstractConnPool.java#AbstractConnPool.closeExpired%28%29
                    connMgr.closeExpiredConnections();
                    connMgr.closeIdleConnections(60, TimeUnit.SECONDS);
                }
            }
        } catch(InterruptedException ex) {
            // terminate
        }
    }


    //////////
    // MISC //
    //////////

    public void shutdown() {
        shutdown = true;
        synchronized (this) {
            notifyAll();
        }
    }
}
