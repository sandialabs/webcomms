package gov.sandia.webcomms.http.stats;

import java.io.Serializable;
import java.util.Comparator;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Predicate;

import replete.compare.CompareUtil;

public class HostSegmentCountNode implements Serializable {


    ////////////
    // FIELDS //
    ////////////

    private HostSegmentCountNode parent;
    private String segment;
    private int count;
    private Map<String, HostSegmentCountNode> children = new TreeMap<>();

    // needs overlay ?   overlay(HostSegmentCountNode other)
    // rename to HostSegmentCountNode

    public HostSegmentCountNode convertToGrouped(
                     Map<String, Predicate<HostSegmentCountNode>> groupings) {
        Map<String, HostSegmentCountNode> newChildren = new TreeMap<>(new Comparator<String>() {
            @Override
            public int compare(String o1, String o2) {
                String[] c1 = groupings.keySet().toArray(new String[0]);
                String[] c2 = new String[c1.length + 1];
                System.arraycopy(c1, 0, c2, 0, c1.length);
                c2[c2.length - 1] = "Other";
                return CompareUtil.compareByGroup(o1, o2, c2);
            }
        });

        for(String childSeg : children.keySet()) {
            HostSegmentCountNode child = children.get(childSeg);

            // Figure out the name of the group to which child applies.
            String groupName = "Other";
            for(String gkey : groupings.keySet()) {
                Predicate<HostSegmentCountNode> criteria = groupings.get(gkey);
                if(criteria.test(child)) {
                    groupName = gkey;
                    break;
                }
            }

            // Get or create the new children that will represent the selected group.
            HostSegmentCountNode groupChild = newChildren.get(groupName);
            if(groupChild == null) {
                groupChild = new HostSegmentCountNode(groupName);  // Group child parent set later
                newChildren.put(groupName, groupChild);
            }

            // Copy current child to a child of the selected group.
            // If we add parent pointers, more to update.
            groupChild.children.put(childSeg, child);
            groupChild.count += child.count;
            child.parent = groupChild;
        }

        // Assume no parent, make detached child copy of this
        HostSegmentCountNode newThis = new HostSegmentCountNode(segment);
        newThis.count = count;
        newThis.children = newChildren;
        for(HostSegmentCountNode groupChild : newChildren.values()) {
            groupChild.setParent(newThis);
        }
        return newThis;
    }


    //////////////////
    // CONSTRUCTORS //
    //////////////////

    public HostSegmentCountNode() {}           // ROOT (ALL) counter
    public HostSegmentCountNode(String segment) {
        this(null, segment);
    }
    public HostSegmentCountNode(HostSegmentCountNode parent, String segment) {
        this.parent = parent;
        this.segment = segment;
    }


    //////////////////////////
    // ACCESSORS / MUTATORS //
    //////////////////////////

    // Accessors

    public Map<String, HostSegmentCountNode> getChildren() {
        return children;
    }
    public String getSegment() {
        return segment;
    }
    public int getCount() {
        return count;
    }
    public HostSegmentCountNode getParent() {
        return parent;
    }


    // Mutators

    public void setParent(HostSegmentCountNode parent) {
        this.parent = parent;
    }
    public void inc() {
        count++;
    }
//    public void incOutgoingUrls(int urls) {
//        outgoingUrls += urls;
//    }

    @Override
    public String toString() {
        return "S:" + segment + " C:" + count;
    }
}
