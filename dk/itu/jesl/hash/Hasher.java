package dk.itu.jesl.hash;

public interface Hasher<T> {
    interface Factory<T> {
	Hasher<T> makeHasher();
    }

    int hashCode(T t);
}
