package nars.game;

import jcog.func.IntIntToObjectFunction;
import jcog.signal.wave2d.Bitmap2D;
import nars.$;
import nars.NAR;
import nars.NARS;
import nars.op.AutoConceptualizer;
import nars.sensor.Bitmap2DSensor;
import nars.term.Term;
import org.junit.jupiter.api.Test;

public class AutoConceptualizerTest extends CameraSensorTest {

    @Test
    public void test1() {
        NAR n = NARS.tmp();
        tmp = new MyGame();
        n.add(tmp);

        int H = 4;
        int W = 4;
        Bitmap2DSensor c = new Bitmap2DSensor<>(n, new Bitmap2D() {
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
        }, new IntIntToObjectFunction<Term>() {
            @Override
            public Term apply(int x, int y) {
                return $.INSTANCE.p(x, y);
            }
        });

        AutoConceptualizer ac = new AutoConceptualizer($.INSTANCE.inh(c.id, "auto"),
                c.concepts.order() /* HACK */, true, 2, n);



        n.log();
        for (int i =0; i< 155; i++) {
            next(n, ac);
            n.run(1);
        }


    }
}