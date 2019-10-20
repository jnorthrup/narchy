package jcog.net.http;

import jcog.exe.Loop;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.channels.ServerSocketChannel;

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

    static final int LINEBUFFER_SIZE = 2048;
    private static final int RCVBUFFER_SIZE = 16384;
    static final int BUFFER_SIZE = RCVBUFFER_SIZE + LINEBUFFER_SIZE;

    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(HttpServer.class);
    private final ServerSocketChannel ssChannel;
    private final HttpModel model;

    private final HttpSelector http;
    private final WebSocketSelector ws;


    public HttpServer(int port, HttpModel model) throws IOException {
        this("0.0.0.0", port, model);
    }

    public HttpServer(String host, int port, HttpModel model) throws IOException {
        this(new InetSocketAddress(host, port), model);
    }

    public HttpServer(InetSocketAddress addr, HttpModel model) throws IOException {
        this(HttpServer.openServerChannel(addr), model);
    }

    private HttpServer(ServerSocketChannel ssChannel, HttpModel model) throws IOException {
        this.ssChannel = ssChannel;
        this.model = model;

        if (ssChannel == null) {
            throw new IllegalArgumentException();
        }




        http = new HttpSelector(this, this);


        ws = new WebSocketSelector(this);

    }

    private static ServerSocketChannel openServerChannel(InetSocketAddress listenAddr) throws IOException {
        ServerSocketChannel ssChannel = ServerSocketChannel.open();
        ssChannel.configureBlocking(false);
        ServerSocket socket = ssChannel.socket();
        socket.bind(listenAddr);
        socket.setReceiveBufferSize(RCVBUFFER_SIZE);

        logger.info("listen {}:{}", socket.getInetAddress(), getListeningPort(ssChannel));
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

    public int getListeningPort() {
        return getListeningPort(ssChannel);
    }

    @Override
    protected void starting() {

        http.start();

        ws.start();

        
    }

    @Override
    protected void stopping() {
        http.stop();

        ws.stop();
    }


    @Override
    public boolean next() {

        http.next(ssChannel);

        ws.next();

        return true;
    }


    @Override
    public void upgradeWebSocketHandler(HttpConnection http, ByteBuffer prependData) {
        ws.addNewChannel(http, prependData);
    }

    @Override
    public final boolean wssConnect(WebSocketConnection conn) {
        return model.wssConnect(conn);
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

    @Override
    public final void response(HttpConnection h) {
        model.response(h);
    }

    /** local address uri */
    public URI getURI() {
        try {
            return URI.create("ws://" + ssChannel.getLocalAddress());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
