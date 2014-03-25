package dk.itu.jesl.hash;

public interface LongHasher {
    interface Factory {
	LongHasher makeHasher();
    }

    int hashCode(long key);
}
