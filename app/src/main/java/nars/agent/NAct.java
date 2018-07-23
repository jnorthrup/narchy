package nars.agent;

import jcog.Util;
import jcog.math.FloatRange;
import jcog.math.FloatSupplier;
import jcog.util.FloatConsumer;
import nars.$;
import nars.NAR;
import nars.Narsese;
import nars.concept.action.ActionConcept;
import nars.concept.action.GoalActionAsyncConcept;
import nars.concept.action.GoalActionConcept;
import nars.control.channel.CauseChannel;
import nars.task.ITask;
import nars.term.Term;
import nars.truth.PreciseTruth;
import nars.truth.Truth;
import org.eclipse.collections.api.block.function.primitive.BooleanToBooleanFunction;
import org.eclipse.collections.api.block.function.primitive.FloatToFloatFunction;
import org.eclipse.collections.api.block.procedure.primitive.BooleanProcedure;
import org.jetbrains.annotations.Nullable;

import java.util.Random;
import java.util.function.BiConsumer;
import java.util.function.IntConsumer;
import java.util.function.IntPredicate;

import static jcog.Util.unitize;
import static nars.Op.BELIEF;

/**
 * Created by me on 9/30/16.
 */
public interface NAct {

    Term PLUS = $.the("\"+\"");
    Term NEG = $.the("\"-\"");


    NAR nar();

    /**
     * master curiosity factor, for all actions
     */
    FloatRange curiosity();

    /**
     * TODO make BooleanPredicate version for feedback
     */
    default void actionToggle(Term t, float thresh, float defaultValue /* 0 or NaN */, float momentumOn, Runnable on, Runnable off) {


        final float[] last = {0};
        actionUnipolar(t, (f) -> {

            boolean unknown = (f != f) || (f < thresh && (f > (1f - thresh)));
            if (unknown) {
                f = defaultValue == defaultValue ? defaultValue : last[0];
            }

            if (last[0] > 0.5f)
                f = Util.lerp(momentumOn, f, last[0]);

            boolean positive = f > 0.5f;


            if (positive) {
                on.run();
                return last[0] = 1f;
            } else {
                off.run();
                return last[0] = 0f;
            }
        });
    }


    @Nullable
    default Truth toggle(@Nullable Truth d, Runnable on, Runnable off, boolean next) {
        float freq;
        if (next) {
            freq = +1;
            on.run();
        } else {
            freq = 0f;
            off.run();
        }

        return $.t(freq,

                nar().confDefault(BELIEF) /*d.conf()*/);
    }

    /**
     * selects one of 2 states until it shifts to the other one. suitable for representing
     * push-buttons like keyboard keys. by default with no desire the state is off.   the off procedure will not be called immediately.
     */
    default void actionTriState(Term s, IntConsumer i) {
        actionTriState(s, (v) -> {
            i.accept(v);
            return true;
        });
    }

    /**
     * tri-state implemented as delta version memory of last state.
     * initial state is neutral.
     */
    default GoalActionAsyncConcept[] actionTriState(Term cc, IntPredicate i) {


        GoalActionAsyncConcept[] g = actionBipolar(cc, true, (float f) -> {

            f = f / 2f + 0.5f;


            float deadZoneFreqRadius =
                    1 / 6f;

            int s;
            if (f > 0.5f + deadZoneFreqRadius)
                s = +1;
            else if (f < 0.5f - deadZoneFreqRadius)
                s = -1;
            else
                s = 0;

            if (i.test(s)) {


                switch (s) {
                    case -1:
                        return -1f;
                    case 0:
                        return 0f;
                    case +1:
                        return +1f;
                    default:
                        throw new RuntimeException();
                }

            }

            return 0f;

        });
        float res = 0.5f;
        g[0].resolution(res);
        g[1].resolution(res);
        return g;
    }

    <A extends ActionConcept> A addAction(A c);

