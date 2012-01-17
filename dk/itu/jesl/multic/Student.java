package dk.itu.jesl.multic;

import java.io.*;
import java.util.*;
import java.util.regex.*;

public class Student {
   private String name = null;
   private double score = 0.0;
   private ArrayList<String> wrong = new ArrayList<String>();
   private ArrayList<String> incomplete = new ArrayList<String>();
   private ArrayList<String> pending = new ArrayList<String>();

   public static Student parse(BufferedReader r, Question[][] corr) throws IOException {
      Student stud = new Student();
      if (stud.parseNonstatic(r, corr)) { return stud; }
      else { return null; }
   }

   public void reportScore(PrintWriter w) {
      w.format("%s: %f%s\n", name, score, (pending.size() > 0 ? " (pending!)" : ""));
   }
	 
   public void reportDetail(PrintWriter w) {
      w.format("%s: %f", name, score);
      if (wrong.size() > 0) {
	 w.format(" wrong");
	 String delim = ": ";
	 for (String q : wrong) {
	    w.format("%s%s", delim, q);
	    delim = ", ";
	 }
	 w.format(".");
      }
      if (incomplete.size() > 0) {
	 w.format(" incomplete");
	 String delim = ": ";
	 for (String q : incomplete) {
	    w.format("%s%s", delim, q);
	    delim = ", ";
	 }
	 w.format(".");
      }
      if (pending.size() > 0) {
	 w.format(" pending");
	 String delim = ": ";
	 for (String q : pending) {
	    w.format("%s%s", delim, q);
	    delim = ", ";
	 }
	 w.format(".");
      }
      w.println();
   }
	 
   private boolean parseNonstatic(BufferedReader r, Question[][] corr) throws IOException {
      String s;
      do {
	 s = r.readLine();
	 if (s == null) { return false; }
	 s = s.trim();
      } while (s.length() == 0);
      name = s;
      try {
	 for (int i = 0; i < corr.length; i++) {
	    try {
	       parsePage(r.readLine(), corr[i]);
	    } catch (Err.FormatException fe) {
	       throw fe.setPage(i + 1);
	    } catch (NumberFormatException nfe) {
	       throw new Err.FormatException(nfe).setPage(i + 1);
	    }
	 }
      } catch (Err.FormatException fe) {
	 throw fe.setSection(name);
      }
      return true;
   }

   private static final Pattern answerP = Pattern.compile("(\\S+) *");

   private void parsePage(String s, Question[] corr) {
      Err.conf(s != null);
      int lastEnd = 0;
      Matcher m = answerP.matcher(s);
      for (int i = 0; i < corr.length; i++) {
	 Question q = corr[i];
	 Err.conf(m.find());
	 Err.conf(m.start() == lastEnd, m.start() + " " + lastEnd);
	 double qs = q.score(m.group(1));
	 if (Double.isNaN(qs)) { pending.add(q.name); }
	 else {
	    if (qs < 0) { wrong.add(q.name); }
	    else if (qs < q.maxScore) { incomplete.add(q.name); }
	    score += qs;
	 }
	 lastEnd = m.end();
      }
   }
}