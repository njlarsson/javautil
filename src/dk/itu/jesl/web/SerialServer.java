package dk.itu.jesl.web;

import java.io.*;
import java.net.*;
import java.util.regex.*;

/**
 * Simple subset of an HTTP server. Services GET requests in sequence.
 *
 * @author Jesper Larsson, IT University of Copenhagen.
 */
public class SerialServer {
    /**
     * Interface for a service that processes queries.
     */
    public interface Service {
        /**
         * Processes a query, returning result as a string.
         */
        String processQuery(String query);
    }

    /**
     * Special exception for graceful HTTP response.
     */
    public static class HttpStatusException extends RuntimeException {
        private final String statusLine;
        
        /**
         * Constructor.
         * @param statusLine An HTTP status line.
         * @param A message to go into the body of the error page.
         */
        public HttpStatusException(String statusLine, String msg) {
            super(msg);
            this.statusLine = statusLine;
        }

        public String getStatusLine() { return statusLine; }
    }

    // The following are the HTTP/1.0 standard status lines, intended to be
    // used as the first constructor parameter for HttpStatusException.

    public final static String STATUS_OK = "HTTP/1.0 200 OK";
    public final static String STATUS_CREATED = "HTTP/1.0 201 Created";
    public final static String STATUS_ACCEPTED = "HTTP/1.0 202 Accepted";
    public final static String STATUS_NO_CONTENT = "HTTP/1.0 204 No Content";
    public final static String STATUS_MOVED_PERMANENTLY = "HTTP/1.0 301 Moved Permanently";
    public final static String STATUS_MOVED_TEMPORARILY = "HTTP/1.0 302 Moved Temporarily";
    public final static String STATUS_NOT_MODIFIED = "HTTP/1.0 304 Not Modified";
    public final static String STATUS_BAD_REQUEST = "HTTP/1.0 400 Bad Request";
    public final static String STATUS_UNAUTHORIZED = "HTTP/1.0 401 Unauthorized";
    public final static String STATUS_FORBIDDEN = "HTTP/1.0 403 Forbidden";
    public final static String STATUS_NOT_FOUND = "HTTP/1.0 404 Not Found";
    public final static String STATUS_INTERNAL_SERVER_ERROR = "HTTP/1.0 500 Internal Server Error";
    public final static String STATUS_NOT_IMPLEMENTED = "HTTP/1.0 501 Not Implemented";
    public final static String STATUS_BAD_GATEWAY = "HTTP/1.0 502 Bad Gateway";
    public final static String STATUS_SERVICE_UNAVAILABLE = "HTTP/1.0 503 Service Unavailable";

    private final int port;
    private final Service service;
    private final String indexHtml;
    private final Pattern queryUrl;
    private final Pattern httpRequest = Pattern.compile("([A-Z]+)\\s+/(\\S*)\\s+HTTP/\\S+\\s*");

    /**
     * Constructor.
     * @param port The port to accept HTTP requests on.
     * @param service The service to process queries.
     * @param serviceName The service name to appear in request URLs.
     */
    public SerialServer(int port, Service service, String serviceName) {
        this.port = port;
        this.service = service;
	indexHtml = "<html xmlns=\"http://www.w3.org/1999/xhtml\">" +
	    "<head><meta http-equiv=\"Content-type\" content=\"text/html;charset=UTF-8\" />" +
	    "<title>Request Form</title></head><body>" +
	    "<form method=\"get\" action=\"" + serviceName + "\" accept-charset=\"UTF-8\">" +
	    "<fieldset><label>Enter query</label><br /><input type=\"text\" name=\"q\" /><br /></fieldset>" +
	    "</form></body></html>";
	queryUrl = Pattern.compile(serviceName + "\\?q=(.*)");
    }
    
    /**
     * Runs the server forever. Queries received via HTTP are passed
     * on to the service, and the serch result returned to the client.
     */
    public void run() throws IOException {
        ServerSocket serv = new ServerSocket(port);
        System.out.println("*** Awaiting requests at: http://" + InetAddress.getLocalHost().getHostName() + ":" + port + "/");
        System.out.println("(Terminate server by pressing \"ctrl C\")");
        while (true) {
            Socket sock = serv.accept();
            HtmlWriter w = new HtmlWriter(sock.getOutputStream());
            String response;
            try {
                response = processRequest(new BufferedReader(new InputStreamReader(sock.getInputStream(), "UTF-8")));
            } catch (HttpStatusException e) {
                w.open(e.getStatusLine(), "SEServer error");
                w.write("<p>"); w.quote(e.getMessage()); w.write("</p>");
                w.close();
                continue;
            } catch (Exception e) {
                w.open(STATUS_INTERNAL_SERVER_ERROR, "SEServer error");
                w.write("<p>"); w.quote(e.toString()); w.write("</p>");
                w.writeStackTrace(e);
                w.close();
                continue;
            }
            w.open(STATUS_OK, "SEServer");
            w.write(response);
            w.close();
        }
    }
    
