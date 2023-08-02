package gov.sandia.webcomms.http.ui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Window;
import java.awt.event.ActionListener;
import java.util.List;

import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;

import gov.sandia.webcomms.http.allowrc.HttpAllowRedirectCriteriaGenerator;
import gov.sandia.webcomms.http.allowrc.HttpAllowRedirectCriteriaParams;
import gov.sandia.webcomms.http.consumeec.HttpConsumeEntityCriteriaGenerator;
import gov.sandia.webcomms.http.consumeec.HttpConsumeEntityCriteriaParams;
import gov.sandia.webcomms.http.options.HttpRequestOptions;
import gov.sandia.webcomms.http.preemptors.HttpRequestPreemptorGenerator;
import gov.sandia.webcomms.http.preemptors.HttpRequestPreemptorParams;
import gov.sandia.webcomms.plugin.WebCommsPlugin;
import replete.numbers.NumUtil;
import replete.plugins.ExtensionPoint;
import replete.plugins.Generator;
import replete.plugins.ParamsAndPanelUiGenerator;
import replete.plugins.PluginManager;
import replete.plugins.RepletePlugin;
import replete.plugins.UiGenerator;
import replete.plugins.ui.GeneratorWrapper;
import replete.text.StringUtil;
import replete.ui.BeanPanel;
import replete.ui.GuiUtil;
import replete.ui.button.RCheckBox;
import replete.ui.combo.RComboBox;
import replete.ui.form2.FieldDescriptor;
import replete.ui.form2.HelpStyle;
import replete.ui.form2.NewRFormPanel;
import replete.ui.images.concepts.CommonConcepts;
import replete.ui.images.concepts.ImageLib;
import replete.ui.lay.Lay;
import replete.ui.text.RTextField;
import replete.ui.text.validating.PositiveIntContextValidator;
import replete.ui.text.validating.PositiveLongContextValidator;
import replete.ui.text.validating.ValidatingTextField;
import replete.ui.validation.ValidationContext;
import replete.ui.windows.Dialogs;
import replete.ui.windows.escape.EscapeDialog;

// WebComms complexity is increasing and the ability to parameterize requests
// is increasing and as such, this panel may eventually need tabs (or drop-down,
// etc.) to section of all of the options into logical sections.

public class HttpRequestOptionsPanel extends BeanPanel<HttpRequestOptions> {


    ////////////
    // FIELDS //
    ////////////

    private JCheckBox chkCleanUrls;
    private JCheckBox chkIgnoreSslProblems;
    private JCheckBox chkPrintExecutionTrace;
    private JCheckBox chkSaveContent;
    private JCheckBox chkSaveResponseHeaders;
    private JCheckBox chkSaveRedirects;
    private JCheckBox chkSaveRedirectResponseHeaders;
    private JCheckBox chkSaveRedirectResponseContent;
    private JCheckBox chkSaveSecurity;
    private JCheckBox chkSaveRequest;
    private ValidatingTextField txtMaxContentLength;
    private ValidatingTextField txtTimeout;
    private RTextField txtOverrideUserAgent;
    private RTextField txtProxyHost;
    private RTextField txtProxyPort;

    private RComboBox<WrapperTuple> cboARC;
    private DefaultComboBoxModel<WrapperTuple> mdlARC;
    private JButton btnEditARC;
    private RComboBox<WrapperTuple> cboCEC;
    private DefaultComboBoxModel<WrapperTuple> mdlCEC;
    private JButton btnEditCEC;
    private RComboBox<WrapperTuple> cboPreemptor;
    private DefaultComboBoxModel<WrapperTuple> mdlPreemptor;
    private JButton btnEditPreemptor;


    //////////////////
    // CONSTRUCTORS //
    //////////////////

    public HttpRequestOptionsPanel() {
        Lay.BLtg(this,
            "C", Lay.sp(new InnerFormPanel(), "eb=0")
        );
    }

//  private void updateCheckboxes() {
//      for(final RCheckBox chk : allCheckboxes) {
//          Map attributes = chk.getFont().getAttributes();
//          if(!chk.isSelected()) {
//              attributes.put(TextAttribute.STRIKETHROUGH, TextAttribute.STRIKETHROUGH_ON);
//              attributes.put(TextAttribute.FOREGROUND, Color.black);
//          } else {
//              attributes.remove(TextAttribute.STRIKETHROUGH);
//              attributes.put(TextAttribute.FOREGROUND, Lay.clr("029100"));
//          }
//          chk.setFont(new Font(attributes));
//      }
//  }


