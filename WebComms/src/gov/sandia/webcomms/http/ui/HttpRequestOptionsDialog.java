package gov.sandia.webcomms.http.ui;

import java.awt.Window;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;

import gov.sandia.webcomms.http.options.HttpRequestOptions;
import gov.sandia.webcomms.plugin.WebCommsPlugin;
import replete.plugins.PluginManager;
import replete.plugins.RepletePlugin;
import replete.ui.images.concepts.CommonConcepts;
import replete.ui.lay.Lay;
import replete.ui.windows.escape.EscapeDialog;

public class HttpRequestOptionsDialog extends EscapeDialog {


    ////////////
    // FIELDS //
    ////////////

    public static final int SET = 0;
    public static final int CANCEL = 1;

    private int result = CANCEL;

    private HttpRequestOptionsPanel pnlOptions;


    //////////////////
    // CONSTRUCTORS //
    //////////////////

    public HttpRequestOptionsDialog(JDialog parent, HttpRequestOptions options) {
        super(parent, "HTTP Request Options", true);
        init(options);
    }
    public HttpRequestOptionsDialog(JFrame parent, HttpRequestOptions options) {
        super(parent, "HTTP Request Options", true);
        init(options);
    }
    public HttpRequestOptionsDialog(Window parent, HttpRequestOptions options) {
        super(parent, "HTTP Request Options", true);
        init(options);
    }

    private void init(HttpRequestOptions options) {
        setIcon(CommonConcepts.OPTIONS);

        JButton btnSet;
        Lay.BLtg(this,
      ///     "N", Lay.lb("<html><i>Set the options for resource requests:</i></html>", "eb=5"),
            "C", pnlOptions = new HttpRequestOptionsPanel(),
            "S", Lay.FL("R",
                btnSet = Lay.btn("Se&t", CommonConcepts.ACCEPT),
                Lay.btn("&Cancel", CommonConcepts.CANCEL, "closer"),
                "bg=100,mb=[1t,black]"
            ),
            "db=Set,size=[650,685],center"
        );

        btnSet.addActionListener(e -> {
            result = SET;
            close();
        });

        pnlOptions.set(options);
    }


    ////////////
    // RESULT //
    ////////////

    public int getResult() {
        return result;
    }
    public HttpRequestOptions getOptions() {
        return pnlOptions.get();
    }


    //////////
    // TEST //
    //////////

    public static void main(String[] args) {
        PluginManager.initialize(
            RepletePlugin.class,
            WebCommsPlugin.class
        );
        HttpRequestOptions options = new HttpRequestOptions()
            .setSaveContent(false)
        ;
        HttpRequestOptionsDialog dlg = new HttpRequestOptionsDialog((JFrame) null, options);
        dlg.setVisible(true);
        if(dlg.getResult() == HttpRequestOptionsDialog.SET) {
            System.out.println(dlg.getOptions());
            System.out.println(dlg.getOptions().toStringLong());
        }
    }
}
