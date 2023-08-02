package gov.sandia.webcomms.http.ui.stats;

import java.util.ArrayList;
import java.util.List;

import gov.sandia.webcomms.http.stats.HostSegmentCountNode;
import gov.sandia.webcomms.http.stats.InterhostLinkCounts;
import replete.text.StringUtil;

public class Converter {

    public List<HostSegmentCountRow> convertToRows(HostSegmentCountNode node) {
        List<HostSegmentCountRow> rows = new ArrayList<>();
        convertToRows(node, rows, 0);
        return rows;
    }

    private void convertToRows(HostSegmentCountNode node, List<HostSegmentCountRow> rows, int level) {
        rows.add(new HostSegmentCountRow(node, level));
        for(String ch : node.getChildren().keySet()) {
            convertToRows(node.getChildren().get(ch), rows, level + 1);
        }
    }


    public List<HostSegmentCountRow> convertToRows(InterhostLinkCounts D) {
        List<HostSegmentCountRow> rows = printHostCounts(D, D.sourceRoot);
        for(HostSegmentCountRow row : rows) {
            System.out.printf("%-12s %3d %3d %3d%n", row.node.getSegment(),
                row.node.getCount(),
                D.getOutgoingUrlCount(row.node),
                row.level);
        }
        return rows;
    }
    private static final String SP_STR = "Source URLs";
    private static final String OL_STR = "Outgoing URLs";
    private static List<HostSegmentCountRow> printHostCounts(InterhostLinkCounts D, HostSegmentCountNode root) {
        int x = SP_STR.length();
        int o = OL_STR.length();
        System.out.printf("%-30s|%" + x + "s|%" + o + "s|%n", "Host Segment", SP_STR, OL_STR);
        System.out.printf("%-30s|%" + x + "s|%" + o + "s|%n", "------------", StringUtil.replicateChar('-', x), StringUtil.replicateChar('-', o));
        List<HostSegmentCountRow> rows = new ArrayList<>();
        printHost(D, root, rows, 0);
        return rows;
    }
    private static void printHost(InterhostLinkCounts D, HostSegmentCountNode node, List<HostSegmentCountRow> rows, int level) {
        int x = SP_STR.length();
        int o = OL_STR.length();
        String sp = StringUtil.spaces(level * 2);
        String hostMsg = node.getSegment() == null ? "(ALL URLs)" : sp + node.getSegment();
        System.out.printf("%-30s|%" + x + "s|%" + o + "s|%n", hostMsg,
            StringUtil.commas(node.getCount()),
            StringUtil.commas(D.getOutgoingUrlCount(node))
        );
        rows.add(new HostSegmentCountRow(node, level));
        for(String ch : node.getChildren().keySet()) {
            printHost(D, node.getChildren().get(ch), rows, level + 1);
        }
    }
}
