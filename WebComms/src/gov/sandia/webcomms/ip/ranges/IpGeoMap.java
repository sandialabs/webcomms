package gov.sandia.webcomms.ip.ranges;

import java.io.Serializable;

public abstract class IpGeoMap implements Serializable {
    public abstract int getRangeCount();
    public abstract Number[] getLoadedMapCount();
    public abstract IpRangeGeo lookup(String ip);
}
