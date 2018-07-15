package nars.web;

import org.teavm.jso.JSObject;
import org.teavm.jso.dom.html.HTMLBodyElement;
import org.teavm.jso.dom.html.HTMLDocument;
import org.teavm.jso.dom.html.HTMLElement;
import org.teavm.jso.json.JSON;

import java.util.ArrayDeque;

/**
 * see: https://github.com/automenta/spimedb/commit/93cc982f6d31cacb9c5d23e29f93d54ac5b9c1a8
 */
public class WebClientJS {


    private final HTMLDocument doc;
    private final HTMLBodyElement body;

    private final WebSocket socket;

    final static int bufferCapacity = 32;
    private final ArrayDeque<HTMLElement> buffer = new ArrayDeque<>(bufferCapacity);

    WebClientJS() {

        doc = HTMLDocument.current();
        body = doc.getBody();
        socket = WebSocket.connect("/");

        socket.setOnData((msg) -> {
            MsgPack.decodeArray(msg, (x) -> {
                push(x);
            });
        });

        socket.setOnOpen(() -> {
            socket.send("(x-->y).");
            socket.send("(x-->a).");
        });
    }


    protected void push(JSObject m) {


        String s = JSON.stringify(m); ///*JSON.stringify(m)*/

        HTMLElement e = doc.createElement("div").withChild(doc.createTextNode(s));

        if (buffer.size() + 1 >= bufferCapacity) {
            HTMLElement r = buffer.removeFirst();
            r.delete();
        }

        buffer.add(e);
        body.appendChild(e);

    }

    public static void main(String[] args) {
        new WebClientJS();
    }

}