    private String processRequest(BufferedReader r) throws IOException {
        String req = r.readLine();
        Matcher reqM = httpRequest.matcher(req);
        if (!reqM.matches()) {
            throw new HttpStatusException(STATUS_BAD_REQUEST, "Invalid request line: " + req);
        }
        String meth = reqM.group(1);
        String url = reqM.group(2);

        while (r.readLine().length() > 0)
            ; // ignore request headers

        if (!"GET".equals(meth)) {
            throw new HttpStatusException(STATUS_NOT_IMPLEMENTED, "Server cannot process " + meth);
        }
        if ("".equals(url) || "index.html".equals(url)) {
            return indexHtml;
        }
        Matcher urlM = queryUrl.matcher(url);
        if (!urlM.matches()) {
            throw new HttpStatusException(STATUS_NOT_FOUND, "Page not found on server: " + url);
        }
	return service.processQuery(translateUrl(new StringReader(urlM.group(1))));
    }

    // Translates any + to space, and decodes any %HH as multibyte UTF-8 sequences.
    private static String translateUrl(Reader r) throws IOException {
        StringBuilder s = new StringBuilder();
        while (true) {
            int c = r.read();
            if (c < 0)         return s.toString();
            else if (c == '%') appendMultibyte(r, s);
            else if (c == '+') s.append(' ');
            else               s.append((char) c);
        }
    }
    
    // Translates one multibyte %HH code. Reader should be positioned after the first % character.
    private static void appendMultibyte(Reader r, StringBuilder s) throws IOException {
        int b = decodeHex(r.read()) << 4 | decodeHex(r.read());
        int u, n;
        if (b < 0x80) { u = b; n = 1; }
        else if (b < 0xe0) { u = b & 0x1f; n = 2; }
        else if (b < 0xf0) { u = b & 0x0f; n = 3; }
        else if (b < 0xf8) { u = b & 0x07; n = 4; }
        else throw new HttpStatusException(STATUS_BAD_REQUEST, "Invalid UTF-8 octet in %HH code");
        for (int i = 1; i < n; ++i) {
            if (r.read() != '%') throw new HttpStatusException(STATUS_BAD_REQUEST, "Invalid UTF-8 sequence in %HH codes");
            b = decodeHex(r.read()) << 4 | decodeHex(r.read());
            if ((b & 0xc0) != 0x80) throw new HttpStatusException(STATUS_BAD_REQUEST, "Invalid UTF-8 octet in %HH code");
            u = u << 6 | b &0x3f;
        }
        if (u < 0x10000) s.append((char) u);
        else {
            s.append((char) (0xd800 + (u >>> 10)));
            s.append((char) (0xdc00 + (u & 0x3ff)));
        }
    }
    
    // Decodes a single hex digit.
    private static int decodeHex(int c) {
        if (c >= '0' && c <= '9')      return c - '0';
        else if (c >= 'a' && c <= 'f') return c - ('a' - 10);
        else if (c >= 'A' && c <= 'F') return c - ('A' - 10);
        else throw new HttpStatusException(STATUS_BAD_REQUEST, "Invalid %HH code");
    }

    // Writer specialized for HTML output.
    private static class HtmlWriter extends Writer {
        private final PrintWriter w;

        public HtmlWriter(OutputStream out) throws IOException {
            w = new PrintWriter(new OutputStreamWriter(out, "UTF-8"));
        }

        public void write(int c) throws IOException { w.write(c); } 
        public void write(char cbuf[], int off, int len) throws IOException { w.write(cbuf, off, len); }
        public void write(String str, int off, int len) throws IOException { w.write(str, off, len); }
        public void flush() throws IOException { w.flush(); }

        public void open(String statusLine, String title) {
            w.write(statusLine); w.write("\r\n");
            w.write("Content-Type: text/html; charset=UTF-8\r\n");
            w.write("\r\n");
            w.write("<html>\n<head>\n<meta http-equiv=\"Content-type\" content=\"text/html;charset=UTF-8\" />\n<title>");
            w.write(title);
            w.write("</title>\n</head>\n<body>\n");
        }

        public void close() throws IOException {
            w.write("\n</body>\n</html>\n");
            w.close();
        }

        public void quote(int c) throws IOException {
            if      (c == '&') w.write("&amp;");
            else if (c == '<') w.write("&lt;");
            else               w.write(c);
        }

        public void quote(CharSequence s) throws IOException {
            for (int i = 0, l = s.length(); i < l; i++) {
                quote(s.charAt(i));
            }
        }

        public void writeStackTrace(Throwable t) throws IOException {
            w.write("\n<pre>");
            t.printStackTrace(w);
            w.write("</pre>\n");
        }
    }
}
