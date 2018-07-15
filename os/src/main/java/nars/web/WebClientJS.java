package nars.web;

import org.teavm.jso.dom.html.HTMLBodyElement;
import org.teavm.jso.dom.html.HTMLDocument;
import org.teavm.jso.dom.html.HTMLElement;
import org.teavm.jso.json.JSON;

import java.util.ArrayDeque;

/** see: https://github.com/automenta/spimedb/commit/93cc982f6d31cacb9c5d23e29f93d54ac5b9c1a8 */
public class WebClientJS {


    private final HTMLDocument doc = HTMLDocument.current();
    private final WebSocket socket;

    final static int bufferCapacity = 8;
    private final ArrayDeque<HTMLElement> buffer = new ArrayDeque<>(bufferCapacity);

    WebClientJS() {

        HTMLBodyElement body = doc.getBody();

        body.appendChild(doc.createTextNode("start"));

        socket = WebSocket.newSocket("me");
        socket.setOnData((msg)->{

            HTMLElement e = doc.createElement("div").withChild(doc.createTextNode(JSON.stringify(msg)));

            if (buffer.size()+1 >= bufferCapacity) {
                HTMLElement r = buffer.removeFirst();
                r.delete();
            }

            buffer.add(e);
            body.appendChild(e);
        });

        socket.setOnOpen(()->{
            socket.send("(x-->y).");
            socket.send("(x-->a).");
        });
    }


    public static void main(String[] args) {
        new WebClientJS();
    }

}
