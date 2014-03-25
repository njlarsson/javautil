public class NextPrime {
    public static void main(String[] args) {
        long x = Long.parseLong(args[0]);
        int d;
        if (x < 0) {
            d = -2;
            x = -x - (1 ^ (x & 1));
        } else {
            d = 2;
            x += 1 ^ (x & 1);
        }

        while (!isPrime(x)) x += d;
        System.out.println(x);
    }

    static boolean isPrime(long n) {
        if (n % 2 == 0) return false;
        for (long i = 3; i*i <= n; i+=2) {
            if (n % i == 0) return false;
        }
        return true;
    }
}