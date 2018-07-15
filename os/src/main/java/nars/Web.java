package nars;

import jcog.Texts;
import jcog.data.map.CustomConcurrentHashMap;
import jcog.event.Off;
import jcog.event.Ons;
import jcog.net.http.HttpConnection;
import jcog.net.http.HttpModel;
import jcog.net.http.HttpServer;
import jcog.net.http.WebSocketConnection;
import nars.index.concept.MaplikeConceptIndex;
import nars.index.concept.ProxyConceptIndex;
import nars.web.WebClientJS;
import org.java_websocket.WebSocket;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.framing.CloseFrame;
import org.java_websocket.handshake.ServerHandshake;
import org.teavm.tooling.RuntimeCopyOperation;
import org.teavm.tooling.TeaVMTool;
import org.teavm.tooling.TeaVMToolException;
import org.teavm.tooling.TeaVMToolLog;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.function.Consumer;
import java.util.function.Function;

import static jcog.data.map.CustomConcurrentHashMap.*;

public class Web implements HttpModel {

    static final int DEFAULT_PORT = 60606;


    private final NAR nar;
    /** adapter */
    private final MaplikeConceptIndex sharedIndex;

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
            case "/websocket.js":
                h.respond(nars.web.WebSocket.ReconnectingWebsocket_js);
                break;
            default:
                h.respond(
                        "<html>\n" +
                                "  <head>\n" +
                                "    <title></title>\n" +
                                "    <meta http-equiv=\"Content-Type\" content=\"text/html;charset=utf-8\">\n" +
                                "    <script type=\"text/javascript\" charset=\"utf-8\" src=\"websocket.js\"></script>\n" +
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

    final CustomConcurrentHashMap<String, NAR> reasoners = new CustomConcurrentHashMap<>(
            STRONG, EQUALS, WEAK, IDENTITY, 64) {
        @Override
        protected void reclaim(NAR n) {
            Web.this.remove(n);
        }
    };

    private void remove(NAR n) {
        stopping(n);
        n.reset();
    }

    @Override
    public boolean wssConnect(WebSocketConnection conn) {
        String url = conn.url().getPath();
        if (url.equals("/")) {
            conn.close(CloseFrame.REFUSE);
            return false;
        }

        NAR n = reasoners.computeIfAbsent(url, (Function<String,NAR>)this::reasoner);
        assert(n!=null);

        System.out.println("NAR " + System.identityHashCode(n) + " for " + url);

        conn.setAttachment(
                new NARConnection(n,
                        n.onTask(new WebSocketLogger(conn, n))
                        //...
                )
        );

        return true;
    }


    static class NARConnection extends Ons {
        public final NAR nar;

        public NARConnection(NAR n, Off... ons) {
            this.nar = n;
            addingAll(ons);
        }
    }

    @Override
    public void wssMessage(WebSocket ws, String message) {
        try {
            ((NARConnection)ws.getAttachment()).nar.input(message);
        } catch (Narsese.NarseseException e) {
            ws.send(e.toString()); //e.printStackTrace();
        }
    }

    @Override
    public void wssClose(WebSocket ws, int code, String reason, boolean remote) {
        Ons o = ws.getAttachment();
        if (o!=null) {
            ws.setAttachment(null);
            o.off();
        }
    }

    protected void stopping(NAR n) {

    }

    protected void starting(NAR n) {
//        try {
//            n.input("a:b.");
//            n.input("a:c.");
//            n.input("b:d.");
//            n.input("b:c.");
//            n.input("c:d.");
//        } catch (Narsese.NarseseException e) {
//            e.printStackTrace();
//        }
    }

    private NAR reasoner(String path) {
        NAR n = new NARS().withNAL(1, 8).index(sharedIndex).get();

        assert (path.charAt(0) == '/');
        path = path.substring(1);
        n.named(path);

        n.log(); //temporary

        int initialFPS = 5;
        n.startFPS(initialFPS);

        starting(n);

        return n;
    }

    public Web() {
        this.nar = NARchy.core();
        this.nar.loop.setFPS(10);
        this.sharedIndex = new ProxyConceptIndex(nar.concepts);
    }

    public static void clientGenerate() {
        try {
            TeaVMTool tea = new TeaVMTool();
//            try {
//                org.apache.commons.io.FileUtils.deleteDirectory(new File("/tmp/teacache"));
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
            try {
                org.apache.commons.io.FileUtils.deleteDirectory(new File("/tmp/tea"));
            } catch (IOException e) {
                e.printStackTrace();
            }

            tea.setMainClass(WebClientJS.class.getName());
            tea.setCacheDirectory(new File("/tmp/teacache"));
            tea.setIncremental(true);
            //tea.setDebugInformationGenerated(true);

            tea.setTargetDirectory(new File("/tmp/tea"));
            tea.setLog(new TeaVMToolLog() {
                @Override
                public void info(String text) {
                    System.out.println(text);
                }

                @Override
                public void debug(String text) {
                    System.out.println(text);
                }

                @Override
                public void warning(String text) {
                    System.err.println(text);
                }

                @Override
                public void error(String text) {
                    System.err.println(text);
                }

                @Override
                public void info(String text, Throwable e) {
                    System.out.println(text);
                }

                @Override
                public void debug(String text, Throwable e) {
                    System.out.println(text);
                }

                @Override
                public void warning(String text, Throwable e) {
                    System.err.println(text + "\n" + e);
                }

                @Override
                public void error(String text, Throwable e) {
                    System.err.println(text + "\n" + e);
                }
            });



            tea.setRuntime(RuntimeCopyOperation.SEPARATE);


            tea.generate();
            System.out.println("TeaVM generate " + tea.getGeneratedFiles());
        } catch (TeaVMToolException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws IOException {

        clientGenerate();

        int port;
        if (args.length > 0) {
            port = Texts.i(args[0]);
        } else {
            port = DEFAULT_PORT;
        }


        jcog.net.http.HttpServer h = new HttpServer("0.0.0.0", port, new Web());
        h.setFPS(20f);


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
        volatile WebSocket w;

        public WebSocketLogger(WebSocket ws, NAR n) {
            this.n = n;
            this.w = ws;
        }

        @Override
        public void accept(Task t) {
            //if (w != null && w.isOpen()) {

                try {
                    w.send(t.toString(true).toString());
                } catch (Exception e) {
                    e.printStackTrace();
                    w = null;
                    w.close();
                }
            //}
        }
    }
}
