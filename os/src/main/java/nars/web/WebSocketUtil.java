package nars.web;

import org.teavm.jso.JSBody;

public class WebSocketUtil {
    @JSBody(params = {"url"},
            script = "console.log('websocket..'); const ws = new WebSocket(url);" +
                    "ws.binaryType = 'arraybuffer';" +
                    "return ws;")
    native static WebSocket newSocket(String url);

    @JSBody(params = {"socket", "each"},
            script = "socket.onmessage = function(m) {  each(m.data); };")
    native static void setMessageConsumer(WebSocket socket, JSConsumer each);

}
