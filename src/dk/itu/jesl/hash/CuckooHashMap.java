package dk.itu.jesl.hash;

import java.util.Map;

/**
 * Cuckoo hashing implemented in a conventional way, using two arrays of
 * Map.Entry objects. Capable of handling any kind of objects as keys, provided
 * that an appropriate Hasher is provided.
 */
public class CuckooHashMap<K, V> {
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

    public CuckooHashMap(Hasher.Factory<K> hfact, int initialCapacity, double epsilon) {
        this.hfact = hfact;
        this.epsilon = epsilon;
        r = 1 << (int) Math.ceil((Math.log(initialCapacity * (1.0 + epsilon)) * 0.69314718055994531));
        recalc();
        a = alloc(2*r);
        h1 = hfact.makeHasher();
        h2 = hfact.makeHasher();
    }

    private Entry<K, V>[] alloc(int size) {
        @SuppressWarnings("unchecked")
        Entry<K, V>[] arr = new Entry[size];
        return arr;
    }

    private void recalc() {
        cap = (int) Math.floor(r * (1 - epsilon));
        maxLoop = Math.min(r, (int) Math.ceil(3*Math.log(r)/Math.log(1.0 + epsilon)));
    }

    private Entry<K, V> lookup(K key) {
        int i = h1.hashCode(key) & r-1;
        Entry<K, V> e1 = a[i];
        if (e1 != null && (key == e1.key || key.equals(e1.key))) return e1;
        int j = h2.hashCode(key) & r-1;
        Entry<K, V> e2 = a[r+j];
        if (e2 != null && (key == e2.key || (key.equals(e2.key)))) return e2;
        return null;
    }
    
    private void rehash() {
        Entry<K, V>[] a0 = a;
        while (true) {          // loop until successful rehash
            Entry<K, V>[] b = a0;
            a = alloc(r);
            int i;
            for (i = 0; i < b.length; i++) {
                Entry<K, V> e = b[i];
                if (e != null) {
                    i = h1.hashCode(e.key) & r-1;
                    Entry<K, V> e1 = a[i];
                    a[i] = e;
                    if (e1 != null && renest(e1, i) != null) break;
                }
            }
            if (i == r) break;  // if all went well, done
            h1 = hfact.makeHasher();
            h2 = hfact.makeHasher();
        }
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
}
