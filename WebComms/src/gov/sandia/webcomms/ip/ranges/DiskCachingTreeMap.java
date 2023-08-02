package gov.sandia.webcomms.ip.ranges;

import java.io.File;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import replete.util.User;
import replete.xstream.ExtensibleMetadataXStream;

public class DiskCachingTreeMap<K, V> extends TreeMap<K, V> {


    ////////////
    // FIELDS //
    ////////////

    private static ExtensibleMetadataXStream xStream = new ExtensibleMetadataXStream();
    private List<K> loadedKeys = new LinkedList<>();
    private File cachingDir;
    private String label;


    //////////////////
    // CONSTRUCTORS //
    //////////////////

    public DiskCachingTreeMap() {
        super();
    }
    public DiskCachingTreeMap(File cachingDir) {
        super();
        this.cachingDir = cachingDir;
    }
    public DiskCachingTreeMap(Comparator<? super K> comparator) {
        super(comparator);
    }
    public DiskCachingTreeMap(Map<? extends K, ? extends V> m) {
        super(m);
    }
    public DiskCachingTreeMap(SortedMap<K, ? extends V> m) {
        super(m);
    }


    //////////////////////////
    // ACCESSORS / MUTATORS //
    //////////////////////////

    // Accessors

    public File getCachingDir() {
        return cachingDir;
    }

    // Accessors (Computed)

    @Override
    public V get(Object key) {
        if(!containsKey(key)) {
            return null;
        }
        V value = super.get(key);
        K k = (K) key;
        if(value == null) {
            value = loadFromCache(k);
            super.put(k, value);
        }
        recordAndDecache(k);
        return value;
    }
    @Override
    public V put(K key, V value) {

        return super.put(key, value);
    }

    // Mutators

    public DiskCachingTreeMap setCachingDir(File cachingDir) {
        this.cachingDir = cachingDir;
        return this;
    }


    //////////
    // MISC //
    //////////

    private V loadFromCache(K key) {
        File cacheFile = createFileForKey(key);
        return xStream.fromXMLExt(cacheFile);
    }
    private File createFileForKey(K key) {
        String fileName = key.toString();
        if(label != null) {
            fileName = label + "-" + fileName;
        }
        File cacheFile = new File(cachingDir, fileName);
        return cacheFile;
    }

    private void recordAndDecache(K key) {
        loadedKeys.add(key);
        if(loadedKeys.size() > MAX_CACHE_SIZE) {
            V value = super.get(key);
            writeToCache(key, value);
        }
    }

    private void writeToCache(K key, V value) {
        File cacheFile = createFileForKey(key);
        xStream.toXML(value, cacheFile);
    }

    private static final int MAX_CACHE_SIZE = 100;



    //////////
    // TEST //
    //////////

    public static void main(String[] args) {
        File dir = User.getDesktop("dctm");
        DiskCachingTreeMap<String, String> x = new DiskCachingTreeMap<>(dir);
        x.put("Colorado",   "Denver");
        x.put("Arizona",    "Denver");
        x.put("California", "Sacramento");
        x.put("Newark",     "New Jersey");
    }
}
