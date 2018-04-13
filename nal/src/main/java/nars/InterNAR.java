package nars;

import jcog.Util;
import jcog.net.UDPeer;
import jcog.util.TriConsumer;
import nars.bag.leak.TaskLeak;
import nars.control.CauseChannel;
import nars.control.TaskService;
import nars.task.ActiveQuestionTask;
import nars.task.ITask;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

import static jcog.net.UDPeer.Command.TELL;

/**
 * InterNAR P2P Network Interface for a NAR
 */
public class InterNAR extends TaskService implements TriConsumer<NAR, ActiveQuestionTask, Task> {

    public static final Logger logger = LoggerFactory.getLogger(InterNAR.class);

    public final TaskLeak buffer;
    final CauseChannel<ITask> recv;
    final MyUDPeer peer;


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

    /**
     * @param nar
     * @param outRate  output rate in tasks per cycle, some value > 0, ammortize over multiple cycles with a fraction < 1
     * @param port
     * @param discover
     * @throws SocketException
     * @throws UnknownHostException
     */
    public InterNAR(NAR nar, float outRate, int port, boolean discover) {
        super(nar);

        try {
            peer = new MyUDPeer(port, discover);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        recv = nar.newCauseChannel(this);

        buffer = new TaskLeak(256, outRate, nar) {

            @Override
            public float value() {
                return recv.value();
            }

            @Override
            protected int next(NAR nar, int work) {
                if (!peer.connected())
                    return -1;

                return super.next(nar, work);
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
        };
    }

    private static byte ttl(Task x) {
        return (byte) (1 + Util.lerp(x.priElseZero() /* * (1f + x.qua())*/, 2, 5));
    }

    @Override
    public void accept(NAR nar, Task t) {
        buffer.accept(t);
    }


    @Override
    protected void stopping(NAR nar) {
        peer.stop();
    }

    InterNAR pri(float priFactor) {
        recv.preAmp = priFactor;
        return InterNAR.this;
    }

    public void ping(InetSocketAddress x) {
        peer.ping(x);
    }

    //        @Override
//        public int send(Msg o, float pri, boolean onlyIfNotSeen) {
//
//            int sent = super.send(o, pri, onlyIfNotSeen);
//            if (sent > 0)
//                System.out.println(me + " SEND " + o + " to " + sent);
//
//            return sent;
//        }

    @Override
    public void accept(NAR nar, ActiveQuestionTask question, Task answer) {
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
        peer.runFPS(fps);
    }

    public InetSocketAddress addr() {
        return peer.addr;
    }

    void receive(UDPeer.UDProfile from, UDPeer.Msg m, Task x) {
        if (x.isQuestionOrQuest()) {
            //reconstruct a question task with an onAnswered handler to reply with answers to the sender
            x = new ActiveQuestionTask(x, 8, nar, (q, a) -> accept(nar, q, a));
            x.meta("UDPeer", m);
        }
        x.budget(nar);

        //System.out.println(me + " RECV " + x + " " + Arrays.toString(x.stamp()) + " from " + m.origin());
        logger.debug("recv {} from {}", x, from);
        recv.input(x);
    }

    class MyUDPeer extends UDPeer {

        MyUDPeer(int port, boolean discovery) throws IOException {
            super(port, discovery);
        }

        @Override
        protected void receive(UDProfile from, Msg m) {

            Task x = IO.taskFromBytes(m.data());
            if (x == null) {
                logger.warn("received invalid task {} {}", from, m);
                return;
            }

            InterNAR.this.receive(from, m, x);
        }
    }
}
