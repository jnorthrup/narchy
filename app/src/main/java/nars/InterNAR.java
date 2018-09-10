package nars;

import jcog.Texts;
import jcog.Util;
import jcog.math.FloatRange;
import jcog.net.UDPeer;
import jcog.util.TriConsumer;
import nars.bag.leak.TaskLeak;
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
import java.util.function.BooleanSupplier;

import static jcog.net.UDPeer.Command.TELL;

/**
 * InterNARchy / InterNARS P2P Network Interface for a NAR
 */
public class InterNAR extends TaskLeak implements TriConsumer<NAR, ActiveQuestionTask, Task> {

    public static final Logger logger = LoggerFactory.getLogger(InterNAR.class);



    public final UDPeer peer;
    protected final CauseChannel<ITask> recv;

    public final FloatRange incomingPriMult = new FloatRange(1f, 0, 2f);

    /**
     * @param nar
     * @param outRate output rate in tasks per cycle, some value > 0, ammortize over multiple cycles with a fraction < 1
     * @param port
     * @throws SocketException
     * @throws UnknownHostException
     */
    public InterNAR(NAR nar, float outRate, int port) {
        this(nar, outRate, port, true);
    }
    public InterNAR(NAR nar, float outRate) {
        this(nar, outRate, 0);
    }

    /**
     * @param nar
     * @param outRate  output rate in tasks per cycle, some value > 0, ammortize over multiple cycles with a fraction < 1
     * @param port
     * @param discover
     * @throws SocketException
     * @throws UnknownHostException
     */
    public InterNAR(NAR nar, float outRate, int port, boolean discover) {
        super(256, nar);

        this.nar = nar;

        assert(nar.time instanceof RealTime.MS && ((RealTime.MS)nar.time).t0 !=0 );
        recv = nar.newChannel(this);

        try {
            peer = new UDPeer(port, discover);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        ons.add(peer.receive.on(this::receive));

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

    }
    @Override
    public float value() {
        return recv.value();
    }

    @Override
    protected void next(NAR nar, BooleanSupplier kontinue) {
        if (enabled())
            super.next(nar, kontinue);
    }

    private boolean enabled() {
        return peer != null && peer.connected();
    }

    @Override
    protected float leak(Task next) {


        logger.debug("{} share {}", peer, next);

        @Nullable byte[] msg = IO.taskToBytes(next);
        assert (msg != null);
        if (peer.tellSome(msg, ttl(next), true) > 0) {
            return 1;
        }


        return 0;
    }

    @Override
    public boolean preFilter(Task next) {
        if (next.isCommand() || !peer.connected())
            return false;

        return super.preFilter(next);
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
            logger.warn("receive {} from {}: {}", m, m.from, e.getMessage());
            return;
        }
    }

    @Override
    protected void stopping(NAR nar) {
        peer.stop();
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
        nar.runLater(()->{
            peer.setFPS(fps);
        });
    }

    public InetSocketAddress addr() {
        return peer.addr;
    }


}
