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
    public static int REHASH_TRIES = 2;

    private final static class Entry<K, V> {
        final K key;
        V value;
        Entry(K key, V value) { this.key = key; this.value = value; }
    }

    private final Hasher.Factory<K> hfact;
    private Hasher<K> h1, h2;
    private final double epsilon;
    private final int minCapacity;
    private int r, n, minN, maxN, maxLoop;
    private Entry<K, V>[] a;

    public CuckooHashMap(Hasher.Factory<K> hfact, int minCapacity, double epsilon) {
        this.hfact = hfact;
        this.minCapacity = minCapacity;
        this.epsilon = epsilon;
        r = 1 << (int) Math.ceil((Math.log(minCapacity * (1 + epsilon)) * 1.4426950408889634));
        recalc();
        a = alloc(2*r);
        h1 = hfact.makeHasher();
        h2 = hfact.makeHasher();
    }

    public CuckooHashMap(Hasher.Factory<K> hfact, int minCap) {
        this(hfact, minCap, 0.1);
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
        maxN = (int) Math.floor(r / (1 + epsilon));
        minN = maxN/2 < minCapacity ? 0 : maxN/4;
        maxLoop = Math.min(r, (int) Math.ceil(3*Math.log(r)/Math.log(1.0 + epsilon)));
    }

    private void expand() {
        r = 2*r;
        recalc();
        rehash();
    }

    public int size() { return n; }

    // Finds index if present, otherwise -1. Exposed package-locally for tests.
    int location(K key) {
        int i = h1.hashCode(key) & r-1;
        Entry<K, V> e1 = a[i];
        if (e1 != null && (key == e1.key || key.equals(e1.key))) return i;
        int j = r + (h2.hashCode(key) & r-1);
        Entry<K, V> e2 = a[j];
        if (e2 != null && (key == e2.key || (key.equals(e2.key)))) return j;
        return -1;
    }

    public V get(K key) {
        int i = location(key);
        return i >= 0 ? a[i].value : null;
    }

    public V remove(K key) {
        int i = location(key);
        if (i >= 0) {
            V value = a[i].value;
            a[i] = null;
            if (--n < minN) { r = r/2; recalc(); if (!rehash()) reseed(null); }
            return value;
        } else return null;
    }

    // Creates new array, reinserts all entries. Returns true iff successful.
    private boolean rehash() {
        Entry<K, V>[] b = a;
        a = alloc(2*r);
        for (int k = 0, m = n; m > 0; k++) {
            Entry<K, V> e = b[k];
            if (e != null) {
                if (attemptInsert(e, h1.hashCode(e.key) & r-1) != null) {
                    a = b;          // failed, restore a
                    return false;
                }
                m--;
            }
        }
        return true;
    }

    // Rehashes with new hash functions, optionally adding one nestless entry.
    private void reseed(Entry<K, V> e) {
        for (int k = 0; k < REHASH_TRIES; k++) {
            h1 = hfact.makeHasher();
            h2 = hfact.makeHasher();
            if (rehash()) {
                // Ok, just the last one, if there is one.
                if (e == null) return;
                e = attemptInsert(e, h1.hashCode(e.key));
                if (e == null) return;
            }
        }
        throw new RehashFailedException();
    }

    public V put(K key, V value) {
        int i = h1.hashCode(key) & r-1;
        Entry<K, V> e1 = a[i];
        if (e1 == null) { put(key, value, i); return null; }
        if (key == e1.key || key.equals(e1.key)) { V old = e1.value; e1.value = value; return old; }

        int j = r + (h2.hashCode(key) & r-1);
        Entry<K, V> e2 = a[j];
        if (e2 == null) { put(key, value, j); return null; }
        if (key == e2.key || key.equals(e2.key)) { V old = e2.value; e2.value = value; return old; }

        if (n == maxN) {
            expand();
            i = h1.hashCode(key);
        }
        Entry<K, V> e = attemptInsert(new Entry<K, V>(key, value), i);
        if (e != null) reseed(e);
        n++;
        return null;
    }

    private void put(K key, V value, int i) {
        if (n < maxN) a[i] = new Entry<K, V>(key, value);
        else {
            expand();
            Entry<K, V> e = attemptInsert(new Entry<K, V>(key, value), h1.hashCode(key));
            if (e != null) reseed(e);
        } 
        n++;
    }

    private Entry<K, V> attemptInsert(Entry<K, V> e, int i) {
        for (int k = 0; k < maxLoop; k++) {
            Entry<K, V> f = a[i];
            a[i] = e;
            if (f == null) { return null; }

            int j = r + (h2.hashCode(f.key) & r-1);
            e = a[j];
            a[j] = f;
            if (e == null) { return null; }

            i = h1.hashCode(e.key) & r-1;
        }
        return e;
    }

    private static int ceilLg(int x) {
        int y = 0;
        while ((1 << y) < x) y++;
        return y;
    }

    // Package local accessors for tests.
    int minN() { return minN; }
    int maxN() { return maxN; }
    int maxLoop() { return maxLoop; }
    int r() { return r; }
}
