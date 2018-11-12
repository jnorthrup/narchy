package nars;

import jcog.Texts;
import jcog.Util;
import jcog.math.FloatRange;
import jcog.net.UDPeer;
import jcog.util.TriConsumer;
import nars.bag.leak.TaskLeak;
import nars.control.NARService;
import nars.control.channel.CauseChannel;
import nars.task.ActiveQuestionTask;
import nars.task.ITask;
import nars.task.NALTask;
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
public class InterNAR extends NARService implements TriConsumer<NAR, ActiveQuestionTask, Task> {

    public static final Logger logger = LoggerFactory.getLogger(InterNAR.class);
    private final int port;
    private final boolean discover; //TODO AtomicBoolean for GUI control etc
    private final NAR nar;

    protected final CauseChannel<ITask> recv;
    private final TaskLeak send;


    public final UDPeer peer;


    public final FloatRange incomingPriMult = new FloatRange(1f, 0, 2f);

    /**
     * @param nar
     * @param port
     * @throws SocketException
     * @throws UnknownHostException
     */
    public InterNAR(NAR nar, int port) {
        this(nar, port, true);
    }
    public InterNAR(NAR nar) {
        this(nar, 0);
    }

    /**
     * @param nar
     * @param port
     * @param discover
     * @throws SocketException
     * @throws UnknownHostException
     */
    public InterNAR(NAR nar, int port, boolean discover) {
        UDPeer p;
        try {
            p = new UDPeer(port, discover);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        this.peer = p;

        this.nar = nar;         assert(nar.time instanceof RealTime.MS );
        this.port = port;
        this.discover = discover;

        this.send = new TaskLeak(256, null) {
            @Override
            public boolean filter(Task next) {
                if (next.isCommand() || !peer.connected())
                    return false;

                return super.filter(next);
            }
            @Override
            public float value() {
                return recv.value();
            }

            @Override
            protected float leak(Task next) {
                return InterNAR.this.leak(next);
            }
        };


        this.recv = nar.newChannel(this);



        nar.on(this);

    }

    @Override
    protected void starting(NAR nar) {

        on(peer.receive.on(this::receive));

        //HACK temporary:
        nar.onOp1("ping", (term,nn)->{
            try {
                String s = Texts.unquote(term.toString());
                String[] addressPort = s.split(":");
                String address = addressPort[0];
                int pport = Texts.i(addressPort[1]);

                InetSocketAddress a = new InetSocketAddress(address, pport);
                logger.info("ping {}", a);
                ping(a);
            } catch (Throwable tt) {
                logger.error("ping {}", tt);
            }
        });

        nar.on(send);


    }

    @Override
    protected void stopping(NAR nar) {

        nar.off(send);

        peer.stop();
    }



    protected float leak(Task next) {


        logger.debug("{} share {}", peer, next);

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
            Task x = IO.bytesToTask(m.data());
            if (x.isQuestionOrQuest()) {

                x = new ActiveQuestionTask(x, 8, nar, (q, a) -> accept(nar, q, a));
                ((NALTask.NALTaskX)x).meta("UDPeer", m);
            }
            x.budget(nar);

            x.priMult(incomingPriMult.floatValue());

            logger.debug("recv {} from {}", x, m.from);
            recv.input(x);
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

    public void runFPS(float fps) {
        //nar.runLater(()->{
            if (peer!=null)
                peer.setFPS(fps);
            else
                logger.error("did not start InterNARS TODO"); //HACK
        //});
    }

    public InetSocketAddress addr() {
        return peer.addr;
    }


}
