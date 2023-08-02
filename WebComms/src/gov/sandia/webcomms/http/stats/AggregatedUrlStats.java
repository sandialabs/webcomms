package gov.sandia.webcomms.http.stats;

import java.io.Serializable;

import replete.numbers.BooleanSummaryStats;
import replete.numbers.IntSummaryStats;
import replete.numbers.NumUtil;
import replete.text.StringUtil;

public class AggregatedUrlStats implements Serializable {


    ////////////
    // FIELDS //
    ////////////

    private IntSummaryStats     urlLength         = new IntSummaryStats();  // Can augment these
    private IntSummaryStats     pathLength        = new IntSummaryStats();  // into histograms too
    private IntSummaryStats     queryParamsLength = new IntSummaryStats();
    private IntSummaryStats     queryParamsCount  = new IntSummaryStats();
    private IntSummaryStats     fragmentLength    = new IntSummaryStats();
    private BooleanSummaryStats authCount         = new BooleanSummaryStats();
    private BooleanSummaryStats portCount         = new BooleanSummaryStats();

    // Could also:
    //   1. Count empty params or empty fragments (which
    //      could be considered slight URL formatting errors).
    //      i.e. http://cnn.com/?  or  http://cnn.com/?a=b#
    //      Right now they are counted as not present.
    //   2. Count multiple occurrences of ? and # (which
    //      should be definite errors).
    //   3. Count distribution of extensions (.html, .jsp).
    //   4. Count distribution of TLDs (.com, .org, new custom TLDs).
    //   5. Count URLs with special & international characters.
    //   6. Count hosts with multiple parts.
    //   7. Count URLs with http vs. https.
    //   8. Count distribution of non-standard ports (non-80, non-443)
    //   9. Count URLs that use IPv4 or IPv6 as the host


    /////////
    // ADD //
    /////////

    public synchronized void add(String urlStr) {
        int uLen = urlStr.length();
        urlLength.add(uLen);

        int prIndex = urlStr.indexOf("://");
        int paIndex = urlStr.indexOf("/", prIndex + 3);
        int qIndex  = urlStr.indexOf('?', paIndex + 1);
        int fIndex  = urlStr.indexOf('#', Math.max(paIndex, qIndex) + 1);
        int hostEnd = NumUtil.smallestNonNegative(paIndex, qIndex, fIndex, uLen);

        if(paIndex != -1) {
            int qpOrfOrEnd = NumUtil.smallestNonNegative(qIndex, fIndex, uLen);
            String path = urlStr.substring(paIndex + 1, qpOrfOrEnd);
            if(!path.isEmpty()) {
                pathLength.add(path.length());
            }
        }

        if(qIndex != -1) {
            int fOrEnd = NumUtil.smallestNonNegative(fIndex, uLen);
            String queryParams = urlStr.substring(qIndex + 1, fOrEnd);
            if(!queryParams.isEmpty()) {
                queryParamsLength.add(queryParams.length());
                int ampCount = 0;
                for(int i = 0; i < queryParams.length(); i++) {
                    if(queryParams.charAt(i) == '&') {
                        ampCount++;
                    }
                }
                queryParamsCount.add(ampCount + 1);  // Currently counts leading or trailing
                                                     // amps as additional params (kinda errors)
            }
        }

        if(fIndex != -1) {
            String fragment = urlStr.substring(fIndex + 1);
            if(!fragment.isEmpty()) {
                fragmentLength.add(fragment.length());
            }
        }

        String auth = urlStr.substring(prIndex + 3, hostEnd);
        int atIndex = auth.indexOf('@');
        boolean hasAuth = atIndex != -1;
        authCount.add(hasAuth);

        int cIndexStart = hasAuth ? atIndex + 1 : 0;
        int cIndex = auth.indexOf(':', cIndexStart);
        boolean hasPort = cIndex != -1;
        portCount.add(hasPort);
    }


    //////////////////////////
    // ACCESSORS / MUTATORS //
    //////////////////////////

    // Accessors (Computed)

    public synchronized AggregatedUrlStats copy() {
        return new AggregatedUrlStats()
            .setUrlLength(urlLength.copy())
            .setPathLength(pathLength.copy())
            .setQueryParamsLength(queryParamsLength.copy())
            .setQueryParamsCount(queryParamsCount.copy())
            .setFragmentLength(fragmentLength.copy())
            .setAuthCount(authCount.copy())
            .setPortCount(portCount.copy())
        ;
    }

    // Mutators

    public AggregatedUrlStats setUrlLength(IntSummaryStats urlLength) {
        this.urlLength = urlLength;
        return this;
    }
    public AggregatedUrlStats setPathLength(IntSummaryStats pathLength) {
        this.pathLength = pathLength;
        return this;
    }
    public AggregatedUrlStats setQueryParamsLength(IntSummaryStats queryParamsLength) {
        this.queryParamsLength = queryParamsLength;
        return this;
    }
    public AggregatedUrlStats setQueryParamsCount(IntSummaryStats queryParamsCount) {
        this.queryParamsCount = queryParamsCount;
        return this;
    }
    public AggregatedUrlStats setFragmentLength(IntSummaryStats fragmentLength) {
        this.fragmentLength = fragmentLength;
        return this;
    }
    public AggregatedUrlStats setAuthCount(BooleanSummaryStats authCount) {
        this.authCount = authCount;
        return this;
    }
    public AggregatedUrlStats setPortCount(BooleanSummaryStats portCount) {
        this.portCount = portCount;
        return this;
    }


    //////////
    // MISC //
    //////////

    public synchronized String toString(int level) {
        StringBuilder buffer = new StringBuilder();
        String sp = StringUtil.spaces(2 * level);
        buffer.append(sp + "URL Length: " + urlLength + "\n");
        buffer.append(sp + "PA  Length: " + pathLength + "\n");
        buffer.append(sp + "QP  Length: " + queryParamsLength + "\n");
        buffer.append(sp + "QP  Count:  " + queryParamsCount + "\n");
        buffer.append(sp + "FG  Length: " + fragmentLength + "\n");
        buffer.append(sp + "Auth Count: " + authCount + "\n");
        buffer.append(sp + "Port Count: " + portCount + "\n");
        return buffer.toString();
    }
}
