package nars;

import jcog.Texts;
import jcog.data.map.CustomConcurrentHashMap;
import jcog.net.http.HttpConnection;
import jcog.net.http.HttpModel;
import jcog.net.http.HttpServer;
import org.eclipse.collections.api.tuple.Pair;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;

import java.io.IOException;
import java.net.InetSocketAddress;

public class Web implements HttpModel {

    static final int DEFAULT_PORT = 60606;

    final CustomConcurrentHashMap<Pair<InetSocketAddress, String>,NAR> nar = new CustomConcurrentHashMap<>(
            CustomConcurrentHashMap.STRONG, CustomConcurrentHashMap.EQUALS,
            CustomConcurrentHashMap.WEAK, CustomConcurrentHashMap.IDENTITY,
            16
    );

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

    @Override
    public void wssOpen(WebSocket ws, ClientHandshake handshake) {
        System.out.println(ws.getAttachment() + " " + ws.getAttachment().getClass());
    }

    public static void main(String[] args) throws IOException {
        int port;
        if (args.length > 0) {
            port = Texts.i(args[0]);
        } else {
            port = DEFAULT_PORT;
        }

        NAR n = NARchy.core();
        jcog.net.http.HttpServer h = new HttpServer("0.0.0.0", port, new Web());
        h.runFPS(10f);
    }
}
