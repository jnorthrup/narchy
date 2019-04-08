package spacegraph;

import jcog.event.Offs;
import jcog.net.http.HttpConnection;
import jcog.net.http.HttpModel;
import org.java_websocket.WebSocket;
import spacegraph.web.util.MsgPack;

import java.io.File;
import java.net.URI;

public abstract class WebServer implements HttpModel {

    @Override
    public void response(HttpConnection h) {

        URI url = h.url();

        String path = url.getPath();
        switch (path) {
            case "/teavm/runtime.js":
                h.respond(new File("/tmp/tea/runtime.js")); //TODO managed temporary directory
                break;
            case "/teavm/classes.js":
                h.respond(new File("/tmp/tea/classes.js")); //TODO managed temporary directory
                break;
            case "/websocket.js":
                h.respond(spacegraph.web.util.WebSocket.websocket_js);
                break;
            case "/msgpack.js":
                h.respond(MsgPack.msgpack_js);
                break;
            default:
                h.respond(
                        "<html>\n" +
                                "  <head>\n" +
                                "    <title></title>\n" +
                                "    <meta http-equiv=\"Content-Type\" content=\"text/html;charset=utf-8\">\n" +
                                "    <script type=\"text/javascript\" charset=\"utf-8\" src=\"websocket.js\"></script>\n" +
                                "    <script type=\"text/javascript\" charset=\"utf-8\" src=\"msgpack.js\"></script>\n" +
                                "    <script type=\"text/javascript\" charset=\"utf-8\" src=\"teavm/runtime.js\"></script>\n" +
                                "    <script type=\"text/javascript\" charset=\"utf-8\" src=\"teavm/classes.js\"></script>\n" +
                                "  </head>\n" +
                                "  <body onload=\"main()\">\n" +
                                "  </body>\n" +
                                "</html>");
                break;

        }


    }



    @Override
    public void wssClose(WebSocket ws, int code, String reason, boolean remote) {
        Offs o = ws.getAttachment();
        if (o != null) {
            ws.setAttachment(null);
            o.off();
        }
    }
}
