package nars.game;

import jcog.Util;
import jcog.math.FloatRange;
import jcog.math.FloatSupplier;
import nars.$;
import nars.NAR;
import nars.game.sensor.Signal;
import org.eclipse.collections.api.block.function.primitive.FloatToFloatFunction;
import org.eclipse.collections.api.block.procedure.primitive.BooleanProcedure;

import static nars.game.GameTime.durs;


/**
 * 1 input x 1 output environment
 */
public class Line1DSimplest extends Game {


    /**
     * the target value
     */
    public final FloatRange i = new FloatRange(0.5f, 0, 1f);
    public final FloatRange o = new FloatRange(0.5f, 0, 1f);

    public final FloatRange speed = new FloatRange(0.25f, 0f, 0.5f);


    /**
     * the current value
     */

    public final Signal in;


    public Line1DSimplest(NAR n) {
        super("", durs(1));

        in = sense(
                $.INSTANCE.the("x"),

                this.i
        );
        sense(
                $.INSTANCE.the("y"),

                this.o
        );


        initBipolar();

        reward(new FloatSupplier() {
            @Override
            public float asFloat() {
                float dist = Math.abs(
                        i.floatValue() -
                                o.floatValue()
                );


                float h = ((1f - dist) - 0.5f) * 2f;

                return h;
            }
        });

    }

    private void initDualToggle() {


        actionPushButton($.INSTANCE.$$("y:up"), new BooleanProcedure() {
            @Override
            public void value(boolean b1) {
                if (b1) {
                    o.set(Util.unitize(o.floatValue() + speed.floatValue()));
                    System.out.println(o);
                }
            }
        });


        actionPushButton($.INSTANCE.$$("y:down"), new BooleanProcedure() {
            @Override
            public void value(boolean b) {
                if (b) {
                    o.set(Util.unitize(o.floatValue() - speed.floatValue()));
                    System.out.println(o);
                }
            }
        });

    }


    private void initDualUnipolar() {
        float[] x = new float[2];
        onFrame(new Runnable() {
            @Override
            public void run() {
                float d = x[0] - x[1];
                Line1DSimplest.this.o.set(Util.unitize(o.floatValue() + d * speed.floatValue()));
            }
        });
        actionUnipolar($.INSTANCE.p("up"), new FloatToFloatFunction() {
            @Override
            public float valueOf(float d) {

                synchronized (o) {


                    return x[0] = d;


                }
            }
        });

        actionUnipolar($.INSTANCE.p("down"), new FloatToFloatFunction() {
            @Override
            public float valueOf(float d) {

                synchronized (o) {


                    return x[1] = d;


                }
            }
        });


    }

    public void initBipolar() {
        actionBipolar($.INSTANCE.the("y"), new FloatToFloatFunction() {
            @Override
            public float valueOf(float v) {
                if (v == v) {
                    o.set(
                            Util.unitize(o.floatValue() + v * speed.floatValue())
                    );


                }

                return v;


            }
        });
    }



    public float target() {
        return i.asFloat();
    }

    public void target(float v) {
        i.set(v);
    }
}
