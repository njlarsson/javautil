package dk.itu.jesl.multic;

import java.io.*;

public class Score {
    private static String HELP_MSG =
        "Arguments: [options] correct_answers_file given_answers_file\n" +
        "Options:\n" +
        "   -d:           Detailed information\n" +
        "   -h or --help: Print this message and quit";

    private static BufferedReader openFile(String[] args, int i, String what) {
        String name = null;
        try {
            name = args[i];
            return new BufferedReader(new InputStreamReader(new FileInputStream(name), "UTF-8"));
        } catch (Exception e) {
            System.err.print("Failed to open " + what);
            if (name != null) { System.err.print(" " + name); }
            System.err.println(": " + e);
            System.exit(66);    // EX_NOINPUT
            throw new IllegalStateException(); // unreachable
        }
    }

    public static void main(String[] args) throws IOException {
        int i = 0;
        boolean detail = false;
        while (i < args.length && args[i].charAt(0) == '-') {
            if ("-d".equals(args[i])) {
                detail = true;
            } else if ("-h".equals(args[i]) || "--help".equals(args[i])) {
                System.out.println(HELP_MSG);
                System.exit(0);
            } else {
                System.err.println("Unrecognized option: " + args[i]);
                System.exit(64);        // EX_USAGE
            }
            i++;
        }
        if (args.length-i != 2) {
            System.out.println(HELP_MSG);
            System.exit(64);    // EX_USAGE
        }
        Question[][] corr = CorrectAnswer.parse(openFile(args, i++, "correct answer"));
        BufferedReader ansFile = openFile(args, i++, "given answers");
        PrintWriter w = new PrintWriter(new OutputStreamWriter(System.out, "UTF-8"));
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