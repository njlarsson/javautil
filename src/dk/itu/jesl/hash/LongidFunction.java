package dk.itu.jesl.hash;

/**
 * Function to convert between long and some objects with a one-to-one mapping
 * to longs.
 */
public interface LongidFunction<K> {
    K fromLong(long l);
    long toLong(K k);
}
