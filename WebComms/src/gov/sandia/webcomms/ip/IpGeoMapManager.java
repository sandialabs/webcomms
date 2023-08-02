package gov.sandia.webcomms.ip;

import java.io.File;

import javax.swing.event.ChangeListener;

import gov.sandia.webcomms.ip.ranges.IpGeoMapMaster;
import gov.sandia.webcomms.ip.ranges.IpRangeGeo;
import replete.event.ChangeNotifier;

public class IpGeoMapManager {


    ////////////
    // FIELDS //
    ////////////

    // Singleton

    // Currently has singleton pattern due to the presumption that
    // almost any system using this class as designed would only
    // ever want 1 instance of it per JVM.  If it ever becomes more
    // configurable or flexible or refactored to represent subsets
    // of the IP->Geo map tree instead of just the whole thing
    // at once, then perhaps the singleton pattern can be removed.
    private static IpGeoMapManager manager = null;

    // Core

    private IpGeoMapMaster ipGeoMapMaster;    // Instance being managed
    private boolean loading;
    private File lastSuccessfulLoadPath;
    private Exception lastLoadException;
    private long lastLoadDuration;


    private IpGeoMapManager() {
        // Future configuration
    }

    public synchronized void load(File path) {
        if(loading) {
            return;
        }
        loading = true;
        Thread t = new Thread() {
            @Override
            public void run() {
                long T = System.currentTimeMillis();
                try {
                    ipGeoMapMaster = DbipIpGeoMapLoader.parseMasterFromCsvFile(path, null /* manager can't provide progress yet */);
                    lastSuccessfulLoadPath = path;
                    lastLoadException = null;
                } catch(Exception e) {
                    // Leaves master & path alone, but sets exception.
                    lastLoadException = e;
                }
                lastLoadDuration = System.currentTimeMillis() - T;    // Measures either load or fail paths above
                loading = false;
                fireLoadCompleteNotifier();  // Fired regardless of success, listeners must inspect results
            }
        };
        t.start();
    }


    ///////////////
    // ACCESSORS //
    ///////////////

    public File getLastSuccessfulLoadPath() {
        return lastSuccessfulLoadPath;
    }
    public Exception getLastLoadException() {
        return lastLoadException;
    }
    public boolean isLoading() {
        return loading;
    }
    public long getLastLoadDuration() {
        return lastLoadDuration;
    }

    // Computed

    public boolean isLoaded() {
        return ipGeoMapMaster != null;
    }
    public IpRangeGeo getIpGeoLookup(String ip) {
        if(ipGeoMapMaster == null) {
            return null;
        }

        // If IP is invalid, let's throw the exception up
        // from here for now.  Client code should decide
        // whether certain exceptions are bad or not.
        return ipGeoMapMaster.lookup(ip);
    }

    // Singleton

    public static IpGeoMapManager getInstance() {
        if(manager == null) {
            manager = new IpGeoMapManager();
        }
        return manager;
    }


    ///////////////
    // NOTIFIERS //
    ///////////////

    private transient ChangeNotifier loadCompleteNotifier = new ChangeNotifier(this);
    public void addLoadCompleteListener(ChangeListener listener) {
        loadCompleteNotifier.addListener(listener);
    }
    private void fireLoadCompleteNotifier() {
        loadCompleteNotifier.fireStateChanged();
    }
}
