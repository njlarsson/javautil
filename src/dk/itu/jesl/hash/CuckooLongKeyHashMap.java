package dk.itu.jesl.hash;

import java.util.*;

/**
 * Cuckoo hashing that uses long (64 bit integer) keys. Does not permit null
 * values. Since keys are not objects, this class does not implement the {@link
 * Map} interface, but it provides generic map views that do, via {@link
 * #genericMap(LongidFunction idf)} and {@link #genericMap()}
 */
public final class CuckooLongKeyHashMap<V> {
    private final LongHasher.Factory hfact;
    private LongHasher h1, h2;
    private final double epsilon;
    private final int minCapacity;
    private int r, n, maxN, maxLoop;

    // Hash table, one array for keys and one for values [0, r), [r, 2r).
    private long[] ak;
    private V[] av;

    // Temporary overflow key/entry pair.
    private long xk;
    private V xv = null;

    // Modfification time for iterators to detect concurrent modification.
    private long modTime = 0;

    public CuckooLongKeyHashMap(LongHasher.Factory hfact, int minCapacity, double epsilon) {
        this.hfact = hfact;
        this.minCapacity = minCapacity;
        this.epsilon = epsilon;
        r = 1 << (int) Math.ceil((Math.log(minCapacity * (1 + epsilon)) * 1.4426950408889634));
        recalc();
        ak = new long[2*r];
        av = alloc(2*r);
        h1 = hfact.makeHasher();
        h2 = hfact.makeHasher();
    }

    public CuckooLongKeyHashMap(LongHasher.Factory hfact, int minCap) {
        this(hfact, minCap, 0.1);
    }

    public CuckooLongKeyHashMap(LongHasher.Factory hfact) {
        this(hfact, 58 /* makes r=64 */, 0.1);
    }