    //////////////////////////
    // ACCESSORS / MUTATORS //
    //////////////////////////

    // Accessors (Computed)

    @Override
    public HttpRequestOptions get() {
        return new HttpRequestOptions()
            .setCleanUrls(chkCleanUrls.isSelected())
            .setIgnoreSslProblems(chkIgnoreSslProblems.isSelected())
            .setPrintExecutionTrace(chkPrintExecutionTrace.isSelected())
            .setSaveContent(chkSaveContent.isSelected())
            .setSaveResponseHeaders(chkSaveResponseHeaders.isSelected())
            .setSaveRedirects(chkSaveRedirects.isSelected())
            .setSaveRedirectResponseHeaders(chkSaveRedirectResponseHeaders.isSelected())
            .setSaveRedirectResponseContent(chkSaveRedirectResponseContent.isSelected())
            .setSaveSecurity(chkSaveSecurity.isSelected())
            .setSaveRequest(chkSaveRequest.isSelected())
            .setMaxContentLength(txtMaxContentLength.getLong())
            .setTimeout(txtTimeout.getInteger())
            .setOverrideUserAgent(StringUtil.forceBlankNull(txtOverrideUserAgent.getTrimmed()))
            .setProxyHost(StringUtil.forceBlankNull(txtProxyHost.getTrimmed()))
            .setProxyPort(txtProxyPort.getInteger() == null ? 0 : txtProxyPort.getInteger())
            .setAllowRedirectCriteriaParams((HttpAllowRedirectCriteriaParams) cboARC.getSelected().params)
            .setConsumeEntityCriteriaParams((HttpConsumeEntityCriteriaParams) cboCEC.getSelected().params)
            .setRequestPreemptorParams((HttpRequestPreemptorParams) cboPreemptor.getSelected().params)
        ;
    }

    // Mutators

    @Override
    public void set(HttpRequestOptions options) {
        chkCleanUrls.setSelected(options.isCleanUrls());
        chkIgnoreSslProblems.setSelected(options.isIgnoreSslProblems());
        chkPrintExecutionTrace.setSelected(options.isPrintExecutionTrace());
        chkSaveContent.setSelected(options.isSaveContent());
        chkSaveResponseHeaders.setSelected(options.isSaveResponseHeaders());
        chkSaveRedirects.setSelected(options.isSaveRedirects());
        chkSaveRedirectResponseHeaders.setSelected(options.isSaveRedirectResponseHeaders());
        chkSaveRedirectResponseContent.setSelected(options.isSaveRedirectResponseContent());
        chkSaveSecurity.setSelected(options.isSaveSecurity());
        chkSaveRequest.setSelected(options.isSaveRequest());
        txtMaxContentLength.setValidText("" + options.getMaxContentLength());
        txtTimeout.setValidText("" + options.getTimeout());
        txtOverrideUserAgent.setText(options.getOverrideUserAgent());
        txtProxyHost.setText(options.getProxyHost());
        txtProxyPort.setText(options.getProxyPort() == 0 ? "" : options.getProxyPort());

        setConfigurableComboBox(cboARC, mdlARC, options.getAllowRedirectCriteriaParams());
        setConfigurableComboBox(cboCEC, mdlCEC, options.getConsumeEntityCriteriaParams());
        setConfigurableComboBox(cboPreemptor, mdlPreemptor, options.getRequestPreemptorParams());

        updateSelectionState();
    }

    private void setConfigurableComboBox(RComboBox cbo, DefaultComboBoxModel mdl, Object params) {
        WrapperTuple tuple = findTupleWithParams(cbo, mdl, params);
        if(tuple == null) {
            tuple = findTupleWithParams(cbo, mdl, null);   // Finds the NONE tuple
        }
        cbo.setSelectedItemForce(tuple);
        tuple.params = params;
    }

