package jcog.net.http;

import jcog.data.list.MetalConcurrentQueue;
import org.java_websocket.*;
import org.java_websocket.drafts.Draft_6455;
import org.java_websocket.framing.CloseFrame;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.handshake.Handshakedata;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Joris
 */
public class WebSocketSelector extends WebSocketAdapter {
    private static final org.slf4j.Logger log = LoggerFactory.getLogger("jcog/net/http");
    private final MetalConcurrentQueue<NewChannel> newChannels = new MetalConcurrentQueue(1024);
    private final HttpModel listener;
    private final Set<WebSocket> connections = new LinkedHashSet<>();
    private final Selector selector = Selector.open();;


    WebSocketSelector(HttpModel listener) throws IOException {
        this.listener = listener;
    }

    public Set<WebSocket> connections() {
        return this.connections;
    }

    private boolean nextRegistration() throws IOException {
        NewChannel newChannel = newChannels.poll();
        if (newChannel == null) {
            return false; 
        }

        WebSocketImpl conn = new ServerWebSocketImpl(this);

        SocketChannel chan = newChannel.sChannel;
        chan.configureBlocking(false);
        chan.socket().setTcpNoDelay(true);

        conn.key = chan.register(selector, SelectionKey.OP_READ, conn);

        if (!onConnect(conn.key)) {
            conn.key.cancel();
            log.info("connect reject: {}", chan.getRemoteAddress());
        } else {
            conn.channel = chan;

            ByteBuffer prependData = newChannel.prependData;
            newChannel.prependData = null;

            conn.decode(prependData);
            log.info("connect: {}", chan.getRemoteAddress());
        }


        return true;
    }

    private boolean readable(WebSocketImpl conn) throws IOException {
        buffer.clear();
        int read = conn.channel.read(buffer);
        buffer.flip();

        if (read == -1) {
            
            conn.eot();
            return true;
        }

        if (read == 0) {
            return true;  
        }

        
        
        

        conn.decode(buffer);

        return false; 
    }

    private static boolean writable(SelectionKey key, WebSocketImpl conn) throws IOException {
        if (SocketChannelIOHelper.batch(conn, conn.channel)) {
            if (key.isValid()) {
                key.interestOps(SelectionKey.OP_READ);
            }
            return true; 
        }

        return false; 
    }

    private final ByteBuffer buffer = ByteBuffer.allocate(WebSocketImpl.RCVBUF);

    synchronized void onStart() {

    }

    synchronized void onStop() {
        for (WebSocket ws : connections) {
            ws.close(CloseFrame.NORMAL);
        }
        connections.clear();
    }

    public boolean next() {
        try {



                try {
                    
                    selector.selectNow();
                } catch (ClosedSelectorException | IOException ex) {
                    return true;
                }

                while (nextRegistration()) {
                }

                Iterator<SelectionKey> it;
                it = selector.selectedKeys().iterator();
                while (it.hasNext()) {

                    SelectionKey key = it.next();

                    if (!key.isValid()) {
                        continue;
                    }

                    WebSocketImpl conn = null;
                    try {
                        if (key.isReadable()) {
                            conn = (WebSocketImpl) key.attachment();
                            if (readable(conn)) {
                                it.remove();
                            }

                        }

                        if (key.isValid() && key.isWritable()) {
                            conn = (WebSocketImpl) key.attachment();
                            if (writable(key, conn)) {
                                try {
                                    it.remove();
                                } catch (IllegalStateException ex) {
                                    
                                }
                            }
                        }

                    } catch (ClosedSelectorException ex) {


                        return false; 
                    } catch (CancelledKeyException ex) {
                        it.remove();

                        
                    } catch (IOException ex) {
                        
                        conn.close();
                        key.cancel();
                        it.remove();

                        handleIOException(conn, ex);
                    }

                }



        } catch (Exception ex) {
            log.warn("{}", ex);

            onError(null, ex);
        }

        return true;
    }

