package gov.sandia.webcomms.http.consumeec.mimetype;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JPanel;

import gov.sandia.webcomms.http.consumeec.HttpConsumeEntityCriteriaParamsPanel;
import replete.text.StringUtil;
import replete.text.patterns.PatternInterpretation;
import replete.text.patterns.PatternInterpretationType;
import replete.ui.button.RCheckBox;
import replete.ui.button.RRadioButton;
import replete.ui.combo.RComboBox;
import replete.ui.form.RFormPanel;
import replete.ui.images.concepts.CommonConcepts;
import replete.ui.lay.Lay;
import replete.ui.list.RList;
import replete.ui.list.RListModel;
import replete.ui.windows.Dialogs;

public class MimeTypeHttpConsumeEntityCriteriaParamsPanel
        extends HttpConsumeEntityCriteriaParamsPanel<MimeTypeHttpConsumeEntityCriteriaParams> {


    ////////////
    // FIELDS //
    ////////////

    private RRadioButton optDeny;
    private RRadioButton optAllow;
    private RComboBox<PatternInterpretationType> cboPatternInterpretationType;
    private RCheckBox chkCaseSensitive;
    private RCheckBox chkWholeMatch;
    private RList<String> lstPatterns;
    private RListModel<String> mdlPatterns;
    private JButton btnAdd, btnEdit, btnRemove;


    /////////////////
    // CONSTRUCTOR //
    /////////////////

    public MimeTypeHttpConsumeEntityCriteriaParamsPanel() {
        Lay.BLtg(this,
            "N", Lay.lb(
                "<html>Here you may define patterns of MIME type strings that, " +
                "when found in a HTTP response's headers, indicate either to " +
                "consume or not consume the response body.</html>", "eb=5,augb=mb(1b,black),bg=white"
            ),
            "C", Lay.hn(new FormPanel(), "eb=5b")
        );
    }

    @Override
    public MimeTypeHttpConsumeEntityCriteriaParams get() {
        PatternInterpretation interp = new PatternInterpretation()
            .setType(cboPatternInterpretationType.getSelected())
            .setCaseSensitive(chkCaseSensitive.isSelected())
            .setWholeMatch(chkWholeMatch.isSelected())
        ;
        return new MimeTypeHttpConsumeEntityCriteriaParams()
            .setPrivilege(optDeny.isSelected() ? PatternPrivilege.DENY : PatternPrivilege.ALLOW)
            .setPatternInterpretation(interp)
            .setPatterns(mdlPatterns.toList())
        ;
    }

    // Mutator

    @Override
    public void set(MimeTypeHttpConsumeEntityCriteriaParams params) {
        if(params.getPrivilege() == PatternPrivilege.DENY) {
            optDeny.setSelected(true);
        } else {
            optAllow.setSelected(true);
        }

        // This special method bypasses the read-only locking
        // mechanism on the RComboBox.  JComboBox seems to
        // be one of the harder ones to cleanly disable
        // on read-only.
        cboPatternInterpretationType.setSelectedItemForce(params.getPatternInterpretation().getType());

        mdlPatterns.setElements(params.getPatterns());

        checkPatternButtonsEnabled();
    }

    public void checkPatternButtonsEnabled() {
        boolean enabled = lstPatterns.getSelectedIndex() != -1;
        btnEdit.setEnabled(enabled);
        btnRemove.setEnabled(enabled);
    }

    public void addPattern() {
        String input = Dialogs.showInput(
            getWindow(),
            "Provide a value for the new pattern.  The interpretation of this string\n" +
            "(e.g. wild cards, regular expressions) is decided by\nthe " +
            "option above.",
            "Add Pattern"
        );
        if(input != null) {
            if(StringUtil.isBlank(input)) {
                Dialogs.showMessage(getWindow(), "Please enter a non-blank value.", "Error");
            } else {
                mdlPatterns.addElement(input);
                lstPatterns.setSelectedIndex(mdlPatterns.size() - 1);
            }
        }
    }


    /////////////////
    // INNER CLASS //
    /////////////////


    public void editPattern() {
        String sel = lstPatterns.getSelectedValue();
        String input = Dialogs.showInput(getWindow(),
            "Enter a new value for this pattern.", "Edit Pattern", sel);
        if(input != null) {
            if(StringUtil.isBlank(input)) {
                Dialogs.showMessage(getWindow(), "Please enter a non-blank value.", "Error");
            } else {
                mdlPatterns.set(lstPatterns.getSelectedIndex(), input);
                lstPatterns.setSelectedIndex(lstPatterns.getSelectedIndex());
            }
        }
    }


    private class FormPanel extends RFormPanel {
        public FormPanel() {
            super(120);
            init();
        }

        @Override
        public void addFields() {
            optDeny  = Lay.opt("DENY: Do not consume response body on pattern match.");
            optAllow = Lay.opt("ALLOW: Consume response body only on pattern match.");

            Lay.grp(optDeny, optAllow);

            JPanel pnlPriv = Lay.GL(2, 1, optDeny, optAllow, "nogap");

            cboPatternInterpretationType = Lay.cb(PatternInterpretationType.values(), "white");
            cboPatternInterpretationType.setSelected(PatternInterpretationType.WILDCARDS);

            mdlPatterns = new RListModel<>();
            lstPatterns = Lay.lst(mdlPatterns);
            JPanel pnlPatterns = Lay.BL(
                "C", Lay.p(Lay.sp(lstPatterns), "eb=5r"),
                "E", Lay.BxL("Y",
                    Lay.BL(
                        btnAdd = Lay.btn(CommonConcepts.ADD, 2, "ttt=Add-Pattern"),
                        "eb=5b,alignx=0.5,maxH=20"
                    ),
                    Lay.BL(
                        btnEdit = Lay.btn(CommonConcepts.EDIT, 2, "ttt=Edit-Pattern..."),
                        "eb=5b,alignx=0.5,maxH=20"
                    ),
                    Lay.BL(
                        btnRemove = Lay.btn(CommonConcepts.REMOVE, 2, "ttt=Remove-Patterns"),
                        "eb=5b,alignx=0.5,maxH=20"
                    ),
                    Box.createVerticalGlue(), "eb=10b"
                )
            );

            lstPatterns.addListSelectionListener(e -> checkPatternButtonsEnabled());
            lstPatterns.addEmptyDoubleClickListener(e -> addPattern());
            lstPatterns.addDoubleClickListener(e -> editPattern());
            lstPatterns.addDeleteListener(e -> lstPatterns.removeSelected());

            btnAdd.addActionListener(e -> addPattern());
            btnEdit.addActionListener(e -> editPattern());
            btnRemove.addActionListener(e -> lstPatterns.removeSelected());

            JPanel pnlInterp = Lay.GL(2, 1,
                cboPatternInterpretationType,
                Lay.FL("L",
                    chkCaseSensitive = Lay.chk("Case Sensitive?"),
                    chkWholeMatch = Lay.chk("Whole Match?"),
                    "nogap"
                )
            );

            addField("Privilege", pnlPriv, 70);
            addField("Interpretation", pnlInterp, 70,
                "<i>(how to interpret the string patterns below)</i>");
            addField("Patterns", pnlPatterns, 40, true,
                "<i>(MIME type patterns against which to compare the response headers)</i>");
        }

        @Override
        protected boolean showSaveButton() {
            return false;
        }
        @Override
        protected boolean showCancelButton() {
            return false;
        }
    }
}
