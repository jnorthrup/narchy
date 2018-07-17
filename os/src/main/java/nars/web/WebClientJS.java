package nars.web;

import nars.web.util.JS;
import nars.web.util.MsgPack;
import nars.web.util.WebSocket;
import org.teavm.jso.JSObject;
import org.teavm.jso.core.JSString;
import org.teavm.jso.dom.events.KeyboardEvent;
import org.teavm.jso.dom.html.HTMLBodyElement;
import org.teavm.jso.dom.html.HTMLDocument;
import org.teavm.jso.dom.html.HTMLElement;
import org.teavm.jso.dom.html.HTMLInputElement;

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

        HTMLInputElement input = (HTMLInputElement) doc.createElement("input");
        input.setAttribute("type", "text");
        input.setAttribute("placeholder", "?");
        input.addEventListener("keyup", (e)->{
            //if (e. instanceof KeyboardEvent) { //TODO getType()
                KeyboardEvent k = (KeyboardEvent)e;
                if ("Enter".equals(k.getCode())) {
                    socket.send(input.getValue());
                    input.setValue("");
                }
            //}
        });
        body.appendChild(input);

        socket.setOnData((msg) -> {
            MsgPack.decodeArray(msg, (x) -> {
                JSObject klass = JS.get(x,0);
                String k = ((JSString) klass).stringValue();
                switch (k) {
                    case ".":
                    case "!":
                    case "?":
                    case "@":
                        push(new TaskJson(
                                JS.getString(x, 2),
                                //new DiscreteTruth(1f, 0.5f),
                                JS.getString(x, 3),
                                k.charAt(0),
                                0, 0,
                                ((float)(JS.getInt(x, 1)))/Short.MAX_VALUE));
                        break;
                    default:
                        throw new UnsupportedOperationException();
                }

                //push(x);
            });
        });

        socket.setOnOpen(() -> {
            socket.send("(x-->y).");
            socket.send("(x-->a).");
        });
    }


    protected void push(TaskJson m) {


        String s = m.toString();

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
