package jcog.net.http;


import org.java_websocket.WebSocketImpl;
import org.java_websocket.WebSocketListener;
import org.java_websocket.drafts.Draft_6455;

import java.util.List;

/**
 * @author Joris
 */
class ServerWebSocketImpl extends WebSocketImpl {
    public ServerWebSocketImpl(WebSocketListener listener) {
        // Draft_17 corresponds to Sec-WebSocket-Version: 13 which is RFC 6455
        super(listener, List.of(new Draft_6455()));
    }
}
