package dk.itu.jesl.hash;

import java.util.Arrays;
import org.junit.*;
import static org.junit.Assert.*;
import static org.hamcrest.CoreMatchers.*;

public class CuckooHashMapTest {
    private static class DebugKey {
        final int[] v;
        DebugKey(int[] v) { this.v = v; }
        public String toString() {
            StringBuilder b = new StringBuilder();
            String d = "{ ";
            for (int i : v) { b.append(d).append(Integer.toString(i)); d = ", "; }
            return b.append(" }").toString();
        }
        public boolean equals(Object that) {
            return Arrays.equals(v, ((DebugKey) that).v);
        }
    }

    private static class DebugHasher implements Hasher<DebugKey> {
        final int serialNo;
        DebugHasher(int serialNo) { this.serialNo = serialNo; }
        public int hashCode(DebugKey key) { return key.v[serialNo % key.v.length]; }
    }

    private static Hasher.Factory<DebugKey> hfact =
        new Hasher.Factory<DebugKey>() {
            private int i = 0;
            public Hasher<DebugKey> makeHasher() { return new DebugHasher(i++); }
        };

    @Test
    public void testParameterCalc1() {
        CuckooHashMap<DebugKey, String> m = new CuckooHashMap<DebugKey, String>(hfact, 100, 0.1);
        assertThat("r", m.r(), is(128));
        assertThat("cap", m.cap(), is(116));
        assertThat("maxLoop", m.maxLoop(), is(128));
    }

    @Test
    public void testParameterCalc2() {
        CuckooHashMap<DebugKey, String> m = new CuckooHashMap<DebugKey, String>(hfact, 1000000, 0.1);
        assertThat("r", m.r(), is(2097152));
        assertThat("cap", m.cap(), is(1906501));
        assertThat("maxLoop", m.maxLoop(), is(459));
    }

    @Test
    public void testParameterCalc3() {
        CuckooHashMap<DebugKey, String> m = new CuckooHashMap<DebugKey, String>(hfact, 1000000, 0.5);
        assertThat("r", m.r(), is(2097152));
        assertThat("cap", m.cap(), is(1398101));
        assertThat("maxLoop", m.maxLoop(), is(108));
    }

    @Test
    public void testBasicPutGet() {
        DebugKey[] keys = {
            new DebugKey(new int[] { 3, 4 }),
            new DebugKey(new int[] { 3, 5 })
        };
        CuckooHashMap<DebugKey, String> m = new CuckooHashMap<DebugKey, String>(hfact);
        for (DebugKey key : keys) {
            m.put(key, key.toString());
        }
        for (DebugKey key : keys) {
            assertThat(m.get(key), is(key.toString()));
            assertThat(m.get(new DebugKey(key.v)), is(key.toString()));
        }
        assertThat(m.get(new DebugKey(new int[] { 3, 6 })), nullValue());
        assertThat(m.locate(keys[0]), is(3 % m.r()));
        assertThat(m.locate(keys[1]), is(m.r() + 5 % m.r()));
    }

    @Test
    public void testFailedRehash() {
        DebugKey[] keys = {
            new DebugKey(new int[] { 3, 4 }),
            new DebugKey(new int[] { 16+3, 16+4 })
        };
        CuckooHashMap<DebugKey, String> m = new CuckooHashMap<DebugKey, String>(hfact, 9);
        assertThat(m.r(), is(16));
        for (DebugKey key : keys) {
            m.put(key, key.toString());
        }
    }
}