    private V[] alloc(int size) {
        @SuppressWarnings("unchecked")
        V[] arr = (V[]) new Object[size];
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
    int location(long key) {
        int i = h1.hashCode(key) & r-1;
        V v1 = av[i];
        if (v1 != null && key == ak[i]) return i;
        int j = r + (h2.hashCode(key) & r-1);
        V v2 = av[j];
        if (v2 != null && key == ak[j]) return j;
        return -1;
    }

    public V get(long key) {
        int i = location(key);
        return i >= 0 ? av[i] : null;
    }

    public V remove(long key) {
        int i = location(key);
        if (i >= 0) {
            modTime++;
            V value = av[i];
            av[i] = null;
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
        long[] bk = ak;
        V[] bv = av;
        ak = new long[2*r];
        av = alloc(2*r);
        for (int k = 0, m = n; m > 0; k++) {
            V ev = bv[k];
            if (ev != null) {
                long ek = bk[k];
                if (!attemptInsert(ek, ev, h1.hashCode(ek) & r-1)) {
                    ak = bk;    // failed, restore a
                    av = bv;
                    return false;
                }
                m--;
            }
        }
        return true;
    }

    // Rehashes with new hash functions, optionally adding one nestless entry.
    private void reseed() {
        for (int k = 0; k < RehashFailedException.REHASH_TRIES; k++) {
            h1 = hfact.makeHasher();
            h2 = hfact.makeHasher();
            if (rehash()) {
                // Ok, just the last one, if there is one.
                if (xv == null || attemptInsert(xk, xv, h1.hashCode(xk) & r-1)) {
                    xv = null;
                    return;
                }
            }
        }
        throw new RehashFailedException();
    }

    public V put(long key, V value) {
        if (value == null) throw new NullPointerException("null value not permitted");
        modTime++;
        int i = h1.hashCode(key) & r-1;
        V e1v = av[i];
        if (e1v != null && key == ak[i]) { av[i] = value; return e1v; }
        int j = r + (h2.hashCode(key) & r-1);
        V e2v = av[j];
        if (e2v != null && key == ak[j]) { av[j] = value; return e2v; }
        
        if (n == maxN) { expand(); i = h1.hashCode(key) & r-1; }

        if (!attemptInsert(key, value, i)) reseed();
        n++;
        return null;
    }

    private boolean attemptInsert(long ek, V ev, int i) {
        for (int k = 0; k < maxLoop; k++) {
            V fv = av[i];
            av[i] = ev;
            if (fv == null) { ak[i] = ek; return true; }
            long fk = ak[i];
            ak[i] = ek;

            int j = r + (h2.hashCode(fk) & r-1);
            ev = av[j];
            av[j] = fv;
            if (ev == null) { ak[j] = fk; return true; }
            ek = ak[j];
            ak[j] = fk;

            i = h1.hashCode(ek) & r-1;
        }
        xk = ek; xv = ev; // keep for new try after reseed
        return false;
    }

    /**
     * Cursor for iterating over the contents of a {@link
     * CuckooLongKeyHashMap}. Differs from a normal iterator in that it has
     * accessor methods for current key and value, rather than producing entry
     * objects.
     */
    public class Cursor {
        private int current = -1, remain = n;
        private long time = modTime;

        final public boolean hasNext() {
            if (time != modTime) throw new ConcurrentModificationException();
            return remain > 0;
        }

        final int nextLocation() {
            toNext();
            return current;
        }
        
        final public void toNext() {
            if (remain == 0) throw new NoSuchElementException();
            if (time != modTime) throw new ConcurrentModificationException();
            remain--;
            while (av[++current] == null)
                ;
        }

        final public long getKey() {
            if (current < 0) throw new IllegalStateException();
            return getKey(current);
        }

        final long getKey(int i) {
            if (time != modTime) throw new ConcurrentModificationException();
            return ak[i];
        }

        final public V getValue() {
            if (current < 0) throw new IllegalStateException();
            return getValue(current);
        }

        final V getValue(int i) {
            if (time != modTime) throw new ConcurrentModificationException();
            return av[i];
        }

        final public V setValue(V value) {
            if (current < 0) throw new IllegalStateException();
            return setValue(current, value); 
        }

        final V setValue(int i, V value) {
            if (value == null) throw new NullPointerException("null value not permitted");
            if (time != modTime) throw new ConcurrentModificationException();
            V old = av[i];
            if (old == null) throw new IllegalStateException();
            av[i] = value;
            return old;
        }

        final public void remove() {
            if (current < 0) throw new IllegalStateException();
            remove(current);
        }

        final void remove(int i) {
            if (time != modTime) throw new ConcurrentModificationException();
            if (av[i] == null) throw new IllegalStateException();
            av[i] = null;
            --n;
            time = modTime;
        }
    }

    /**
     * Gets a cursor (similar to an iterator) to loop across the elements in the
     * hash table.
     */
    public Cursor cursor() { return new Cursor(); }

    // Package local accessors for tests.
    int maxN() { return maxN; }
    int maxLoop() { return maxLoop; }
    int r() { return r; }

    private final class GenericKeyView<K> extends AbstractMap<K, V> {
        private final LongidFunction<K> idf;
        
        GenericKeyView(LongidFunction<K> idf) { this.idf = idf; }
        
        @SuppressWarnings("unchecked")
        public V get(Object key) { return get(idf.toLong((K) key)); }

        public V put(K key, V value) { return put(idf.toLong(key), value); }

        @SuppressWarnings("unchecked")
        public V remove(Object key) { return remove(idf.toLong((K) key)); }

        private final class EntryIterator extends Cursor implements Iterator<Map.Entry<K, V>> {
            public Map.Entry<K, V> next() {
                return new Map.Entry<K, V>() {
                    private final int i = nextLocation();
                    public K getKey() { return idf.fromLong(EntryIterator.this.getKey(i)); }
                    public V getValue() { return EntryIterator.this.getValue(i); }
                    public V setValue(V value) { return EntryIterator.this.setValue(i, value); }
                    public boolean equals(Object o) {
                        if (!(o instanceof Map.Entry)) return false;
                        Map.Entry that = (Map.Entry) o;
                        return getKey().equals(that.getKey()) && getValue().equals(that.getValue());
                    }
                    public int hashCode() { return getKey().hashCode() ^ getValue().hashCode(); }
                };
            }
        }

        public Set<Map.Entry<K, V>> entrySet() {
            return new AbstractSet<Map.Entry<K, V>>() {
                public Iterator<Map.Entry<K, V>> iterator() { return new EntryIterator(); }
                public int size() { return n; }
            };
        }
    }

    /**
     * Gets a generic {@link Map} view of the hash map, given a one-to-one
     * transformation between long and the generic key type.
     */
    public <K> Map<K, V> genericMap(LongidFunction<K> idf) {
        return new GenericKeyView<K>(idf);
    }

    /**
     * Gets a {@link Map} view of the hash map where the key type is the boxing
     * type Long.
     */
    public Map<Long, V> genericMap() {
        return new GenericKeyView<Long>(new LongidFunction<Long>() {
                public Long fromLong(long l) { return l; }
                public long toLong(Long l) { return l; }
            });
    }
}
