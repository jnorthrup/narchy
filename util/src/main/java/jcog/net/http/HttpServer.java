package jcog.net.http;

import jcog.exe.Loop;
import jcog.list.FastCoWList;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A simple http 1.1 server with WebSockets.
 * <p>
 * Supports:
 * + Sending static files (GET & HEAD)
 * + Directory index files
 * + Resumeable downloads (range header)
 * + Last-Modified & If-Modified-Since
 * + WebSockets using the java_websocket lib
 * <p>
 * Threading model: HttpServer only has a server socket that it runs accept() on. This can be used in the main loop. All
 * sockets are then passed on to the HttpDownloadThread thread, which uses a select loop to serve all downloads from a
 * singe thread. If an Upgrade: WebSocket header is present, the socket is then removed from HttpDownloadThread and
 * added to one of the HttpWebSocketServer threads that all run their own select loop. The number of HttpWebSocketServer
 * threads that are spawned, depends on the number of cpu cores (including HyperThreading). select loop and parsing
 * happen on the same thread. This ensures the anti congestion features of TCP can do their thing properly.
 * <p>
 * HttpWebsocketListener callbacks will originate from one of the HttpWebSocketServer threads.
 *
 * @author Joris
 */
public class HttpServer extends Loop implements HttpWebSocketServer.UpgradeWebSocketHandler, HttpWebSocketServerListener {
    
    static final int LINEBUFFER_SIZE = 512; // Used to combine a line that spans multiple TCP segments
    private static final int RCVBUFFER_SIZE = 16384;
    static final int BUFFER_SIZE = RCVBUFFER_SIZE + LINEBUFFER_SIZE;
    private static final int WEBSOCKET_HANDLERS = 1;
    
    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(HttpServer.class);
    private final ServerSocketChannel ssChannel;
    private final List<HttpWebSocketServer> websocketServers = new FastCoWList(HttpWebSocketServer[]::new);
    private final AtomicInteger upgradeWebSocketHandler_counter = new AtomicInteger();
    private final Set<WebSocket> websockets = new LinkedHashSet<>();
    private final HttpWebSocketServerListener websocketListener;
    private volatile boolean stop = false;
    private final HttpTransfer downloadThread;

    public HttpServer(String host, int port, File httpdocs_, HttpWebSocketServerListener websocketListener) throws IOException {
        this(new InetSocketAddress(host, port), httpdocs_, websocketListener);
    }

    public HttpServer(InetSocketAddress addr, File httpdocs_, HttpWebSocketServerListener websocketListener) throws IOException {
        this(HttpServer.openServerChannel(addr), httpdocs_, websocketListener);
    }

    public HttpServer(ServerSocketChannel ssChannel, File httpdocs_, HttpWebSocketServerListener websocketListener) throws IOException {
        this.ssChannel = ssChannel;
        this.websocketListener = websocketListener;

        if (ssChannel == null) {
            throw new IllegalArgumentException();
        }


        File httpdocs = null;
        if (httpdocs_ != null) {
            httpdocs = httpdocs_.getCanonicalFile();
            if (!httpdocs.isDirectory()) {
                //httpdocs = null;
                logger.warn("argument httpdocs is not a directory");
            }
        }

        downloadThread = new HttpTransfer(
                httpdocs == null ? null : new File(httpdocs.getPath()), this);

        for (int a = 0; a < WEBSOCKET_HANDLERS; ++a) {
            websocketServers.add(new HttpWebSocketServer(this));
        }
    }

    static ServerSocketChannel openServerChannel(InetSocketAddress listenAddr) throws IOException {
        ServerSocketChannel ssChannel = ServerSocketChannel.open();
        ssChannel.configureBlocking(false);
        ServerSocket socket = ssChannel.socket();
        socket.bind(listenAddr);
        socket.setReceiveBufferSize(RCVBUFFER_SIZE);

        logger.info("Listening on {}:{}", socket.getInetAddress(), getListeningPort(ssChannel));
        return ssChannel;
    }

