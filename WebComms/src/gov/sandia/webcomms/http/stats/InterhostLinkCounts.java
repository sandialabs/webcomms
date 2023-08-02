package gov.sandia.webcomms.http.stats;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import replete.web.UrlUtil;

public class InterhostLinkCounts implements Serializable {


    ////////////
    // FIELDS //
    ////////////

    public HostSegmentCountNode sourceRoot;
    public Map<HostSegmentCountNode, HostSegmentCountNode> sourceToOutgoing;


    /////////////////
    // CONSTRUCTOR //
    /////////////////

    public InterhostLinkCounts() {
        sourceRoot = new HostSegmentCountNode();
        sourceToOutgoing = new HashMap<>();
        sourceToOutgoing.put(sourceRoot, new HostSegmentCountNode() /*no parent*/);
    }


    //////////////
    // MUTATORS //
    //////////////

    public synchronized void add(String sourceUrl, List<String> outgoingUrls) {
        String host = UrlUtil.getHostUrl(sourceUrl);
        String[] sourceUrlSegs = host.split("\\.");
        Map<String, String[]> outgoingUrlHostSegs = cacheOutgoing(outgoingUrls);

        HostSegmentCountNode sourceCur = sourceRoot;
        sourceRoot.inc();

        attachOutgCountsToSource(outgoingUrlHostSegs, sourceCur);

//e        sourceRoot.incOutgoingUrls(outgoingUrls.size());    // unify root & loop?

        for(int i = sourceUrlSegs.length - 1; i >= 0; i--) {
            String seg = sourceUrlSegs[i].toLowerCase();              // TODO: ensure unneeded
            HostSegmentCountNode sourceChild = sourceCur.getChildren().get(seg);
            if(sourceChild == null) {
                sourceChild = new HostSegmentCountNode(sourceCur, seg);
                sourceCur.getChildren().put(seg, sourceChild);
                sourceToOutgoing.put(sourceChild, new HostSegmentCountNode() /*no parent*/);
            }
            sourceChild.inc();
            attachOutgCountsToSource(outgoingUrlHostSegs, sourceChild);
//            child.incOutgoingUrls(outgoingUrls.size());

            sourceCur = sourceChild;
        }
    }


    private void attachOutgCountsToSource(Map<String, String[]> outgoingUrlHostSegs,
                                          HostSegmentCountNode sourceCur) {
        HostSegmentCountNode outgRoot = sourceToOutgoing.get(sourceCur);
        for(String outgoingUrl : outgoingUrlHostSegs.keySet()) {
            String[] outgoingSegs = outgoingUrlHostSegs.get(outgoingUrl);
            HostSegmentCountNode outgCur = outgRoot;
            outgRoot.inc();
            for(int i = outgoingSegs.length - 1; i >= 0; i--) {
                String seg = outgoingSegs[i].toLowerCase();              // TODO: ensure unneeded
                HostSegmentCountNode outgChild = outgCur.getChildren().get(seg);
                if(outgChild == null) {
                    outgChild = new HostSegmentCountNode(outgCur, seg);
                    outgCur.getChildren().put(seg, outgChild);
                }
                outgChild.inc();
                outgCur = outgChild;
            }
        }
    }


    private Map<String, String[]> cacheOutgoing(List<String> outgoingUrls) {
        Map<String, String[]> outUrlHostSegs = new HashMap<>();
        for(String outgoingUrl : outgoingUrls) {
            String host = UrlUtil.getHostUrl(outgoingUrl);
            String[] segs = host.split("\\.");
            outUrlHostSegs.put(host, segs);
        }
        return outUrlHostSegs;
    }


    public int getOutgoingUrlCount(HostSegmentCountNode sourceNode) {
        HostSegmentCountNode outgRoot = sourceToOutgoing.get(sourceNode);
        return outgRoot.getCount();                                 // TODO: NPE Seen here before
    }
    public HostSegmentCountNode getOutgoing(HostSegmentCountNode sourceNode) {
        return sourceToOutgoing.get(sourceNode);
    }
}
