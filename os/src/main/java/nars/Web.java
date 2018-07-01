package nars;

import jcog.Texts;
import jcog.data.map.CustomConcurrentHashMap;
import jcog.net.http.HttpConnection;
import jcog.net.http.HttpModel;
import jcog.net.http.HttpServer;
import nars.index.concept.MaplikeConceptIndex;
import nars.index.concept.ProxyConceptIndex;
import nars.web.WebClientJS;
import org.java_websocket.WebSocket;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.framing.CloseFrame;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.handshake.ServerHandshake;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.channels.SelectionKey;
import java.util.function.Consumer;
import java.util.function.Function;

import static jcog.data.map.CustomConcurrentHashMap.*;

public class Web implements HttpModel {

    static final int DEFAULT_PORT = 60606;


    private final NAR nar;
    private final MaplikeConceptIndex sharedIndexAdapter;

    @Override
    public void response(HttpConnection h) {

        URI url = h.url();


            String path = url.getPath();
            switch (path) {
                case "/teavm/runtime.js":
                    h.respond(new File("/tmp/tea/runtime.js"));
                    break;
                case "/teavm/classes.js":
                    h.respond(new File("/tmp/tea/classes.js"));
                    break;
                default:
                    h.respond(
                            "<html>\n" +
                                    "  <head>\n" +
                                    "    <title></title>\n" +
                                    "    <meta http-equiv=\"Content-Type\" content=\"text/html;charset=utf-8\">\n" +
                                    "    <script type=\"text/javascript\" charset=\"utf-8\" src=\"teavm/runtime.js\"></script>\n" +
                                    "    <script type=\"text/javascript\" charset=\"utf-8\" src=\"teavm/classes.js\"></script>\n" +
                                    "  </head>\n" +
                                    "  <body onload=\"main()\">\n" +
//                        "    <h1>Hello web application</h1>\n" +
//                        "    <button id=\"hello-button\">Hello, server!</button>\n" +
//                        "    <div id=\"response-panel\">\n" +
//                        "    </div>\n" +
//                        "    <div style=\"display:none\" id=\"thinking-panel\"><i>Server is thinking...</i></div>\n" +
                                    "  </body>\n" +
                                    "</html>");
                    break;

            }


    }

    final CustomConcurrentHashMap<WebSocket, NAR> reasoners = new CustomConcurrentHashMap<>(
            WEAK, IDENTITY, STRONG, IDENTITY, 64
    ) {
        @Override
        protected void reclaim(NAR value) {
            value.stop();
            value.reset();
        }
    };

    @Override
    public boolean wssConnect(SelectionKey key) {

        return true;
    }

    @Override
    public void wssOpen(WebSocket ws, ClientHandshake handshake) {

        if (ws.getResourceDescriptor().equals("/")) {
            ws.close(CloseFrame.REFUSE);
            return;
        }

        NAR n = reasoners.computeIfAbsent(ws, (Function<WebSocket, NAR>) this::reasoner);
        ws.setAttachment(n);

    }

    private NAR reasoner(WebSocket ws) {
        NAR n = new NARS().withNAL(1, 8).index(sharedIndexAdapter).get();

        String path = ws.getResourceDescriptor();
        assert (path.charAt(0) == '/');
        path = path.substring(1);
        n.named(path);

        int initialFPS = 5;
        n.startFPS(initialFPS);
//        try {
//            n.input("a:b.");
//            n.input("b:c.");
//        } catch (Narsese.NarseseException e) {
//            e.printStackTrace();
//        }
        n.onTask(new WebSocketLogger(ws, n, initialFPS));


        return n;
    }

    public Web() {
        this.nar = NARchy.core();
        this.sharedIndexAdapter = new ProxyConceptIndex(nar.concepts);
    }

    public static void main(String[] args) throws IOException {

        WebClientJS.generate();

        int port;
        if (args.length > 0) {
            port = Texts.i(args[0]);
        } else {
            port = DEFAULT_PORT;
        }


        jcog.net.http.HttpServer h = new HttpServer("0.0.0.0", port, new Web());
        h.runFPS(10f);


//        Util.sleep(100);
//
//        WebClient c1 = new WebClient(URI.create("ws://localhost:60606/a"));
//        WebClient c2 = new WebClient(URI.create("ws://localhost:60606/b"));
//
//        Util.sleep(500);
//        c1.closeBlocking();
//        c2.closeBlocking();
    }

    public static class WebClient extends WebSocketClient {

        public WebClient(URI serverUri) {
            super(serverUri);
            try {
                connectBlocking();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }

        }

        @Override
        public void onOpen(ServerHandshake handshakedata) {

        }

        @Override
        public void onMessage(String message) {
            System.out.println(message);
        }

        @Override
        public void onClose(int code, String reason, boolean remote) {

        }

        @Override
        public void onError(Exception ex) {

        }
    }

    private static class WebSocketLogger implements Consumer<Task> {

        private final NAR n;
        private final int initialFPS;
        volatile WebSocket w;

        public WebSocketLogger(WebSocket ws, NAR n, int initialFPS) {
            this.n = n;
            this.initialFPS = initialFPS;
            w = ws;
        }

        @Override
        public void accept(Task t) {
            if (w != null && w.isOpen()) {
                if (!n.loop.isRunning())
                    n.startFPS(initialFPS);

                try {
                    w.send(t.toString(true).toString());
                } catch (Exception e) {
                    w = null;
                    n.stop();
                    w.close();
                }
            } else {

                if (n.loop.isRunning()) {
                    w = null;
                    n.stop();
                }
            }
        }
    }
}