    @Nullable
    default GoalActionConcept actionTriStateContinuous(Term s, IntPredicate i) {

        GoalActionConcept m = new GoalActionConcept(s, nar(), (b, d) -> {


            int ii;
            if (d == null) {
                ii = 0;
            } else {
                float f = d.freq();
                float deadZoneFreqRadius = 1f / 6;
                if (f > 0.5f + deadZoneFreqRadius)
                    ii = +1;
                else if (f < 0.5f - deadZoneFreqRadius)
                    ii = -1;
                else
                    ii = 0;
            }

            boolean accepted = i.test(ii);
            if (!accepted)
                ii = 0;

            float f;
            switch (ii) {
                case 1:
                    f = 1f;
                    break;
                case 0:
                    f = 0.5f;
                    break;
                case -1:
                    f = 0f;
                    break;
                default:
                    throw new RuntimeException();
            }

            return $.t(f, nar().confDefault(BELIEF));
        });


        return addAction(m);
    }

    @Nullable
    default ActionConcept actionTriStatePWM(Term s, IntConsumer i) {
        ActionConcept m = new GoalActionConcept(s, nar(), (b, d) -> {


            int ii;
            if (d == null) {
                ii = 0;
            } else {
                float f = d.freq();
                if (f == 1f) {
                    ii = +1;
                } else if (f == 0) {
                    ii = -1;
                } else if (f > 0.5f) {
                    ii = nar().random().nextFloat() <= ((f - 0.5f) * 2f) ? +1 : 0;
                } else if (f < 0.5f) {
                    ii = nar().random().nextFloat() <= ((0.5f - f) * 2f) ? -1 : 0;
                } else
                    ii = 0;
            }

            i.accept(ii);

            float f;
            switch (ii) {
                case 1:
                    f = 1f;
                    break;
                case 0:
                    f = 0.5f;
                    break;
                case -1:
                    f = 0f;
                    break;
                default:
                    throw new RuntimeException();
            }

            return

                    $.t(f,

                            nar().confDefault(BELIEF)
                    )

                    ;
        });
        return addAction(m);
    }


    default void actionToggle(Term s, Runnable r) {
        actionToggle(s, (b) -> {
            if (b) {
                r.run();
            }
        });
    }

    default void actionPushButton(Term s, Runnable r) {
        actionPushButton(s, (b) -> {
            if (b) {
                r.run();
            }
        });
    }

    default void actionToggle(Term s, BooleanProcedure onChange) {


        actionPushButton(s, onChange);

    }

    default void actionPushReleaseButton(Term t, BooleanProcedure on) {

        float thresh = 0.1f;
        action(t, (b, g) -> {
            float G = g != null ? g.expectation() : 0.0f;
            boolean positive;
            if (G > 0.5f) {
                float f = G - (b != null ? b.expectation() : 0.5f);
                positive = f >= thresh;
            } else {
                positive = false;
            }
            on.value(positive);
            return $.t(positive ? 1 : 0, nar().confDefault(BELIEF));
        });
    }

    default void actionPushButton(Term t, BooleanProcedure on) {
        actionPushButton(t, (x) -> {
            on.value(x);
            return x;
        });
    }

    default void actionPushButtonMutex0(Term l, Term r, BooleanProcedure L, BooleanProcedure R) {
        FloatSupplier thresh = () ->
                //0.66f;
                0.5f;


        boolean[] lr = new boolean[2];

        actionPushButton(l, thresh, ll -> {
            boolean x = ll;
            if (x && lr[1]) {
                x = false;
            }
            lr[0] = x;
            L.value(x);
            return x;
        });
        actionPushButton(r, thresh, rr -> {
            boolean x = rr;
            if (x && lr[0]) {
                x = false;
            }
            lr[1] = x;
            R.value(x);
            return x;
        });
    }

    default void actionPushButtonMutex(Term l, Term r, BooleanProcedure L, BooleanProcedure R) {

        float thresh =
                0.5f;
        //0.66f;

        float[] lr = new float[2];

        GoalActionConcept LA = action(l, (b, g) -> {
            float ll = g != null ? g.freq() : 0;
            boolean x = ll > thresh;
            if (x) {
                if (lr[1] > thresh) {
                    x = false;
                    //ll = 0;
                }
            }
            lr[0] = ll;
            L.value(x);
            //System.out.println("L=" + x  + " <- " + ll );
            return $.t(x ? 1 : 0, nar().confDefault(BELIEF));
        });
        GoalActionConcept RA = action(r, (b, g) -> {
            float rr = g != null ? g.freq() : 0;
            boolean x = rr > thresh;
            if (x) {
                if (lr[0] > thresh) {
                    x = false;
                    //rr = 0;
                }
            }
            lr[1] = rr;
            R.value(x);
            //System.out.println("R=" + x  + " <- " + rr );
            return $.t(x ? 1 : 0, nar().confDefault(BELIEF));
        });
        LA.resolution(1f);
        RA.resolution(1f);

    }

