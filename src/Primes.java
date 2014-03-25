import java.util.Random;

/**
 * Program to generate a sequence of roughly doubling pseudo-random
 * prime numbers, not too close to powers of two. Useful for hash
 * table sizes.
 */
public class Primes {
    static class MyRandom extends Random {
        public int next(int bits) { return super.next(bits); }
    }

    public static void main(String[] args) {
        MyRandom random = new MyRandom();
        
        for (int i = 1; i < 31; i++) {
            int j = Math.max(0, i-2);
            int r = (random.next(j) & (1<<j)-1) * (random.next(1) * 2 - 1);
            //System.out.print("{" + (1<<i) + "," + (1<<i-1) + "," + (1<<j) + "}");
            int n = (1 << i) + (1 << i-1) + r | 1;
            while (!isPrime(n)) n += 2;
            //System.out.print("[" + Integer.toString(n, 2) + "]");
            System.out.print(n + ", ");
        }
    }

    static boolean isPrime(int n) {
        if (n % 2 == 0) return false;
        int lim = (int) Math.sqrt(n) + 1;
        for (int i = 3; i <= lim; i+=2) {
            if (n % i == 0) return false;
        }
        return true;
    }
}