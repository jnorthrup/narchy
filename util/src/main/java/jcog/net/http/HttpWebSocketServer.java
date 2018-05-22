package jcog.net.http;

import org.java_websocket.SocketChannelIOHelper;
import org.java_websocket.WebSocket;
import org.java_websocket.WebSocketAdapter;
import org.java_websocket.WebSocketImpl;
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
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * @author Joris
 */
class HttpWebSocketServer extends WebSocketAdapter implements Runnable {
    private static final org.slf4j.Logger log = LoggerFactory.getLogger("jcog/net/http");
    public final Thread thread = new Thread(this);
    private final ConcurrentLinkedQueue<NewChannel> newChannels = new ConcurrentLinkedQueue<>();
    private final HttpWebSocketServerListener listener;
    private final Set<WebSocket> connections = new LinkedHashSet<>();
    private volatile boolean ready = false;
    private Selector selector;
    private ByteBuffer buffer;

    HttpWebSocketServer(HttpWebSocketServerListener listener) {
        this.listener = listener;
        if (this.listener == null) {
            throw new IllegalArgumentException();
        }
    }

    @ThreadSafe
    public void startWaitReady() {
        thread.start();

        while (!ready) {
            try {
                Thread.sleep(1);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }

    @ThreadSafe
    public void stop() {
        thread.interrupt();
    }

    public void setDeamon(boolean on) {
        thread.setDaemon(on);
    }

    public Set<WebSocket> connections() {
        return this.connections;
    }

    private boolean registerNewChannel() throws IOException {
        NewChannel newChannel = newChannels.poll();
        if (newChannel == null) {
            return false; // done
        }

        log.info("new websocket connection: {}", newChannel.sChannel.getRemoteAddress());

        newChannel.sChannel.configureBlocking(false);


        WebSocketImpl conn = new ServerWebSocketImpl(this);

        newChannel.sChannel.socket().setTcpNoDelay(true);
        conn.key = newChannel.sChannel.register(selector, SelectionKey.OP_READ, conn);

        if (!onConnect(conn.key)) {
            conn.key.cancel();
        } else {
            conn.channel = newChannel.sChannel;

            ByteBuffer prependData = newChannel.prependData;
            newChannel.prependData = null;

            conn.decode(prependData);
        }

        return true;
    }

    private boolean readable(SelectionKey key, WebSocketImpl conn) throws IOException {
        buffer.clear();
        int read = conn.channel.read(buffer);
        buffer.flip();

        if (read == -1) {
            // connection closed
            conn.eot();
            return true;
        }

        if (read == 0) {
            return true;  // true = done reading
        }

        // Something has been read (up to WebSocket.RCVBUF)
        // Perhaps there is more in the TCP receive buffer,
        // but other connections will get a chance first

        conn.decode(buffer);

        return false; // false = keep this connection in the selector list
    }

    private boolean writable(SelectionKey key, WebSocketImpl conn) throws IOException {
        if (SocketChannelIOHelper.batch(conn, conn.channel)) {
            if (key.isValid()) {
                key.interestOps(SelectionKey.OP_READ);
            }
            return true; // true = done writing
        }

        return false; // false = there is more to write, but give other connections a chance to write something
    }

    @Override
    public void run() {
        thread.setName("WebSocketServer-" + thread.getId());

        buffer = ByteBuffer.allocate(WebSocketImpl.RCVBUF);

        try {
            try {
                selector = Selector.open();
            } catch (IOException ex) {
                log.warn("Unable to open selector {}", ex);
                return;
            }

            ready = true;

            while (!thread.isInterrupted()) {
                SelectionKey key = null;

                Iterator<SelectionKey> it;
                try {
                    selector.select();

                    while (registerNewChannel()) {
                    }

                    it = selector.selectedKeys().iterator();

                } catch (ClosedSelectorException ex) {
                    break;
                } catch (IOException ex) {
                    log.warn("IOException in select() {}", ex);
                    break;
                }

                while (it.hasNext()) {
                    WebSocketImpl conn = null;
                    key = it.next();

                    if (!key.isValid()) {
                        continue;
                    }

                    try {
                        if (key.isReadable()) {
                            conn = (WebSocketImpl) key.attachment();
                            if (readable(key, conn)) {
                                it.remove();
                            }

                        }

                        if (key.isValid() && key.isWritable()) {
                            conn = (WebSocketImpl) key.attachment();
                            if (writable(key, conn)) {
                                try {
                                    it.remove();
                                } catch (IllegalStateException ex) {
                                    // already removed
                                }
                            }
                        }

                    } catch (ClosedSelectorException ex) {
                        break;
                    } catch (CancelledKeyException ex) {
                        it.remove();

                        // an other thread may cancel the key
                    } catch (IOException ex) {
                        log.warn("IOException while parsing selector {}", ex);
                        key.cancel();
                        it.remove();

                        handleIOException(conn, ex);
                    }

                }
            }


            for (WebSocket ws : connections) {
                ws.close(CloseFrame.NORMAL);
            }
        } catch (RuntimeException ex) {
            log.warn("{}", ex);

            onError(null, ex);
        }
    }

    private void handleIOException(WebSocket conn, IOException ex) {
        onWebsocketError(conn, ex); // conn may be null here

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

    @ThreadSafe
    void addNewChannel(SocketChannel sChannel, ByteBuffer prependData) {
        newChannels.add(new NewChannel(sChannel, prependData));
        try {
            selector.wakeup();
        } catch (IllegalStateException | NullPointerException ex) {
            // Thread has not started yet, or it just stopped
            assert false;
        }
    }

    @Override
    public void onWebsocketClosing(WebSocket ws, int code, String reason, boolean remote) {
    }

    @Override
    public void onWebsocketCloseInitiated(WebSocket ws, int code, String reason) {
    }

    private Socket getSocket(WebSocket conn) {
        WebSocketImpl impl = (WebSocketImpl) conn;
        return ((SocketChannel) impl.key.channel()).socket();
    }

    @Override
    public InetSocketAddress getLocalSocketAddress(WebSocket conn) {
        return (InetSocketAddress) getSocket(conn).getLocalSocketAddress();
    }

    @Override
    public InetSocketAddress getRemoteSocketAddress(WebSocket conn) {
        return (InetSocketAddress) getSocket(conn).getRemoteSocketAddress();
    }

    private static final class NewChannel {
        final SocketChannel sChannel;
        ByteBuffer prependData;

        NewChannel(SocketChannel sChannel, ByteBuffer prependData) {
            this.sChannel = sChannel;
            this.prependData = prependData;
        }
    }
}
