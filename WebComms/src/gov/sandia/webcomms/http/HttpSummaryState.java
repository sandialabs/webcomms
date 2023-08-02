package gov.sandia.webcomms.http;

import java.io.Serializable;
import java.util.Map;

import gov.sandia.webcomms.http.options.HttpRequestOptions;
import replete.collections.mm.MembershipMap;

public class HttpSummaryState implements Serializable {


    ////////////
    // FIELDS //
    ////////////

    // Config
    private HttpRequestOptions defaultOptions;
    private int timeout;
    private String userAgent;
    private String proxy;
    private int[] poolStats;
    private boolean recordHeaderStats;
    private Map<String, String> headerCollapsePatterns;

    // Request/Response Counts (for non-preempted requests)
    private int numRequests;
    private int numActualSizes;
    private int numReportedSizes;
    private int numCorrectSizes;
    private int numIncorrectSizes;
    private int numIncorrectEntCe;
    private int numIncorrectEntCl;
    private int numIncorrectEntCt;
    private int numUnknownSizes;
    private int numTooBig;
    private int numErrors;
    private int numRedirects;
    private int numCleanReq;
    private int numCleanRedir;
    private int numUnexpectedRc;

    // Request/Response Sizes (for non-preempted requests)
    private long sumActualSizes;
    private long sumReportedSizes;
    private long avgActualSize;
    private long avgReportedSize;

    // Request/Response Other (for non-preempted requests)
    private Map<Integer, Integer> statusCodeHistogram;
    private Map<String, Integer> exceptionCounts;
    private MembershipMap<String, String> allSeenHeaders;
    // private UrlStats urlStats;                           // Not included due to memory concerns (might be premature optimization)

    // Request/Response Preemption
    private int numRequestsPreempted;


    //////////////////////////
    // ACCESSORS / MUTATORS //
    //////////////////////////

    // Accessors

    // Config
    public HttpRequestOptions getDefaultOptions() {
        return defaultOptions;
    }
    public int getTimeout() {
        return timeout;
    }
    public String getUserAgent() {
        return userAgent;
    }
    public String getProxy() {
        return proxy;
    }
    public int[] getPoolStats() {
        return poolStats;
    }
    public boolean isRecordHeaderStats() {
        return recordHeaderStats;
    }
    public Map<String, String> getHeaderCollapsePatterns() {
        return headerCollapsePatterns;
    }

    // Request/Response Counts (for non-preempted requests)
    public int getNumRequests() {
        return numRequests;
    }
    public int getNumActualSizes() {
        return numActualSizes;
    }
    public int getNumReportedSizes() {
        return numReportedSizes;
    }
    public int getNumCorrectSizes() {
        return numCorrectSizes;
    }
    public int getNumIncorrectSizes() {
        return numIncorrectSizes;
    }
    public int getNumIncorrectEntCe() {
        return numIncorrectEntCe;
    }
    public int getNumIncorrectEntCl() {
        return numIncorrectEntCl;
    }
    public int getNumIncorrectEntCt() {
        return numIncorrectEntCt;
    }
    public int getNumUnknownSizes() {
        return numUnknownSizes;
    }
    public int getNumTooBig() {
        return numTooBig;
    }
    public int getNumErrors() {
        return numErrors;
    }
    public int getNumRedirects() {
        return numRedirects;
    }
    public int getNumCleanReq() {
        return numCleanReq;
    }
    public int getNumCleanRedir() {
        return numCleanRedir;
    }
    public int getNumUnexpectedRc() {
        return numUnexpectedRc;
    }

    // Request/Response Sizes (for non-preempted requests)
    public long getSumActualSizes() {
        return sumActualSizes;
    }
    public long getSumReportedSizes() {
        return sumReportedSizes;
    }
    public long getAvgActualSize() {
        return avgActualSize;
    }
    public long getAvgReportedSize() {
        return avgReportedSize;
    }

    // Request/Response Other (for non-preempted requests)
    public Map<Integer, Integer> getStatusCodeHistogram() {
        return statusCodeHistogram;
    }
    public Map<String, Integer> getExceptionCounts() {
        return exceptionCounts;
    }
    public MembershipMap<String, String> getAllSeenHeaders() {
        return allSeenHeaders;
    }

    // Request/Response Preemption
    public int getNumRequestsPreempted() {
        return numRequestsPreempted;
    }

    // Accessors (Computed)

    public int countAllResponseCodes() {
        int httpTotalCount = 0;
        for(Integer key : statusCodeHistogram.keySet()) {
            int count = statusCodeHistogram.get(key);
            httpTotalCount += count;
        }
        return httpTotalCount;
    }
    public int count400PlusResponseCodes() {
        int httpErrorCount = 0;
        for(Integer key : statusCodeHistogram.keySet()) {
            int count = statusCodeHistogram.get(key);
            if(key >= 400) {
                httpErrorCount += count;
            }
        }
        return httpErrorCount;
    }
    public int countJavaExceptions() {
        int javaErrorCount = 0;
        for(String key : exceptionCounts.keySet()) {
            javaErrorCount += exceptionCounts.get(key);
        }
        return javaErrorCount;
    }

