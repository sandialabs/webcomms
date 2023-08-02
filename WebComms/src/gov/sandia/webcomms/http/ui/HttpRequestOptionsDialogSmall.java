package gov.sandia.webcomms.http.ui;

import java.awt.Color;
import java.awt.Font;
import java.awt.Window;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.font.TextAttribute;
import java.util.Map;

import javax.swing.Box;
import javax.swing.JDialog;
import javax.swing.JFrame;

import gov.sandia.webcomms.http.allowrc.nofollow.NoFollowHttpAllowRedirectCriteriaParams;
import gov.sandia.webcomms.http.options.HttpRequestOptions;
import replete.numbers.NumUtil;
import replete.ui.button.RButton;
import replete.ui.button.RCheckBox;
import replete.ui.images.concepts.CommonConcepts;
import replete.ui.lay.Lay;
import replete.ui.text.RTextField;
import replete.ui.windows.escape.EscapeDialog;

// TODO: This class is being transitioned out in favor of
// HttpRequestOptionsDialg.  The reason is the latter is
// just contains a bean panel with all the options and
// that panel could be used in other contexts.  Whereas,
// this dialog box, although compact and pleasant to
// use, has the controls directly in the dialog class
// itself and does not currently manifest all the
// properties of an HttpRequestOptions object.

public class HttpRequestOptionsDialogSmall extends EscapeDialog {


    ////////////
    // FIELDS //
    ////////////

    public static final int SET = 0;
    public static final int CANCEL = 1;

    private int result = CANCEL;

    private RCheckBox chkCleanUrls;
    private RCheckBox chkSaveContent;
    private RCheckBox chkSaveResponseHeaders;
    private RCheckBox chkSaveRedirects;
    private RCheckBox chkSaveRedirectResponseHeaders;
    private RCheckBox chkSaveSecurity;
    private RCheckBox chkSaveRequest;
    private RCheckBox chkFollowRedirects;
    private RTextField txtOverrideUserAgent;
    private RTextField txtProxyHost;
    private RTextField txtProxyPort;

    private RCheckBox[] allCheckboxes;

    private RButton btnSet;
    private RButton btnCancel;

    private HttpRequestOptions options;
    private String proxyHost;
    private int proxyPort;


    /////////////////
    // CONSTRUCTOR //
    /////////////////

    public HttpRequestOptionsDialogSmall(JDialog parent, HttpRequestOptions options, String proxyHost, int proxyPort) {
        super(parent, "HTTP Request Options", true);
        init(options, proxyHost, proxyPort);
    }
    public HttpRequestOptionsDialogSmall(JFrame parent, HttpRequestOptions options, String proxyHost, int proxyPort) {
        super(parent, "HTTP Request Options", true);
        init(options, proxyHost, proxyPort);
    }
    public HttpRequestOptionsDialogSmall(Window parent, HttpRequestOptions options, String proxyHost, int proxyPort) {
        super(parent, "HTTP Request Options", true);
        init(options, proxyHost, proxyPort);
    }
    private void init(HttpRequestOptions options, String proxyHost, int proxyPort) {
        this.options = options;
        this.proxyHost = proxyHost;
        this.proxyPort = proxyPort;
        setIcon(CommonConcepts.OPTIONS);

        Lay.BLtg(this,
            "N", Lay.lb("<html><i>Set the options for resource requests:</i></html>", "eb=5,bg=white,augb=mb(1,black)"),
            "C", Lay.BxL(
                chkCleanUrls = Lay.chk("Clean URLs", "alignx=0"),
                chkSaveContent = Lay.chk("Save Content", "alignx=0"),
                chkSaveResponseHeaders = Lay.chk("Save Response Headers", "alignx=0"),
                chkSaveRedirects = Lay.chk("Save Redirects", "alignx=0"),
                chkSaveRedirectResponseHeaders = Lay.chk("Save Redirect Response Headers", "alignx=0"),
                chkSaveSecurity = Lay.chk("Save Security", "alignx=0"),
                chkSaveRequest = Lay.chk("Save Request", "alignx=0"),
                chkFollowRedirects = Lay.chk("Follow Redirects", "alignx=0"),
                Lay.BL(
                    "W", Lay.lb("Override User Agent: ", "prefw=130"),
                    "C", txtOverrideUserAgent = Lay.tx("", "selectall"),
                    "alignx=0,dimh=25"
                ),
                Box.createVerticalStrut(5),
                Lay.BL(
                    "W", Lay.lb("Proxy (Client Only): ", "prefw=130"),
                    "C", txtProxyHost = Lay.tx("", "selectall"),
                    "E", Lay.BL(
                        "W", Lay.lb(" : "),
                        "C", txtProxyPort = Lay.tx("", "selectall,center"),
                        "prefw=55"
                    ),
                    "alignx=0,dimh=25"
                ),
                Box.createVerticalGlue(),
                "eb=15l5tr"
            ),
            "S", Lay.FL("R",
                btnSet = Lay.btn("Se&t", CommonConcepts.ACCEPT),
                btnCancel = Lay.btn("&Cancel", CommonConcepts.CANCEL),
                "bg=100,mb=[1t,black]"
            ),
            "size=[480,360],center"
        );

        allCheckboxes = new RCheckBox[] {
             chkCleanUrls,
             chkSaveContent,
             chkSaveResponseHeaders,
             chkSaveRedirects,
             chkSaveRedirectResponseHeaders,
             chkSaveSecurity,
             chkSaveRequest,
             chkFollowRedirects
        };

        for(final RCheckBox chk : allCheckboxes) {
            chk.addItemListener(new ItemListener() {
                public void itemStateChanged(ItemEvent e) {
                    Map attributes = chk.getFont().getAttributes();
                    if(!chk.isSelected()) {
                        attributes.put(TextAttribute.STRIKETHROUGH, TextAttribute.STRIKETHROUGH_ON);
                        attributes.put(TextAttribute.FOREGROUND, Color.black);
                    } else {
                        attributes.remove(TextAttribute.STRIKETHROUGH);
                        attributes.put(TextAttribute.FOREGROUND, Lay.clr("029100"));
                    }
                    chk.setFont(new Font(attributes));
                }
            });
        }

        setDefaultButton(btnSet);

        btnSet.addActionListener(e -> {
            saveToSource();
            result = SET;
            close();
        });
        btnCancel.addActionListener(e -> close());

        readFromSource();
        updateCheckboxes();
    }

