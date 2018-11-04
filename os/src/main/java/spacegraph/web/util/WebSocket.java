package spacegraph.web.util;

import org.teavm.jso.JSBody;
import org.teavm.jso.JSObject;
import org.teavm.jso.browser.Location;
import org.teavm.jso.browser.Window;

/** TeaVM WebSocket client.
 *
 * includes a small (1.93kb) ReconnectingWebsocket wrapper for the server to issue
 * as a separate file for include on client pages.
 *    from: https://github.com/jackmac92/reconnecting-websocket/blob/master/reconnecting-websocket.min.js
 *
 * hopefully this wrapper code will not be compiled into the client.  TeaVM should optimize it
 * away as dead code. It is here for convenience
 * */
public interface WebSocket extends JSObject {

    /** creates a socket back to the server that provided the location of the current page */
    static WebSocket connect(String path) {
        Location l = Window.current().getLocation();
        return connect(l.getHostName(), Integer.parseInt(l.getPort()), path);
    }

    static WebSocket connect(String host, int port, String path) {
        StringBuilder s = new StringBuilder(128).append("ws://").append(host).append(':').append(port);
        if (!path.startsWith("/"))
            s.append('/');
        s.append(path);
        return WebSocketUtil.newSocket(
                s.toString()
        );
    }

    void send(JSObject obj);

    void send(String text);

    /** overwrites any previously attached onData handler */
    default void setOnData(JSConsumer each) {
        WebSocketUtil.setOnMessage(this, each);
    }

    /** overwrites any previously attached onData handler */
    default void setOnClose(JSRunnable each) {
        WebSocketUtil.setOnClose(this, each);
    }

    /** overwrites any previously attached onData handler */
    default void setOnOpen(JSRunnable each) {
        WebSocketUtil.setOnOpen(this, each);
    }

//    @JSProperty("onopen")
//    void setOnOpen(JSRunnable r);
//
//    @JSProperty("onclose")
//    void setOnClose(JSRunnable r);

    class WebSocketUtil {
        /** https://github.com/joewalnes/reconnecting-websocket#parameters */
        @JSBody(params = {"url"},
                script = "const ws = new ReconnectingWebSocket(url); " +
                        //"ws.debug = true;" +
                        "ws.ws.binaryType = 'arraybuffer';" +
                        "return ws;")
        native static WebSocket newSocket(String url);

        @JSBody(params = {"socket", "each"},
                script = "socket.onmessage = function(m) { each(m.data); };")
        native static void setOnMessage(WebSocket socket, JSConsumer each);

        @JSBody(params = {"socket", "each"},
                script = "socket.onopen = each; ")
        native static void setOnOpen(WebSocket socket, JSRunnable each);

        @JSBody(params = {"socket", "each"},
                script = "socket.onclose = each; ")
        native static void setOnClose(WebSocket socket, JSRunnable each);
    }


    /** https://github.com/jackmac92/reconnecting-websocket/blob/master/reconnecting-websocket.min.js */
    String websocket_js =
            "var ReconnectingWebSocket=function(){function a(a,b){\"undefined\"==typeof b&&(b=[]),this.debug=!1,this.reconnectInterval=1e3,this.timeoutInterval=2e3,this.forcedClose=!1,this.timedOut=!1,this.protocols=[],this.onopen=function(a){},this.onclose=function(a){},this.onconnecting=function(){},this.onmessage=function(a){},this.onerror=function(a){},this.url=a,this.protocols=b,this.readyState=WebSocket.CONNECTING,this.connect(!1)}return a.prototype.connect=function(a){var b=this;this.ws=new WebSocket(this.url,this.protocols),this.onconnecting(),this.log(\"ReconnectingWebSocket\",\"attempt-connect\",this.url);var c=this.ws,d=setTimeout(function(){b.log(\"ReconnectingWebSocket\",\"connection-timeout\",b.url),b.timedOut=!0,c.close(),b.timedOut=!1},this.timeoutInterval);this.ws.onopen=function(c){clearTimeout(d),b.log(\"ReconnectingWebSocket\",\"onopen\",b.url),b.readyState=WebSocket.OPEN,a=!1,b.onopen(c)},this.ws.onclose=function(c){clearTimeout(d),b.ws=null,b.forcedClose?(b.readyState=WebSocket.CLOSED,b.onclose(c)):(b.readyState=WebSocket.CONNECTING,b.onconnecting(),a||b.timedOut||(b.log(\"ReconnectingWebSocket\",\"onclose\",b.url),b.onclose(c)),setTimeout(function(){b.connect(!0)},b.reconnectInterval))},this.ws.onmessage=function(a){b.log(\"ReconnectingWebSocket\",\"onmessage\",b.url,a.data),b.onmessage(a)},this.ws.onerror=function(a){b.log(\"ReconnectingWebSocket\",\"onerror\",b.url,a),b.onerror(a)}},a.prototype.send=function(a){if(this.ws)return this.log(\"ReconnectingWebSocket\",\"send\",this.url,a),this.ws.send(a);throw\"INVALID_STATE_ERR : Pausing to reconnect websocket\"},a.prototype.close=function(){return this.ws?(this.forcedClose=!0,this.ws.close(),!0):!1},a.prototype.refresh=function(){return this.ws?(this.ws.close(),!0):!1},a.prototype.log=function(){for(var b=[],c=0;c<arguments.length-0;c++)b[c]=arguments[c+0];(this.debug||a.debugAll)&&console.debug.apply(console,b)},a.debugAll=!1,a}();window.ReconnectingWebSocket=ReconnectingWebSocket;";

}