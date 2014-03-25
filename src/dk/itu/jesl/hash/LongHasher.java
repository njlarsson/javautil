package dk.itu.jesl.hash;

/**
 * Interface for providing hash function for long integer keys, from a family of
 * hash functions.
 *
 * @see Hasher
 */
public interface LongHasher {
    /**
     * Factory of hashers, which should typically provide a randomly chosen
     * function from a family of hash function.
     */
    interface Factory {
	LongHasher makeHasher();
    }

    /**
     * Computes the hash value for the given key, for the hash function that
     * this hasher represents.
     */
    int hashCode(long key);
}
