import java.util.Map;
import dk.itu.jesl.hash.*;

public class CuckooLongExample {
    public static void main(String[] args) {
        Map<Long, String> m = new CuckooHashMap<Long, String>(TabHasher.longHasherFactory());
        m.put(5L, "five");
        m.put(10L, "ten");
        m.put(100000000000L, "a hundred billion");
        m.put(500000000000L, "five hundred billion");

        System.out.println("get: " + m.get(100000000000L));
        System.out.println("\nkeys:");
        for (long key : m.keySet()) {
            System.out.println(key);
        }
        System.out.println("\nvalues");
        for (String s : m.values()) {
            System.out.println(s);
        }
    }
}
