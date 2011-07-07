/**
 * Calculation for assessing multichoice exam.
 *
 * Jesper Larsson, IT University of Copenhagen.
 */
public class MultiChoice {
    /**
     * Computes the score for one question.
     *
     * @param k Number of possible options.
     * @param a Number of checked options.
     * @param c True if the correct answer is among those checked.
     * @param w The maximum score.
     * @return The weighted score.
     */
    public static double score(int k, int a, boolean c, double max) {
        if (a < 0 || a > k) throw new IllegalArgumentException("Impossible number of checked options: " + a);
        if (a == 0 || a == k) return 0;
        double idealBits = log2(k);
        double givenBits = idealBits - log2(a);
        double bitWeight = max / idealBits;
        if (c) return                        givenBits * bitWeight;
        else   return - (double) a / (k-a) * givenBits * bitWeight;
    }

    public static void main(String[] args) {
        int k = Integer.parseInt(args[0]);
        int a = Integer.parseInt(args[1]);
        boolean c = Boolean.parseBoolean(args[2]);
        double max = Double.parseDouble(args[3]);
        System.out.println(score(k, a, c, max));
    }

    public static double log2(double x) { return Math.log(x)/Math.log(2); }
}