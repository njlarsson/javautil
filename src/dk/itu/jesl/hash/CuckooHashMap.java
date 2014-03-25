package dk.itu.jesl.hash;

import java.util.*;

/**
 * Cuckoo hashing implemented in a conventional way, using an array of Map.Entry
 * objects. Capable of handling any kind of objects as keys, provided that an
 * appropriate Hasher is provided.
 */
public final class CuckooHashMap<K, V> extends AbstractMap<K, V> {
    private final static class Entry<K, V> implements Map.Entry<K, V> {
        final K key;
        V value;
        Entry(K key, V value) { this.key = key; this.value = value; }

        public K getKey() { return key; }
        public V getValue() { return value; }
        public V setValue(V value) { V old = this.value; this.value = value; return old; }
        public boolean equals(Object o) {
            if (!(o instanceof Map.Entry)) return false;
            Map.Entry that = (Map.Entry) o;
            Object thatKey = that.getKey();
            if (key == thatKey || key != null && key.equals(thatKey)) {
                Object thatValue = that.getValue();
                if (value == thatValue || value != null && value.equals(thatValue)) return true;
            }
            return false;
        }
        public int hashCode() {
            return (key == null ? 0 : key.hashCode()) ^ (value == null ? 0 : value.hashCode());
        }
    }

    private final Hasher.Factory<K> hfact;
    private Hasher<K> h1, h2;
    private final double epsilon;
    private final int minCapacity;
    private int r, n, maxN, maxLoop;
    private Entry<K, V>[] a;    // hash tables: [0, r), [r, 2r).

    // Modfification time for iterators to detect concurrent modification.
    private long modTime = 0;

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
        // Not doing shrink, see comment in remove.
        // minN = maxN/2 < minCapacity ? 0 : (maxN+3)/4;
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
        if (e2 != null && (key == e2.key || key.equals(e2.key))) return j;
        return -1;
    }

    public V get(Object key) {
        @SuppressWarnings("unchecked")
        int i = location((K) key);
        return i >= 0 ? a[i].value : null;
    }

    public V remove(Object key) {
        @SuppressWarnings("unchecked")
        int i = location((K) key);
        if (i >= 0) {
            modTime++;
            V value = a[i].value;
            a[i] = null;
            n--;
            // Can't support shrinking arrys in any clean way, since we need to
            // be able to use iterator's remove, and if that would do rehashing
            // it loses track of where it was. But if we were allowed this is
            // how we would do it:
            // if (n < minN) { r = r/2; recalc(); if (!rehash()) reseed(null); }
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
        for (int k = 0; k < RehashFailedException.REHASH_TRIES; k++) {
            h1 = hfact.makeHasher();
            h2 = hfact.makeHasher();
            if (rehash()) {
                // Ok, just the last one, if there is one.
                if (e == null) return;
                e = attemptInsert(e, h1.hashCode(e.key) & r-1);
                if (e == null) return;
            }
        }
        throw new RehashFailedException();
    }

    public V put(K key, V value) {
        modTime++;
        int i = h1.hashCode(key) & r-1;
        Entry<K, V> e1 = a[i];
        if (e1 != null && key.equals(e1.key)) { V old = e1.value; e1.value = value; return old; }
        
        int j = r + (h2.hashCode(key) & r-1);
        Entry<K, V> e2 = a[j];
        if (e2 != null && key.equals(e2.key)) { V old = e2.value; e2.value = value; return old; }

        if (n == maxN) { expand(); i = h1.hashCode(key) & r-1; }

        Entry<K, V> e = attemptInsert(new Entry<K, V>(key, value), i);
        if (e != null) reseed(e);
        n++;
        return null;
    }

    private Entry<K, V> attemptInsert(Entry<K, V> e, int i) {
        for (int k = 0; k < maxLoop; k++) {
            Entry<K, V> f = a[i];
            a[i] = e;
            if (f == null) return null;

            int j = r + (h2.hashCode(f.key) & r-1);
            e = a[j];
            a[j] = f;
            if (e == null) return null;

            i = h1.hashCode(e.key) & r-1;
        }
        return e;
    }

    // Package local accessors for tests.
    int maxN() { return maxN; }
    int maxLoop() { return maxLoop; }
    int r() { return r; }

    private final class EntryIterator implements Iterator<Map.Entry<K, V>> {
        private long time = modTime;
        private int i = -1, m = n;

        public boolean hasNext() {
            if (time != modTime) throw new ConcurrentModificationException();
            return m > 0;
        }
        
        public Entry<K, V> next() {
            if (time != modTime) throw new ConcurrentModificationException();
            if (m == 0) throw new NoSuchElementException();
            m--;
            while (true) {
                Entry<K, V> e = a[++i];
                if (e != null) return e;
            }
        }

        public void remove() {
            if (i < 0) throw new IllegalStateException();
            if (time != modTime) throw new ConcurrentModificationException();
            if (a[i] == null) throw new IllegalStateException();
            a[i] = null;
            n--;
            time = modTime;
        }
    }
            
    public Set<Map.Entry<K, V>> entrySet() {
        return new AbstractSet<Map.Entry<K, V>>() {
            public Iterator<Map.Entry<K, V>> iterator() { return new EntryIterator(); }
            public int size() { return n; }
        };
    }
}
