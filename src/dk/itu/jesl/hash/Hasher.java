package dk.itu.jesl.hash;

public interface Hasher<K> {
    interface Factory<K> {
	Hasher<K> makeHasher();
    }

    int hashCode(K key);
}
