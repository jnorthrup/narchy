package nars;

import jcog.Texts;
import jcog.Util;
import jcog.data.map.CustomConcurrentHashMap;
import jcog.net.http.HttpConnection;
import jcog.net.http.HttpModel;
import jcog.net.http.HttpServer;
import nars.index.concept.MaplikeConceptIndex;
import nars.index.concept.ProxyConceptIndex;
import org.java_websocket.WebSocket;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.framing.CloseFrame;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.handshake.ServerHandshake;

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
        h.respond(
                "<html>\n" +
                "    <head>\n" +
                "      <link rel=\"stylesheet\" href=\"https://cdnjs.cloudflare.com/ajax/libs/xterm/2.9.2/xterm.css\" />\n" +
                "      <script src=\"https://cdnjs.cloudflare.com/ajax/libs/xterm/2.9.2/xterm.js\"></script>\n" +
                "    </head>\n" +
                "    <body>\n" +
                "      <div id=\"terminal\"></div>\n" +
                "      <script>\n" +
                "      \tvar term = new Terminal();\n" +
                "        term.open(document.getElementById('#terminal'));\n" +
                "        term.write('Hello from \\033[1;3;31mxterm.js\\033[0m $ ');\n" +
                    "var socket = new WebSocket('ws://localhost:60606');\n" +
                    "term.terminadoAttach(socket);  // Attach the above socket to `term` using the Terminado protocol\n" +
                "      </script>\n" +
                "    </body>\n" +
                "  </html>");
    }

    final CustomConcurrentHashMap<WebSocket,NAR> reasoners = new CustomConcurrentHashMap<>(
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
//        ((WebSocketSelector.ServerWebSocketImpl)key.attachment()).
        return true;
    }

    @Override
    public void wssOpen(WebSocket ws, ClientHandshake handshake) {

        if(ws.getResourceDescriptor().equals("/")) {
            ws.close(CloseFrame.REFUSE);
            return;
        }

        NAR n = reasoners.computeIfAbsent(ws, (Function<WebSocket,NAR>)this::reasoner);
        ws.setAttachment(n);
        //System.out.println(ws.getAttachment() + " " + ws.getAttachment().getClass());
    }

    private NAR reasoner(WebSocket ws) {
        NAR n = new NARS().withNAL(1,8).index(sharedIndexAdapter).get();

        String path = ws.getResourceDescriptor();
        assert(path.charAt(0)=='/');
        path = path.substring(1);
        n.named(path);

        int initialFPS = 5;
        n.startFPS(initialFPS);
        try {
            n.input("a:b.");
            n.input("b:c.");
        } catch (Narsese.NarseseException e) {
            e.printStackTrace();
        }
        n.onTask(new WebSocketLogger(ws, n, initialFPS));
//        n.log(new Appendable() {
//
//            @Override
//            public Appendable append(CharSequence charSequence) throws IOException {
//                ws.send(charSequence.toString());
//                return this;
//            }
//
//            @Override
//            public Appendable append(CharSequence charSequence, int i, int i1) throws IOException {
//                append(charSequence.subSequence(i, i1));
//                return null;
//            }
//
//            @Override
//            public Appendable append(char c) throws IOException {
//                append(String.valueOf(c));
//                return null;
//            }
//        });
        return n;
    }

    public Web() {
        this.nar = NARchy.core();
        this.sharedIndexAdapter = new ProxyConceptIndex(nar.concepts);
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        int port;
        if (args.length > 0) {
            port = Texts.i(args[0]);
        } else {
            port = DEFAULT_PORT;
        }



        jcog.net.http.HttpServer h = new HttpServer("0.0.0.0", port, new Web());
        h.runFPS(10f);


        Util.sleep(100);

        WebClient c1 = new WebClient(URI.create("ws://localhost:60606/a"));
        WebClient c2 = new WebClient(URI.create("ws://localhost:60606/b"));

        Util.sleep(500);
        c1.closeBlocking();
        c2.closeBlocking();
    }

    public static class WebClient extends WebSocketClient {

        public WebClient(URI serverUri) {
            super(serverUri);
            try {
                connectBlocking();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            //connect();
        }

        @Override
        public void onOpen(ServerHandshake handshakedata) {
            //System.out.println(handshakedata);
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
            if (w!=null && w.isOpen()) {
                if (!n.loop.isRunning())
                    n.startFPS(initialFPS);

                try {
                    w.send(t.toString(true).toString());
                } catch (Exception e) {
                    w = null; //remove reference to websocket allowing it to be reclaimed
                    n.stop();
                    w.close();
                }
            } else {
                //if it was already running
                if (n.loop.isRunning()) {
                    w = null;  //remove reference to websocket allowing it to be reclaimed
                    n.stop();
                }
            }
        }
    }
}
