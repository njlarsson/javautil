package dk.itu.jesl.multic;

import java.io.*;

public class Score {
   private static BufferedReader openFile(String[] args, int i, String what) {
      String name = null;
      try {
	 name = args[i];
	 return new BufferedReader(new InputStreamReader(new FileInputStream(name), "UTF-8"));
      } catch (Exception e) {
	 System.err.print("Failed to open " + what);
	 if (name != null) { System.err.print(" " + name); }
	 System.err.println(": " + e);
	 System.exit(66);	// EX_NOINPUT
	 throw new IllegalStateException(); // unreachable
      }
   }

   public static void main(String[] args) throws IOException {
      int i = 0;
      boolean detail = false;
      while (i < args.length && args[i].charAt(0) == '-') {
	 switch(args[i].charAt(1)) {
	 case 'd': detail = true; break;
	 default:
	    System.err.println("Unrecognized option: " + args[i]);
	    System.exit(64);	// EX_USAGE
	 }
	 i++;
      }
      Question[][] corr = CorrectAnswer.parse(openFile(args, i++, "correct answer"));
      BufferedReader ansFile = openFile(args, i++, "given answers");
      PrintWriter w = new PrintWriter(new OutputStreamWriter(System.out));
      while (true) {
	 Student stud = Student.parse(ansFile, corr);
	 if (stud == null) { break; }
	 if (detail) { stud.reportDetail(w); }
	 else { stud.reportScore(w); }
      }
      w.flush();
      System.exit(0);
   }
}