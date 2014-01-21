package dk.itu.jesl.multic;

import java.io.*;
import java.util.*;

public class ReadCSV {
    public static class CSVFormatException extends IOException {
        CSVFormatException(String msg) { super(msg); }
    }

    public static String[][] readFile(String fileName) throws IOException {
        ArrayList<String[]> lines = new ArrayList<String[]>();
        Reader r = new InputStreamReader(new FileInputStream(fileName), "UTF-8");
        while (true) {
            String[] line = readLine(r);
            if (line == null) {
                String[][] a = lines.toArray(new String[lines.size()][]);
                for (int i = 1; i < a.length; i++) {
                    if (a[i-1].length != a[i].length) {
                        throw new CSVFormatException("Line length mismatch on line " + i + ": " + a[i-1].length + ", " + a[i].length);
                    }
                }
                return a;
            }
            lines.add(line);
        }
    }
            
    private static String[] readLine(Reader r) throws IOException {
        ArrayList<String> line = new ArrayList<String>();
        while (true) {
            String field = readField(r);
            if (field == null) {
                if (line.size() == 0) {
                    return null;
                } else {
                    field = "-";
                }
            }
            line.add(field.trim());
            if (c == '\n' || c < 0) {
                return line.toArray(new String[line.size()]);
            }
        }
    }

    private static int c = ',';
            
    private static String readField(Reader r) throws IOException {
        StringBuilder b = new StringBuilder();
        int stop = ',';
        c = r.read();
        if (c < 0) {
            return null;
        }
        if (c == '"') {
            stop = '"';
            c = r.read();
        }
        while (true) {
            if (c < 0 || c == stop || c == '\n') {
                break;
            }
            b.append((char) c);
            c = r.read();
        }
        if (c == '"') {
            c = r.read();
        }
        return b.length() == 0 ? "-" : b.toString();
    }

    public static void main(String[] args) throws IOException {
        String[][] f = readFile(args[0]);
        System.out.println(f.length + " lines of " + f[0].length + " fields");
        for (int i = 0; i < f.length; i++) {
            for (int j = 0; j < f[i].length; j++) {
                System.out.print("\"" + f[i][j] + "\", ");
            }
            System.out.println();
        }
    }
}