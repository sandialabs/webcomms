package gov.sandia.webcomms.http.ui.stats;

import java.awt.Color;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.List;

import javax.swing.Icon;
import javax.swing.SwingConstants;

import gov.sandia.webcomms.http.stats.HostSegmentCountNode;
import gov.sandia.webcomms.http.stats.InterhostLinkCounts;
import gov.sandia.webcomms.http.ui.images.flags.FlagImageLib;
import gov.sandia.webcomms.http.ui.tld.CountryCode;
import replete.collections.Pair;
import replete.text.StringUtil;
import replete.ui.lay.Lay;
import replete.ui.table.DefaultUiHintedTableModel;

public class SourceTableModel extends DefaultUiHintedTableModel {
    private List<Pair<HostSegmentCountRow, Integer>> sourceRows;

    public Pair<HostSegmentCountRow, Integer> getRow(int r) {
        return sourceRows.get(r);
    }

    public void setCounts(InterhostLinkCounts D) {
        Converter C = new Converter();
        List<HostSegmentCountRow> hostRows = C.convertToRows(D.sourceRoot);
        sourceRows = new ArrayList<>();
        for(HostSegmentCountRow row : hostRows) {
            int outCount = D.getOutgoingUrlCount(row.node);
            Pair<HostSegmentCountRow, Integer> sourceRow = new Pair<>(row, outCount);
            sourceRows.add(sourceRow);
        }
        fireTableDataChanged();
    }

    @Override
    protected void init() {
        addColumn("",                           30);
        addColumn("Hierarchical Host Segment",  -1);
        addColumn("# Source URLs",             120);
        addColumn("# Outgoing URLs",           120);
    }

    @Override
    public int getRowCount() {
        if(sourceRows == null) {
            return 0;
        }
        return sourceRows.size();
    }

    @Override
    public Object getValueAt(int r, int c) {
        Pair<HostSegmentCountRow, Integer> row = sourceRows.get(r);
        HostSegmentCountRow hostRows = row.getValue1();
        HostSegmentCountNode hostNode = hostRows.node;
        Integer outCount = row.getValue2();
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
            case 3: return StringUtil.commas(outCount);             // TODO: fix column sorting
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
    public Color getBackgroundColor(int row, int col) {
        return row == 0 ? Lay.clr("205,255,205") : super.getBackgroundColor(row, col);
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
        return col >= 2 ? SwingConstants.RIGHT : super.getAlignment(row, col);
    }

    private CountryCode getCountryCode(int r) {
        Pair<HostSegmentCountRow, Integer> row = sourceRows.get(r);
        HostSegmentCountRow hostRow = row.getValue1();
        if(hostRow.level == 1) {
            String segment = hostRow.node.getSegment();
            return CountryCode.getAll().get(segment);
        }
        return null;
    }
}
