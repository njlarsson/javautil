package dk.itu.jesl.hash;

import java.util.*;
import org.junit.*;
import static org.junit.Assert.*;
import static org.hamcrest.CoreMatchers.*;

public class CuckooHashMapTest {
    private static class DebugKey {
        final int[] v;
        final String name;
        DebugKey(int[] v, String name) { this.v = v; this.name = name; }
        public boolean equals(Object that) {
            return Arrays.equals(v, ((DebugKey) that).v);
        }
    }

    private static class DebugHasher implements Hasher<DebugKey> {
        final int serialNo;
        DebugHasher(int serialNo) { this.serialNo = serialNo; }
        public int hashCode(DebugKey key) { return key.v[serialNo % key.v.length]; }
    }

    private static class DebugHasherFactory implements Hasher.Factory<DebugKey> {
        private int i = 0;
        public Hasher<DebugKey> makeHasher() { return new DebugHasher(i++); }
    }

    @Test
    public void testParameterCalc1() {
        CuckooHashMap<DebugKey, String> m = new CuckooHashMap<DebugKey, String>(new DebugHasherFactory(), 100, 0.1);
        assertThat("r", m.r(), is(128));
        assertThat("maxN", m.maxN(), is(116));
        // assertThat("minN", m.minN(), is(0));
        assertThat("maxLoop", m.maxLoop(), is(128));
    }

    @Test
    public void testParameterCalc2() {
        CuckooHashMap<DebugKey, String> m = new CuckooHashMap<DebugKey, String>(new DebugHasherFactory(), 1000000, 0.1);
        assertThat("r", m.r(), is(2097152));
        assertThat("maxN", m.maxN(), is(1906501));
        // assertThat("minN", m.minN(), is(0));
        assertThat("maxLoop", m.maxLoop(), is(459));
    }

    @Test
    public void testParameterCalc3() {
        CuckooHashMap<DebugKey, String> m = new CuckooHashMap<DebugKey, String>(new DebugHasherFactory(), 1000000, 0.5);
        assertThat("r", m.r(), is(2097152));
        assertThat("maxN", m.maxN(), is(1398101));
        // assertThat("minN", m.minN(), is(0));
        assertThat("maxLoop", m.maxLoop(), is(108));
    }

    @Test
    public void testBasicPutGet() {
        DebugKey[] keys = {
            new DebugKey(new int[] { 3, 4 }, "a"),
            new DebugKey(new int[] { 3, 5 }, "b")
        };
        DebugHasherFactory hfact = new DebugHasherFactory();
        CuckooHashMap<DebugKey, String> m = new CuckooHashMap<DebugKey, String>(hfact);
        for (DebugKey key : keys) {
            m.put(key, key.name);
        }
        assertThat(m.size(), is(keys.length));
        for (DebugKey key : keys) {
            assertThat(m.get(key), is(key.name));
            assertThat(m.get(new DebugKey(key.v, "x")), is(key.name));
        }
        assertThat(m.get(new DebugKey(new int[] { 3, 6 }, "y")), nullValue());
        assertThat(m.location(keys[0]), is(m.r() + 4 % m.r()));
        assertThat(m.location(keys[1]), is(3 % m.r()));
        assertThat(hfact.i, is(2));
    }

    @Test
    public void testRemovePut() {
        DebugKey[] keys = {
            new DebugKey(new int[] { 0, 0 }, "a"),
            new DebugKey(new int[] { 0, 1 }, "b")
        };
        DebugHasherFactory hfact = new DebugHasherFactory();
        CuckooHashMap<DebugKey, String> m = new CuckooHashMap<DebugKey, String>(hfact);
        m.put(keys[0], keys[0].name);
        m.put(keys[1], keys[1].name);
        String v = m.remove(keys[1]);
        assertThat(m.size(), is(1));
        assertThat(v, is("b"));
        v = m.put(keys[0], keys[0].name);
        assertThat(m.size(), is(1));
        assertThat(v, is("a"));
        assertThat(m.location(keys[0]), is(m.r()));
        v = m.put(keys[1], keys[1].name);
        assertThat(m.size(), is(2));
        assertThat(v, nullValue());
        assertThat(m.location(keys[0]), is(m.r()));
        assertThat(m.location(keys[1]), is(0));
        v = m.put(keys[0], keys[0].name);
        assertThat(m.size(), is(2));
        assertThat(v, is("a"));
        assertThat(m.location(keys[0]), is(m.r()));
        assertThat(m.location(keys[1]), is(0));
        v = m.remove(keys[0]);
        assertThat(m.size(), is(1));
        assertThat(v, is("a"));
        assertThat(m.location(keys[0]), is(-1));
        assertThat(m.location(keys[1]), is(0));
        v = m.remove(keys[0]);
        assertThat(m.size(), is(1));
        assertThat(v, nullValue());
        assertThat(m.location(keys[0]), is(-1));
        assertThat(m.location(keys[1]), is(0));
        v = m.put(keys[0], keys[0].name);
        assertThat(m.size(), is(2));
        assertThat(v, nullValue());
        assertThat(m.location(keys[0]), is(0));
        assertThat(m.location(keys[1]), is(m.r()+1));
        v = m.remove(keys[0]);
        assertThat(m.size(), is(1));
        assertThat(v, is("a"));
        v = m.remove(keys[1]);
        assertThat(m.size(), is(0));
        assertThat(v, is("b"));
        v = m.remove(keys[1]);
        assertThat(m.size(), is(0));
        assertThat(v, nullValue());
        v = m.put(keys[1], keys[1].name);
        assertThat(m.size(), is(1));
        assertThat(v, nullValue());
        v = m.put(keys[0], keys[0].name);
        assertThat(m.size(), is(2));
        assertThat(v, nullValue());
        assertThat(m.location(keys[0]), is(0));
        assertThat(m.location(keys[1]), is(m.r() + 1));
        assertThat(hfact.i, is(2));
    }

