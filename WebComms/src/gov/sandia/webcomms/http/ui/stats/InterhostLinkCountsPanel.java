package gov.sandia.webcomms.http.ui.stats;

import javax.swing.JLabel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import gov.sandia.webcomms.http.stats.HostSegmentCountNode;
import gov.sandia.webcomms.http.stats.InterhostLinkCounts;
import replete.collections.Pair;
import replete.ui.lay.Lay;
import replete.ui.panels.RPanel;
import replete.ui.table.RTable;

public class InterhostLinkCountsPanel extends RPanel {


    ////////////
    // FIELDS //
    ////////////

    private RTable tblSource;
    private SourceTableModel mdlSource;
    private RTable tblOutgoing;
    private OutgoingTableModel mdlOutgoing;
    private JLabel lblDestTitle;
    private InterhostLinkCounts counts;


    /////////////////
    // CONSTRUCTOR //
    /////////////////

    public InterhostLinkCountsPanel() {
        String lblStyle = "bold,size=14,fg=white,eb=3,augb=mb(1tlr,black)";
        Lay.BLtg(this,
            "C", Lay.GL(1, 2,
                Lay.BL(
                    "N", Lay.lb("Source Pages", lblStyle, "bg=blue"),
                    "C", Lay.tblp(
                        tblSource = Lay.tbl(
                            mdlSource = new SourceTableModel(),
                            "size=14,seltype=single"
                        )
                    ),
                    "eb=10"
                ),
                Lay.BL(
                    "N", lblDestTitle = Lay.lb("Destination Pages", lblStyle, "bg=007F0E"),
                    "C", Lay.tblp(
                        tblOutgoing = Lay.tbl(
                            mdlOutgoing = new OutgoingTableModel(),
                            "size=14,seltype=single"
                        )
                    ),
                    "eb=10trb"
                )
            )
        );

        tblSource.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e) {
                int selRow = tblSource.getSelectedModelRow();
                if(selRow != -1) {
                    SourceTableModel mdl = (SourceTableModel) tblSource.getModel();
                    Pair<HostSegmentCountRow, Integer> row = mdl.getRow(selRow);
                    System.out.println(row + " O:" + row.getValue2());
                    HostSegmentCountNode hostNode = row.getValue1().node;
                    HostSegmentCountNode outgoingNode = counts.getOutgoing(hostNode);
                    mdlOutgoing.setNode(outgoingNode);
                    String path = "";
                    while(hostNode != null) {
                        if(hostNode.getSegment() != null) {
                            path = hostNode.getSegment() + (!path.isEmpty() ? "." + path : "");
                        }
                        hostNode = hostNode.getParent();
                    }
                    if(path.isEmpty()) {
                        path = "<i>All Source Pages</i>";
                    }
                    lblDestTitle.setText("<html>Destination Pages From: " + path + "</html>");
                } else {
                    mdlOutgoing.clear();
                    lblDestTitle.setText("Destination Pages");
                }
            }
        });
    }

    public void setCounts(InterhostLinkCounts counts) {
        this.counts = counts;
        mdlSource.setCounts(counts);
        mdlOutgoing.clear();
    }
}
