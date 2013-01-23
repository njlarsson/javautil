package dk.itu.jesl.multic;

import java.io.*;
import java.util.*;
import java.util.regex.*;

public class CorrectAnswer {
    private static final int K = 4; // hardcoded number of alternatives
    private static final double DEFAULT_MULTI_SCORE = 1.0;

    private int pages = 0;

    public static Question[][] parse(BufferedReader r) throws IOException {
	return new CorrectAnswer().parseNonstatic(r);
    }

    private Question[][] parseNonstatic(BufferedReader r) throws IOException {
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
	    } catch (NumberFormatException nfe) {
		throw new Err.FormatException(nfe).setPage(pages);
	    }
	}
	Question[][] v = new Question[pageList.size()][];
	for (int i = 0; i < v.length; i++) {
	    ArrayList<Question> p = pageList.get(i);
            // System.out.print("page " + (i+1) + ":");
            // for (Question q : p) {
            //     System.out.print(" " + q.name());
            // }
            // System.out.println();
	    v[i] = p.toArray(new Question[p.size()]);
	}
	return v;
    }

    private static Pattern
	probNo = Pattern.compile("(\\d+) +"),
	defaultMulti = Pattern.compile("(\\d)"),
	specMulti = Pattern.compile("\\[(\\d+(\\.\\d+)?):(\\d)\\]"),
	essay = Pattern.compile("\\((\\d+(\\.\\d+)?)\\)"),
	probEnd = Pattern.compile("; *");

    private ArrayList<Question> parsePage(String s, Question pred) {
	ArrayList<Question> v = new ArrayList<Question>();
	int pos = 0;
	do {
	    pos = parseProblem(s, pos, v, pred);
            pred = v.get(v.size()-1);
	} while (pos < s.length());
	return v;
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
		    int j = s.charAt(defaultMultiM.start(1)) - '0';
		    Err.conf(j >= 1 && j <= K);
		    v.add(Question.multi(pred, pages, problem, K, j, DEFAULT_MULTI_SCORE));
		    pos = defaultMultiM.end();
		} else if (specMultiM.region(pos, end).lookingAt()) {
		    double score = Double.parseDouble(specMultiM.group(1));
		    char c = s.charAt(specMultiM.start(3));
		    Err.conf(c >= '1' && c <= '0' + K, c + " out of range");
		    v.add(Question.multi(pred, pages, problem, K, c-'0', score));
		    pos = specMultiM.end();
		} else if (essayM.region(pos, end).lookingAt()) {
		    v.add(Question.essay(pred, pages, problem, Double.parseDouble(essayM.group(1))));
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
	} catch (NumberFormatException nfe) {
	    throw new Err.FormatException(nfe).setProblem("" + problem);
	}
    }
}
