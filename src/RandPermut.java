import java.util.Random;

/**
 * Program that produces a series of N random numbers between 0 and N-1.
 */
public class RandPermut {
    public static void main(String[] args) {
        int n = Integer.parseInt(args[0]);

        int[] a = new int[n];

        for (int i = 0; i < n; i++) {
            a[i] = i;
        }
        
        Random random = new Random();
        
        for (int m = n; m > 1; m--) {
            int i = (random.nextInt() & Integer.MAX_VALUE) % m;
            int j = m-1;
            int t = a[j]; a[j] = a[i]; a[i] = t;
        }

        for (int i = 0; i < n; i++) {
            System.out.print(a[i] + " ");
        }
        System.out.println();
    }
}
