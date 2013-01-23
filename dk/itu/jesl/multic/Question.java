package dk.itu.jesl.multic;

import java.util.regex.*;

public abstract class Question {
    public final int page;
    public final double maxScore;
    private int problem, subProblem;
   
    private Question(Question pred, int page, int problem, double maxScore) {
        this.page = page; 
        this.problem = problem;
        this.maxScore = maxScore;
        if (pred != null && pred.problem == problem) {
            if (pred.subProblem == 0) pred.subProblem++;
            this.subProblem = pred.subProblem + 1;
        }
    }

    public String name() {
        StringBuilder b = new StringBuilder();
        b.append(Integer.toString(problem));
        if (subProblem > 0) {
            b.append((char) ('a'-1 + subProblem));
        }
        return b.toString();
    }

    public abstract double score(String answer);

    public abstract double rescaleFactor();

    private static double log2(double x) { return Math.log(x) / Math.log(2); }

    private static class MultiQuestion extends Question {
	final int k, correct;
	final double scaleFactor;
      
	MultiQuestion(Question pred, int page, int problem, int k, int correct, double maxScore) {
	    super(pred, page, problem, maxScore);
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
        
        public double rescaleFactor() { return 1.0; }
    }
      
    Pattern essayScorePattern = Pattern.compile("\\d*\\.\\d+");

    private static class EssayQuestion extends Question {
        private final double rescale;

	EssayQuestion(Question pred, int page, int problem, double maxScore, double rescale) {
	    super(pred, page, problem, maxScore);
            this.rescale = rescale;
	}
      
	public double score(String answer) {
	    if ("-".equals(answer)) { return 0.0; }
	    if ("*".equals(answer)) { return Double.NaN; }
	    Err.conf(essayScorePattern.matcher(answer).matches(), "Score must be given in decimal point form");
	    double score = Double.parseDouble(answer);
	    Err.conf(score >= 0 && score <= maxScore, "Score out of range: " + score);
	    return score;
	}

        public double rescaleFactor() { return rescale; }
    }

    public static Question multi(Question pred, int page, int problem, int k, int correct, double maxScore) {
	return new MultiQuestion(pred, page, problem, k, correct, maxScore);
    }

    public static Question essay(Question pred, int page, int problem, double maxScore, double rescale) {
	return new EssayQuestion(pred, page, problem, maxScore, rescale);
    }
}
