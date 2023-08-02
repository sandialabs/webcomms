package gov.sandia.webcomms.http.ui.images.flags;

import javax.swing.ImageIcon;

import gov.sandia.webcomms.http.ui.tld.CountryCode;
import replete.ui.GuiUtil;
import replete.ui.lay.Lay;

public class FlagImageLib {

    public static ImageIcon getByCountryCode(String countryCode) {
        try {
            String fileName = countryCode.toLowerCase() + ".png";
            return getByFileName(fileName);
        } catch(Exception e) {
            return null;     // Soft fail on all errors, null or otherwise
        }
    }
    public static ImageIcon getByFileName(String name) {
        try {
            name = name.toLowerCase();
            return GuiUtil.getImageLocal(name, false);
            // ^false -> makes call return null if not found (instead of missing placeholder icon)
        } catch(Exception e) {
            return null;     // Soft fail on all errors, null or otherwise
        }
    }


    //////////
    // TEST //
    //////////

    public static void main(String[] args) {
//        ImageIcon icon = getByCountryCode("ca");
//        ImageIcon icon = getByCountryCode("cn");
        CountryCode milCode = CountryCode.getByUrl("http://navy.mil/page");
        ImageIcon icon = getByFileName(milCode.getIconLocalFileName());
        Lay.BLtg(Lay.fr("Test Flags"),
            "C", Lay.FL(Lay.lb("<--", icon)),
            "size=400,center,visible"
        );
    }
}
