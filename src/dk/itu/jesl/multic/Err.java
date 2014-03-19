package dk.itu.jesl.multic;

public class Err {
    public static class FormatException extends RuntimeException {
	private int page = 0;
	private String problem = null, spec = null, sect = null;

	FormatException(String spec) { this.spec = spec; }
	FormatException(Throwable cause) { super(cause); }

	FormatException setPage(int page) { this.page = page; return this; }
	FormatException setProblem(String problem) { this.problem = problem; return this; }
	FormatException setSection(String sect) { this.sect = sect; return this; }

	public String getMessage() {
	    StringBuilder b = new StringBuilder();
	    b.append("Format problem");
	    if (sect != null) { b.append(" in ").append(sect); }
	    if (page != 0) { b.append(" on page ").append(page); }
	    if (problem != null) { b.append(" in problem ").append(problem); }
	    if (spec != null) { b.append(" (").append(spec).append(")"); }
	    return b.toString();
	}
    }

    static void conf(boolean cond, String spec) { if (! cond) throw new FormatException(spec); }
    static void conf(boolean cond) { conf(cond, null); }
}