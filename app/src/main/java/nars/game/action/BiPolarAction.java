package nars.game.action;

import jcog.TODO;
import jcog.Util;
import nars.$;
import nars.NAR;
import nars.Task;
import nars.attention.PriBranch;
import nars.attention.PriNode;
import nars.control.channel.CauseChannel;
import nars.game.Game;
import nars.game.sensor.AbstractSensor;
import nars.term.Term;
import nars.term.Termed;
import nars.truth.PreciseTruth;
import nars.truth.Truth;
import org.eclipse.collections.api.block.function.primitive.BooleanToObjectFunction;
import org.eclipse.collections.api.block.function.primitive.FloatToFloatFunction;
import org.jetbrains.annotations.Nullable;

import java.util.List;

import static java.lang.Float.MIN_NORMAL;
import static nars.Op.BELIEF;
import static nars.Op.PROD;

/** sensor that integrates and manages a pair of oppositely polarized AsyncActionConcept to produce a net result.
 * implements Sensor but actually manages Actions internally.
 * TODO fix, combine with Push Button Mutex
 * */
public class BiPolarAction extends AbstractSensor {

    private final Polarization model;
    private final FloatToFloatFunction motor;

    public final PriNode pri;


    /** model for computing the net result from the current truth inputs */
    @FunctionalInterface
    public interface Polarization {

        /** produce a value in -1..+1 range, or NaN if undetermined */
        float update(Truth pos, Truth neg, long prev, long now);
    }

    public final AbstractGoalActionConcept pos;
    public final AbstractGoalActionConcept neg;

//    public BiPolarAction(Term id, Polarization model, FloatToFloatFunction motor, NAR nar) {
//        this(posOrNeg ->
//                        $.p(id, posOrNeg ? PLUS : NEG)
//                //$.inh(id, posOrNeg ? PLUS : NEG)
//                , model, motor, nar);
//    }

    public BiPolarAction(BooleanToObjectFunction<Term> id, Polarization model, FloatToFloatFunction motor, NAR n) {
        this(id.valueOf(true), id.valueOf(false), model, motor, n);
    }

    //TODO BooleanObjectFunction<Term> target namer
    private BiPolarAction(Term pos, Term neg, Polarization model, FloatToFloatFunction motor, NAR n) {
        super(PROD.the(pos, neg), n);

        CauseChannel<Task> cause = n.newChannel(id);
        short[] causeArray = {cause.id};



        this.pos = new AbstractGoalActionConcept(pos, n) {

            @Override
            protected @Nullable Truth updateAction(@Nullable Truth beliefTruth, @Nullable Truth actionTruth, Game g) {
                throw new TODO();
            }
        };
        this.neg = new AbstractGoalActionConcept(neg, n) {
            @Override
            protected @Nullable Truth updateAction(@Nullable Truth beliefTruth, @Nullable Truth actionTruth, Game g) {
                throw new TODO();
            }
        };
        this.pri = new PriBranch(id, List.of(pos, neg));

//                //TemplateTermLinker.of(neg),
//                //TemplateTermLinker.of(neg, 4, pos),
//                nar);

        this.model = model;
        this.motor = motor;
    }


    /** the pos and neg .update() method should have been called just prior to this since this is
     * invoked by the frame listeners at the end of the NAgent cycle
     */
    @Override public void accept(Game g) {


        Truth p = pos.actionTruth();

        Truth n = neg.actionTruth();


        float x = model.update(p, n, g.nowPercept.start, g.nowPercept.end);

        //System.out.println(p + " vs " + n + " -> " + x);


        if (x==x) {
            x = Util.clamp(x, -1f, +1f);
        }

        float y = motor.valueOf(x);

        //TODO configurable feedback model

        PreciseTruth Nb, Pb;

        if (y == y) {

            y = Util.clamp(y, -1, +1);
            //y = (y + 1)/2; //0...1 range

            //            yp = 0.5f + y / 2f;
//            yn = 1f - yp;

//            float thresh = nar.freqResolution.floatValue();
//            if (Math.abs(y) < thresh) { yp = yn = 0; } else if (y > 0) { yp = y; yn = 0; } else { yp = 0; yn = -y; }

//            float thresh = nar.freqResolution.floatValue()/2;
//            if (Math.abs(y) < thresh) {
//                //deadzone
//                yp = yn =
//                        //0;
//                        //Float.NaN;
//            }

            //only one side gets feedback:
            //else if (y > 0) { yp = 0.5f + y/2; yn = 0; }
            //else { yn = 0.5f - y/2; yp = 0; }

            //balanced around 0.5

            float yn = 0.5f - y / 2;
            float yp = 0.5f + y / 2;
            //System.out.println(p + "," + n + "\t" + y + "\t" + yp + "," + yn);

//            if ((p == null && n == null) /* curiosity */ || (p!=null && n!=null) /* both active */) {
//                float zeroThresh = ScalarValue.EPSILON;
//                if (y >= zeroThresh) {
//                    yp = y;
//                    yn = Float.NaN;
//                } else if (y <= -zeroThresh) {
//                    yn = -y;
//                    yp = Float.NaN;
//                } else {
//                    yp = yn = Float.NaN;
//                }
//            }  else if (p!=null) {
//                yp = Math.max(0, y);
//                yn = Float.NaN;
//            } else if (n!=null) {
//                yn = Math.max(0, -y);
//                yp = Float.NaN;
//            } else {
//                throw new UnsupportedOperationException();
//            }


            float feedbackConf = nar.confDefault(BELIEF);

            Pb = yp == yp ? $.t(yp, feedbackConf).dither(nar) : null;
            Nb = yn == yn ? $.t(yn, feedbackConf).dither(nar) : null;

        } else {
            Pb = Nb = null;
        }

//        pos.feedback(Pb, causeArray, g);
//        neg.feedback(Nb, causeArray, g);

    }



