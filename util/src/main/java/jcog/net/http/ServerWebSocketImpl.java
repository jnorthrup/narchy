package jcog.net.http;


import org.java_websocket.WebSocketImpl;
import org.java_websocket.WebSocketListener;
import org.java_websocket.drafts.Draft;
import org.java_websocket.drafts.Draft_6455;

import java.util.Arrays;

/**
 *
 * @author Joris
 */
class ServerWebSocketImpl extends WebSocketImpl
{
        public ServerWebSocketImpl(WebSocketListener listener)
        {
                // Draft_17 corresponds to Sec-WebSocket-Version: 13 which is RFC 6455
                super(listener, Arrays.asList(new Draft[]{new Draft_6455()}));
        }
}