    private void handleIOException(WebSocket conn, IOException ex) {
        onWebsocketError(conn, ex); 

        try {
            if (conn != null) {
                conn.close(CloseFrame.ABNORMAL_CLOSE);
            }
        } catch (CancelledKeyException ex2) {
            onWebsocketClose(conn, CloseFrame.ABNORMAL_CLOSE, null, true);
        }
    }

    @Override
    public final void onWebsocketMessage(WebSocket conn, String message) {
        onMessage(conn, message);
    }

    @Override
    public final void onWebsocketMessage(WebSocket conn, ByteBuffer blob) {
        onMessage(conn, blob);
    }

    @Override
    public final void onWebsocketOpen(WebSocket conn, Handshakedata handshake) {

        if (this.connections.add(conn)) {
            onOpen(conn, (ClientHandshake) handshake);
        }

    }

    @Override
    public final void onWebsocketClose(WebSocket conn, int code, String reason, boolean remote) {
        try {
            selector.wakeup();
        } catch (IllegalStateException ex) {
        }

        if (this.connections.remove(conn)) {
            onClose(conn, code, reason, remote);
        }

    }

    /**
     * @param conn may be null if the error does not belong to a single connection
     */
    @Override
    public final void onWebsocketError(WebSocket conn, Exception ex) {
        onError(conn, ex);
    }

    @Override
    public final void onWriteDemand(WebSocket w) {
        WebSocketImpl conn = (WebSocketImpl) w;
        conn.key.interestOps(SelectionKey.OP_READ | SelectionKey.OP_WRITE);
        try {
            selector.wakeup();
        } catch (IllegalStateException ex) {
        }
    }

    private boolean onConnect(SelectionKey key) {
        return listener.wssConnect(key);
    }

    private void onOpen(WebSocket conn, ClientHandshake handshake) {
        listener.wssOpen(conn, handshake);
    }

    private void onClose(WebSocket conn, int code, String reason, boolean remote) {
        listener.wssClose(conn, code, reason, remote);
    }

    private void onMessage(WebSocket conn, String message) {
        listener.wssMessage(conn, message);
    }

    private void onMessage(WebSocket conn, ByteBuffer message) {
        listener.wssMessage(conn, message);
    }

    private void onError(WebSocket conn, Exception ex) {
        listener.wssError(conn, ex);
    }

    
    void addNewChannel(SocketChannel sChannel, ByteBuffer prependData) {
        if (!newChannels.offer(new NewChannel(sChannel, prependData))) {
            System.err.println("newChannel queue overflow");
        }

        try {
            selector.wakeup();
        } catch (IllegalStateException | NullPointerException ex) {
            
            assert false;
        }
    }

    @Override
    public void onWebsocketClosing(WebSocket ws, int code, String reason, boolean remote) {
    }

    @Override
    public void onWebsocketCloseInitiated(WebSocket ws, int code, String reason) {
    }

    private static Socket socket(WebSocket conn) {
        return ((SocketChannel) ((WebSocketImpl) conn).key.channel()).socket();
    }

    @Override
    public InetSocketAddress getLocalSocketAddress(WebSocket conn) {
        return (InetSocketAddress) socket(conn).getLocalSocketAddress();
    }

    @Override
    public InetSocketAddress getRemoteSocketAddress(WebSocket conn) {
        return (InetSocketAddress) socket(conn).getRemoteSocketAddress();
    }

    private static final class NewChannel {
        final SocketChannel sChannel;
        ByteBuffer prependData;

        NewChannel(SocketChannel sChannel, ByteBuffer prependData) {
            this.sChannel = sChannel;
            this.prependData = prependData;
        }
    }

    interface UpgradeWebSocketHandler {
        void upgradeWebSocketHandler(SocketChannel sChannel, ByteBuffer prependData);
    }

    /**
     * @author Joris
     */
    public static class ServerWebSocketImpl extends WebSocketImpl {

        final static AtomicInteger serial = new AtomicInteger();

        private final int hash;

        ServerWebSocketImpl(WebSocketListener listener) {
            
            super(listener, List.of(new Draft_6455()));
            this.hash = serial.incrementAndGet();
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
}