    private WrapperTuple findTupleWithParams(RComboBox cbo, DefaultComboBoxModel mdl, Object params) {
        for(int e = 0; e < mdl.getSize(); e++) {
            WrapperTuple tuple = (WrapperTuple) mdl.getElementAt(e);
            if(params == null && tuple.params == null ||
                    params != null && tuple.params != null &&
                    params.getClass().equals(tuple.params.getClass())) {
                return tuple;
            }
        }
        return null;
    }

    @Override
    public void validateInput(ValidationContext context) {
        context.check("Max Content Length", txtMaxContentLength);
        context.check("Timeout", txtTimeout);
    }

    private void updateSelectionState() {
        WrapperTuple tuple = cboPreemptor.getSelected();
        UiGenerator generator = tuple.wrapper.getGenerator();
        boolean enabled = (generator == null);
        chkCleanUrls.setEnabled(enabled);
        chkIgnoreSslProblems.setEnabled(enabled);
        chkPrintExecutionTrace.setEnabled(enabled);     // Should this be enabled?  Perhaps pre-emptors can use this facility.  It is actually in a separate category.
        chkSaveContent.setEnabled(enabled);
        chkSaveResponseHeaders.setEnabled(enabled);
        chkSaveRedirects.setEnabled(enabled);
        chkSaveRedirectResponseHeaders.setEnabled(enabled && chkSaveRedirects.isSelected());
        chkSaveRedirectResponseContent.setEnabled(enabled && chkSaveRedirects.isSelected());
        chkSaveSecurity.setEnabled(enabled);
        chkSaveRequest.setEnabled(enabled);
        txtMaxContentLength.setEnabled(enabled);
        txtMaxContentLength.setBackground(Color.white);    // To correct Swing irregularities
        //txtTimeout.setEnabled(enabled);                  // Not modifiable
        txtOverrideUserAgent.setEnabled(enabled);
        txtProxyHost.setEnabled(enabled);
        txtProxyPort.setEnabled(enabled);
        cboARC.setEnabled(enabled);
        cboCEC.setEnabled(enabled);

        WrapperTuple tupleARC = cboARC.getSelected();
        UiGenerator generatorARC = tupleARC.wrapper.getGenerator();
        boolean enabledARC = (generatorARC != null);
        btnEditARC.setEnabled(enabled && enabledARC);

        WrapperTuple tupleCEC = cboCEC.getSelected();
        UiGenerator generatorCEC = tupleCEC.wrapper.getGenerator();
        boolean enabledCEC = (generatorCEC != null);
        btnEditCEC.setEnabled(enabled && enabledCEC);

        btnEditPreemptor.setEnabled(!enabled);
    }

    public void setEditable(boolean editable) {
        JComponent[] checkBoxes = new JComponent[] {
            chkCleanUrls,
            chkIgnoreSslProblems,
            chkPrintExecutionTrace,
            chkSaveContent,
            chkSaveResponseHeaders,
            chkSaveRedirects,
            chkSaveRedirectResponseHeaders,
            chkSaveRedirectResponseContent,
            chkSaveSecurity,
            chkSaveRequest
        };

        if(editable) {
            for(JComponent cmp : checkBoxes) {
                Lay.hn(cmp, "editable=true,cursor=hand");
            }
        } else {
            for(JComponent cmp : checkBoxes) {
                Lay.hn(cmp, "editable=false,cursor=default");
            }
        }

        txtMaxContentLength.setEditable(editable);
        txtTimeout.setEditable(editable);             // ??
        txtOverrideUserAgent.setEditable(editable);
        txtProxyHost.setEditable(editable);
        txtProxyPort.setEditable(editable);
//        txtTimeout.setEditable(editable);           // Currently disabled, unnecessary for now
        cboARC.setSelectionChangeable(editable);
        cboCEC.setSelectionChangeable(editable);
        cboPreemptor.setSelectionChangeable(editable);
    }


    ///////////////////
    // INNER CLASSES //
    ///////////////////

    private class InnerFormPanel extends NewRFormPanel {

        public InnerFormPanel() {
            super(210);
            setMargin(5);                // These compact the form a little bit
            setInterFieldSpacing(5);     // for readability.
            init();
        }

