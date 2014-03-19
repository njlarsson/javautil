package dk.itu.jesl.hash;

import java.util.Map;
import java.util.logging.Logger;

/**
 * Cuckoo hashing implemented in a conventional way, using two arrays of
 * Map.Entry objects. Capable of handling any kind of objects as keys, provided
 * that an appropriate Hasher is provided.
 */
public class CuckooHashMap<K, V> {
    /** 
     * Number of times to try rehashing before deciding that the factory isn't
     * going to produce a working pair, giving up, and throwing an exception.
     *
     * @see RehashFailedException
     */
    public static int REHASH_TRIES = 100;

    private final static class Entry<K, V> {
        final K key;
        V value;
        Entry(K key, V value) { this.key = key; this.value = value; }
    }

    private final Hasher.Factory<K> hfact;
    private Hasher<K> h1, h2;
    private final double epsilon;
    private int r, n, cap, maxLoop;
    private Entry<K, V>[] a;

    private final static Logger logger = Logger.getLogger(CuckooHashMap.class.getName()); 

    public CuckooHashMap(Hasher.Factory<K> hfact, int initialCapacity, double epsilon) {
        logger.fine("Creating, cap=" + initialCapacity + ", eps =" + epsilon);
        this.hfact = hfact;
        this.epsilon = epsilon;
        r = 1 << (int) Math.ceil((Math.log(initialCapacity * (1 + epsilon)) * 1.4426950408889634));
        recalc();
        a = alloc(2*r);
        h1 = hfact.makeHasher();
        h2 = hfact.makeHasher();
    }

    public CuckooHashMap(Hasher.Factory<K> hfact, int initialCapacity) {
        this(hfact, initialCapacity, 0.1);
    }

    public CuckooHashMap(Hasher.Factory<K> hfact) {
        this(hfact, 58 /* makes r=64 */, 0.1);
    }

    private Entry<K, V>[] alloc(int size) {
        @SuppressWarnings("unchecked")
        Entry<K, V>[] arr = new Entry[size];
        return arr;
    }

    private void recalc() {
        cap = (int) Math.floor(r / (1 + epsilon));
        maxLoop = Math.min(r, (int) Math.ceil(3*Math.log(r)/Math.log(1.0 + epsilon)));
        logger.fine("r=" + r + ", cap=" + cap + ", maxLoop=" + maxLoop);
    }

    // Finds index if present, otherwise -1. Package local for testing purposes.
    int locate(K key) {
        int i = h1.hashCode(key) & r-1;
        Entry<K, V> e1 = a[i];
        if (e1 != null && (key == e1.key || key.equals(e1.key))) return i;
        int j = r + (h2.hashCode(key) & r-1);
        Entry<K, V> e2 = a[j];
        if (e2 != null && (key == e2.key || (key.equals(e2.key)))) return j;
        return -1;
    }

    public V get(K key) {
        int i = locate(key);
        if (i >= 0) return a[i].value;
        else        return null;
    }

    public V remove(K key) {
        int i = locate(key);
        if (i >= 0) { V value = a[i].value; a[i] = null; return value; }
        else        return null;
    }

    // Creates new array, reinserts all entries. Returns true iff successful.
    private boolean rehash() {
        Entry<K, V>[] b = a;
        a = alloc(r);
        for (int i = 0, m = n; m > 0; i++) {
            Entry<K, V> e = b[i];
            if (e != null) {
                i = h1.hashCode(e.key) & r-1;
                Entry<K, V> e1 = a[i];
                a[i] = e;
                if (e1 != null && renest(e1, i) != null) { a = b; return false; }
                m--;
            }
        }
        return true;
    }

    // Rehashes with new hash functions.
    private void reseed() {
        for (int k = 0; k < REHASH_TRIES; k++) {
            h1 = hfact.makeHasher();
            h2 = hfact.makeHasher();
            if (rehash()) {
                logger.fine("Successfully rehashed after " + k + " retries");
                return;
            }
        }
        throw new RehashFailedException();
    }

    public V put(K key, V value) {
        if (n == cap) { r = 2*r; recalc(); rehash(); }
        int i = h1.hashCode(key) & r-1;
        Entry<K, V> e1 = a[i];
        if (e1 == null) { a[i] = new Entry<K, V>(key, value); return null; }
        if (key == e1.key || key.equals(e1.key)) { V old = e1.value; e1.value = value; return old; }
        int j = r + (h2.hashCode(key) & r-1);
        Entry<K, V> e2 = a[j];
        if (e2 == null) { a[j] = new Entry<K, V>(key, value); return null; }
        if (key == e2.key || (key.equals(e2.key))) { V old = e2.value; e2.value = value; return old; }
        Entry<K, V> e = new Entry<K, V>(key, value);
        while (true) {
            a[i] = e;
            if (e1 == null) return null;
            e = renest(e1, i);
            if (e == null) return null;
            h1 = hfact.makeHasher();
            h2 = hfact.makeHasher();
            rehash();
            i = h1.hashCode(e.key) & r-1;
            e1 = a[i];
        }
    }

    // The cuckoo kick-out loop. Starts one half iteration in, when e1 has just
    // been kicked out of a[i]. Returns null if succeeded, otherwise returns the
    // entry that is left out of the hash table.
    private Entry<K, V> renest(Entry<K, V> e1, int i) {
        int k = maxLoop;
        while (true) {
            int j = r + (h2.hashCode(e1.key) & r-1);
            Entry<K, V> e2 = a[j];
            a[j] = e1;
            if (e2 == null) return null;

            if (--k == 0) return e2; // give up, need rehash

            i = h1.hashCode(e2.key) & r-1;
            e1 = a[i];
            a[i] = e2;
            if (e1 == null) return null;
        }
    }

    private static int ceilLg(int x) {
        int y = 0;
        while ((1 << y) < x) y++;
        return y;
    }

    // Package local accessors for testing
    int cap() { return cap; }
    int maxLoop() { return maxLoop; }
    int r() { return r; }
}
