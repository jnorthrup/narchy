package nars;

import jcog.Util;
import jcog.func.TriConsumer;
import jcog.math.FloatRange;
import jcog.net.UDPeer;
import nars.attention.What;
import nars.control.NARPart;
import nars.control.channel.CauseChannel;
import nars.io.IO;
import nars.io.TaskIO;
import nars.op.TaskLeak;
import nars.task.ActiveQuestionTask;
import nars.time.clock.RealTime;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

import static jcog.net.UDPeer.Command.TELL;

/**
 * InterNARchy / InterNARS P2P Network Interface for a NAR
 */
public class InterNAR extends NARPart implements TriConsumer<NAR, ActiveQuestionTask, Task> {

    public static final Logger logger = LoggerFactory.getLogger(InterNAR.class);
    private final int port;
    private final boolean discover; //TODO AtomicBoolean for GUI control etc

    static final int outCapacity = 128; //TODO abstract

    private final CauseChannel<Task> recv;
    private final TaskLeak send;


    public final UDPeer peer;


    public final FloatRange incomingPriMult = new FloatRange(1f, 0, 2f);

    final What what;

    private float peerFPS = 1;

    /**
     * @param port
     * @param nar
     * @throws SocketException
     * @throws UnknownHostException
     */
    public InterNAR(int port, What w) {
        this(port, true, w);
    }

    public InterNAR(What w) {
        this(0, w);
    }

    @Deprecated public InterNAR(NAR n) {
        this(n.what());
    }


    /**
     * @param port
     * @param discover
     * @param nar
     * @throws SocketException
     * @throws UnknownHostException
     */
    public InterNAR(int port, boolean discover, What w) {
        super();

        NAR nar = w.nar;
        UDPeer p;
        try {
            p = new UDPeer(port, discover);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        this.peer = p;

        this.what = w;
        /*this.nar = nar;       should be set in start()  */assert(nar.time instanceof RealTime.MS );
        this.port = port;
        this.discover = discover;


        this.send = new TaskLeak(outCapacity, nar) {
            @Override
            public boolean filter(Task next) {
                if (next.isCommand() || !peer.connected() || next.stamp().length == 0 /* don't share assumptions */)
                    return false;

                return !next.isBeliefOrGoal() || !(next.conf() < nar.confMin.floatValue());

            }
            @Override
            public float value() {
                return recv.value();
            }

            @Override
            protected float leak(Task next, What what) {
                return InterNAR.this.send(next);
            }
        };


        this.recv = nar.newChannel(this);


        add(send);
        finallyRun(peer.receive.on(this::receive));

        nar.start(this);

    }

    public InterNAR fps(float fps) {
        peerFPS = fps;
        if (peer.isRunning())
            peer.setFPS(fps);
        return this;
    }

    @Override
    protected void starting(NAR nar) {
        peer.setFPS(peerFPS);
    }

    @Override
    protected void stopping(NAR nar) {
        peer.stop();
    }


    //    @Override
//    protected void starting(NAR nar) {
//
//
//        //HACK temporary:
//        nar.addOp1("ping", (term, nn)->{
//            try {
//                String s = Texts.unquote(term.toString());
//                String[] addressPort = s.split(":");
//                String address = addressPort[0];
//                int pport = Texts.i(addressPort[1]);
//
//                InetSocketAddress a = new InetSocketAddress(address, pport);
//                logger.info("ping {}", a);
//                ping(a);
//            } catch (Throwable tt) {
//                logger.error("ping {}", tt);
//            }
//        });
//
//
//
//    }




    protected float send(Task next) {


        logger.debug("{} send {}", peer, next);

        @Nullable byte[] msg = IO.taskToBytes(next);
        assert (msg != null);
        if (peer.tellSome(msg, ttl(next), true) > 0) {
            return 1;
        }


        return 0;
    }



    private static byte ttl(Task x) {
        return (byte) (1 + Util.lerp(x.priElseZero() /* * (1f + x.qua())*/, 2, 5));
    }



    protected void receive(UDPeer.MsgReceived m) {

        try {
            Task x = TaskIO.bytesToTask(m.data());
//            if (x.isQuestionOrQuest()) {
//
//                //expensive use a lighter weight common answer handler
//                x = new ActiveQuestionTask(x, 8, nar, (q, a) -> accept(nar, q, a));
//                ((NALTask.NALTaskX)x).meta("UDPeer", m);
//            }
            x.budget(nar);

            x.priMult(incomingPriMult.floatValue());

            if (logger.isDebugEnabled())
                logger.debug("recv {} from {}", x, m.from);

            recv.accept(x, what);
        } catch (Exception e) {
            logger.warn("recv {} from {}: {}", m, m.from, e.getMessage());
            return;
        }
    }







    public void ping(InetSocketAddress x) {
        peer.ping(x);
    }

    









    @Override
    public void accept(NAR NAR, ActiveQuestionTask question, Task answer) {
        UDPeer.Msg q = question.meta("UDPeer");
        if (q == null)
            return;

        @Nullable byte[] a = IO.taskToBytes(answer);
        if (a != null) {
            UDPeer.Msg aa = new UDPeer.Msg(TELL.id, ttl(answer), peer.me, null, a);
            if (!peer.seen(aa, 1f))
                peer.send(aa, q.origin());
        }


    }


    public InetSocketAddress addr() {
        return peer.addr;
    }


}
