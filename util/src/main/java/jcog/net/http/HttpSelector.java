package jcog.net.http;

import jcog.net.http.HttpConnection.ConnectionStateChangeListener;
import jcog.net.http.HttpConnection.STATE;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.Iterator;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * @author Joris
 */
class HttpSelector implements ConnectionStateChangeListener {

    //private final long TIMEOUT_CHECK_PERIOD_ms = 1_000;
    private final long TIMEOUT_PERIOD_ms = 5_000;

    private static final Logger log = LoggerFactory.getLogger("jcog/net/http");
    private final WebSocketSelector.UpgradeWebSocketHandler upgradeWebSocketHandler;
    private final ByteBuffer buf = ByteBuffer.allocateDirect(HttpServer.BUFFER_SIZE);
    private final ConcurrentLinkedQueue<SocketChannel> newChannels = new ConcurrentLinkedQueue<>();
    private final HttpModel model;
    
    
    private Selector selector;
    

    HttpSelector(HttpModel model, WebSocketSelector.UpgradeWebSocketHandler upgradeWebSocketHandler) {
        this.model = model;
        this.upgradeWebSocketHandler = upgradeWebSocketHandler;
    }


    @Override
    public void connectionStateChange(HttpConnection conn, STATE oldState, STATE newState) {
        if (newState == STATE.CLOSED) {
        conn.key.attach(null);
        conn.key.cancel();
            try {
                conn.channel.close();
            } catch (IOException ex) {
                log.error("{}",ex);
            }

        } else if (newState == STATE.UPGRADE) {
            conn.key.attach(null);
            conn.key.cancel();

            if (conn.websocket && upgradeWebSocketHandler != null) {
                ByteBuffer rawHead = conn.rawHead;
                conn.rawHead = null; 
                rawHead.flip();
                upgradeWebSocketHandler.upgradeWebSocketHandler(conn.channel, rawHead);
            } else {
                try {
                    conn.channel.close();
                } catch (IOException ex) {
                    log.error("{}",ex);
                }
            }
        }
    }

    public synchronized void onStart() {
        try {
            selector = Selector.open();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }



    public void next() throws IOException {

        try {
            selector.selectNow(); 
        } catch (ClosedSelectorException | IOException ex) {
            return;
        }

        SocketChannel sChannel = newChannels.poll();
        if (sChannel != null) {
            sChannel.configureBlocking(false);
            sChannel.socket().setTcpNoDelay(false);
            SelectionKey key = sChannel.register(selector, SelectionKey.OP_READ);
            key.attach(new HttpConnection(this, model, key, sChannel));
        }

        Iterator<SelectionKey> it;

        long now = System.nanoTime();


        {


            it = selector.keys().iterator();

            while (it.hasNext()) {
                SelectionKey key = it.next();
                HttpConnection conn = (HttpConnection) key.attachment();
                if (now - conn.lastReceivedNS > TIMEOUT_PERIOD_ms * 1_000_000L) {
                    key.attach(null);
                    key.cancel();
                    SocketAddress remote = conn.channel.getRemoteAddress();
                    conn.channel.close();
                    conn.closed();
                    log.debug("timeout {}", remote);
                }
            }
        }

        it = selector.selectedKeys().iterator();

        while (it.hasNext()) {
            SelectionKey key = it.next();

            HttpConnection conn = (HttpConnection) key.attachment();

            it.remove();

            if (conn == null) {
                
                continue;
            }

            try {

                if (key.isReadable()) {
                    if (!readable(conn)) {
                        key.attach(null);
                        key.cancel();
                        conn.channel.close();
                        conn.closed();
                        continue;
                    }
                }

                if (key.isValid() && key.isWritable()) {
                    conn.writeable();
                }
            } catch (IOException ex) {
                log.warn("{}",ex);

                key.attach(null);
                key.cancel();
                conn.channel.close();
                conn.closed();
            }
        }
    }


    public synchronized void onStop() {
        try {
            selector.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }


    /**
     * @return false if this connection should be removed
     */
    private boolean readable(HttpConnection conn) throws IOException {
        buf.clear();

        
        buf.limit(buf.capacity());
        buf.position(HttpServer.LINEBUFFER_SIZE);
        buf.mark();

        int read;

        try {
            read = conn.channel.read(buf);
        } catch (ClosedChannelException ex) {
            return false;
        }

        if (read > 0) {
            buf.limit(buf.position());
            buf.reset();

            conn.read(buf);
            return true;
        } else {
            return false;
        }


    }

    /**
     * Add a new socket channel to be handled by this thread.
     */

    
    void addNewChannel(SocketChannel sChannel) {
        newChannels.add(sChannel);

        try {
            selector.wakeup();
        } catch (IllegalStateException | NullPointerException ex) {
            
            assert false;
        }
    }
}
