package spacegraph;

import org.jetbrains.annotations.Nullable;
import org.teavm.jso.dom.html.HTMLBodyElement;
import org.teavm.jso.dom.html.HTMLCanvasElement;
import org.teavm.jso.dom.html.HTMLDocument;
import org.teavm.jso.dom.html.HTMLElement;
import org.teavm.jso.webgl.WebGLBuffer;
import org.teavm.jso.webgl.WebGLProgram;
import org.teavm.jso.webgl.WebGLRenderingContext;
import org.teavm.jso.webgl.WebGLShader;
import spacegraph.web.util.WebSocket;

import static org.teavm.jso.browser.Window.alert;

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
        body.appendChild(canvas);

        gl = (WebGLRenderingContext) canvas.getContext("webgl");

        initUI();
    }

    /**
     * https://webglfundamentals.org/webgl/lessons/webgl-fundamentals.html
     * https://webglfundamentals.org/webgl/lessons/webgl-2d-translation.html
     * https://github.com/WebGLSamples/WebGLSamples.github.io/blob/master/hello-webgl/hello-webgl.html
     * */
    private void initUI() {

        if (gl==null) {
//            if (!gl)
//                gl = canvas.getContext("experimental-webgl");
            // no webgl
            body.removeChild(canvas);
            body.appendChild(doc.createTextNode("WebGL unavailable"));
            return;
        }


        initShaders();
        initGeometry();

        render();

    }


    @Nullable WebGLShader getShader(WebGLRenderingContext gl, String id) {
        HTMLElement script = doc.getElementById(id);
        if (script==null) {
            return null;
        }

        WebGLShader shader;
        String scriptType = script.getAttribute("type");
        if (scriptType.equals("x-shader/x-fragment")) {
            shader = gl.createShader(gl.FRAGMENT_SHADER);
        } else if (scriptType.equals("x-shader/x-vertex")) {
            shader = gl.createShader(gl.VERTEX_SHADER);
        } else {
            return null;
        }

        gl.shaderSource(shader, script.getInnerHTML());
        gl.compileShader(shader);

        if (gl.getShaderParameter(shader, gl.COMPILE_STATUS)==null) {
            alert(gl.getShaderInfoLog(shader));
            return null;
        }

        return shader;
    }

    private void render() {
        gl.viewport(0, 0, canvas.getWidth(), canvas.getHeight());
        gl.disable(gl.DEPTH_TEST);
        gl.clearColor(0, 0, 0, 0);
        gl.clear(gl.COLOR_BUFFER_BIT);

        gl.bindBuffer(gl.ARRAY_BUFFER, buffer);
        // There are 7 floating-point values per vertex
        int stride = 7 * 4;/*Float32Array.BYTES_PER_ELEMENT*/
        // Set up position stream
        gl.vertexAttribPointer(positionAttr, 3, gl.FLOAT, false, stride, 0);
        // Set up color stream
        gl.vertexAttribPointer(colorAttr, 4, gl.FLOAT, false, stride, 3 * /*Float32Array.BYTES_PER_ELEMENT*/ 4);
        gl.drawArrays(gl.TRIANGLES, 0, 3);
    }

    WebGLProgram program;
    private int positionAttr, colorAttr;

    void initShaders() {


        body.appendChild(doc.createElement("script").withAttr("id", "shader-fs")
                .withAttr("type", "x-shader/x-fragment").withText(

                        "    precision mediump float;\n" +
                                "    varying vec4 vColor;\n" +
                                "    void main(void) {\n" +
                                "        gl_FragColor = vColor;\n" +
                                "    }\n" ));
        body.appendChild(doc.createElement("script").withAttr("id", "shader-vs")
                .withAttr("type", "x-shader/x-vertex").withText(
                        "                attribute vec3 positionAttr;\n" +
                                "        attribute vec4 colorAttr;\n" +
                                "\n" +
                                "        varying vec4 vColor;\n" +
                                "\n" +
                                "        void main(void) {\n" +
                                "                gl_Position = vec4(positionAttr, 1.0);\n" +
                                "        vColor = colorAttr;\n" +
                                "    }"));


        WebGLShader vertexShader = getShader(gl, "shader-vs");
        WebGLShader fragmentShader = getShader(gl, "shader-fs");

        program = gl.createProgram();
        gl.attachShader(program, vertexShader);
        gl.attachShader(program, fragmentShader);
        gl.linkProgram(program);

        if (gl.getProgramParameter(program, gl.LINK_STATUS)==null) {
            alert("Could not initialise shaders");
        }

        gl.useProgram(program);

        gl.enableVertexAttribArray(positionAttr = gl.getAttribLocation(program, "positionAttr"));
        gl.enableVertexAttribArray(colorAttr = gl.getAttribLocation(program, "colorAttr"));
    }

    WebGLBuffer buffer;

    void initGeometry() {
        buffer = gl.createBuffer();
        gl.bindBuffer(gl.ARRAY_BUFFER, buffer);
        // Interleave vertex positions and colors
//        float[] vertexData = new float[]{
//                // X    Y     Z     R     G     B     A
//                0.0f, 0.8f, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f,
//                // X    Y     Z     R     G     B     A
//                -0.8f, -0.8f, 0.0f, 0.0f, 1.0f, 0.0f, 1.0f,
//                // X    Y     Z     R     G     B     A
//                0.8f, -0.8f, 0.0f, 0.0f, 0.0f, 1.0f, 1.0f
//        };
//        Float32Array a = Float32Array.create(vertexData.length);
//        a.set(vertexData);
//        gl.bufferData(gl.ARRAY_BUFFER, a, gl.STATIC_DRAW);
    }




    public static void main(String[] args) {
        new WebClient();
    }

}
