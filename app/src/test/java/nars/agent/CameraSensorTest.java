package nars.agent;

import jcog.signal.wave2d.ArrayBitmap2D;
import nars.$;
import nars.NAR;
import nars.NARS;
import nars.concept.TaskConcept;
import nars.concept.sensor.AbstractSensor;
import nars.sensor.Bitmap2DSensor;
import nars.term.Term;
import nars.truth.Truth;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static nars.$.$$;
import static org.junit.jupiter.api.Assertions.*;


public class CameraSensorTest {

    @Test
    public void test1() {
        int w = 2, h = 2;

        NAR n = NARS.tmp();
        float[][] f = new float[w][h];

        Bitmap2DSensor c = new Bitmap2DSensor($.the("x"),
                new ArrayBitmap2D(f), n);


        n.log();

        //check for cause applied
        final int[] causesDetected = {0};
        Term aPixel = $$("x(0,0)");
        short[] cause = new short[] { c.in.id };
        n.onTask(t -> {
           if (t.term().equals(aPixel)) {
               if (t.why().length <= 1) //ignore revisions
                 assertArrayEquals(cause, t.why());
               causesDetected[0]++;
           }
        });


        System.out.println("all 0");
        for (int i = 0; i < w; i++) for (int j = 0; j < h; j++) f[i][j] = 0f;
        next(n, c);
        n.run(1);
        assertEquals(c, f, n.time(),n);


        n.run(3);


        System.out.println("all 1");
        for (int i = 0; i < w; i++) for (int j = 0; j < h; j++) f[i][j] = 1f;
        next(n, c);
        n.run(3);  
        assertEquals(c, f, n.time(),n);


        System.out.println("all 0");
        for (int i = 0; i < w; i++) for (int j = 0; j < h; j++) f[i][j] = 0f;
        next(n, c);
        n.run(1);
        assertEquals(c, f, n.time(), n);

        n.run(3); 

        System.out.println("all 1");
        for (int i = 0; i < w; i++) for (int j = 0; j < h; j++) f[i][j] = 1f;
        next(n, c);
        n.run(1);
        assertEquals(c, f, n.time(), n);

        assertTrue(causesDetected[0] > 0);



    }

    MyGame tmp;

    void next(NAR n, AbstractSensor c) {
        if (tmp==null)
             tmp = new MyGame(n);

        tmp.prev(n.time()-1);
        tmp.now(n.time() );
        c.update(tmp);
    }

    static final float tolerance = 0.47f;

    static void assertEquals(Bitmap2DSensor c, float[][] f, long when, NAR n) {

        for (int i = 0; i < c.width; i++) {
            for (int j = 0; j < c.height; j++) {
                TaskConcept p = c.get(i, j);
                Truth t = n.beliefTruth(p, when);
                if (t == null || Math.abs(f[i][j] - t.freq()) > tolerance) {
                    System.err.println("pixel " + p + " incorrect @ t=" + n.time() + " for " + when);
                    n.beliefTruth(p, when);
                    p.beliefs().print(System.out);
                }
                assertNotNull(t, ()->p.term + " is null");
                Assertions.assertEquals(f[i][j], t.freq(), tolerance, ()->p + " has inaccurate result @ t=" + n.time());
            }
        }
    }

    static class MyGame extends Game {
        public MyGame(NAR n) {
            super("tmp", n);
        }
        public void prev(long l) {
            prev = l;
        }
        public void now(long l) {
            now = l;
        }
    }

}