package jcog.net;

import jcog.Util;
import jcog.exe.Loop;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.SocketTimeoutException;
import java.util.Arrays;

/**
 * https://github.com/jackss011/java-netdiscovery-sample
 */
abstract public class UDiscover<P> extends Loop {

    static protected final Logger logger = LoggerFactory.getLogger(UDiscover.class);

    final static String address = "228.5.6.7";
    final static int port = 6576;
    public static final int MAX_PAYLOAD_ID = 256;
    private final P id;

    MulticastSocket ms;
    private DatagramPacket p, q;
    private InetAddress ia;
    private byte[] myID;
    private byte[] theirID;


    public UDiscover(P payloadID, int periodMS) {
        super(periodMS);
        this.id = payloadID;
    }


    abstract protected void found(P theirs, InetAddress who, int port);

    @Override
    protected synchronized void onStart() {
        super.onStart();

        try {
            ia = InetAddress.getByName(address);

            ms = new MulticastSocket(port);

            ms.setBroadcast(true);

            ms.setReuseAddress(true);

            //ms.setTrafficClass();
            ms.setSoTimeout(periodMS.intValue() /* assuming it isnt changed while running but the initial value should be reasonable.. */);
            ms.joinGroup(ia);

            theirID = new byte[MAX_PAYLOAD_ID];
            myID = Util.toBytes(id);
            p = new DatagramPacket(myID, myID.length, ia, port);
            q = new DatagramPacket(theirID, theirID.length);
        
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    @Override
    public boolean next() {


        try {

            try {
                ms.send(p);
            } catch (IOException e) {
                logger.warn("{}", e);
            }
            //System.out.println(this + " Sent...");

            try {
                ms.receive(q);
                P theirPayload;
                try {

                    int len = q.getLength();
                    byte[] qd = q.getData();
                    if (!Arrays.equals(myID, 0, myID.length, qd, 0, len)) {
                        theirPayload = (P) Util.fromBytes(qd, len, id.getClass());
                        found(theirPayload, q.getAddress(), q.getPort());
                        //System.out.println(this + " recv: " + new String(p.getData()) + " - " + "From: " + p.getAddress());
                    }
                    Arrays.fill(qd, (byte) 0);
                } catch (Exception e) {
                    logger.error("deserializing {}", e);
                }
            } catch (SocketTimeoutException ignored) {

            }

        } catch (Exception e) {
            logger.error("{} {}", this, e);
        }

        return true;
    }

    @Override
    protected synchronized void onStop() {
        super.onStop();
        if (ms != null) {
            ms.close();
            ms = null;
        }
    }

    //
//        class ServerThread extends Thread {
//            MulticastSocket ms;
//
//            @Override
//            public void run() {
//                try {
//                    InetAddress ia = InetAddress.getByName(address);
//
//                    ms = new MulticastSocket(port);
//
//                    byte[] mess;// new String(String.valueOf(Math.random())).getBytes();
//
//                    for (; ; ) {
//                        mess = String.valueOf(Math.random()).getBytes();
//                        DatagramPacket p = new DatagramPacket(mess, mess.length, ia, port);
//                        ms.send(p);
//                        System.out.println(this + " Sent...");
//
//                        Thread.sleep(1000);
//                    }
//                } catch (UnknownHostException e) {
//                    e.printStackTrace();
//                } catch (IOException e) {
//                    e.printStackTrace();
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }
//
//                if (ms != null) ms.close();
//            }
//        }


}
//package jcog.net;
//
//import java.io.BufferedReader;
//import java.io.IOException;
//import java.io.InputStreamReader;
//import java.net.*;
//import java.nio.channels.DatagramChannel;
//import java.util.ArrayList;
//import java.util.Collections;
//import java.util.List;
//import java.util.Random;
//
///**
// * Performs broadcast and multicast peer detection. How well this
// * works depends on your network configuration
// *
// * @author ryanm
// *         http://www.java2s.com/Code/Java/Network-Protocol/Performsbroadcastandmulticastpeerdetection.htm
// *         https://codereview.stackexchange.com/questions/91675/simple-character-by-character-peer-to-peer-chat-application
// */
//public class UDiscover {
//    private static final byte QUERY_PACKET = 80;
//
//    private static final byte RESPONSE_PACKET = 81;
//
//    /**
//     * The group identifier. Determines the set of peers that are able
//     * to discover each other
//     */
//    public int group;
//
//    /**
//     * The port number that we operate on
//     */
//    public int port;
//
//    /**
//     * Data returned with discovery
//     */
//    public int peerData;
//
//    private DatagramSocket bcastSocket;
//
//    private boolean shouldStop = false;
//
//    private List<Peer> responseList = null;
//
//    /**
//     * Used to detect and ignore this peers response to it's own query.
//     * When we send a response packet, we set this to the destination.
//     * When we receive a response, if this matches the source, we know
//     * that we're talking to ourselves and we can ignore the response.
//     */
//    private InetAddress lastResponseDestination = null;
//
//    /**
//     * Redefine this to be notified of exceptions on the listen thread.
//     * Default behaviour is to print to stdout. Can be left as null for
//     * no-op
//     */
//    public ExceptionHandler rxExceptionHandler = new ExceptionHandler();
//
//    private Thread bcastListen = new Thread(UDiscover.class.getSimpleName()
//            + "_BroadcastListen") {
//        @Override
//        public void run() {
//            try {
//                byte[] buffy = new byte[5];
//                DatagramPacket rx = new DatagramPacket(buffy, buffy.length);
//
//                while (!shouldStop) {
//                    try {
//                        buffy[0] = 0;
//
//                        bcastSocket.receive(rx);
//
//                        int recData = decode(buffy, 1);
//
//                        if (buffy[0] == QUERY_PACKET && recData == group) {
//                            byte[] data = new byte[5];
//                            data[0] = RESPONSE_PACKET;
//                            encode(peerData, data, 1);
//
//                            DatagramPacket tx =
//                                    new DatagramPacket(data, data.length, rx.getAddress(), port);
//
//                            lastResponseDestination = rx.getAddress();
//
//                            bcastSocket.send(tx);
//                        } else if (buffy[0] == RESPONSE_PACKET) {
//                            if (responseList != null && !rx.getAddress().equals(lastResponseDestination)) {
//                                synchronized (responseList) {
//                                    responseList.add(new Peer(rx.getAddress(), recData));
//                                }
//                            }
//                        }
//                    } catch (SocketException se) {
//                        // someone may have called disconnect()
//                    }
//                }
//
//                bcastSocket.disconnect();
//                bcastSocket.close();
//            } catch (Exception e) {
//                if (rxExceptionHandler != null) {
//                    rxExceptionHandler.handle(e);
//                }
//            }
//        }
//
//    };
//
//
//    public static final int UDPPORT = 9090;
//    // delay in milliseconds between broadcasts
//    public static final int UDPINTERVAL = 1000;
//    public static final InetAddress broadcastAddress;
//
//    static {
//        // create broadcast address object refrencing the local machine's
//        // broadcasting address for use with UDP
//        broadcastAddress = getBroadcastAddress();
//        assert (broadcastAddress != null);
//    }
//
//    private static InetAddress getBroadcastAddress() {
//        ArrayList<NetworkInterface> interfaces = new ArrayList<>();
//        try {
//            interfaces.addAll(Collections.list(
//                    NetworkInterface.getNetworkInterfaces()));
//        } catch (SocketException ex) {
//            ex.printStackTrace();
//            return null;
//        }
//        for (NetworkInterface nic : interfaces) {
//            try {
//                if (!nic.isUp() || nic.isLoopback())
//                    continue;
//            } catch (SocketException ex) {
//                continue;
//            }
//            for (InterfaceAddress ia : nic.getInterfaceAddresses()) {
//                if (ia == null || ia.getBroadcast() == null)
//                    continue;
//                return ia.getBroadcast();
//            }
//        }
//        return null;
//    }
//
//
//    private final Runnable receiver;
//    private final Runnable sender;
//    private boolean run = true;
//
//    public UDiscover() {
//        receiver = new Runnable() {
//            public static final int TIMEOUT = 2000;
//
//            public void run() {
//                byte data[] = new byte[0];
//                DatagramSocket socket = null;
//                try {
//
//                    socket = new DatagramSocket(UDPPORT);
//
//                    socket.setBroadcast(true);
//                    socket.setSoTimeout(TIMEOUT);
//                } catch (SocketException ex) {
//                    ex.printStackTrace();
//                    return;
//                }
//                DatagramPacket packet = new DatagramPacket(data, data.length);
//                while (run) {
//                    try {
//                        socket.receive(packet);
//                    } catch (IOException ex) {
//                        ex.printStackTrace();
//                        break;
//                    }
//
//                    pong(packet);
//                    //parent.newAddress(packet.getAddress());
//                }
//            }
//        };
//        sender = new Runnable() {
//            public void run() {
//                byte data[] = new byte[0];
//                DatagramSocket socket = null;
//                try {
//                    socket = new DatagramSocket();
//                } catch (SocketException ex) {
//                    ex.printStackTrace();
//                    return;
//                }
//                DatagramPacket packet = new DatagramPacket(
//                        data,
//                        data.length,
//                        broadcastAddress,
//                        UDPPORT);
//                while (run) {
//                    try {
//                        socket.send(packet);
//                        Thread.sleep(UDPINTERVAL);
//                    } catch (IOException ex) {
//                        ex.printStackTrace();
//                        //parent.quit();
//                        break;
//                    } catch (InterruptedException ex) {
//                        ex.printStackTrace();
//                        //parent.quit();
//                        break;
//                    }
//                }
//            }
//        };
//        new Thread(receiver).start();
//        new Thread(sender).start();
//    }
//
//    private void pong(DatagramPacket packet) {
//        System.err.println("GOT: " + packet);
//    }
//
//    public void quit() {
//        run = false;
//    }
//
//    /**
//     * Constructs a UDP broadcast-based peer
//     *
//     * @param group The identifier shared by the peers that will be
//     *              discovered.
//     * @param port  a valid port, i.e.: in the range 1025 to 65535
//     *              inclusive
//     * @throws IOException
//     */
//    public void UDiscoverX(int group, int port) throws IOException {
//        this.group = group;
//        this.port = port;
//
//        bcastSocket = new DatagramSocket(port);
//        //bcastSocket.setBroadcast(true);
//
//        bcastListen.setDaemon(true);
//        bcastListen.start();
//    }
//
//    /**
//     * Signals this {@link UDiscover} to shut down. This call will
//     * block until everything's timed out and closed etc.
//     */
//    public void disconnect() {
//        shouldStop = true;
//
//        bcastSocket.close();
//        bcastSocket.disconnect();
//
//        try {
//            bcastListen.join();
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }
//    }
//
////    /**
////     * Queries the network and finds the addresses of other peers in
////     * the same group
////     *
////     * @param timeout  How long to wait for responses, in milliseconds. Call
////     *                 will block for this long, although you can
////     *                 {@link Thread#interrupt()} to cut the wait short
////     * @param peerType The type flag of the peers to look for
////     * @return The addresses of other peers in the group
////     * @throws IOException If something goes wrong when sending the query packet
////     */
////    public Peer[] getPeers(int timeout, byte peerType) throws IOException {
////        responseList = new ArrayList<Peer>();
////
////        // send query byte, appended with the group id
////        byte[] data = new byte[5];
////        data[0] = QUERY_PACKET;
////        encode(group, data, 1);
////
////        DatagramPacket tx = new DatagramPacket(data, data.length, broadcastAddress);
////
////        bcastSocket.send(tx);
////
////        // wait for the listen thread to do its thing
////        try {
////            Thread.sleep(timeout);
////        } catch (InterruptedException e) {
////        }
////
////        Peer[] peers;
////        synchronized (responseList) {
////            peers = responseList.toArray(new Peer[responseList.size()]);
////        }
////
////        responseList = null;
////
////        return peers;
////    }
//
//    /**
//     * Record of a peer
//     *
//     * @author ryanm
//     */
//    public static class Peer {
//        /**
//         * The ip of the peer
//         */
//        public final InetAddress ip;
//
//        /**
//         * The data of the peer
//         */
//        public final int data;
//
//        private Peer(InetAddress ip, int data) {
//            this.ip = ip;
//            this.data = data;
//        }
//
//        @Override
//        public String toString() {
//            return ip.getHostAddress() + " " + data;
//        }
//    }
//
//    /**
//     * Handles an exception.
//     *
//     * @author ryanm
//     */
//    public class ExceptionHandler {
//        /**
//         * Called whenever an exception is thrown from the listen
//         * thread. The listen thread should now be dead
//         *
//         * @param e
//         */
//        public void handle(Exception e) {
//            e.printStackTrace();
//        }
//    }
//
//    /**
//     * @param args
//     */
//    public static void main(String[] args) {
//        new UDiscover();
//    }
//
////    public static void main2(String[] args) {
////        try {
////
////            UDiscover mp = new UDiscover(12345, 10000 + new Random().nextInt(1000));
////
////            boolean stop = false;
////
////            BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
////
////            while (!stop) {
////                System.out.println("enter \"q\" to quit, or anything else to query peers");
////                String s = br.readLine();
////
////                if (s.equals("q")) {
////                    System.out.print("Closing down...");
////                    mp.disconnect();
////                    System.out.println(" done");
////                    stop = true;
////                } else {
////                    System.out.println("Querying");
////
////                    Peer[] peers = mp.getPeers(100, (byte) 0);
////
////                    System.out.println(peers.length + " peers found");
////                    for (Peer p : peers) {
////                        System.out.println("\t" + p);
////                    }
////                }
////            }
////        } catch (Exception e) {
////            e.printStackTrace();
////        }
////    }
//
//    private static int decode(byte[] b, int index) {
//        int i = 0;
//
//        i |= b[index] << 24;
//        i |= b[index + 1] << 16;
//        i |= b[index + 2] << 8;
//        i |= b[index + 3];
//
//        return i;
//    }
//
//    private static void encode(int i, byte[] b, int index) {
//        b[index] = (byte) (i >> 24 & 0xff);
//        b[index + 1] = (byte) (i >> 16 & 0xff);
//        b[index + 2] = (byte) (i >> 8 & 0xff);
//        b[index + 3] = (byte) (i & 0xff);
//    }
//}
//
//
//
//
//
//
//
//
