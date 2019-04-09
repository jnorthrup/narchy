package nars.concept.action;

import jcog.Util;
import nars.$;
import nars.NAR;
import nars.agent.Game;
import nars.attention.AttnBranch;
import nars.attention.PriNode;
import nars.concept.sensor.AbstractSensor;
import nars.control.channel.CauseChannel;
import nars.task.ITask;
import nars.term.Term;
import nars.term.Termed;
import nars.truth.PreciseTruth;
import nars.truth.Truth;
import org.eclipse.collections.api.block.function.primitive.BooleanToObjectFunction;
import org.eclipse.collections.api.block.function.primitive.FloatToFloatFunction;

import java.util.List;

import static java.lang.Float.MIN_NORMAL;
import static nars.Op.BELIEF;
import static nars.Op.PROD;

/** sensor that integrates and manages a pair of oppositely polarized AsyncActionConcept to produce a net result.
 * implements Sensor but actually manages Actions internally. */
public class BiPolarAction extends AbstractSensor {

    private final Polarization model;
    private final FloatToFloatFunction motor;

    public final PriNode attn;
    private final CauseChannel<ITask> cause;


    /** model for computing the net result from the current truth inputs */
    public interface Polarization {

        /** produce a value in -1..+1 range, or NaN if undetermined */
        float update(Truth pos, Truth neg, long prev, long now);
    }

    public final AbstractGoalActionConcept pos, neg;

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
    public BiPolarAction(Term pos, Term neg, Polarization model, FloatToFloatFunction motor, NAR n) {
        super(PROD.the(pos, neg), n);

        this.cause = n.newChannel(id);
        this.attn = new AttnBranch(id, List.of(pos, neg));

        this.pos = new AbstractGoalActionConcept(pos, n) {
            @Override
            protected CauseChannel<ITask> channel(NAR n) {
                return BiPolarAction.this.cause;
            }
        };
        this.neg = new AbstractGoalActionConcept(neg, n);


//                //TemplateTermLinker.of(neg),
//                //TemplateTermLinker.of(neg, 4, pos),
//                nar);

        this.model = model;
        this.motor = motor;
    }


    /** the pos and neg .update() method should have been called just prior to this since this is
     * invoked by the frame listeners at the end of the NAgent cycle
     */
    @Override public void update(Game g) {


        Truth p, n;

            p = pos.actionTruth();

            n = neg.actionTruth();


        final long prev = g.prev, now = g.now;
        float x = model.update(p, n, prev, now);

        //System.out.println(p + " vs " + n + " -> " + x);


        if (x==x) {
            x = Util.clamp(x, -1f, +1f);
        }

        float y = motor.valueOf(x);

        //TODO configurable feedback model

        PreciseTruth Nb, Pb;

        if (y == y) {

            y = Util.clamp(y, -1, +1);

            float yp, yn;
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

            { yn = 0.5f - y/2; yp = 0.5f + y/2; }
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

        short cid = cause.id;
        pos.feedback(Pb, cid, g);
        neg.feedback(Nb, cid, g);

    }



    @Override
    public Iterable<Termed> components() {
        return List.of(pos, neg);
    }


    /** offers a few parameters */
    public static class DefaultPolarization implements Polarization {

        final float[] lastX = new float[] { 0};



        /** how much coherence can shrink the amplitude of the resulting bipolar signal. 0 means not at all, +1 means fully attenuable */
        private final boolean fair;

        boolean freqOrExp = true;
//        boolean latch = false;

        /** adjustable q+ lowpass filters */
        //final FloatAveraged fp = new FloatAveraged(0.99f, true);
        /** adjustable q- lowpass filters */
        //final FloatAveraged fn = new FloatAveraged(0.99f, true);
        private final boolean normalize = false;

        public DefaultPolarization(boolean fair) {
            this.fair = fair;

        }

        @Override
        public float update(Truth pos, Truth neg, long prev, long now) {


            float pq = q(pos), nq = q(neg);

            //fill in missing NaN values
            float pg, ng;
//            if (pq!=pq) pg = latch ? fp.floatValue() : 0;
//            else pg = fp.valueOf(pq);
//            if (nq!=nq) ng = latch ? fn.floatValue() : 0;
//            else ng = fn.valueOf(nq);
            pg = pq == pq ? pq : 0;
            ng = nq == nq ? nq : 0;



            float x;
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
                float pe = c(pos), ne = c(neg);
                float eMax = Math.max(pe, ne);

                //TODO make as an adaptive AGC
                if (normalize && eMax > MIN_NORMAL) {
                    pe/=eMax;
                    ne/=eMax;
                }

                float eMin = Math.min(pe, ne);

                //coherence: either they are equally confident, or they specify the same net x value
                float coherence = Util.unitize(
                        //Util.
                            //or
                            //and(
                             (eMax > MIN_NORMAL) ? eMin / eMax : 0
                        //, Math.abs(pe - ne))
                );

//                assert(coherence <= 1f): "strange coherence=" + coherence;

                x = coherence * x;


            }

            if (Float.isFinite(x)) x = Util.clamp(motorization(x), -1, +1);
            else x = Float.NaN;

            lastX[0] = x;


//            //filter negative result
//            if (Math.abs(x) < 0.5f) {
//                x = 0;
//            }

            return x;

        }

        /** calculates the effective output motor value returned by the model.
         * input and output to this function are in the domain/range of -1..+1
         * a default linear response is implemented here. */
        public float motorization(float input) {
            return input;
        }

        /** confidence/evidence strength.  could be truth conf or evidence, zero if null. used in determining coherence
         *  TODO return double
         * */
        public float c(Truth t) {
            return t != null ? (float) t.evi() : 0;
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
