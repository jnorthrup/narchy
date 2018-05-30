package nars.nar;

import jcog.Util;
import nars.InterNAR;
import nars.NAR;
import nars.NARS;
import nars.Narsese;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;

import static nars.$.$$;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Created by me on 7/8/16.
 */
public class InterNARTest {

    static synchronized void testAB(BiConsumer<NAR, NAR> beforeConnect, BiConsumer<NAR, NAR> afterConnect) {

        final int MAX_CONNECT_INTERVALS = 20;
        final int CONNECT_INTERVAL_MS = 200; 

        final float NET_FPS = 10f;
        final float REASONER_FPS = 20f;
        final int INTERACT_TIME = 1500; 

        int preCycles = 1;
        int postCycles = 100;

        NAR a = NARS.realtime(10f).get().named("a");
        NAR b = NARS.realtime(10f).get().named("b");


        beforeConnect.accept(a, b);

        for (int i = 0; i < preCycles; i++) {
            a.run(1);
            b.run(1);
        }


        InterNAR ai = new InterNAR(a, 1, 0, false) {
            @Override
            protected void starting(NAR nar) {
                runFPS(NET_FPS);
            }
        };
        InterNAR bi = new InterNAR(b, 1, 0, false) {

            @Override
            protected void starting(NAR nar) {
                runFPS(NET_FPS);
            }
        };
        assertTrue(ai.id!=bi.id);
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

        
        a.startFPS(REASONER_FPS);
        b.startFPS(REASONER_FPS);

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

    /** direct question answering */
    @Test public void testInterNAR1() {
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
            b.question($$("(a --> d)"));

        });

        assertTrue(recv.get());

    }

}