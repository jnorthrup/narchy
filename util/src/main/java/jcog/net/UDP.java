package jcog.net;


import com.fasterxml.jackson.core.JsonProcessingException;
import jcog.Util;
import jcog.exe.Loop;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;

/**
 * generic UDP server & utilities
 * TODO Datagram TLS
 */
public class UDP extends Loop {


    static {
        System.setProperty("java.net.preferIPv6Addresses",
                "true"
                //"false"
        );
    }


    /**
     * in bytes
     */
    static final int MAX_PACKET_SIZE = 1024;


    private static final Logger logger = LoggerFactory.getLogger(UDP.class);


    private final int port;
    protected final DatagramChannel c;
    public final InetSocketAddress addr;


    public UDP(@Nullable InetAddress a, int port) throws IOException {
        super();

        c = DatagramChannel.open();
        c.configureBlocking(false);

        c.setOption(StandardSocketOptions.SO_RCVBUF, 1024 * 128);
        c.setOption(StandardSocketOptions.SO_SNDBUF, 1024 * 128);
        //c.socket().setBroadcast(true);
        ///c.setOption(StandardSocketOptions.SO_BROADCAST, true);
        c.setOption(StandardSocketOptions.SO_REUSEADDR, true);
        c.setOption(StandardSocketOptions.SO_REUSEPORT, true);
        c.bind(new InetSocketAddress(a, port));


        this.addr = (InetSocketAddress) c.getLocalAddress();
        this.port = ((InetSocketAddress) c.getLocalAddress()).getPort();

        logger.info("start {}", addr);
    }

//    public UDP(int port) throws IOException {
//        this(null, port);
//    }
//
//    public UDP() throws IOException {
//        this(null, 0);
//    }

    public int port() {
        return port;
    }


    @Override
    protected void stopping() {
        try {
            synchronized (c) {
                c.configureBlocking(true);
                c.disconnect();
                c.configureBlocking(false);
            }
            logger.info("stop {}", addr);
        } catch (IOException e) {
            logger.error("close {}", e);
        }
    }


    final ByteBuffer b = ByteBuffer.allocate(MAX_PACKET_SIZE);

    @Override
    public boolean next() {
        try {

            SocketAddress from;
            while ((from = c.receive(b.rewind())) != null) {
                if (!isRunning())
                    return false;
                in((InetSocketAddress) from, b.array(), b.position());
            }
        } catch (ClosedChannelException closed) {
            return false;
        } catch (Throwable t) {
            logger.error("recv {}", t);
        }
        return true;
    }

    protected SocketAddress receive(ByteBuffer byteBuffer, SelectionKey selectionKey) throws IOException {
        return c.receive(byteBuffer);
    }

    public boolean out(String data, String host, int port) throws UnknownHostException {
        return out(data.getBytes(), host, port);
    }

    public boolean out(byte[] data, int port) {
        return outBytes(data, new InetSocketAddress(port));
    }

    public boolean out(byte[] data, String host, int port) throws UnknownHostException {
        return outBytes(data, new InetSocketAddress(InetAddress.getByName(host), port));
    }

    public boolean outJSON(Object x, InetSocketAddress addr) {


        byte[] b;
        try {
            b = Util.toBytes(x);
        } catch (JsonProcessingException e) {
            logger.error("{} ", e);
            return false;
        }

        return outBytes(b, addr);
    }


    public boolean outBytes(byte[] data, InetSocketAddress to) {
        try {
            int sent = c.send(ByteBuffer.wrap(data), to);
            if (sent < data.length) {
                logger.warn("output overflow: {}/{} bytes sent to {}", sent, data.length, to);
            }
            return true;
        } catch (Exception e) {
            logger.error("send {} {} {}", to, e.getMessage());
            return false;
        }
    }


    /**
     * override in subclasses
     */
    protected void in(InetSocketAddress msgOrigin, byte[] data, int position) {

    }


}
























































