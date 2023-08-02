package gov.sandia.webcomms.http.ui.stats;

import java.util.List;

import javax.swing.JButton;

import gov.sandia.webcomms.http.stats.InterhostLinkCounts;
import gov.sandia.webcomms.http.stats.RandomUrlGenerator;
import replete.numbers.RandomUtil;
import replete.ui.images.concepts.CommonConcepts;
import replete.ui.lay.Lay;
import replete.ui.windows.escape.EscapeFrame;

public class InterhostLinkCountsFrame extends EscapeFrame {


    ////////////
    // FIELDS //
    ////////////

    private InterhostLinkCountsPanel pnlCounts;
    private JButton btnGen;
    private JButton btnClose;


    /////////////////
    // CONSTRUCTOR //
    /////////////////

    public InterhostLinkCountsFrame() {
        super("Interhost Link Counts");
        setIcon(CommonConcepts.INTERNET);

        String sp = "<font color='blue'><u>Source Pages</u></font>";
        String content =
            "The " + sp + " table breaks down the hosts of " +
            "visited (downloaded) pages hierarchically by host name segments. " +
            "The <font color='blue'><u># Source URLs</u></font> column reports how many pages were visited at the " +
            "given depth in the segment hierarchy. The <font color='blue'><u># Outgoing URLs</u></font> column " +
            "shows how many additional links <i>(which may or may not have been followed)</i> " +
            "were found on those same pages. " +
            "The <font color='blue'><u>Destination Pages</u></font> table represents the " +
            "breakdown of outgoing URLs for a single selected host segment from the " + sp + " table. " +
            "The table also contains a <font color='blue'><u># Outgoing URLs</u></font> column showing " +
            "a per-host segment count and a <font color='blue'><u>Flag Column</u></font> for known country TLDs.";

        Lay.BLtg(this,
            "N", Lay.lb("<html>" + content + "</html>", "eb=10tlr"),
            "C", pnlCounts = new InterhostLinkCountsPanel(),
            "S", Lay.BL(
                "W", Lay.FL(
                    btnGen = Lay.btn("&Generate New Source Links", CommonConcepts.ACTION)
                 ),
                "E", Lay.FL("R",
                    btnClose = Lay.btn("&Close", CommonConcepts.CANCEL)
                ),
                "bg=100,mb=[1t,black],chtransp"
            ),
            "size=[1000,600],center"
        );

        setDefaultButton(btnGen);

        btnGen.addActionListener(e -> {
            pnlCounts.setCounts(genNew());
        });
        btnClose.addActionListener(e -> close());
    }

    private static InterhostLinkCounts genNew() {
        InterhostLinkCounts counts = new InterhostLinkCounts();
        RandomUrlGenerator gen = new RandomUrlGenerator();
        int sourceCount = 20;
        List<String> sourceUrls = gen.genSourceUrls(sourceCount);
        for(String sourceUrl : sourceUrls) {
            int outCount = RandomUtil.getRandomWithinRange(2, 3);
            List<String> outgoingUrls = gen.genOutgoingUrls(outCount);
            counts.add(sourceUrl, outgoingUrls);
        }
        return counts;
    }


    //////////
    // TEST //
    //////////

    public static void main(String[] args) {
        InterhostLinkCountsFrame frame = new InterhostLinkCountsFrame();
        Lay.hn(frame, "toplevel");
        frame.setVisible(true);
//        Converter C = new Converter();
//        C.convertToRows(genNew());
    }
}