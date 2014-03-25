import java.util.Random;

/**
 * Program that produces a series of random numbers.
 */
public class RandSeries {
    public static void main(String[] args) {
        int n = Integer.parseInt(args[0]);
        int lo = Integer.parseInt(args[1]);
        int hi = Integer.parseInt(args[2]);
        
        Random random = new Random();
        
        for (int i = 0; i < n; i++) System.out.print(lo + (random.nextInt() & Integer.MAX_VALUE) % (hi-lo+1) + " ");
        System.out.println();
    }
}