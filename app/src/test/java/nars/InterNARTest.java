package nars;

import jcog.Util;
import nars.attention.What;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static java.util.stream.Collectors.toList;
import static nars.$.$$;
import static nars.Op.BELIEF;
import static nars.Op.QUESTION;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Created by me on 7/8/16.
 */
@Execution(ExecutionMode.SAME_THREAD)
public class InterNARTest {

    @ParameterizedTest
    @ValueSource(ints={1,0})
    public void testDiscoverDoesntSelfConnect(int pingSelf) {
        NAR n = NARS.realtime(1f).get();
        InterNAR x = new InterNAR(n);
        x.fps(8f);

        n.synch();
        if (pingSelf==1) {
            x.ping(x.addr());
        }
        for (int i = 0; i < 8; i++) {
            assertFalse(x.peer.them.contains(x.peer.me));
            assertFalse(x.peer.connected(), new Supplier<String>() {
                @Override
                public String get() {
                    return x.peer.them.stream().collect(toList()).toString();
                }
            });
            Util.sleepMS(100);
        }

        n.delete();
    }

    static void testAB(BiConsumer<NAR, NAR> beforeConnect, BiConsumer<NAR, NAR> afterConnect) {

        final float NET_FPS = 20f;
        final float NAR_FPS = NET_FPS * 2;

        int preCycles = 1;

        NAR a = NARS.realtime(NAR_FPS).withNAL(1, 1).get();
        NAR b = NARS.realtime(NAR_FPS).withNAL(1, 1).get();

        What aa = a.what();
        What bb = b.what();

        for (int i = 0; i < preCycles; i++) {
            a.run(1);
            b.run(1);
        }

        beforeConnect.accept(a, b);

        InterNAR ai = new InterNAR(0, false, aa);
        InterNAR bi = new InterNAR(0, false, bb);

        ai.fps(NET_FPS);
        bi.fps(NET_FPS);

        a.synch();
        b.synch();

        assertTrue(ai.id != bi.id);
        assertTrue(!ai.addr().equals(bi.addr()));
        assertTrue(!ai.peer.name().equals(bi.peer.name()));


        ai.peer.ping(bi.peer);

        boolean connected = false;
        final int CONNECT_INTERVAL_MS = 30;
        final int MAX_CONNECT_INTERVALS = 100;
        for (int i = 0; !connected && i < MAX_CONNECT_INTERVALS; i++) {
            Util.sleepMS(CONNECT_INTERVAL_MS);
            connected = ai.peer.connected() && bi.peer.connected();
        }
        assertTrue(connected);


        System.out.println("connected. interacting...");

        afterConnect.accept(a, b);

        /* init */
        int duringCycles = 128;
        for (int i = 0; i < duringCycles; i++) {
            a.run(1);
            b.run(1);
        }

        System.out.println("disconnecting..");

        /* init */
        int postCycles = 128;
        for (int i = 0; i < postCycles; i++) {
            a.run(1);
            b.run(1);
        }


        a.delete();
        b.delete();

    }

    /**
     * direct question answering
     */
    @Test
    public void testInterNAR1() {
        AtomicBoolean aRecvQuestionFromB = new AtomicBoolean();

        testAB(new BiConsumer<NAR, NAR>() {
            @Override
            public void accept(NAR a, NAR b) {

                a.onTask(new Consumer<Task>() {
                    @Override
                    public void accept(Task task) {
                        if (task.toString().contains("(?1-->y)"))
                            aRecvQuestionFromB.set(true);
                    }
                }, QUESTION);


                try {
                    b.believe("(X --> y)");
                } catch (Narsese.NarseseException e) {
                    fail(e);
                }

            }
        }, new BiConsumer<NAR, NAR>() {
            @Override
            public void accept(NAR a, NAR b) {

                try {
                    a.input("(?x --> y)?");
                } catch (Narsese.NarseseException e) {
                    e.printStackTrace();
                }

            }
        });

        assertTrue(aRecvQuestionFromB.get());

    }

    /**
     * cooperative solving
     */
    @Test
    public void testInterNAR2() {

        AtomicBoolean recv = new AtomicBoolean();

        testAB(new BiConsumer<NAR, NAR>() {
            @Override
            public void accept(NAR a, NAR b) {

                a.termVolMax.set(3);
                b.termVolMax.set(3);

                b.onTask(new Consumer<Task>() {
                    @Override
                    public void accept(Task bt) {
                        if (bt.isBelief() && bt.term().toString().contains("(a-->d)"))
                            recv.set(true);
                    }
                }, BELIEF);

            }
        }, new BiConsumer<NAR, NAR>() {
            @Override
            public void accept(NAR a, NAR b) {


                a.believe($$("(b --> c)"));
                b.believe($$("(a --> b)"));
                b.believe($$("(c --> d)"));
                b.question($$("(a --> d)"));

            }
        });

        assertTrue(recv.get());

    }

}