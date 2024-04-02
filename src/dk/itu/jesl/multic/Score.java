package dk.itu.jesl.multic;

import java.io.*;

public class Score {
    private static String HELP_MSG =
        "Arguments: [options] correct_answers_file given_answers_file\n" +
        "Options:\n" +
        "   -d:           Detailed information\n" +
        "   -p:           Individual points (can't combine with -d)\n" +
        "   -A:           Choices are A, B, C (not 1, 2, 3)\n" +
        "   -F:           Read answers in student-submitted format from individual files\n" +
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

    private static void report(Student stud, PrintWriter w, boolean detail, boolean points) throws IOException {
        if      (detail) { stud.reportDetail(w); }
        else if (points) { stud.reportPoints(w); }
        else             { stud.reportScore(w); }
    }

    public static void main(String[] args) throws IOException {
        int i = 0;
        boolean detail = false;
        boolean files = false;
        boolean points = false;
        int multLetterBase = '0';
        while (i < args.length && args[i].charAt(0) == '-') {
            if ("-d".equals(args[i])) {
                detail = true;
            } else if ("-h".equals(args[i]) || "--help".equals(args[i])) {
                System.out.println(HELP_MSG);
                System.exit(0);
            } else if ("-A".equals(args[i])) {
                multLetterBase = 'A'-1;
            } else if ("-F".equals(args[i])) {
                files = true;
            } else if ("-p".equals(args[i])) {
                points = true;
            } else {
                System.err.println("Unrecognized option: " + args[i]);
                System.exit(64);        // EX_USAGE
            }
            i++;
        }
        if (args.length-i < 2 || !files && args.length-i > 2) {
            System.out.println(HELP_MSG);
            System.exit(64);    // EX_USAGE
        }
        PrintWriter w = new PrintWriter(new OutputStreamWriter(System.out, "UTF-8"));
        if (files) {
            Question[][] corr = CorrectAnswer.parseProblems(openFile(args, i++, "correct answer"), multLetterBase);
            while (i < args.length) {
                BufferedReader studFile = openFile(args, i, "single student answers");
                Student stud = Student.parseF(studFile, corr, args[i]);
                studFile.close();
                i++;
                report(stud, w, detail, points);
            }
        } else {
            Question[][] corr = CorrectAnswer.parsePages(openFile(args, i++, "correct answer"), multLetterBase);
            BufferedReader ansFile = openFile(args, i++, "given answers");
            while (true) {
                Student stud = Student.parse(ansFile, corr);
                if (stud == null) { break; }
                report(stud, w, detail, points);
            }
        }
        w.flush();
        System.exit(0);
    }
}