    default void actionPushButton(Term t, BooleanToBooleanFunction on) {
        actionPushButton(t, () -> 0.5f /*+ nar().freqResolution.get()*/, on);
    }

    default void actionPushButton(Term t, FloatSupplier thresh, BooleanToBooleanFunction on) {


        GoalActionConcept b = actionUnipolar(t, true, (x) -> 0, (f) -> {
            boolean posOrNeg = f > thresh.asFloat();
            return on.valueOf(posOrNeg) ? 1f : 0f;
        });
        b.resolution(1f);
    }


    /**
     * the supplied value will be in the range -1..+1. if the predicate returns false, then
     * it will not allow feedback through. this can be used for situations where the action
     * hits a limit or boundary that it did not pass through.
     * <p>
     * TODO make a FloatToFloatFunction variation in which a returned value in 0..+1.0 proportionally decreasese the confidence of any feedback
     */

    default GoalActionConcept action(String s, GoalActionConcept.MotorFunction update) throws Narsese.NarseseException {
        return action($.$(s), update);
    }


    default GoalActionConcept action(Term s, GoalActionConcept.MotorFunction update) {
        return addAction(new GoalActionConcept(s, nar(), update));
    }

//    default BeliefActionConcept react( Term s,  Consumer<Truth> update) {
//        return addAction(new BeliefActionConcept(s, nar(), update));
//    }

    default GoalActionAsyncConcept[] actionBipolar(Term s, FloatToFloatFunction update) {
        return actionBipolar(s, false, update);
    }

    default GoalActionAsyncConcept[] actionBipolar(Term s, boolean fair, FloatToFloatFunction update) {
        return actionBipolarFrequencyDifferential(s, fair, false, update);


    }

    default void actionBipolarSteering(Term s, FloatConsumer act) {
        final float[] amp = new float[1];
        float dt = 0.1f;
        float max = 1f;
        float decay = 0.9f;
        actionTriState(s, (i) -> {
            float a = amp[0];
            float b = Util.clamp((a * decay) + dt * i, -max, max);
            amp[0] = b;

            act.accept(b);

            return !Util.equals(a, b, Float.MIN_NORMAL);
        });


    }

