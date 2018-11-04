package spacegraph;

import org.teavm.jso.dom.html.HTMLBodyElement;
import org.teavm.jso.dom.html.HTMLCanvasElement;
import org.teavm.jso.dom.html.HTMLDocument;
import org.teavm.jso.webgl.WebGLRenderingContext;
import spacegraph.web.util.WebSocket;

public class WebClient {

    protected final HTMLDocument doc;
    protected final HTMLBodyElement body;

    protected final WebSocket socket;
    private final WebGLRenderingContext gl;
    private final HTMLCanvasElement canvas;

    protected WebClient() {

        doc = HTMLDocument.current();
        body = doc.getBody();

        socket = WebSocket.connect("/");


        socket.setOnData((msg) -> {
//                MsgPack.decodeArray(msg, (x) -> {
//                    JSObject klass = JS.get(x,0);
        });

        socket.setOnOpen(() -> {
            //socket.send("..");
        });

        canvas = (HTMLCanvasElement) doc.createElement("canvas")
                .withAttr("id", "c")
                .withAttr("style", "width: 100%; height: 100%")
                ;
        doc.appendChild(canvas);

        gl = (WebGLRenderingContext) canvas.getContext("webgl");

        initUI();
    }

    /**
     * https://webglfundamentals.org/webgl/lessons/webgl-fundamentals.html
     * https://webglfundamentals.org/webgl/lessons/webgl-2d-translation.html
     * */
    private void initUI() {

        if (gl==null) {
            // no webgl
            doc.removeChild(canvas);
            doc.appendChild(doc.createTextNode("WebGL unavailable"));
            return;
        }

        render(canvas, gl);
    }

    private void render(HTMLCanvasElement canvas, WebGLRenderingContext gl) {
        gl.viewport(0, 0, canvas.getWidth(), canvas.getHeight());
        gl.clearColor(0, 0, 0, 0);
        gl.clear(gl.COLOR_BUFFER_BIT);
        gl.
    }


    public static void main(String[] args) {
        new WebClient();
    }

}