    @Test
    public void testFillTwoSlots() {
        DebugKey[] keys = {
            new DebugKey(new int[] { 0, 0 }, "a"),
            new DebugKey(new int[] { 0, 1 }, "b"),
            new DebugKey(new int[] { 0, 2 }, "c"),
            new DebugKey(new int[] { 1, 0 }, "d"),
            new DebugKey(new int[] { 1, 1 }, "e") 
        };
        // 0: a d
        // 1: e b
        // 2: - c

        DebugHasherFactory hfact = new DebugHasherFactory();
        CuckooHashMap<DebugKey, String> m = new CuckooHashMap<DebugKey, String>(hfact);
        for (int i = 0; i < 2; i++) {
            DebugKey key = keys[i];
            m.put(key, key.name);
        }
        assertThat(m.location(keys[0]), is(m.r()));
        assertThat(m.location(keys[1]), is(0));
        for (int i = 2; i < keys.length; i++) {
            DebugKey key = keys[i];
            m.put(key, key.name);
        }
        assertThat(m.size(), is(keys.length));
        assertThat(m.location(keys[0]), is(0));
        assertThat(m.location(keys[1]), is(m.r()+1));
        assertThat(m.location(keys[2]), is(m.r()+2));
        assertThat(m.location(keys[3]), is(m.r()+0));
        assertThat(m.location(keys[4]), is(1));
        assertThat(hfact.i, is(2));
    }

    @Test
    public void testIterator() {
        DebugKey[] keys = {
            new DebugKey(new int[] { 0, 0 }, "a"),
            new DebugKey(new int[] { 0, 1 }, "b"),
            new DebugKey(new int[] { 0, 2 }, "c"),
            new DebugKey(new int[] { 1, 0 }, "d"),
            new DebugKey(new int[] { 1, 1 }, "e") 
        };
        DebugHasherFactory hfact = new DebugHasherFactory();
        CuckooHashMap<DebugKey, Integer> m = new CuckooHashMap<DebugKey, Integer>(hfact);
        for (int i = 0; i < keys.length; i++) {
            DebugKey key = keys[i];
            m.put(key, i);
        }
        boolean[] present = new boolean[keys.length];
        for (int i : m.values()) {
            assertThat(present[i], is(false));
            present[i] = true;
        }
        for (boolean b : present) {
            assertThat(b, is(true));
        }
    }

    @Test
    public void testIteratorRemove() {
        DebugKey[] keys = {
            new DebugKey(new int[] { 0, 0 }, "a"),
            new DebugKey(new int[] { 0, 1 }, "b"),
            new DebugKey(new int[] { 0, 2 }, "c"),
            new DebugKey(new int[] { 1, 0 }, "d"),
            new DebugKey(new int[] { 1, 1 }, "e") 
        };
        DebugHasherFactory hfact = new DebugHasherFactory();
        CuckooHashMap<DebugKey, Integer> m = new CuckooHashMap<DebugKey, Integer>(hfact);
        for (int i = 0; i < keys.length; i++) {
            DebugKey key = keys[i];
            m.put(key, i);
        }
        boolean[] check = new boolean[keys.length];
        boolean[] present = new boolean[keys.length];
        Iterator<Integer> it = m.values().iterator();
        for (int k = 0; it.hasNext(); k++) {
            int i = it.next();
            assertThat(present[i], is(false));
            present[i] = true;
            if ((k & 1) == 0) {
                it.remove();
                check[i] = true;
            }
        }
        assertThat(m.size(), is(2));
        for (int i : m.values()) {
            assertThat(check[i], is(false));
            check[i] = true;
        }
        for (boolean b : present) {
            assertThat(b, is(true));
        }
        for (boolean b : check) {
            assertThat(b, is(true));
        }
    }

