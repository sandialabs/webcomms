package gov.sandia.webcomms.http.ui.stats;

import java.awt.Color;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import javax.swing.Icon;
import javax.swing.SwingConstants;

import gov.sandia.webcomms.http.stats.HostSegmentCountNode;
import gov.sandia.webcomms.http.ui.images.flags.FlagImageLib;
import gov.sandia.webcomms.http.ui.tld.CountryCode;
import replete.collections.Pair;
import replete.text.StringUtil;
import replete.ui.lay.Lay;
import replete.ui.table.DefaultUiHintedTableModel;

public class OutgoingTableModel extends DefaultUiHintedTableModel {
    private List<Pair<HostSegmentCountRow, Integer>> outgoingRows;
    // Could this Integer be used as "incoming links"?

    public Pair<HostSegmentCountRow, Integer> getRow(int r) {
        return outgoingRows.get(r);
    }

    public void setNode(HostSegmentCountNode outgoingNode) {
        Map<String, Predicate<HostSegmentCountNode>> criteria = new LinkedHashMap<>();
        criteria.put("US Government", e -> CountryCode.isUsGovt(e.getSegment()));
        criteria.put("US Other", e -> CountryCode.isUsOther(e.getSegment()));
        criteria.put("Foreign", e -> {
            String tld = e.getSegment();
            if(CountryCode.isUsGovt(tld) || CountryCode.isUsOther(tld)) {
                return false;   // Not foreign
            }
            return CountryCode.getAll().containsKey(tld);
        });

        HostSegmentCountNode outNodeGrouped = outgoingNode.convertToGrouped(criteria);

        Converter C = new Converter();
        List<HostSegmentCountRow> hostRows = C.convertToRows(outNodeGrouped);
        outgoingRows = new ArrayList<>();
        for(HostSegmentCountRow row : hostRows) {
            Pair<HostSegmentCountRow, Integer> sourceRow = new Pair<>(row, -1);
            outgoingRows.add(sourceRow);
        }
        fireTableDataChanged();
    }
    public void clear() {
        if(outgoingRows != null) {
            outgoingRows.clear();
            fireTableDataChanged();
        }
    }

    @Override
    protected void init() {
        addColumn("",                           30);
        addColumn("Hierarchical Host Segment",  -1);
        addColumn("# Outgoing URLs",           120);
//        addColumn("# Incoming URLs",   120); ??
    }

    @Override
    public int getRowCount() {
        if(outgoingRows == null) {
            return 0;
        }
        return outgoingRows.size();
    }

    @Override
    public Object getValueAt(int r, int c) {
        Pair<HostSegmentCountRow, Integer> row = outgoingRows.get(r);
        HostSegmentCountRow hostRows = row.getValue1();
        HostSegmentCountNode hostNode = hostRows.node;
//        Integer outCount = row.getValue2();
        switch(c) {
            case 0:
                CountryCode code = getCountryCode(r);
                if(code == null) {
                    return "";
                }
                return code.getCountryName();
            case 1:
                int level = hostRows.level;
                String sp = StringUtil.spaces(level * 4);
                String hostMsg = hostNode.getSegment() == null ? "(ALL URLs)" : sp + hostNode.getSegment();
                return hostMsg;
            case 2: return StringUtil.commas(hostNode.getCount());  // TODO: fix column sorting
//            case 2: return StringUtil.commas(outCount);
        }
        return null;
    }

    @Override
    public Insets getInsets(int row, int col) {
        return new Insets(0, 2, 0, 2);
    }

    @Override
    public int getRowHeight(int row) {
        return 20;
    }

    @Override
    public Color getBackgroundColor(int r, int c) {
        Pair<HostSegmentCountRow, Integer> row = outgoingRows.get(r);
        HostSegmentCountRow hostRow = row.getValue1();
        switch(hostRow.level) {
            case 0:  return Lay.clr("205,255,205");
            case 1:  return Lay.clr("255,225,205");
            default: return super.getBackgroundColor(r, c);
        }
    }

    @Override
    public Icon getIcon(int r, int c) {
        if(c == 0) {
            CountryCode code = getCountryCode(r);
            if(code != null) {
                return FlagImageLib.getByFileName(code.getIconLocalFileName());
            }
        }
        return super.getIcon(r, c);
    }

    @Override
    public Boolean isBold(int row, int col) {
        return true;
    }

    @Override
    public Boolean isItalic(int row, int col) {
        return row == 0 ? true : super.isItalic(row, col);
    }

    @Override
    public int getAlignment(int row, int col) {
        return col == 2 ? SwingConstants.RIGHT : super.getAlignment(row, col);
    }

    private CountryCode getCountryCode(int r) {
        Pair<HostSegmentCountRow, Integer> row = outgoingRows.get(r);
        HostSegmentCountRow hostRow = row.getValue1();
        if(hostRow.level == 2) {
            String segment = hostRow.node.getSegment();
            return CountryCode.getAll().get(segment);
        }
        return null;
    }
}
