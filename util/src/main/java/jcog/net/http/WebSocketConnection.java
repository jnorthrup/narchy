package jcog.net.http;

import org.java_websocket.WebSocketImpl;
import org.java_websocket.drafts.Draft_6455;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Joris
 */
class WebSocketConnection extends WebSocketImpl {

    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(WebSocketConnection.class);

    final static AtomicInteger serial = new AtomicInteger();

    private final int hash;

    WebSocketConnection(WebSocketSelector.NewChannel newChannel, WebSocketSelector ws) throws IOException {
        super(ws, List.of(new Draft_6455()));
        this.hash = serial.incrementAndGet();

        SocketChannel chan = newChannel.sChannel;
        chan.configureBlocking(false);
        chan.socket().setTcpNoDelay(true);

        key = chan.register(ws.selector, SelectionKey.OP_READ, this);

        if (ws.listener.wssConnect(key)) {
            channel = chan;

            ByteBuffer prependData = newChannel.prependData;
            newChannel.prependData = null;

            decode(prependData);
            logger.info("connect {} {}", chan.getRemoteAddress(), getResourceDescriptor());
        } else {
            key.cancel();
            logger.info("non-connect {}", chan.getRemoteAddress(), this);
        }

    }

    @Override
    public boolean equals(Object obj) {
        return this == obj;
    }

    @Override
    public int hashCode() {
        return hash;
    }
}