    @Test(expected = RehashFailedException.class)
    public void testFailedReseed() {
        DebugKey[] keys = {
            new DebugKey(new int[] { 0, 0 }, "a"),
            new DebugKey(new int[] { 0, 1 }, "b"),
            new DebugKey(new int[] { 0, 2 }, "c"),
            new DebugKey(new int[] { 1, 0 }, "d"),
            new DebugKey(new int[] { 1, 1 }, "e"),
            new DebugKey(new int[] { 1, 2 }, "f")
        };
        DebugHasherFactory hfact = new DebugHasherFactory();
        CuckooHashMap<DebugKey, String> m = new CuckooHashMap<DebugKey, String>(hfact);
        for (DebugKey key : keys) {
            m.put(key, key.name);
        }
    }

    @Test
    public void testReseed() {
        DebugKey[] keys = {
            new DebugKey(new int[] { 0, 0 }, "a"),
            new DebugKey(new int[] { 0, 1 }, "b"),
            new DebugKey(new int[] { 0, 2 }, "c"),
            new DebugKey(new int[] { 1, 0 }, "d"),
            new DebugKey(new int[] { 1, 1 }, "e"),
            new DebugKey(new int[] { 1, 2, 1, 3 }, "f")
        };
        DebugHasherFactory hfact = new DebugHasherFactory();
        CuckooHashMap<DebugKey, String> m = new CuckooHashMap<DebugKey, String>(hfact, 100);
        for (DebugKey key : keys) {
            m.put(key, key.name);
        }
        assertThat(m.size(), is(keys.length));
        assertThat(hfact.i, is(4));
    }

    @Test
    public void moreReseed() {
        DebugKey[] keys = {
            new DebugKey(new int[] { 0, 0, 10, 10 }, "a"),
            new DebugKey(new int[] { 0, 1, 10, 11 }, "b"),
            new DebugKey(new int[] { 0, 2, 10, 12 }, "c"),
            new DebugKey(new int[] { 1, 0, 11, 10 }, "d"),
            new DebugKey(new int[] { 1, 1, 11, 11 }, "e"),
            new DebugKey(new int[] { 1, 2, 11, 12, 1, 3 }, "f")
        };
        DebugHasherFactory hfact = new DebugHasherFactory();
            CuckooHashMap<DebugKey, String> m = new CuckooHashMap<DebugKey, String>(hfact, 100);
        for (DebugKey key : keys) {
            m.put(key, key.name);
        }
        assertThat(m.size(), is(keys.length));
        assertThat(hfact.i, is(6));
        for (DebugKey key : keys) {
            m.put(key, key.name);
        }
        assertThat(m.size(), is(keys.length));
        assertThat(hfact.i, is(6));
        for (DebugKey key : keys) {
            m.remove(key);
        }
        assertThat(m.size(), is(0));
        for (DebugKey key : keys) {
            m.put(key, key.name);
        }
        assertThat(m.size(), is(keys.length));
        assertThat(hfact.i, is(6));
    }

    @Test
    public void testExpandAndReseed() {
        DebugKey[] keys = {
            new DebugKey(new int[] { 0, 0, 10, 10 }, "a"),
            new DebugKey(new int[] { 0, 1, 10, 11 }, "b"),
            new DebugKey(new int[] { 0, 2, 10, 12 }, "c"),
            new DebugKey(new int[] { 1, 0, 11, 10 }, "d"),
            new DebugKey(new int[] { 1, 1, 11, 11 }, "e"),
            new DebugKey(new int[] { 1, 2, 11, 12, 1, 3 }, "f")
        };
        DebugHasherFactory hfact = new DebugHasherFactory();
        CuckooHashMap<DebugKey, String> m = new CuckooHashMap<DebugKey, String>(hfact, 1, 0.1);
        assertThat(m.r(), is(2));
        m.put(keys[0], keys[0].name);
        assertThat(m.r(), is(2));
        m.put(keys[0], keys[0].name);
        assertThat(m.r(), is(2));
        m.put(keys[1], keys[1].name);
        assertThat(m.r(), is(4));
        m.put(keys[2], keys[2].name);
        assertThat(m.r(), is(4));
        m.put(keys[3], keys[3].name);
        assertThat(m.r(), is(8));
        m.put(keys[4], keys[4].name);
        assertThat(m.r(), is(8));
        m.put(keys[5], keys[5].name);
        assertThat(m.r(), is(8));
        assertThat(hfact.i, is(6));

        // The following would be applicable if we did shrink.
        // for (int i = 0; i < 4; i++) {
        //     m.remove(keys[i]);
        // }
        // assertThat(m.r(), is(8));
        // m.remove(keys[4]);
        // assertThat(m.r(), is(4));
    }
}