    @Override
    public Iterable<? extends Termed> components() {
        return List.of(pos, neg);
    }


    /** offers a few parameters */
    public static class DefaultPolarization implements Polarization {

        final float[] lastX = { 0};



        /** how much coherence can shrink the amplitude of the resulting bipolar signal. 0 means not at all, +1 means fully attenuable */
        private final boolean fair;

        boolean freqOrExp = true;
//        boolean latch = false;

        /** adjustable q+ lowpass filters */
        //final FloatAveraged fp = new FloatAveraged(0.99f, true);
        /** adjustable q- lowpass filters */
        //final FloatAveraged fn = new FloatAveraged(0.99f, true);
//        private final boolean normalize = false;

        public DefaultPolarization(boolean fair) {
            this.fair = fair;

        }

        @Override
        public float update(Truth pos, Truth neg, long prev, long now) {


            float pq = q(pos), nq = q(neg);

            //fill in missing NaN values
            //            if (pq!=pq) pg = latch ? fp.floatValue() : 0;
//            else pg = fp.valueOf(pq);
//            if (nq!=nq) ng = latch ? fn.floatValue() : 0;
//            else ng = fn.valueOf(nq);
            float pg = pq == pq ? pq : 0;
            float ng = nq == nq ? nq : 0;



            double x;
            //x = //(pg - ng);


            if (pq==pq && nq==nq) {
                //x = ThreadLocalRandom.current().nextBoolean() ? pg : -ng;
                x = pg - ng;
            } else {
                if (pq == pq)
                    x = pg;
                else
                    x = -ng;
            }

            //System.out.println(pg + "|" + ng + "=" + x);

            if (fair) {
                double pe = c(pos), ne = c(neg);
                double eMax = Math.max(pe, ne);

//                //TODO make as an adaptive AGC
//                if (normalize && eMax > MIN_NORMAL) {
//                    pe/=eMax;
//                    ne/=eMax;
//                }

                double eMin = Math.min(pe, ne);

                //coherence: either they are equally confident, or they specify the same net x value
                double coherence = Util.unitize(
                        //Util.
                            //or
                            //and(
                             (eMax > MIN_NORMAL) ? eMin / eMax : 0
                        //, Math.abs(pe - ne))
                );

//                assert(coherence <= 1f): "strange coherence=" + coherence;

                x = coherence * x;


            }

            if (Double.isFinite(x)) x = Util.clamp(motorization((float)x), -1, +1);
            else x = Float.NaN;

            return (lastX[0] = (float)x);
        }

        /** calculates the effective output motor value returned by the model.
         * input and output to this function are in the domain/range of -1..+1
         * a default linear response is implemented here. */
        public static float motorization(float input) {
            return input;
        }

        /** confidence/evidence strength.  could be truth conf or evidence, zero if null. used in determining coherence
         *  TODO return double
         * */
        public static double c(Truth t) {
            //return t != null ? t.evi() : 0;
            return t != null ? t.conf() : 0;
        }

        /** "Q" desire/value function. produces the scalar summary of the goal truth desire that will be
         * used in the difference comparison. return NaN or value  */
        public float q(Truth t) {

            float q = t != null ? ((freqOrExp ? t.freq() : t.expectation()) ) : Float.NaN;
            if (q==q)
                q = (q-0.5f)*2;
            return q;
            //return t != null ? ((freqOrExp ? t.freq() : t.expectation()) - 0.5f)*2 : Float.NaN;
            //return t != null ? ((freqOrExp ? (t.freq()>=0.5f ? t.freq() : 0) : t.expectation()) ) : Float.NaN;
        }

    }


}