        @Override
        public void addFields() {

            // Clean URLs
            chkCleanUrls = chk("<i>(whether to clean URLs before request)</i>");
            addField(
                new FieldDescriptor()
                    .setCaption("Clean URLs?")
                    .setComponent(chkCleanUrls)
                    .setHeight(24)
            );

            // Ignore SSL Problems
            chkIgnoreSslProblems = chk("<i>(whether to ignore SSL problems: self-signed, expired, bad ciphers, etc.)</i>");
            addField(
                new FieldDescriptor()
                    .setCaption("Ignore SSL Problems?")
                    .setComponent(chkIgnoreSslProblems)
                    .setHeight(24)
            );

            // Print Execution Trace
            chkPrintExecutionTrace = chk("<i>(whether to display debug information on the server for each request)</i>");
            addField(
                new FieldDescriptor()
                    .setCaption("Print Execution Trace?")
                    .setComponent(chkPrintExecutionTrace)
                    .setHeight(24)
            );

            // Save Content
            chkSaveContent = chk("<i>(whether to save the actual bytes returned from server)</i>");
            addField(
                new FieldDescriptor()
                    .setCaption("Save Content?")
                    .setComponent(chkSaveContent)
                    .setHeight(24)
            );

            // Save Response Headers
            chkSaveResponseHeaders = chk("<i>(whether to save the response headers)</i>");
            addField(
                new FieldDescriptor()
                    .setCaption("Save Response Headers?")
                    .setComponent(chkSaveResponseHeaders)
                    .setHeight(24)
            );

            // Save Redirects
            chkSaveRedirects = chk("<i>(whether to save redirect responses)</i>");
            chkSaveRedirects.addActionListener(e -> updateSelectionState());    // Turns the 2 checkboxes below on/off
            addField(
                new FieldDescriptor()
                    .setCaption("Save Redirects?")
                    .setComponent(chkSaveRedirects)
                    .setHeight(24)
            );

            // Save Redirect Response Headers
            chkSaveRedirectResponseHeaders = chk("<i>(whether to save the redirects' response headers)</i>");
            addField(
                new FieldDescriptor()
                    .setCaption("Save Redirect Response Headers?")
                    .setComponent(chkSaveRedirectResponseHeaders)
                    .setHeight(24)
            );

            // Save Redirect Response Content
            chkSaveRedirectResponseContent = chk("<i>(whether to save the redirects' response content)</i>");
            addField(
                new FieldDescriptor()
                    .setCaption("Save Redirect Response Content?")
                    .setComponent(chkSaveRedirectResponseContent)
                    .setHeight(24)
            );

            // Save Security
            chkSaveSecurity = chk("<i>(whether to save SSL details)</i>");
            addField(
                new FieldDescriptor()
                    .setCaption("Save Security?")
                    .setComponent(chkSaveSecurity)
                    .setHeight(24)
            );

            // Save Request
            chkSaveRequest = chk("<i>(whether to save request info, original and redirects)</i>");
            addField(
                new FieldDescriptor()
                    .setCaption("Save Request?")
                    .setComponent(chkSaveRequest)
                    .setHeight(24)
            );

            // Max Content Length
            txtMaxContentLength = Lay.tx("", 8, new PositiveLongContextValidator(), "selectall,prefh=24");
            txtMaxContentLength.setUnvalidatableDecider(null);
            addField(
                new FieldDescriptor()
                    .setCaption("Max Content Length")
                    .setComponent(Lay.FL("L",
                        txtMaxContentLength,
                        Lay.lb("B ", "eb=5l"),
                        "nogap"
                    ))
                    .setHeight(24)
                    .setHelpText("<i>(maximum download size per request)</i>")
                    .setHelpStyle(HelpStyle.INLINE_RIGHT)
            );

            // Timeout
            txtTimeout = Lay.tx("", 8, new PositiveIntContextValidator(), "selectall,prefh=24,enabled=false");
            txtTimeout.setUnvalidatableDecider(null);
            addField(
                new FieldDescriptor()
                    .setCaption("Timeout")
                    .setComponent(Lay.FL("L",
                        txtTimeout,
                        Lay.lb("ms ", "eb=5l"),
                        "nogap"
                    ))
                    .setHeight(24)
                    .setHelpText("<i>(timeout interval per request)</i>")
                    .setHelpStyle(HelpStyle.INLINE_RIGHT)
            );

            // Override User Agent
            txtOverrideUserAgent = Lay.tx("", "selectall,prefw=300,prefh=24");
            addField(
                new FieldDescriptor()                          // Doesn't expand...
                    .setCaption("Override User Agent")
                    .setComponent(txtOverrideUserAgent)
                    .setHeight(24)
            );

            // Proxy
            txtProxyHost = Lay.tx("", "selectall,prefw=200,prefh=24");
            txtProxyPort = Lay.tx("", "selectall,center,prefw=55,prefh=24");
            addField(
                new FieldDescriptor()
                    .setCaption("Proxy")
                    .setComponent(Lay.FL("L",
                        txtProxyHost,
                        Lay.lb(" : "),
                        txtProxyPort,
                        "nogap"
                    ))
                    .setHeight(24)
            );

            // Allow Redirect Criteria
            Object[] cmpARC = createConfigurableExtensibleComboBox(
                HttpAllowRedirectCriteriaGenerator.class,
                "allows request responses to be generated internally", "Allow Redirect Criteria",
                "Will always allow redirects"
            );
            JPanel pnlARC = (JPanel) cmpARC[0];
            cboARC        = (RComboBox<WrapperTuple>) cmpARC[1];
            mdlARC        = (DefaultComboBoxModel<WrapperTuple>) cmpARC[2];
            btnEditARC    = (JButton) cmpARC[3];
            addField(
                new FieldDescriptor()
                    .setCaption("Allow Redirect Criteria")
                    .setComponent(pnlARC)
                    .setHeight(26)
            );

            // Consume Entity Criteria
            Object[] cmpCEC = createConfigurableExtensibleComboBox(
                HttpConsumeEntityCriteriaGenerator.class,
                "allows request responses to be generated internally", "Consume Response Body",
                "Will always consume response body"
            );
            JPanel pnlCEC = (JPanel) cmpCEC[0];
            cboCEC        = (RComboBox<WrapperTuple>) cmpCEC[1];
            mdlCEC        = (DefaultComboBoxModel<WrapperTuple>) cmpCEC[2];
            btnEditCEC    = (JButton) cmpCEC[3];
            addField(
                new FieldDescriptor()
                    .setCaption("Consume Response Body")
                    .setComponent(pnlCEC)
                    .setHeight(26)
            );

            // Preemptor
            Object[] cmpRP = createConfigurableExtensibleComboBox(
                HttpRequestPreemptorGenerator.class,
                "Allows HTTP responses to be completely generated internally, " +
                "without any communication over the internet.", "Request Preemptor",
                "Will not perform any preemption"
            );
            JPanel pnlPreemptor = (JPanel) cmpRP[0];
            cboPreemptor        = (RComboBox<WrapperTuple>) cmpRP[1];
            mdlPreemptor        = (DefaultComboBoxModel<WrapperTuple>) cmpRP[2];
            btnEditPreemptor    = (JButton) cmpRP[3];
            cboPreemptor.addActionListener(e -> updateSelectionState());    // Turns other controls on/off
            addField(
                new FieldDescriptor()
                    .setCaption("Request Preemptor")
                    .setComponent(pnlPreemptor)
                    .setHeight(26)
            );

            // Make 3 combo boxes above have an equal width
            int maxPrefW = NumUtil.max(cboARC.getPreferredSize().width, cboCEC.getPreferredSize().width, cboPreemptor.getPreferredSize().width);
            Lay.hn(cboARC, "prefw=" + maxPrefW);
            Lay.hn(cboCEC, "prefw=" + maxPrefW);
            Lay.hn(cboPreemptor, "prefw=" + maxPrefW);
        }

