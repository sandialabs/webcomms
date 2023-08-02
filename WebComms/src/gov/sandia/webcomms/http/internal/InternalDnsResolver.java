package gov.sandia.webcomms.http.internal;

import java.net.InetAddress;
import java.net.UnknownHostException;

import org.apache.http.conn.DnsResolver;
import org.apache.http.impl.conn.SystemDefaultDnsResolver;

// Just a placeholder in case we want to customize this someday.
// Could use this to record resolved IP addresses, inject our
// own IP addresses, or potentially refuse to connect to some?

public class InternalDnsResolver implements DnsResolver {
    private SystemDefaultDnsResolver system = new SystemDefaultDnsResolver();
    @Override
    public InetAddress[] resolve(String host) throws UnknownHostException {
        InetAddress[] addr = system.resolve(host);
//        System.out.println(host + " => " + Arrays.toString(addr));
        return addr;
    }
}
