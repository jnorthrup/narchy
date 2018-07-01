package nars.web;

import org.teavm.jso.JSObject;
import org.teavm.jso.JSProperty;
import org.teavm.jso.browser.Location;
import org.teavm.jso.browser.Window;

public interface WebSocket extends JSObject {


    /** creates a socket back to the server that provided the location of the current page */
    static WebSocket newSocket(String path) {
        Location l = Window.current().getLocation();
        return newSocket(l.getHostName(), Integer.parseInt(l.getPort()), path);
    }

    static WebSocket newSocket(String host, int port, String path) {
        return WebSocketUtil.newSocket("ws://" + host + ":" + port + "/" + path);
    }

    void send(JSObject obj);

    void send(String text);

    default void setOnData(JSConsumer each) {
        WebSocketUtil.setMessageConsumer(this, each);
    }

    @JSProperty("onopen")
    void setOnOpen(JSRunnable r);

    @JSProperty("onclose")
    void setOnClose(JSRunnable r);

}