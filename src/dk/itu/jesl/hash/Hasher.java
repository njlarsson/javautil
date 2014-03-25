package dk.itu.jesl.hash;

/**
 * Interface for providing hash function for general keys, from a family of hash
 * functions.
 *
 * @see LongHasher
 */
public interface Hasher<K> {
    /**
     * Factory of hashers, which should typically provide a randomly chosen
     * function from a family of hash function.
     */
    interface Factory<K> {
	Hasher<K> makeHasher();
    }

    /**
     * Computes the hash value for the given key, for the hash function that
     * this hasher represents.
     */
    int hashCode(K key);
}
