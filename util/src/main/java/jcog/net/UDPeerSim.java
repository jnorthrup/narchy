package jcog.net;

import jcog.data.atomic.AtomicFloat;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Simulates network of peers for experiments
 */
public class UDPeerSim {


    public final MyUDPeer[] peer;


    private final Random random = new Random();
    private final Timer sim = new Timer();


    public UDPeerSim(int population) throws IOException {

        peer = new MyUDPeer[population];
        var port = 10000;
        for (var i = 0; i < population; i++)
            peer[i] = new MyUDPeer(port, i);

    }


    public void tellSome(int from, int payloadLength, int ttl) {
        var msg = new byte[payloadLength];
        random.nextBytes(msg);
        peer[from].tellSome(msg , (byte)ttl );
    }






    public void onTell(UDPeer.UDProfile sender, UDPeer recver, UDPeer.Msg msg) {

    }

    protected long delay(InetSocketAddress from, InetSocketAddress to, int length) {
        return 0;
    }

    public void pingRing(int depth) {
        var p = peer.length;
        for (var i = 0; i < p; i++) {
            for (var d = 0; d < Math.min(p-1, depth); d++) {
                peer[i].ping(peer[(i + 1 + d) % p].port());
            }
        }
    }
    public void pingRandom(int num) {
        var p = peer.length;

            for (var d = 0; d < num; d++) {
                var i = random.nextInt(p);
                var j = random.nextInt(p);
                if (i!=j)
                    peer[i].ping(peer[j].port());
            }

    }

    public static void main(String[] args) throws IOException {
        new UDPeerSim(5) {
            @Override
            protected long delay(InetSocketAddress from, InetSocketAddress to, int length) {
                return 25 + Math.abs(from.getPort() - to.getPort()) * 50;
            }
        };
    }


    static final int delayThreshold = 1;

    public void start(float fps) {
        for (UDPeer p : peer)
            p.setFPS(fps);
    }

    public class MyUDPeer extends UDPeer {

        final Random random = new Random();
        public final Number packetLossRate = new AtomicFloat(0.05f);

        public MyUDPeer(int port, int i) throws IOException {
            super(port + i);
        }

        public void actuallySend(Msg o, InetSocketAddress to) {
            super.send(o, to);
        }

        @Override
        public void send(Msg o, InetSocketAddress to) {
            
            if (random.nextFloat() < packetLossRate.floatValue())
                return;

            var d = delay(addr, to, o.length());
            if (d > delayThreshold) {
                sim.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        actuallySend(o, to);
                    }
                }, d);
            } else {
                actuallySend(o, to);
            }
        }

        @Override
        protected void receive(@Nullable UDPeer.UDProfile sender, @NotNull UDPeer.Msg m) {
            UDPeerSim.this.onTell(sender, this, m);
        }
    }


}
