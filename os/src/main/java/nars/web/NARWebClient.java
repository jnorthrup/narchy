package nars.web;

import org.teavm.jso.dom.events.KeyboardEvent;
import org.teavm.jso.dom.html.HTMLElement;
import org.teavm.jso.dom.html.HTMLInputElement;
import org.teavm.runtime.Console;
import spacegraph.WebClient;

import java.util.ArrayDeque;

/**
 * see: https://github.com/automenta/spimedb/commit/93cc982f6d31cacb9c5d23e29f93d54ac5b9c1a8
 */
public class NARWebClient extends WebClient {



    final static int bufferCapacity = 32;
    private final ArrayDeque<HTMLElement> buffer = new ArrayDeque<>(bufferCapacity);

    public NARWebClient() {

        super();

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

        socket.onMessage((msg) -> {
            Console.printString(msg.getDataAsString());
//            MsgPack.decodeArray(msg, (x) -> {
//                JSObject klass = JS.get(x,0);
//                String k = ((JSString) klass).stringValue();
//                switch (k) {
//                    case ".":
//                    case "!":
//                    case "?":
//                    case "@":
//                        push(new TaskJson(
//                                JS.getString(x, 2),
//                                //new DiscreteTruth(1f, 0.5f),
//                                JS.getString(x, 3),
//                                k.charAt(0),
//                                0, 0,
//                                ((float)(JS.getInt(x, 1)))/Short.MAX_VALUE));
//                        break;
//                    default:
//                        throw new UnsupportedOperationException();
//                }
//
//                //push(x);
//            });
        });

        socket.onOpen((e) -> {
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
        new NARWebClient();
    }

}