        private RCheckBox chk(String title) {      // Small convenience method
            return Lay.chk("<html>" + title + "</html>", "cursor=hand");   // Should be 24 pix high by default
        }

        private Object[] createConfigurableExtensibleComboBox(
               Class<? extends ParamsAndPanelUiGenerator> clazz, String desc,
               String title, String noneLabel) {
            DefaultComboBoxModel<WrapperTuple> mdl = new DefaultComboBoxModel<>();
            mdl.addElement(
                new WrapperTuple(new GeneratorWrapper(null, noneLabel), null));
            RComboBox<WrapperTuple> cbo = Lay.cb(mdl, new WrapperTupleRenderer());
            List<ExtensionPoint> exts = PluginManager.getExtensionsForPoint((Class<? extends ExtensionPoint>) clazz);
            for(ExtensionPoint ext : exts) {
                ParamsAndPanelUiGenerator gen = (ParamsAndPanelUiGenerator) ext;
                Object params = gen.createParams();
                mdl.addElement(
                    new WrapperTuple(new GeneratorWrapper(gen), params)
                );
            }
            JButton btnEdit, btnInfo;
            JPanel pnl = Lay.BL(
                "C", cbo,
                "E", Lay.GBL(
                    Lay.FL(
                        btnEdit = Lay.btn(CommonConcepts.EDIT, 2),
                        Lay.p(btnInfo = Lay.btn(CommonConcepts.INFO, 2), "eb=5l"),
                        "eb=5l,nogap"
                    )
                )
            );
            btnEdit.addActionListener(e -> {
                WrapperTuple tuple = cbo.getSelected();
                Object params = tuple.params;
                ParamsAndPanelUiGenerator generator = Generator.lookup(params);
                BeanPanel pnlParams = generator.createParamsPanel();
                pnlParams.set(params);
                Window win = GuiUtil.win(this);
                EscapeDialog dlg;
                JButton btnSet, btnCancel;
                Lay.BLtg(dlg = Lay.dlg(win, title + " Parameters: " + generator.getName(), CommonConcepts.OPTIONS),
                    "C", pnlParams,
                    "S", Lay.FL("R",
                        btnSet = Lay.btn("&Set", CommonConcepts.ACCEPT),
                        btnCancel = Lay.btn("&Cancel", CommonConcepts.CANCEL),
                        "bg=100"
                    ),
                    "size=600,center"
                );
                btnSet.addActionListener(e2 -> {
                    tuple.params = pnlParams.get();
                    dlg.close();
                });
                btnCancel.addActionListener(e2 -> dlg.close());
                dlg.setVisible(true);
            });
            btnInfo.addActionListener(e -> {
                WrapperTuple tuple = cbo.getSelected();
                UiGenerator generator = tuple.wrapper.getGenerator();
                String msg = desc + "\n\nSelected:\n" +
                (generator == null ? "  (No Selection)" : "  " + generator.getDescription());
                String extra = generator == null ? "" : ": " + generator.getName();
                Window win = GuiUtil.win(this);
                Dialogs.showMessage(win, msg, title + " Information" + extra);
            });
            cbo.addActionListener(e -> updateSelectionState());
            return new Object[] {pnl, cbo, mdl, btnEdit};
        }
    }

