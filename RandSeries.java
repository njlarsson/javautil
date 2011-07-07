import java.util.Random;

/**
 * Program that produces a series of random numbers.
 */
public class RandSeries {
    public static void main(String[] args) {
        int n = Integer.parseInt(args[0]);
        int m = Integer.parseInt(args[1]);
        
        Random random = new Random();
        
        for (int i = 1; i < n; i++) System.out.print((random.nextInt() & Integer.MAX_VALUE) % m + 1 + " ");
        System.out.println();
    }
}