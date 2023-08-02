package gov.sandia.webcomms.http.ui.stats;

import gov.sandia.webcomms.http.stats.HostSegmentCountNode;

public class HostSegmentCountRow {


    ////////////
    // FIELDS //
    ////////////

    public HostSegmentCountNode node;
    public int level;


    /////////////////
    // CONSTRUCTOR //
    /////////////////

    public HostSegmentCountRow(HostSegmentCountNode node, int level) {
        this.node = node;
        this.level = level;
    }


    ////////////////
    // OVERRIDDEN //
    ////////////////

    @Override
    public String toString() {
        return node + " L:" + level;
    }
}