    // Mutators

    // Config
    public HttpSummaryState setDefaultOptions(HttpRequestOptions defaultOptions) {
        this.defaultOptions = defaultOptions;
        return this;
    }
    public HttpSummaryState setTimeout(int timeout) {
        this.timeout = timeout;
        return this;
    }
    public HttpSummaryState setUserAgent(String userAgent) {
        this.userAgent = userAgent;
        return this;
    }
    public HttpSummaryState setProxy(String proxy) {
        this.proxy = proxy;
        return this;
    }
    public HttpSummaryState setPoolStats(int[] poolStats) {
        this.poolStats = poolStats;
        return this;
    }
    public HttpSummaryState setRecordHeaderStats(boolean recordHeaderStats) {
        this.recordHeaderStats = recordHeaderStats;
        return this;
    }
    public HttpSummaryState setHeaderCollapsePatterns(Map<String, String> headerCollapsePatterns) {
        this.headerCollapsePatterns = headerCollapsePatterns;
        return this;
    }

    // Request/Response Counts (for non-preempted requests)
    public HttpSummaryState setNumRequests(int numRequests) {
        this.numRequests = numRequests;
        return this;
    }
    public HttpSummaryState setNumActualSizes(int numActualSizes) {
        this.numActualSizes = numActualSizes;
        return this;
    }
    public HttpSummaryState setNumReportedSizes(int numReportedSizes) {
        this.numReportedSizes = numReportedSizes;
        return this;
    }
    public HttpSummaryState setNumCorrectSizes(int numCorrectSizes) {
        this.numCorrectSizes = numCorrectSizes;
        return this;
    }
    public HttpSummaryState setNumIncorrectSizes(int numIncorrectSizes) {
        this.numIncorrectSizes = numIncorrectSizes;
        return this;
    }
    public HttpSummaryState setNumIncorrectEntCe(int numIncorrectEntCe) {
        this.numIncorrectEntCe = numIncorrectEntCe;
        return this;
    }
    public HttpSummaryState setNumIncorrectEntCl(int numIncorrectEntCl) {
        this.numIncorrectEntCl = numIncorrectEntCl;
        return this;
    }
    public HttpSummaryState setNumIncorrectEntCt(int numIncorrectEntCt) {
        this.numIncorrectEntCt = numIncorrectEntCt;
        return this;
    }
    public HttpSummaryState setNumUnknownSizes(int numUnknownSizes) {
        this.numUnknownSizes = numUnknownSizes;
        return this;
    }
    public HttpSummaryState setNumTooBig(int numTooBig) {
        this.numTooBig = numTooBig;
        return this;
    }
    public HttpSummaryState setNumErrors(int numError) {
        numErrors = numError;
        return this;
    }
    public HttpSummaryState setNumRedirects(int numRedirects) {
        this.numRedirects = numRedirects;
        return this;
    }
    public HttpSummaryState setNumCleanReq(int numCleanReq) {
        this.numCleanReq = numCleanReq;
        return this;
    }
    public HttpSummaryState setNumCleanRedir(int numCleanRedir) {
        this.numCleanRedir = numCleanRedir;
        return this;
    }
    public HttpSummaryState setNumUnexpectedRc(int numUnexpectedRc) {
        this.numUnexpectedRc = numUnexpectedRc;
        return this;
    }

    // Request/Response Sizes (for non-preempted requests)
    public HttpSummaryState setSumActualSizes(long sumActualSizes) {
        this.sumActualSizes = sumActualSizes;
        return this;
    }
    public HttpSummaryState setSumReportedSizes(long sumReportedSizes) {
        this.sumReportedSizes = sumReportedSizes;
        return this;
    }
    public HttpSummaryState setAvgActualSize(long avgActualSize) {
        this.avgActualSize = avgActualSize;
        return this;
    }
    public HttpSummaryState setAvgReportedSize(long avgReportedSize) {
        this.avgReportedSize = avgReportedSize;
        return this;
    }

    // Request/Response Other (for non-preempted requests)
    public HttpSummaryState setStatusCodeHistogram(Map<Integer, Integer> statusCodeHistogram) {
        this.statusCodeHistogram = statusCodeHistogram;
        return this;
    }
    public HttpSummaryState setExceptionCounts(Map<String, Integer> exceptionCounts) {
        this.exceptionCounts = exceptionCounts;
        return this;
    }
    public HttpSummaryState setAllSeenHeaders(MembershipMap<String, String> allSeenHeaders) {
        this.allSeenHeaders = allSeenHeaders;
        return this;
    }

    // Request/Response Preemption
    public HttpSummaryState setNumRequestsPreempted(int numRequestsPreempted) {
        this.numRequestsPreempted = numRequestsPreempted;
        return this;
    }
}