    private class WrapperTuple {
        GeneratorWrapper wrapper;
        Object params;
        public WrapperTuple(GeneratorWrapper wrapper, Object params) {
            this.wrapper = wrapper;
            this.params = params;
        }
        @Override
        public String toString() {
            return wrapper.toString();
        }
    }

    private class WrapperTupleRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList list, Object value, int index,
                                                      boolean isSelected, boolean cellHasFocus) {
            JLabel renderer =
                (JLabel) super.getListCellRendererComponent(
                    list, value, index, isSelected, cellHasFocus);
            WrapperTuple tuple = (WrapperTuple) value;
            GeneratorWrapper wrapper = tuple.wrapper;
            UiGenerator generator = wrapper.getGenerator();
            if(generator != null) {
                renderer.setIcon(generator.getIcon());
            } else {
                renderer.setIcon(ImageLib.get(CommonConcepts.CLOSE));
            }
            return renderer;
        }
    }


    //////////
    // TEST //
    //////////

    public static void main(String[] args) {
        PluginManager.initialize(
            RepletePlugin.class,
            WebCommsPlugin.class
        );
        HttpRequestOptionsPanel pnl = new HttpRequestOptionsPanel();
        Lay.BLtg(Lay.fr("HTTP Request Options Test"),
            "N", Lay.FL("L",
                Lay.btn("&Enable", (ActionListener) e -> pnl.setEditable(true)),
                Lay.btn("&Disable", (ActionListener) e -> pnl.setEditable(false)),
                "bg=100,mb=[1b,black]"
            ),
            "C", pnl,
            "size=[700,600],center,visible"
        );
        pnl.set(new HttpRequestOptions());
    }
}
