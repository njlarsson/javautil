package dk.itu.jesl.hash;

/**
 * Exception thrown when a hash table gives up trying to rehash a set of keys,
 * e.g., after trying a large number of generated hash function pairs for cuckoo
 * hashing. This typically means that the provided hash function generator is
 * flawed or misused.
 */
public class RehashFailedException extends RuntimeException {
    /** 
     * Number of times to try rehashing before deciding that the factory isn't
     * going to produce a working pair, giving up, and throwing an
     * exception. Set, very ad hoc, to 100 by default. Can be changed, which
     * takes effect globally, and non-thread-safely.
     */
    public static int REHASH_TRIES = 100;

    public RehashFailedException() { super(); }
}