    default GoalActionAsyncConcept[] actionBipolarFrequencyDifferential(Term s, boolean fair, boolean latchPreviousIfUndecided, FloatToFloatFunction update) {

        Term pt =

                $.inh(s, PLUS);


        Term nt =

                $.inh(s, NEG);


        final float g[] = new float[2];
        final float e[] = new float[2];
        final long[] lastUpdate = {nar().time()};
        final long[] lastFeedback = {nar().time()};

        final float[] lastX = {0};

        GoalActionAsyncConcept[] CC = new GoalActionAsyncConcept[2];

        BiConsumer<GoalActionAsyncConcept, Truth> u = (action, gg) -> {


            NAR n = nar();
            long now = n.time();

            if (now != lastUpdate[0]) {
                lastUpdate[0] = now;
                CC[0] = CC[1] = null;
            }


            float confMin = n.confMin.floatValue();

            float feedbackConf =
                    n.confDefault(BELIEF);


            boolean p = action.term().equals(pt);
            int ip = p ? 0 : 1;
            CC[ip] = action;
            g[ip] = gg != null ?
                    gg.freq()
                    //gg.exp()

                    :

                    Float.NaN;
            e[ip] = gg != null ?
                    gg.evi()

                    :
                    0f;


            float x;

            boolean curious;
            if (CC[0] != null && CC[1] != null /* both ready */) {

                if (g[0] != g[0] && g[1] == g[1]) {
                    g[0] = 1 - g[1];
                } else if (g[1] != g[1] && g[0] == g[0]) {
                    g[1] = 1 - g[0];
                } else if (g[1] != g[1] && g[0] != g[0]) {
                    g[0] = g[1] = 0.5f;
                }

                float cMax = Math.max(e[0], e[1]);
                float cMin = Math.min(e[0], e[1]);
                float coherence = cMin / cMax;

                Random rng = n.random();
                float cur = curiosity().floatValue();
                if (cur > 0 && rng.nextFloat() <= cur) {
                    x = (rng.nextFloat() - 0.5f) * 2f;


                    e[0] = e[1] = feedbackConf;
                    coherence = 1f;
                    curious = true;
                } else {
                    curious = false;


                    if (cMax < confMin) {
                        if (latchPreviousIfUndecided) {
                            x = lastX[0];
                        } else {
                            x = 0;
                        }
                    } else {


                        x = ((g[0] - g[1]));


                        if (fair) {

                            x *= coherence;


                        }


                    }


                }

                x = Util.clamp(x, -1f, +1f);

                lastX[0] = x;

                float y = update.valueOf(x);


                PreciseTruth Nb, Ng, Pb, Pg;

                if (y == y) {

                    float yp, yn;
                    if (Math.abs(y) >= n.freqResolution.floatValue()) {
                        yp = 0.5f + y / 2f;
                        yn = 1f - yp;
                    } else {
                        yp = yn = 0.5f;
                    }


                    float pbf = yp;
                    float nbf = yn;
                    Pb = $.t(pbf, feedbackConf);
                    Nb = $.t(nbf, feedbackConf);


                    Pg = null;
                    Ng = null;


                } else {
                    Pb = Nb = Pg = Ng = null;
                }


                long lastFb = lastFeedback[0];
                lastFeedback[0] = now;
                CC[0].feedback(Pb, Pg, lastFb, now, n);
                CC[1].feedback(Nb, Ng, lastFb, now, n);


            }
        };

        CauseChannel<ITask> cause = nar().newChannel(s);
        GoalActionAsyncConcept p = new GoalActionAsyncConcept(pt, nar(), cause, u);
        GoalActionAsyncConcept n = new GoalActionAsyncConcept(nt, nar(), cause, u);

        addAction(p);
        addAction(n);

        CC[0] = p;
        CC[1] = n;
        return CC;
    }

    default GoalActionConcept actionUnipolar(Term s, FloatConsumer update) {
        return actionUnipolar(s, (x) -> {
            update.accept(x);
            return x;
        });
    }

    default GoalActionConcept actionUnipolar(Term s, FloatToFloatFunction update) {
        return actionUnipolar(s, true, (x) -> Float.NaN, update);
    }


    /**
     * update function receives a value in 0..1.0 corresponding directly to the present goal frequency
     */
    default GoalActionConcept actionUnipolar(Term s, boolean freqOrExp, FloatToFloatFunction ifGoalMissing, FloatToFloatFunction update) {


        final float[] lastF = {0.5f};
        return action(s, (b, g) -> {
            float gg = (g != null) ?
                    (freqOrExp ? g.freq() : g.expectation()) : ifGoalMissing.valueOf(lastF[0]);

            lastF[0] = gg;

            float bFreq = (gg == gg) ? update.valueOf(gg) : Float.NaN;
            if (bFreq == bFreq) {
                float confFeedback =
                        nar().confDefault(BELIEF);


                return $.t(bFreq, confFeedback);
            } else
                return null;

        });
    }

    /**
     * supplies values in range -1..+1, where 0 ==> expectation=0.5
     */

    default GoalActionConcept actionExpUnipolar(Term s, FloatToFloatFunction update) {
        final float[] x = {0f}, xPrev = {0f};

        return action(s, (b, d) -> {
            float o = (d != null) ?

                    d.expectation() - 0.5f
                    : xPrev[0];
            float ff;
            if (o >= 0f) {


                float fb = update.valueOf(o /*y.asFloat()*/);
                if (fb != fb) {

                    return null;
                } else {
                    xPrev[0] = fb;
                }
                ff = (fb / 2f) + 0.5f;
            } else {
                ff = 0f;
            }
            return $.t(unitize(ff), nar().confDefault(BELIEF));
        });
    }

}


