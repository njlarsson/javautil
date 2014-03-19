package dk.itu.jesl.web;

import java.io.IOException;

public class TalkbackService implements SerialServer.Service {
    public String processQuery(String query) {
	return "<p>You said: '" + query + "'</p>";
    }

    public static void main(String[] args) throws IOException {
	TalkbackService service = new TalkbackService();
        SerialServer server = new SerialServer(8888, service, "talkback");
        server.run();
    }
}