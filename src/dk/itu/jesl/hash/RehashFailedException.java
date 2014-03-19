package dk.itu.jesl.hash;

/**
 * Exception thrown when a hash table gives up trying to rehash a set of keys,
 * e.g., after trying a large number of generated hash function pairs for cuckoo
 * hashing. This typically means that the provided hash function generator is
 * flawed or misused.
 *
 * @see CuckooHashMap#REHASH_TRIES
 */
public class RehashFailedException extends RuntimeException {
    public RehashFailedException() { super(); }
}
