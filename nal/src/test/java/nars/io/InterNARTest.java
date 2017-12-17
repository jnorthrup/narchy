package nars.io;

import jcog.Util;
import nars.*;
import nars.time.Tense;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;

import static nars.$.$;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Created by me on 7/8/16.
 */
public class InterNARTest {

    static void testAB(BiConsumer<NAR, NAR> beforeConnect, BiConsumer<NAR, NAR> afterConnect) {

        final int CONNECTION_TIME = 200;
        int preCycles = 1;
        int postCycles = 25;

        Param.ANSWER_REPORTING = false;

        NAR a = NARS.threadSafe();
        a.setSelf("a");

        NAR b = NARS.threadSafe();
        b.setSelf("b");

        try {


            beforeConnect.accept(a, b);

            for (int i = 0; i < preCycles; i++) {
                a.run(1);
                b.run(1);
            }

            InterNAR ai = new InterNAR(a, 10, 0, false) {
                @Override
                protected void start(NAR nar) {
                    super.start(nar);
                    {

                        runFPS(5f);

//                        Util.sleep(CONNECTION_TIME);


                    }
                }
            };
            InterNAR bi = new InterNAR(b, 10, 0, false) {
                @Override
                protected void start(NAR nar) {
                    super.start(nar);
                    {
                        runFPS(5f);

//                        Util.sleep(CONNECTION_TIME);

                        ping(ai.addr());


                    }
                }
            };

            /* init */
            for (int i = 0; i < 1; i++) {
                a.run(1);
                b.run(1);
            }
            Util.sleep(CONNECTION_TIME * 4);

            afterConnect.accept(a, b);

            /* init */
            for (int i = 0; i < postCycles; i++) {
                a.run(1);
                b.run(1);
            }

            ai.off();
            bi.off();
//            a.stop();
//            b.stop();


        } catch (Exception e) {
            e.printStackTrace();
            fail(e.toString());
        }
    }

    @Test
    public void testInterNAR1() {
        AtomicBoolean aRecvQuestionFromB = new AtomicBoolean();

        testAB((a, b) -> {

            a.onTask(tt -> {
                //System.out.println(b + ": " + tt);
                if (tt.toString().contains("(?1-->y)"))
                    aRecvQuestionFromB.set(true);
            });

            try {
                b.believe("(X --> y)");
            } catch (Narsese.NarseseException e) {
                fail(e);
            }


            //a.log();

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


            b.onTask(tt -> {
                //System.out.println(b + ": " + tt);
                if (tt.isBelief() && tt.toString().contains("(a-->d)"))
                    recv.set(true);
            });

        }, (a, b) -> {

            try {
                b.believe("(a --> b)");
                b.believe("(c --> d)");
            } catch (Narsese.NarseseException e) {
                fail(e);
            }

            try {
                a.believe($("(b --> c)"), Tense.Eternal, 1f, 0.9f);
            } catch (Narsese.NarseseException e) {
                e.printStackTrace();
            }

            try {
                b.question("(a --> d)");
            } catch (Narsese.NarseseException e) {
                e.printStackTrace();
            }


        });

        assertTrue(recv.get());

    }

}