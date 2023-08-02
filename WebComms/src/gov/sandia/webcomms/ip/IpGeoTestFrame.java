package gov.sandia.webcomms.ip;

import java.awt.event.ActionListener;
import java.io.File;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JLabel;

import gov.sandia.webcomms.http.ui.images.flags.FlagImageLib;
import gov.sandia.webcomms.ip.ranges.IpGeoMapMaster;
import gov.sandia.webcomms.ip.ranges.IpRangeGeo;
import replete.ui.combo.RecentComboBox;
import replete.ui.form.FileSelectionPanel;
import replete.ui.form2.FieldDescriptor;
import replete.ui.form2.NewRFormPanel;
import replete.ui.images.concepts.CommonConcepts;
import replete.ui.lay.Lay;
import replete.ui.windows.Dialogs;
import replete.ui.windows.notifications.NotificationFrame;
import replete.ui.worker.RWorker;

// -Dwebcomms.dbip.file=C:\Users\dtrumbo\work\eclipse-main\WebComms\dbip-city-2016-10.csv

// The only country code listed in the DBIP CSV file for which we don't
// have corresponding country info from Wikipedia is XK - Kosovo.  This
// country does not yet have its own ccTLD.

public class IpGeoTestFrame extends NotificationFrame {


    ////////////
    // FIELDS //
    ////////////

    private IpGeoMapMaster master;
    private RecentComboBox<String> cboIp;
    private DefaultComboBoxModel<String> mdlIp =  new DefaultComboBoxModel<>();
    private JButton btnCheck;
    private JLabel lblLoadResult;
    private JLabel lblLookupResult;
    private FileSelectionPanel pnlFile;
    private JButton btnPrintStats;
    private JButton btnCompress;
    private static final File DEFAULT_FILE = new File("C:\\Users\\dtrumbo\\work\\eclipse-main\\WebComms\\dbip-city-2016-10.csv");


    //////////////////
    // CONSTRUCTORS //
    //////////////////

    public IpGeoTestFrame() {
        super("IP -> GEO", CommonConcepts.WORLD);

        mdlIp.addElement("127.0.0.1");                                // US/California/Los Angeles
        mdlIp.addElement("122.43.70.2");                              // KR/Seoul/Seoul
        mdlIp.addElement("220.43.70.2");                              // JP/Tokyo/Chiyoda
        mdlIp.addElement("2001:db8:85a3:8d3:1319:8a2e:370:7348");     // AU/Queensland/Brisbane
        mdlIp.addElement("FE80:0000:0000:0000:0202:B3FF:FE1E:8329");  // (None)

        Lay.BLtg(this,
            "C", Lay.sp(new InnerFormPanel(), "eb=0"),
            "size=[650,400],center"
        );

        setShowStatusBar(true);
        btnCheck.addActionListener(e -> {
            String ip = cboIp.getSelected();
            cboIp.pushHistory();
            IpRangeGeo range = master.lookup(ip);
            if(range == null) {
                lblLookupResult.setText("Unknown!");
                lblLookupResult.setIcon(null);
            } else {
                lblLookupResult.setIcon(FlagImageLib.getByCountryCode(range.getCountry()));
                lblLookupResult.setText(range.toString());
            }
        });
    }


    ///////////////////
    // INNER CLASSES //
    ///////////////////

    private class InnerFormPanel extends NewRFormPanel {
        public InnerFormPanel() {
            init();
        }

        @Override
        protected void addFields() {

            // DBIP CSV File
            pnlFile = new FileSelectionPanel(IpGeoTestFrame.this).setPath(DEFAULT_FILE);
            addField(
                new FieldDescriptor()
                    .setCaption("DBIP CSV")
                    .setComponent(pnlFile)
                    .setFill(true)
            );

            // Actions
            JButton btnLoad = Lay.btn("&Load", CommonConcepts.PROGRESS, (ActionListener) e -> {
                RWorker<Void, IpGeoMapMaster> worker = new RWorker<Void, IpGeoMapMaster>() {
                    @Override
                    protected IpGeoMapMaster background(Void gathered) throws Exception {
                        return DbipIpGeoMapLoader.parseMasterFromCsvFile(pnlFile.getPath(), ttContext);
                    }
                    @Override
                    protected void complete() {
                        try {
                            master = getResult();
                            cboIp.setEnabled(true);
                            btnCheck.setEnabled(true);
                            btnPrintStats.setEnabled(true);
                            btnCompress.setEnabled(true);
                            Number[] ipV4Stats = master.ipV4Map.getLoadedMapCount();
                            Number[] ipV6Stats = master.ipV6Map.getLoadedMapCount();

                            String info =
                                "IPv4: " + master.ipV4Map.getRangeCount() + "/" + ipV4Stats[0] + ";" + ipV4Stats[1] + ", " +
                                "IPv6: " + master.ipV6Map.getRangeCount() + "/" + ipV6Stats[0] + ";" + ipV6Stats[1];
                            lblLoadResult.setText(info);
                        } catch(Exception e) {
                            Dialogs.showDetails(IpGeoTestFrame.this, e);
                        }
                    }
                };
                addTaskAndExecuteFg("Loading data", worker);
            });
            btnPrintStats = Lay.btn("&Print", (ActionListener) e -> master.printDist(), "!enabled");
            btnCompress = Lay.btn("&Compress", (ActionListener) e -> master.compress(), "!enabled");
            addField(
                new FieldDescriptor()
                .setCaption("Actions")
                    .setComponent(Lay.FL(
                        btnLoad, Lay.hs(5), btnPrintStats, Lay.hs(5), btnCompress,
                        "nogap"
                    ))
            );

            // Load Result
            lblLoadResult = Lay.lb();
            addField(
                new FieldDescriptor()
                    .setCaption("Load Result")
                    .setComponent(lblLoadResult)
            );

            // Lookup
            cboIp = Lay.cb(mdlIp, "selectall,prefw=400,enabled=false,recent,editable=true");
            cboIp.setInputCleaner(e -> e.trim());
            btnCheck = Lay.btn("Check", CommonConcepts.ACCEPT, "enabled=false");
            addField(
                new FieldDescriptor()
                    .setCaption("Lookup")
                    .setComponent(Lay.FL(
                        cboIp, Lay.hs(5), btnCheck,
                        "nogap"
                    ))
            );

            // Lookup Result
            lblLookupResult = Lay.lb();
            addField(
                new FieldDescriptor()
                    .setCaption("Lookup Result")
                    .setComponent(lblLookupResult)
            );
        }
    }

    //////////
    // TEST //
    //////////

    public static void main(String[] args) {
        IpGeoTestFrame win = new IpGeoTestFrame();
        win.setVisible(true);
    }
}
