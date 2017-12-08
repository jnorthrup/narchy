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

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

import static jcog.net.UDPeer.Command.TELL;

/**
 * InterNAR P2P Network Interface for a NAR
 */
public class InterNAR extends TaskService implements TriConsumer<NAR, ActiveQuestionTask, Task> {

    //public static final Logger logger = LoggerFactory.getLogger(InterNAR.class);

    public final TaskLeak buffer;
    final CauseChannel<ITask> recv;
    public MyUDPeer peer;


    @Override
    public void accept(NAR nar, Task t) {
        buffer.accept(nar, t);
    }

    @Override
    protected void start(NAR nar)  {
        super.start(nar);
        try {
            peer = new MyUDPeer(nar, 0, true);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected synchronized void stop(NAR nar)  {
        super.stop(nar);
        if (peer!=null) {
            peer.stop();
            peer = null;
        }
    }


    InterNAR pri(float priFactor) {
        recv.preAmp = priFactor;
        return InterNAR.this;
    }

    /**
     * @param nar
     * @param outRate output rate in tasks per cycle, some value > 0, ammortize over multiple cycles with a fraction < 1
     * @param port
     * @throws SocketException
     * @throws UnknownHostException
     */
    public InterNAR(NAR nar, float outRate, int port) throws IOException {
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
    public InterNAR(NAR nar, float outRate, int port, boolean discover) throws IOException {
        super(nar);

        peer = new MyUDPeer(nar, port, discover);

        recv = nar.newCauseChannel(this);

        buffer = new TaskLeak(256, outRate, nar) {

            @Override
            public float value() {
                return 1;
            }

            @Override
            protected float leak(Task next) {

                if (peer.connected()) {
                    try {
                        //if (x!=null) {
                        @Nullable byte[] msg = IO.taskToBytes(next);
                        if (msg != null) {
                            if (peer.tellSome(msg, ttl(next), true) > 0) {
                                return 1;
                            }
                        }
                        //}
                    } catch (RuntimeException e) {
                        e.printStackTrace();
                    }
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

    public void ping(InetSocketAddress x) {
        peer.ping(x);
    }

    private static byte ttl(Task x) {
        return (byte) (1 + Util.lerp(x.priElseZero() /* * (1f + x.qua())*/, 2, 5));
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

    private class MyUDPeer extends UDPeer {

        private final NAR nar;

        public MyUDPeer(NAR nar, int port, boolean discovery) throws IOException {
            super(port, discovery);
            this.nar = nar;
        }

        @Override
        protected void onTell(UDProfile connected, Msg m) {


            Task x = IO.taskFromBytes(m.data());
            if (x != null) {
                if (x.isQuestOrQuestion()) {
                    //reconstruct a question task with an onAnswered handler to reply with answers to the sender
                    x = new ActiveQuestionTask(x, 8, nar, (q,a)->accept(nar, q, a));
                    x.meta("UDPeer", m);
                }
                x.budget(nar);

                //System.out.println(me + " RECV " + x + " " + Arrays.toString(x.stamp()) + " from " + m.origin());
                logger.debug("recv {} from {}", x, m.origin());
                recv.input(x);
            }
        }
    }
}