    private void updateCheckboxes() {
        for(final RCheckBox chk : allCheckboxes) {
            Map attributes = chk.getFont().getAttributes();
            if(!chk.isSelected()) {
                attributes.put(TextAttribute.STRIKETHROUGH, TextAttribute.STRIKETHROUGH_ON);
                attributes.put(TextAttribute.FOREGROUND, Color.black);
            } else {
                attributes.remove(TextAttribute.STRIKETHROUGH);
                attributes.put(TextAttribute.FOREGROUND, Lay.clr("029100"));
            }
            chk.setFont(new Font(attributes));
        }
    }


    ////////////
    // RESULT //
    ////////////

    public int getResult() {
        return result;
    }
    public String getProxyHost() {
        return proxyHost;
    }
    public int getProxyPort() {
        return proxyPort;
    }
    // shared 'options' modified in place


    //////////
    // SAVE //
    //////////

    private void readFromSource() {
        chkCleanUrls.setSelected(options.isCleanUrls());
        chkSaveContent.setSelected(options.isSaveContent());
        chkSaveResponseHeaders.setSelected(options.isSaveResponseHeaders());
        chkSaveRedirects.setSelected(options.isSaveRedirects());
        chkSaveRedirectResponseHeaders.setSelected(options.isSaveRedirectResponseHeaders());
        chkSaveSecurity.setSelected(options.isSaveSecurity());
        chkSaveRequest.setSelected(options.isSaveRequest());
        chkFollowRedirects.setSelected(options.getAllowRedirectCriteriaParams() == null);
        txtOverrideUserAgent.setText(options.getOverrideUserAgent());
        if(proxyHost != null) {
            txtProxyHost.setText(proxyHost);
            txtProxyPort.setText(proxyPort);
        }
    }

    private void saveToSource() {
        options.setCleanUrls(chkCleanUrls.isSelected());
        options.setSaveContent(chkSaveContent.isSelected());
        options.setSaveResponseHeaders(chkSaveResponseHeaders.isSelected());
        options.setSaveRedirects(chkSaveRedirects.isSelected());
        options.setSaveRedirectResponseHeaders(chkSaveRedirectResponseHeaders.isSelected());
        options.setSaveSecurity(chkSaveSecurity.isSelected());
        options.setSaveRequest(chkSaveRequest.isSelected());
        //options.setReplaceAjaxFragment(chkXXX.isSelected());

        if(!chkFollowRedirects.isSelected()) {
            options.setAllowRedirectCriteriaParams(new NoFollowHttpAllowRedirectCriteriaParams());
        } else {
            options.setAllowRedirectCriteriaParams(null);
        }

        String ua = txtOverrideUserAgent.getTrimmed();
        options.setOverrideUserAgent(ua.equals("") ? null : ua);

        String px = txtProxyHost.getTrimmed();
        if(px.equals("")) {
            proxyHost = null;
            proxyPort = 0;
        } else {
            proxyHost = px;
            proxyPort = NumUtil.i(txtProxyPort.getTrimmed(), 0);
        }
    }


    //////////
    // TEST //
    //////////

    public static void main(String[] args) {
        HttpRequestOptionsDialogSmall dlg = new HttpRequestOptionsDialogSmall((JFrame) null, new HttpRequestOptions(), null, 0);
        dlg.setVisible(true);
    }
}
