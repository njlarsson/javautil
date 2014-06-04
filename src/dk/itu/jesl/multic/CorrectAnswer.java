package dk.itu.jesl.multic;

import java.io.*;
import java.util.*;
import java.util.regex.*;

public class CorrectAnswer {
    private static final int K = 4; // hardcoded number of alternatives
    private static final double DEFAULT_MULTI_SCORE = 1.0;

    private int pages = 0;

    /** Parses a correct-answer file, returning an array of pages, where each
      * page is an array of the questions on that page (corresponding to lines
      * in the correct-answer file.
      */
    public static Question[][] parsePages(BufferedReader r, int multLetterBase) throws IOException {
	return listlistToArrayarray(new CorrectAnswer(multLetterBase).parseNonstatic(r));
    }

    /** Parses a correct-answer file, returning an array of problems, where each
      * problem is an array of the subproblems of that problem.
      */
    public static Question[][] parseProblems(BufferedReader r, int multLetterBase) throws IOException {
	ArrayList<ArrayList<Question>> pages = new CorrectAnswer(multLetterBase).parseNonstatic(r);
	ArrayList<ArrayList<Question>> problems = new ArrayList<ArrayList<Question>>();
        int i = 0;
        ArrayList<Question> p = null;
        for (ArrayList<Question> page : pages) {
            for (Question q : page) {
                if (p == null) {
                    p = new ArrayList<Question>();
                } else if (q.mainProblem() != i) {
                    problems.add(p);
                    p = new ArrayList<Question>();
                }
                i = q.mainProblem();
                p.add(q);
            }
        }
        if (p != null) problems.add(p);
        return listlistToArrayarray(problems);
    }

    private ArrayList<ArrayList<Question>> parseNonstatic(BufferedReader r) throws IOException {
	ArrayList<ArrayList<Question>> pageList = new ArrayList<ArrayList<Question>>();
        Question pred = null;
	while (true) {
	    String s = r.readLine();
	    if (s == null) { break; }
	    s = s.trim();
	    pages++;
	    try {
		if (s.length() > 0) {
                    ArrayList<Question> page = parsePage(s, pred);
		    pageList.add(page);
                    pred = page.get(page.size()-1);
		}
	    } catch (Err.FormatException fe) {
		throw fe.setPage(pages);
	    } catch (RuntimeException rte) {
		throw new Err.FormatException(rte).setPage(pages);
	    }
	}
        return pageList;
    }

    private static Question[][] listlistToArrayarray(ArrayList<ArrayList<Question>> pageList) {
        Question[][] v = new Question[pageList.size()][];
	for (int i = 0; i < v.length; i++) {
	    ArrayList<Question> p = pageList.get(i);
	    v[i] = p.toArray(new Question[p.size()]);
	}
	return v;
    }

    private static String fp = "\\d+(?:\\.\\d+)?";

    private final int multLetterBase;
    private final Pattern probNo, defaultMulti, specMulti, essay, probEnd;

    private ArrayList<Question> parsePage(String s, Question pred) {
	ArrayList<Question> v = new ArrayList<Question>();
	int pos = 0;
	do {
	    pos = parseProblem(s, pos, v, pred);
            pred = v.get(v.size()-1);
	} while (pos < s.length());
	return v;
    }

    public CorrectAnswer(int multLetterBase) {
        this.multLetterBase = multLetterBase;
	probNo = Pattern.compile("(\\d+) +");
	essay = Pattern.compile("\\((" + fp + ")(?:\\*(" + fp + "))?\\)");
	probEnd = Pattern.compile("; *");
        
        if (multLetterBase == '0') {
            defaultMulti = Pattern.compile("(\\d)");
            specMulti = Pattern.compile("\\[(" + fp + "):(\\d)\\]");
        } else if (multLetterBase == 'A'-1) {
            defaultMulti = Pattern.compile("([A-Z])");
            specMulti = Pattern.compile("\\[(" + fp + "):([A-Z])\\]");
        } else if (multLetterBase == 'a'-1) {
            defaultMulti = Pattern.compile("([a-z])");
            specMulti = Pattern.compile("\\[(" + fp + "):([a-z])\\]");
        } else {
            throw new IllegalStateException("No pattern for multLetterBase=" + multLetterBase);
        }
    }

