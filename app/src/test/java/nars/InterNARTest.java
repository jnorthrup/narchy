package nars;

import jcog.Util;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;

import static nars.$.$$;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Created by me on 7/8/16.
 */
public class InterNARTest {

    @ParameterizedTest
    @ValueSource(ints={1,0})
    public void testDiscoverDoesntSelfConnect(int pingSelf) {
        NAR a = NARS.realtime(1f).get();
        InterNAR x = new InterNAR(a, 4f);
        a.synch();
        x.runFPS(8f);
        if (pingSelf==1) {
            x.ping(x.addr());
        }
        for (int i = 0; i < 8; i++) {
            assertFalse(x.peer.them.contains(x.peer.me));
            assertFalse(x.peer.connected());
            Util.sleep(100);
        }
        a.stop();
    }

    static synchronized void testAB(BiConsumer<NAR, NAR> beforeConnect, BiConsumer<NAR, NAR> afterConnect) {

        final int MAX_CONNECT_INTERVALS = 10;
        final int CONNECT_INTERVAL_MS = 30;
        int outRate = 8;

        final float NET_FPS = 20f;
        final float NAR_FPS = NET_FPS * 2;
        final int INTERACT_TIME = 700;

        int volMax = 8;

        int preCycles = 1;
        int postCycles = 64;

        NAR a = NARS.realtime(NAR_FPS).withNAL(1, 1).get().named("a");
        NAR b = NARS.realtime(NAR_FPS).withNAL(1, 1).get().named("b");

        a.termVolumeMax.set(volMax);
        b.termVolumeMax.set(volMax);

        beforeConnect.accept(a, b);

        for (int i = 0; i < preCycles; i++) {
            a.run(1);
            b.run(1);
        }


        InterNAR ai = new InterNAR(a, outRate, 0, false) {
            @Override
            protected void starting(NAR nar) {
                super.starting(nar);
                runFPS(NET_FPS);
            }
        };
        InterNAR bi = new InterNAR(b, outRate, 0, false) {

            @Override
            protected void starting(NAR nar) {
                super.starting(nar);
                runFPS(NET_FPS);
            }
        };
        assertTrue(ai.id != bi.id);
        assertTrue(!ai.addr().equals(bi.addr()));
        assertTrue(!ai.peer.name().equals(bi.peer.name()));

        /* init */
        a.run(1);
        b.run(1);

        ai.peer.ping(bi.peer);

        boolean connected = false;
        for (int i = 0; !connected && i < MAX_CONNECT_INTERVALS; i++) {
            Util.sleep(CONNECT_INTERVAL_MS);
            connected = ai.peer.connected() && bi.peer.connected();
        }
        assertTrue(connected);


        System.out.println("connected. interacting...");

        afterConnect.accept(a, b);


        a.startFPS(NAR_FPS);
        b.startFPS(NAR_FPS);

        Util.sleep(INTERACT_TIME);


        a.pause();
        b.pause();

        System.out.println("disconnecting..");

        /* init */
        for (int i = 0; i < postCycles; i++) {
            a.run(1);
            b.run(1);
        }


        a.stop();
        b.stop();

    }

    /**
     * direct question answering
     */
    @Test
    public void testInterNAR1() {
        AtomicBoolean aRecvQuestionFromB = new AtomicBoolean();

        testAB((a, b) -> {

            a.onTask(task -> {
                if (task.toString().contains("(?1-->y)"))
                    aRecvQuestionFromB.set(true);
            });


            try {
                b.believe("(X --> y)");
            } catch (Narsese.NarseseException e) {
                fail(e);
            }

        }, (a, b) -> {

            try {
                a.input("(?x --> y)?");
            } catch (Narsese.NarseseException e) {
                e.printStackTrace();
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

        testAB((a, b) -> {

            b.onTask(bt -> {

                if (bt.isBelief() && bt.toString().contains("(a-->d)"))
                    recv.set(true);
            });

        }, (a, b) -> {


            a.believe($$("(b --> c)"));

            b.believe($$("(a --> b)"));
            b.believe($$("(c --> d)"));
            try {
                b.input(("$1.0 (a --> d)?"));
            } catch (Narsese.NarseseException e) {
                e.printStackTrace();
            }

        });

        assertTrue(recv.get());

    }

}