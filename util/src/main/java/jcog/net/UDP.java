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
 * TODO Datagram TLS http://openjdk.java.net/jeps/219
 */
public class UDP extends Loop {


    static {
//        System.setProperty("java.net.preferIPv6Addresses",
//            //"true"
//            "false"
//        );
    }


    /**
     * in bytes
     */
    static final int MAX_PACKET_SIZE = 1024;

    //static final int DEFAULT_socket_BUFFER_SIZE = 64 * 1024;

    private static final Logger logger = LoggerFactory.getLogger(UDP.class);

    //private static final InetAddress local = new InetSocketAddress(0).getAddress();

    private final int port;
    protected final DatagramChannel c;
    public final InetSocketAddress addr;

//    public UDP(String host, int port) throws SocketException, UnknownHostException {
//        this(InetAddress.getByName(host), port);
//    }

//    public UDP() {
//        DatagramSocket iin;
//        try {
//            iin = new DatagramSocket();
//        } catch (SocketException e) {
//            logger.error("{}", e);
//            iin = null;
//        }
//        this.in = iin;
//        this.recvThread = null;
//        this.port = -1;
//    }


//    public static final InetAddress WAN() {
//        try {
//            return InetAddress.getByName("::");
//        } catch (UnknownHostException e) {
//            logger.error("could not determine WAN address {}", e);
//            return null;
//        }
//    }

    public UDP(@Nullable InetAddress a, int port) throws IOException {
        super();

        c = DatagramChannel.open();
        c.configureBlocking(false);
        c.socket().setBroadcast(true);
        c.setOption(StandardSocketOptions.SO_RCVBUF, 1024 * 1024);
        c.setOption(StandardSocketOptions.SO_SNDBUF, 1024 * 1024);
        c.setOption(StandardSocketOptions.SO_BROADCAST, true);
        c.setOption(StandardSocketOptions.SO_REUSEADDR, true);
        c.setOption(StandardSocketOptions.SO_REUSEPORT, true);
        c.bind(new InetSocketAddress(a, port));

//        final Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
//        while (networkInterfaces.hasMoreElements()) {
//            NetworkInterface nic = networkInterfaces.nextElement();
//            DatagramChannel dc = DatagramChannel.open(StandardProtocolFamily.INET)
//                    .setOption(StandardSocketOptions.SO_REUSEADDR, true)
//                    .setOption(StandardSocketOptions.IP_MULTICAST_IF, nic);
//
//            MembershipKey key = dc.join(LAN.getAddress(), nic);
//
//        }

        addr = (InetSocketAddress) c.getLocalAddress();



        //in.setTrafficClass(0x10 /*IPTOS_LOWDELAY*/); //https://docs.oracle.com/javase/8/docs/api/java/net/DatagramSocket.html#setTrafficClass-int-
//        in.setSoTimeout(0);
//        in.setSendBufferSize(DEFAULT_socket_BUFFER_SIZE);
//        in.setReceiveBufferSize(DEFAULT_socket_BUFFER_SIZE);
        this.port = port;
    }

    public UDP(int port) throws IOException {
        this(null, port);
    }

    public UDP() throws IOException {
        this(null, 0);
    }

    public int port() {
        return port;
    }


    @Override
    protected void onStop() {
        try {
            c.close();
        } catch (IOException e) {
            logger.error("close {}", e);
        }
    }


//    protected void recv() {
//
//        while (isRunning()) {
//            try {
//
//                byte[] receiveData = new byte[MAX_PACKET_SIZE];
//                DatagramPacket p = new DatagramPacket(receiveData, receiveData.length);
//                in.receive(p);
//
//                in(p, receiveData);
//
//            } catch (Throwable e) {
//                if (in.isClosed()) {
//                    stop();
//                    break;
//                } else {
//                    logger.warn("{}", e);
//                }
//            }
//        }
//
//    }


    final ByteBuffer b = ByteBuffer.allocate(MAX_PACKET_SIZE);