    /**
     * Returns the TCP port that we are currently listening on
     *
     * @param ssChannel
     * @return -1 if not yet listening, otherwise the listening port.
     */
    private static int getListeningPort(ServerSocketChannel ssChannel) {
        if (ssChannel == null) {
            return -1;
        }

        ServerSocket sock = ssChannel.socket();
        if (sock == null) {
            return -1;
        }

        return sock.getLocalPort();
    }

    public void addRouteStatic(String path, File file) throws IOException, SecurityException {
        downloadThread.addRouteStatic(path, file);
    }

    @Override
    protected synchronized void onStart() {
        if (stop) {
            throw new IllegalStateException();
        }

        downloadThread.onStart();

        for (HttpWebSocketServer s : websocketServers) {
            try {
                s.onStart();
            } catch (IOException e) {
                logger.error("starting {}: {}", s, e);
            }
        }

        //logger.info("Http server setup at {0}:{1,number,#}", new Object[]{ssChannel.socket().getInetAddress(), getListeningPort(ssChannel)});
    }

    public int getListeningPort() {
        return getListeningPort(ssChannel);
    }

    @Override
    protected synchronized void onStop() {
        stop = true;

        downloadThread.onStop();

        for (HttpWebSocketServer s : websocketServers) {
            s.onStop();
        }

        logger.info("{} stop", this);
    }


    @Override
    public boolean next() {
        if (stop) {
            //throw new IllegalStateException();
            return false;
        }

        try {
            SocketChannel sChannel;
            while ((sChannel = ssChannel.accept()) != null) {
                downloadThread.addNewChannel(sChannel);
            }

        } catch (ClosedChannelException ex) {
            // This exception is okay after stop() has been called.
            // However normally this error should only shown once.
            // Stop running the loop (or unregister this object from the loop)
            // when calling close()
            logger.info("Channel closed in accept()");
        } catch (IOException ex) {
            logger.error("IOException in accept()", ex);
        }


        websocketServers.forEach(HttpWebSocketServer::next);

        try {
            downloadThread.next();
        } catch (ClosedSelectorException e) {
            //probably normal disconnect of server socket
        } catch (IOException e) {
            logger.error("IOException in download thread()", e);
        }

        return true;
    }


    @Override
    // The list is not modified after the constructor
    public void upgradeWebSocketHandler(SocketChannel sChannel, ByteBuffer prependData) {
        HttpWebSocketServer s = websocketServers.get(upgradeWebSocketHandler_counter.getAndIncrement() % websocketServers.size());
        s.addNewChannel(sChannel, prependData);
    }

    @Override

    public boolean wssConnect(SelectionKey key) {
        HttpWebSocketServerListener listener = this.websocketListener;

        if (listener == null) {
            return true;
        }

        return listener.wssConnect(key);
    }

    @Override

    public void wssOpen(WebSocket conn, ClientHandshake handshake) {
        HttpWebSocketServerListener listener = this.websocketListener;
        websockets.add(conn);

        if (listener != null) {
            listener.wssOpen(conn, handshake);
        }
    }

    @Override

    public void wssClose(WebSocket conn, int code, String reason, boolean remote) {
        HttpWebSocketServerListener listener = this.websocketListener;
        try {
            if (listener != null) {
                listener.wssClose(conn, code, reason, remote);
            }
        } finally {
            websockets.remove(conn);
        }
    }

    @Override

    public void wssMessage(WebSocket conn, String message) {
        HttpWebSocketServerListener listener = this.websocketListener;
        if (listener != null) {
            listener.wssMessage(conn, message);
        }
    }

    @Override

    public void wssMessage(WebSocket conn, ByteBuffer message) {
        HttpWebSocketServerListener listener = this.websocketListener;
        if (listener != null) {
            listener.wssMessage(conn, message);
        }
    }

    @Override

    public void wssError(WebSocket conn, Exception ex) {
        HttpWebSocketServerListener listener = this.websocketListener;
        if (listener != null) {
            listener.wssError(conn, ex);
        }
    }
}
