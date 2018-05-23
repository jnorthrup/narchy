package jcog.net.http;

import jcog.exe.Loop;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.ByteBuffer;
import java.nio.channels.*;

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
public class HttpServer extends Loop implements WebSocketSelector.UpgradeWebSocketHandler, HttpModel {

    static final int LINEBUFFER_SIZE = 512; // Used to combine a line that spans multiple TCP segments
    private static final int RCVBUFFER_SIZE = 16384;
    static final int BUFFER_SIZE = RCVBUFFER_SIZE + LINEBUFFER_SIZE;

    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(HttpServer.class);
    private final ServerSocketChannel ssChannel;
    private final HttpModel model;

    private final HttpSelector http;
    private final WebSocketSelector ws;

    private volatile boolean stop = false;

    public HttpServer(String host, int port, File httpdocs_, HttpModel model) throws IOException {
        this(new InetSocketAddress(host, port), httpdocs_, model);
    }

    public HttpServer(InetSocketAddress addr, File httpdocs_, HttpModel model) throws IOException {
        this(HttpServer.openServerChannel(addr), httpdocs_, model);
    }

    public HttpServer(ServerSocketChannel ssChannel, File httpdocs_, HttpModel model) throws IOException {
        this.ssChannel = ssChannel;
        this.model = model;

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

        http = new HttpSelector(this,
                httpdocs == null ? null : new File(httpdocs.getPath()), this);


        ws = new WebSocketSelector(this);

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
        http.addRouteStatic(path, file);
    }

    @Override
    protected synchronized void onStart() {
        if (stop) {
            throw new IllegalStateException();
        }

        http.onStart();
        ws.onStart();

        //logger.info("Http server setup at {0}:{1,number,#}", new Object[]{ssChannel.socket().getInetAddress(), getListeningPort(ssChannel)});
    }

    public int getListeningPort() {
        return getListeningPort(ssChannel);
    }

    @Override
    protected synchronized void onStop() {
        stop = true;

        http.onStop();

        ws.onStop();

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
                http.addNewChannel(sChannel);
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


        ws.next();

        try {
            http.next();
        } catch (ClosedSelectorException e) {
            //?
        } catch (IOException e) {
            logger.error("IOException in download thread()", e);
        }

        return true;
    }


    @Override
    public void upgradeWebSocketHandler(SocketChannel sChannel, ByteBuffer prependData) {
        ws.addNewChannel(sChannel, prependData);
    }

    @Override
    public final boolean wssConnect(SelectionKey key) {
        return model.wssConnect(key);
    }

    @Override
    public final void wssOpen(WebSocket conn, ClientHandshake handshake) {
        model.wssOpen(conn, handshake);
    }

    @Override
    public final void wssClose(WebSocket conn, int code, String reason, boolean remote) {
        model.wssClose(conn, code, reason, remote);
    }

    @Override
    public final void wssMessage(WebSocket conn, String message) {
        model.wssMessage(conn, message);
    }

    @Override
    public final void wssMessage(WebSocket conn, ByteBuffer message) {
        model.wssMessage(conn, message);
    }

    @Override
    public final void wssError(WebSocket conn, Exception ex) {
        model.wssError(conn, ex);
    }
}
