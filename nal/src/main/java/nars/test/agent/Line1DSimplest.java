package nars.test.agent;

import jcog.Util;
import jcog.math.FloatRange;
import nars.$;
import nars.NAgent;
import nars.concept.scalar.Scalar;


/**
 * 1 input x 1 output environment
 */
public class Line1DSimplest extends NAgent {

    //public final SensorConcept in;

    /**
     * the target value
     */
    public final FloatRange i = new FloatRange(0.5f, 0, 1f);
    public final FloatRange o = new FloatRange(0.5f, 0, 1f);

    public final FloatRange speed = new FloatRange(0.25f, 0f, 0.5f);

//    @NotNull
//    public GoalActionConcept up, down;

    /**
     * the current value
     */

    public final Scalar in;


    public Line1DSimplest() {
        super( );


        in = senseNumber(
                $.the("x"),
                //$.$safe("y:i"),                //$.inh($.the("i"), id),                 //$.inh(Atomic.the("i"), id),
                this.i
        );
        senseNumber(
                $.the("y"),
                //$.$safe("y:o"),                //$.inh($.the("i"), id),                 //$.inh(Atomic.the("i"), id),
                this.o
        );

        //initDualToggle();
        //initDualUnipolar();
        initBipolar();

//        in = senseNumber(
//                //$.inh($.the("i"), id),
//                $.the("i"),
//                this.i, 2, ScalarConcepts.Needle);

//        action( new GoalActionConcept($.p($.the("o"), id), nar, (b,d) -> {
//            if (d!=null) {
//                o.setValue( .freq() + (-.5f)*2f*0.1f );
//            }
//        }));




//        out = actionUnipolar(O, (d) -> {
//            this.o.setValue(d);
//            return d;
//        });
//        out = actionTriState(O, (d) -> {
//            switch (d) {
//                case -1:
//                case +1:
//                    this.o.setValue(Math.max(0, Math.min(1f, this.o.floatValue() + d * speed.floatValue())));
//                    break;
//            }
//        });


//        out = action(
//                //$.inh($.the("o"), id),
//                $.p($.the("o"), id),
//                (b, d) -> {
//
//            if (d != null) {
//                float f = d.freq();
////                float inc = ((f - 0.5f) * 2f) * 0.04f;
////                o.setValue(unitize(o.getValue() + inc));
//
//                o.setValue(f);
//
////                else {
////                    //no change
////                    return null;
////                }
//
//
//                return $.t(o.floatValue(), d.conf());
//            }
//
//            return null;
//
//
//        });


    }

    private void initDualToggle() {
        actionToggle($.$$("y:up"), (b) -> {
            if(b) {
                o.set(Util.unitize(o.floatValue() + speed.floatValue()));
                System.out.println(o);
            }
        });
        actionToggle($.$$("y:down"), (b) -> {
            if (b) {
                o.set(Util.unitize(o.floatValue() - speed.floatValue()));
                System.out.println(o);
            }
        });
    }


    private void initDualUnipolar() {
        float[] x = new float[2];
        onFrame(() -> {
            float d = x[0] - x[1];
            this.o.set(Util.unitize(o.floatValue() + d * speed.floatValue()));
        });
        actionUnipolar($.p("up"), d -> {
            //if (d < 0.5f)  return 0; d -= 0.5f; d *= 2f;
            synchronized (o) {
                //float prev = o.floatValue();
                //float sp = speed.floatValue();
                //float next = Math.min(1, prev + d * sp);
//                if (!Util.equals(prev, next, sp )) {

//                if (d > 0.5f)
//                    x[0] = d - 0.5f;
//                else
//                    x[0] = 0;
                    //this.o.setValue(next);
                return x[0] = d;
//                } else {
//                    return 0f;
//                }
            }
        });

        actionUnipolar($.p("down"), d -> {
            //if (d < 0.5f)  return 0; d -= 0.5f; d *= 2f;
            synchronized (o) {
                //float prev = o.floatValue();
                //float sp = speed.floatValue();
                //float next = Math.max(0, prev - d * sp);
                //if (!Util.equals(prev, next, sp )) {
                    //this.o.setValue(next);
//                if (d > 0.5f)
//                    x[1] = d - 0.5f;
//                else
//                    x[1] = 0;
                return x[1] = d;
//                } else {
//                    return 0f;
//                }
            }
        });
//        n.goal(up.term(), Eternal, 0f, 0.01f);
//        n.goal(down.term(), Eternal, 0f, 0.01f);

    }

    public void initBipolar() {
        actionBipolar($.the("y"), v -> {
            if (v == v) {
                o.set(
                        Util.unitize(o.floatValue() + v * speed.floatValue())
                );

//                o.setValue( (v/2f)+0.5f );

            }

            return v;

            //float current = this.o.floatValue();

            //if (!Util.equals(nv, o.floatValue(), Param.TRUTH_EPSILON)) {
//                o.setValue(
//                        //Util.unitize( current + v * speed.floatValue())
//                        v
//                );
//            }
//            return o.floatValue();
            //}
            //return false;
        });
    }

    @Override
    protected float act() {
        float dist = Math.abs(
                i.floatValue() -
                        o.floatValue()
        );

        //dist = (float)(Math.sqrt(dist)); //more challenging

        float h = ((1f - dist)-0.5f)*2f;
        //System.out.println(h + " " + happy.sensor.get());
        return h;
        //return (float) Math.pow((1f - dist), 2);
        //return (1f - dist) * 2 - 1; //unipolar, 0..1.0


//        float r = (1f - dist/2f); //bipolar, normalized to -1..+1
//        if (r < 0.25f) return 1f;
//        return -1f;

        //return Util.sqr(r);
        //return (r-1f);
    }


    public float target() {
        return i.asFloat();
    }

    public void target(float v) {
        i.set(v);
    }
}
