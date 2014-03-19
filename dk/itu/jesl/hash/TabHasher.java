package dk.itu.jesl.hash;

import java.util.Random;

public final class TabHasher {
    public interface ByteSequence {
	byte byteAt(int i);
    }

    private final int keyBytes;
    private final int[] tab;

    TabHasher(int keyBytes) {
        this.keyBytes = keyBytes;
        tab = new int[256*keyBytes];
        Random rand = new Random();
        for (int i = 0; i < tab.length; i++) {
	    tab[i] = (int) rand.nextInt();
	}
    }
    
    /**
     * Gets hash code for key stored at an offset in a byte array.
     */
    public int hashCode(byte[] a, int off) {
        int h = tab[a[off] & 255];
	for (int i = 1, j = 0; i < keyBytes; i++) {
	    h ^= tab[(j+=256) + (a[off+i] & 255)];
	}
        return h;
    }

    /**
     * Gets hash code for key stored at an offset in a ByteSequence.
     */
    public int hashCode(ByteSequence s, int off) {
        int h = tab[s.byteAt(off) & 255];
	for (int i = 1, j = 0; i < keyBytes; i++) {
	    h ^= tab[(j+=256) + (s.byteAt(off+i) & 255)];
	}
        return h;
    }

    /**
     * Gets hash code for a 32-bit int key. Assumes that keyBytes is at least 4
     * (otherwise you should get ArrayIndexOutOfBoundsException).
     */
    public int intHashCode(int key) {
        int h = tab[key & 255];
        h ^= tab[1*256 + ((key >>>= 8) & 255)];
        h ^= tab[2*256 + ((key >>>= 8) & 255)];
        h ^= tab[3*256 + ((key >>>= 8) & 255)];
        return h;
    }

    /**
     * Gets hash code for a 64-bit long key. Assumes that keyBytes is at least 8
     * (otherwise you should get ArrayIndexOutOfBoundsException).
     */
    public int longHashCode(long key) {
        int h = tab[((int) key) & 255];
        h ^= tab[1*256 + (((int) (key >>>= 8)) & 255)];
        h ^= tab[2*256 + (((int) (key >>>= 8)) & 255)];
        h ^= tab[3*256 + (((int) (key >>>= 8)) & 255)];
        h ^= tab[4*256 + (((int) (key >>>= 8)) & 255)];
        h ^= tab[5*256 + (((int) (key >>>= 8)) & 255)];
        h ^= tab[6*256 + (((int) (key >>>= 8)) & 255)];
        h ^= tab[7*256 + (((int) (key >>>= 8)) & 255)];
        return h;
    }

    // public static <T> Hasher.Factory<T> hashCodeHasher(Class<T> klass) {
    // 	return new Hasher.Factory<T> () {
    // 	    public Hasher<T> makeHasher() {
    // 		return new Hasher<T>() {
    // 		    private TabHasher th = new TabHasher(4);
    // 		    public int hashCode(T key) { return th.intHashCode(key.hashCode()); }
    // 		};
    // 	    }
    // 	};
    // }
}