    private int parseProblem(String s, int pos, ArrayList<Question> v, Question pred) {
	int end = s.length();
	Matcher probNoM = probNo.matcher(s).region(pos, end);
	Err.conf(probNoM.lookingAt(), "Not start of problem: " + s.substring(pos));
	int problem = Integer.parseInt(probNoM.group(1));
	pos = probNoM.end();
	try {
	    Matcher defaultMultiM = defaultMulti.matcher(s);
	    Matcher specMultiM = specMulti.matcher(s);
	    Matcher essayM = essay.matcher(s);
	    Matcher probEndM = probEnd.matcher(s);
	    for (int i = 0; pos < end; i++) {
		if (defaultMultiM.region(pos, end).lookingAt()) {
		    int j = Character.toUpperCase(s.charAt(defaultMultiM.start(1))) - multLetterBase;
		    Err.conf(j >= 1 && j <= K);
		    v.add(Question.multi(pred, pages, problem, K, j, DEFAULT_MULTI_SCORE, multLetterBase));
		    pos = defaultMultiM.end();
		} else if (specMultiM.region(pos, end).lookingAt()) {
		    double score = Double.parseDouble(specMultiM.group(1));
		    char c = Character.toUpperCase(s.charAt(specMultiM.start(2)));
		    Err.conf(c > multLetterBase && c <= multLetterBase + K, c + " out of range");
		    v.add(Question.multi(pred, pages, problem, K, c-multLetterBase, score, multLetterBase));
		    pos = specMultiM.end();
		} else if (essayM.region(pos, end).lookingAt()) {
                    String rescaleString = essayM.group(2);
                    double rescale = rescaleString == null ? 1.0 : Double.parseDouble(rescaleString);
		    v.add(Question.essay(pred, pages, problem, Double.parseDouble(essayM.group(1)), rescale));
		    pos = essayM.end();
		} else if (probEndM.region(pos, end).lookingAt()) {
		    return probEndM.end();
		} else {
		    throw new Err.FormatException("Syntax error: " + s.substring(pos));
		}
                pred = v.get(v.size()-1);
	    }
	    throw new Err.FormatException("Unexpected end of line. Missing semicolon?");
	} catch (Err.FormatException fe) {
	    throw fe.setProblem("" + problem);
	} catch (RuntimeException rte) {
	    throw new Err.FormatException(rte).setProblem("" + problem);
	}
    }

    /** Parses and prints a correct answer file. */
    public static void main(String[] args) throws IOException {
        boolean files = false;
        int multLetterBase = '0';
        String arrayMeans = "page";
        int i = 0;
        while (i < args.length && args[i].charAt(0) == '-') {
            if ("-A".equals(args[i])) {
                multLetterBase = 'A'-1;
            } else if ("-F".equals(args[i])) {
                files = true;
                arrayMeans = "question";
            } else {
                System.err.println("Unrecognized option: " + args[i]);
                System.exit(64);        // EX_USAGE
            }
            i++;
        }
        BufferedReader r = new BufferedReader(new InputStreamReader(new FileInputStream(args[i]), "UTF-8"));
        Question[][] corr;
        if (files) {
            corr = CorrectAnswer.parseProblems(r, multLetterBase);
        } else {
            corr = CorrectAnswer.parsePages(r, multLetterBase);
        }            
        for (int k = 0; k < corr.length; k++) {
            System.out.print(arrayMeans + " " + k + ":");
            for (int j = 0; j < corr[k].length; j++) {
                System.out.print( " [" + corr[k][j].toString() + "]");
            }
            System.out.println();
        }
    }
}