    @Override
    public boolean next() {
        try {

            SocketAddress from;
            while ((from = c.receive(b.rewind())) != null) {
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

    @Deprecated
    public boolean out(String data, int port) {
        return out(data.getBytes(), port);
    }

    public boolean out(byte[] data, int port) {
        return outBytes(data, new InetSocketAddress(port));
    }

    public boolean out(byte[] data, String host, int port) throws UnknownHostException {
        return outBytes(data, new InetSocketAddress(InetAddress.getByName(host), port));
    }

    public boolean outJSON(Object x, InetSocketAddress addr) {
        //DynByteSeq dyn = new DynByteSeq(MAX_PACKET_SIZE); //TODO wont work with the current hacked UTF output

//        ByteArrayDataOutput dyn = ByteStreams.newDataOutput();
//        json.toJson(x, new PrintStream());
//        return outBytes(dyn.array(), addr);


        byte[] b;
        try {
            b = Util.toBytes(x);
        } catch (JsonProcessingException e) {
            logger.error("{} ", e);
            return false;
        }

        return outBytes(b, addr);
    }


//    final static ThreadLocal<DatagramPacket> packet = ThreadLocal.withInitial(() -> {
//        return new DatagramPacket(ArrayUtils.EMPTY_BYTE_ARRAY, 0, 0);
//    });

    public boolean outBytes(byte[] data, InetSocketAddress to) {
        try {
            int sent = c.send(ByteBuffer.wrap(data), to);
            if (sent < data.length) {
                logger.warn("output overflow: {}/{} bytes sent to {}", sent, data.length, to );
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


//    static class UDPClient {
//        public static void main(String args[]) throws Exception {
//            BufferedReader inFromUser =
//                    new BufferedReader(new InputStreamReader(System.in));
//            DatagramSocket clientSocket = new DatagramSocket();
//            InetAddress IPAddress = InetAddress.getByName("localhost");
//            byte[] sendData = new byte[1024];
//            byte[] receiveData = new byte[1024];
//            String sentence = inFromUser.readLine();
//            sendData = sentence.getBytes();
//            DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, 9876);
//            clientSocket.send(sendPacket);
//            DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
//            clientSocket.receive(receivePacket);
//            String modifiedSentence = new String(receivePacket.getData());
//            System.out.println("FROM SERVER:" + modifiedSentence);
//            clientSocket.close();
//        }
//    }
}


///** https://github.com/msgpack/msgpack-java */
//abstract public class ObjectUDP extends UDP {
//
//    private static final Logger logger = LoggerFactory.getLogger(ObjectUDP.class);
//
//
//
//    public ObjectUDP(String host, int port) throws SocketException, UnknownHostException {
//        super(host, port);
//    }
//
////
////    public boolean out(Object x, String host, int port)  {
////        try {
////            return out(toBytes(x), host, port);
////        } catch (IOException e) {
////            logger.error("{}", e);
////            return false;
////        }
////    }
//
////    protected byte[] toBytes(Object x) throws IOException {
////        return msgpack.write(x);
////    }
////
////    protected <X> byte[] toBytes(X x, Template<X> t) throws IOException {
////        return msgpack.write(x, t);
////    }
//
//
//    protected String stringFromBytes(byte[] x) {
//        try {
//            return MessagePack.newDefaultUnpacker(x).unpackString();
//        } catch (IOException e) {
//            logger.error("{}", e);
//            return null;
//        }
//
//        //Templates.tList(Templates.TString)
////        System.out.println(dst1.get(0));
////        System.out.println(dst1.get(1));
////        System.out.println(dst1.get(2));
////
////// Or, Deserialze to Value then convert type.
////        Value dynamic = msgpack.read(raw);
////        List<String> dst2 = new Converter(dynamic)
////                .read(Templates.tList(Templates.TString));
////        System.out.println(dst2.get(0));
////        System.out.println(dst2.get(1));
////        System.out.println(dst2.get(2));
//
//    }
//
//}
