package dk.itu.jesl.multic;

import java.io.*;
import java.util.*;
import java.util.regex.*;

public class CorrectAnswer {
   private static final int K = 4; // harcoded number of alternatives
   private static final double MULTI_SCORE = 1.0; // hardcoded score of multiple-choice questions

   private int pages = 0;
   private int problems = 0;
   private int questions = 0;

   public static Question[][] parse(BufferedReader r) throws IOException {
      return new CorrectAnswer().parseNonstatic(r);
   }

   private Question[][] parseNonstatic(BufferedReader r) throws IOException {
      ArrayList<ArrayList<Question>> pageList = new ArrayList<ArrayList<Question>>();
      while (true) {
	 String s = r.readLine();
	 if (s == null) { break; }
	 s = s.trim();
	 pages++;
	 try {
	    if (s.length() > 0) {
	       pageList.add(parsePage(s));
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
	 v[i] = p.toArray(new Question[p.size()]);
      }
      return v;
   }

   private static Pattern problemP = Pattern.compile(" *(\\d*) +(\\d*|\\(\\d*\\)) *; *");

   private ArrayList<Question> parsePage(String s) {
      Matcher m = problemP.matcher(s);
      int lastEnd = 0;
      ArrayList<Question> v = new ArrayList<Question>();
      do {
	 Err.conf(m.find(), lastEnd + " " + s.length());
	 Err.conf(m.start() == lastEnd, m.start() + " " + lastEnd);
	 int problem = Integer.parseInt(m.group(1));
	 String spec = m.group(2);
	 Err.conf(problem == ++problems);
	 try {
	    if (spec.charAt(0) == '(') {
	       v.add(Question.essay(pages, "" + problems, Double.parseDouble(spec.substring(1, spec.length() - 1))));
	    } else {
	       for (int i = 0; i < spec.length(); i++) {
		  int j = spec.charAt(i) - '0';
		  Err.conf(j >= 1 && j <= K, j + "/" + i + "/" + spec + "/" + spec.charAt(i));
		  v.add(Question.multi(pages, problems + (spec.length() == 1 ? "" : "" + (char) ('a' + i)), K, j, MULTI_SCORE));
	       }
	    }
	 } catch (Err.FormatException fe) {
	    throw fe.setProblem(problems);
	 } catch (NumberFormatException nfe) {
	    throw new Err.FormatException(nfe).setProblem(problems);
	 }
	 lastEnd = m.end();
      } while (lastEnd < s.length());
      return v;
   }
}
