package dk.itu.jesl.hash;

import java.util.Arrays;
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
        assertThat("minN", m.minN(), is(0));
        assertThat("maxLoop", m.maxLoop(), is(128));
    }

    @Test
    public void testParameterCalc2() {
        CuckooHashMap<DebugKey, String> m = new CuckooHashMap<DebugKey, String>(new DebugHasherFactory(), 1000000, 0.1);
        assertThat("r", m.r(), is(2097152));
        assertThat("maxN", m.maxN(), is(1906501));
        assertThat("minN", m.minN(), is(0));
        assertThat("maxLoop", m.maxLoop(), is(459));
    }

    @Test
    public void testParameterCalc3() {
        CuckooHashMap<DebugKey, String> m = new CuckooHashMap<DebugKey, String>(new DebugHasherFactory(), 1000000, 0.5);
        assertThat("r", m.r(), is(2097152));
        assertThat("maxN", m.maxN(), is(1398101));
        assertThat("minN", m.minN(), is(0));
        assertThat("maxLoop", m.maxLoop(), is(108));
    }

    @Test
    public void testBasicPutGet() {
        DebugKey[] keys = {
            new DebugKey(new int[] { 3, 4 }, "a"),
            new DebugKey(new int[] { 3, 5 }, "b")
        };
        CuckooHashMap<DebugKey, String> m = new CuckooHashMap<DebugKey, String>(new DebugHasherFactory());
        for (DebugKey key : keys) {
            m.put(key, key.name);
        }
        assertThat(m.size(), is(keys.length));
        for (DebugKey key : keys) {
            assertThat(m.get(key), is(key.name));
            assertThat(m.get(new DebugKey(key.v, "x")), is(key.name));
        }
        assertThat(m.get(new DebugKey(new int[] { 3, 6 }, "y")), nullValue());
        assertThat(m.location(keys[0]), is(3 % m.r()));
        assertThat(m.location(keys[1]), is(m.r() + 5 % m.r()));
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
        CuckooHashMap<DebugKey, String> m = new CuckooHashMap<DebugKey, String>(new DebugHasherFactory());
        for (DebugKey key : keys) {
            m.put(key, key.name);
        }
        assertThat(m.size(), is(keys.length));
        assertThat(m.location(keys[0]), is(0));
        assertThat(m.location(keys[1]), is(m.r()+1));
        assertThat(m.location(keys[2]), is(m.r()+2));
        assertThat(m.location(keys[3]), is(m.r()+0));
        assertThat(m.location(keys[4]), is(1));
    }

    @Test(expected = RehashFailedException.class)
    public void testFailedRehash() {
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
    }
}
