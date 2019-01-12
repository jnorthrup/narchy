package nars.agent;

import jcog.signal.wave2d.Bitmap2D;
import nars.$;
import nars.NAR;
import nars.NARS;
import nars.op.AutoConceptualizer;
import nars.sensor.Bitmap2DSensor;
import org.junit.jupiter.api.Test;

public class AutoConceptualizerTest {

    @Test
    public void test1() {
        int W = 4, H = 4;
        NAR n = NARS.tmp();
        Bitmap2DSensor c = new Bitmap2DSensor<>((x, y) -> $.p(x,y), new Bitmap2D() {
            @Override
            public int width() {
                return W;
            }

            @Override
            public int height() {
                return H;
            }

            @Override
            public float brightness(int xx, int yy) {
                return (((xx + yy + n.time())) % 2) > 0 ? 1f : 0f;
            }
        }, n);

        AutoConceptualizer ac = new AutoConceptualizer(
                c.concepts.order() /* HACK */, true, 2, n);



        n.log();
        for (int i =0; i< 155; i++) {
            CameraSensorTest.next(n, ac);
            n.run(1);
        }


    }
}