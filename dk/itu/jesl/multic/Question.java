package dk.itu.jesl.multic;

import java.util.regex.*;

public abstract class Question {
    public final int page;
    public final String name;
    public final double maxScore;
   
    private Question(int page, String name, double maxScore) { this.page = page; this.name = name; this.maxScore = maxScore; }

    public abstract double score(String answer);

    private static double log2(double x) { return Math.log(x) / Math.log(2); }

    private static class MultiQuestion extends Question {
	final int k, correct;
	final double scaleFactor;
      
	MultiQuestion(int page, String name, int k, int correct, double maxScore) {
	    super(page, name, maxScore);
	    Err.conf(k > 1 && correct >= 1 && correct <= 4);
	    this.k = k;
	    this.correct = correct;
	    scaleFactor = maxScore / log2(k);
	}

	public double score(String answer) {
	    boolean c = false;
	    int a = answer.length();
	    int max = 0;
	    for (int i = 0; i < a; i++) {
		if (answer.charAt(i) == '-') {
		    Err.conf(i == 0 && a == 1);
		    return 0.0;
		}
		if (answer.charAt(i) == '*') {
		    Err.conf(i == 0 && a == 1);
		    return Double.NaN;
		}
		int j = answer.charAt(i) - '0';
		Err.conf(j > max && j <= k, j + "");
		c |= j == correct;
		max = j;
	    }
	    if (a == k) { return 0; }
	    if (c) {
		return log2((double) k / a) * scaleFactor;
	    } else {
		return -(double) a / (k - a) * log2((double) k / a) * scaleFactor;
	    }
	}
    }
      
    Pattern essayScorePattern = Pattern.compile("\\d*\\.\\d+");

    private static class EssayQuestion extends Question {
	EssayQuestion(int page, String name, double maxScore) {
	    super(page, name, maxScore);
	}
      
	public double score(String answer) {
	    if ("-".equals(answer)) { return 0.0; }
	    if ("*".equals(answer)) { return Double.NaN; }
	    Err.conf(essayScorePattern.matcher(answer).matches(), "Score must be given in decimal point form");
	    double score = Double.parseDouble(answer);
	    Err.conf(score >= 0 && score <= maxScore, "Score out of range: " + score);
	    return score;
	}
    }

    public static Question multi(int page, String name, int k, int correct, double maxScore) {
	return new MultiQuestion(page, name, k, correct, maxScore);
    }

    public static Question essay(int page, String name, double maxScore) {
	return new EssayQuestion(page, name, maxScore);
    }
}
