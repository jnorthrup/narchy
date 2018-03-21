package nars;

import jcog.Util;
import jcog.math.FloatRange;
import jcog.util.FloatConsumer;
import nars.concept.action.ActionConcept;
import nars.concept.action.BeliefActionConcept;
import nars.concept.action.GoalActionAsyncConcept;
import nars.concept.action.GoalActionConcept;
import nars.control.CauseChannel;
import nars.task.ITask;
import nars.term.Term;
import nars.truth.PreciseTruth;
import nars.truth.Truth;
import org.eclipse.collections.api.block.function.primitive.FloatToFloatFunction;
import org.eclipse.collections.api.block.procedure.primitive.BooleanProcedure;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Random;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.function.IntPredicate;

import static jcog.Util.unitize;
import static nars.Op.BELIEF;
import static nars.time.Tense.ETERNAL;
import static nars.truth.TruthFunctions.c2w;
import static nars.truth.TruthFunctions.w2c;

/**
 * Created by me on 9/30/16.
 */
public interface NAct {

    Term PLUS = $.the("\"+\"");
    Term NEG = $.the("\"-\"");

    @NotNull Map<ActionConcept, CauseChannel<ITask>> actions();

    NAR nar();

    /**
     * master curiosity factor, for all actions
     */
    FloatRange curiosity();

    /** TODO make BooleanPredicate version for feedback */
    default void actionToggle(@NotNull Term t, float thresh, float defaultValue /* 0 or NaN */, float momentumOn, @NotNull Runnable on, @NotNull Runnable off) {


        final float[] last = {0};
        actionUnipolar(t, (f) -> {

            boolean unknown = (f!=f) || (f < thresh && (f > (1f-thresh)));
            if (unknown) {
                f = defaultValue==defaultValue ? defaultValue : last[0];
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

//    /**
//     * latches to either one of 2 states until it shifts to the other one. suitable for representing
//     * push-buttons like keyboard keys. by default with no desire the state is off.  the 'on' and 'off'
//     * procedures will be called only as necessary (when state changes).  the off procedure will not be called immediately.
//     * its initial state will remain indetermined until the first feedback is generated.
//     */
//    default void actionToggleBi(@NotNull Term t, @NotNull Runnable on, @NotNull Runnable off) {
//        //float THRESH = 0.5f;
////        GoalActionConcept m = new GoalActionConcept(s, this, (b, d) -> {
////            boolean next = d != null && d.freq() > THRESH;
////            return toggle(d, on, off, next);
////        });
//
//        //m.resolution(0.5f);
//        float deadZoneFreqRadius = 1f / 6;
//
//        final boolean[] last = {false};
//        actionBipolar(t, (float f) -> {
//
//            //radius of center dead zone; diameter = 2x this
//
//            if (f > deadZoneFreqRadius) {
//                on.run();
//                last[0] = true;
//                return 1f;
//            } else if (f < -deadZoneFreqRadius) {
//                off.run();
//                last[0] = false;
//                return -1f;
//            } else {
//                return last[0] ? 1f : -1f;
//            }
//        });
//        //m.resolution(1f);
//        //return addAction(m);
//    }

//    /** softmax-like signal corruption that emulates PWM (pulse-width modulation) modulated by desire frequency */
//    @Nullable default ActionConcept actionTogglePWM(@NotNull Compound s, @NotNull Runnable on, @NotNull Runnable off) {
//        ActionConcept m = new GoalActionConcept(s, this, (b, d) -> {
//            float df = d != null ? d.freq() : 0.5f;
//            boolean corrupt = nar().random().nextFloat() > Math.abs(df - 0.5f) * 2f;
//
//            boolean next = df > 0.5f;
//            if (corrupt) next = !next;
//
//            return toggle(on, off, next);
//        });
//
//        actions().add(m);
//        return m;
//    }

    @Nullable
    default Truth toggle(@Nullable Truth d, @NotNull Runnable on, @NotNull Runnable off, boolean next) {
        float freq;
        if (next) {
            freq = +1;
            on.run();
        } else {
            freq = 0f;
            off.run();
        }

        return $.t(freq,
                //d!=null ? d.conf() : nar().confMin.floatValue());
                nar().confDefault(BELIEF) /*d.conf()*/);
    }

    /**
     * selects one of 2 states until it shifts to the other one. suitable for representing
     * push-buttons like keyboard keys. by default with no desire the state is off.   the off procedure will not be called immediately.
     */
    default void actionTriState(@NotNull Term s, @NotNull IntConsumer i) {
        actionTriState(s, (v) -> {
            i.accept(v);
            return true;
        });
    }

    /**
     * tri-state implemented as delta version memory of last state.
     * initial state is neutral.
     */
    default GoalActionAsyncConcept[] actionTriState(@NotNull Term cc, @NotNull IntPredicate i) {
        //final int[] state = {0};
        //new GoalActionConcept(cc, this, (b, d) -> {
        GoalActionAsyncConcept[] g = actionBipolar(cc, true, (float f) -> {

            f = f / 2f + 0.5f;

            //radius of center dead zone; diameter = 2x this
            float deadZoneFreqRadius =
                    1 / 6f;
            //1/4f;
            int s;
            if (f > 0.5f + deadZoneFreqRadius)
                s = +1;
            else if (f < 0.5f - deadZoneFreqRadius)
                s = -1;
            else
                s = 0;

            if (i.test(s)) {

                //            int curState = state[0];
                //            state[0] = Math.min(Math.max(curState + deltaState, -1), +1);
                //
                //            //float f = curState != state[0] ? (deltaState > 0 ? 1f : 0f) : 0.5f /* had no effect */;
                switch (s) { //state[0]) {
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
            //return Float.NaN;
        });
        float res = 0.5f; //0.0, 0.5, 1.0
        g[0].resolution.set(res);
        g[1].resolution.set(res);
        return g;
    }

    default <A extends ActionConcept> A addAction(A c) {
        CauseChannel existing = actions().put(c, nar().newCauseChannel(c));
        assert (existing == null);
        nar().on(c);
        return c;
    }

    @Nullable
    default GoalActionConcept actionTriStateContinuous(@NotNull Term s, @NotNull IntPredicate i) {

        GoalActionConcept m = new GoalActionConcept(s, this, (b, d) -> {
            //radius of center dead zone; diameter = 2x this
            // 1f/4;
            //1f/3f;


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
                ii = 0; //HACK

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
        //m.resolution.setValue(0.5f);

        return addAction(m);
    }

    @Nullable
    default ActionConcept actionTriStatePWM(@NotNull Term s, @NotNull IntConsumer i) {
        ActionConcept m = new GoalActionConcept(s, this, (b, d) -> {


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
                    //d!=null ?
                    $.t(f,
                            //d.conf()
                            nar().confDefault(BELIEF)
                    )
                    //: null
                    ;
        });
        return addAction(m);
    }


    default void actionToggle(@NotNull Term s, @NotNull Runnable r) {
        actionToggle(s, (b) -> { if (b) { r.run(); } } );
    }
    default void actionPushButton(@NotNull Term s, @NotNull Runnable r) {
        actionPushButton(s, (b) -> { if (b) { r.run(); } } );
    }

    default void actionToggle(@NotNull Term s, @NotNull BooleanProcedure onChange) {
        //SUSPECT
//        float thresh =
//                //0.5f + Param.TRUTH_EPSILON;
//                0.55f;
//                //0.75f;
//
//        actionToggle(s, thresh, Float.NaN, 0f, () -> onChange.value(true), () -> onChange.value(false));

        actionPushButton(s, onChange);

    }

    default void actionPushReleaseButton(@NotNull Term t, @NotNull BooleanProcedure on) {

        float thresh = 0.1f; // + nar().freqResolution.get()
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
    default void actionPushButton(@NotNull Term t, @NotNull BooleanProcedure on) {
        float thresh =
                nar().freqResolution.get();
                //0f;

        actionUnipolar(t, true, (x)->0, (f) -> {
            boolean positive = f >= 0.5f + thresh;
            on.value(positive);
            return positive ? 1f : 0f;
        });

//        float thresh =
//                //0.5f + Param.TRUTH_EPSILON;
//                0.6f;
//                //0.75f;
//                //0.6f;
//        actionToggle(s, thresh, 0, 0f, () -> onChange.value(true), () -> onChange.value(false));
    }
//
//    @Nullable
//    default ActionConcept actionToggleRapid(@NotNull Compound s, @NotNull BooleanProcedure onChange, int minPeriod) {
//        return actionToggleRapid(s, () -> onChange.value(true), () -> onChange.value(false), minPeriod);
//    }
//
//    /**
//     * rapid-fire pushbutton with a minPeriod after which it is reset to off, allowing
//     * re-triggering to ON while the true state remains enabled
//     * <p>
//     * TODO generalize to actionPWM (pulse width modulation) with controllable reset period (ex: by frequency, or conf etc)
//     */
//    @Nullable
//    default ActionConcept actionToggleRapid(@NotNull Compound term, @NotNull Runnable on, @NotNull Runnable off, int minPeriod) {
//
//        if (minPeriod < 1)
//            throw new UnsupportedOperationException();
//
//        final long[] reset = {Tense.ETERNAL}; //last enable time
//        final int[] state = {0}; // 0: unknown, -1: false, +1: true
//
//        ActionConcept m = new GoalActionConcept(term, this, (b, d) -> {
//
//            boolean next = d != null && d.freq() >= 0.5f;
//
//            float alpha = nar().confDefault(BELIEF);
//            int v;
//            int s;
//            if (!next) {
//                reset[0] = Tense.ETERNAL;
//                s = -1;
//                v = 0;
//            } else {
//
//                long lastReset = reset[0];
//                long now = nar().time();
//                if (lastReset == Tense.ETERNAL) {
//                    reset[0] = now;
//                    s = -1;
//                } else {
//                    if ((now - lastReset) % minPeriod == 0) {
//                        s = -1;
//                    } else {
//                        s = +1;
//                    }
//                }
//                v = 1;
//            }
//
//            if (state[0] != s) {
//                if (s < 0)
//                    off.run();
//                else
//                    on.run();
//                state[0] = s;
//            }
//
//            return $.t(v, alpha);
//        });
//
//        actions().add(m);
//        return m;
//    }

    /**
     * the supplied value will be in the range -1..+1. if the predicate returns false, then
     * it will not allow feedback through. this can be used for situations where the action
     * hits a limit or boundary that it did not pass through.
     * <p>
     * TODO make a FloatToFloatFunction variation in which a returned value in 0..+1.0 proportionally decreasese the confidence of any feedback
     */
    @NotNull
    default GoalActionConcept action(@NotNull String s, @NotNull GoalActionConcept.MotorFunction update) throws Narsese.NarseseException {
        return action($.$(s), update);
    }


    default GoalActionConcept action(@NotNull Term s, @NotNull GoalActionConcept.MotorFunction update) {
        return addAction(new GoalActionConcept(s, this, update));
    }
    default BeliefActionConcept react(@NotNull Term s, @NotNull Consumer<Truth> update) {
        return addAction(new BeliefActionConcept(s, nar(), update));
    }

    default GoalActionAsyncConcept[] actionBipolar(@NotNull Term s, @NotNull FloatToFloatFunction update) {
        return actionBipolar(s, false, update);
    }

    default GoalActionAsyncConcept[] actionBipolar(@NotNull Term s, boolean fair, @NotNull FloatToFloatFunction update) {
        return actionBipolarFrequencyDifferential(s, fair, false, update);
        //actionBipolarExpectation(s, update);
        //actionBipolarExpectationNormalized(s, update);
        //actionBipolarGreedy(s, update);
        //actionBipolarMutex3(s, update);
    }

    default void actionBipolarSteering(@NotNull Term s, FloatConsumer act) {
        final float[] amp = new float[1];
        float dt = 0.1f;
        float max = 1f;
        float decay = 0.9f;
        actionTriState(s, (i) -> {
            float a = amp[0];
            float b = Util.clamp( (a * decay) + dt * i, -max, max);
            amp[0] = b;

            act.accept(b);

            return !Util.equals(a, b, Float.MIN_NORMAL);
        });

//        actionUnipolar($.p($.the("\"*\""), s), (u)->{
//            amp[0] =
//                    u;
//                    //Math.max(u-0.5f, 0);
//            return u;
//        });
//        actionBipolar($.p($.the("\"+-\""), s), (xy)->{
//            act.accept(xy * amp[0]);
//            return xy;
//        });
    }

    default GoalActionAsyncConcept[] actionBipolarFrequencyDifferential(@NotNull Term s, boolean fair, boolean latchPreviousIfUndecided, @NotNull FloatToFloatFunction update) {

        Term pt =
                $.p(s, PLUS);
                //$.inh(s, PLUS);
                //$.prop(s,$.the("\"+\""));
                //$.p(s, ZeroProduct);
                //$.p(s,$.the("\"+\""));
        Term nt =
                $.p(s, NEG);
                //$.inh(s, NEG);
                //$.prop(s, $.the("\"-\""));
                //$.p(ZeroProduct, s);
                //$.p(s,$.the("\"-\""));

        final float g[] = new float[2];
        final float c[] = new float[2];
        final long[] lastUpdate = {ETERNAL};

        final float[] lastX = {0};

        GoalActionAsyncConcept[] CC = new GoalActionAsyncConcept[2]; //hack

        @NotNull BiConsumer<GoalActionAsyncConcept, Truth> u = (action, gg) -> {


            NAR n = nar();
            long now = n.time();
            if (now!= lastUpdate[0]) {
                lastUpdate[0] = now;
                CC[0] = CC[1] = null; //reset
            }



//            float freqEps = n.freqResolution.floatValue();
            float confMin = n.confMin.floatValue();
//            float eviMin = c2wSafe(confMin);
            float feedbackConf =
                    w2c(c2w(n.confDefault(BELIEF))/2f); //fairly shared to sum to default
                    // n.confDefault(BELIEF);
                    // n.confDefault(GOAL);
                    //confMin * ...;



            boolean p = action.term().equals(pt);
            int ip = p ? 0 : 1;
            CC[ip] = action;
            g[ip] = gg != null ?
                    //gg.freq()
                    gg.expectation()
                    :
                    0f;
                    //0.5f;
            c[ip] = gg != null ?
                    //gg.evi()
                    gg.conf()
                    :
                    0f;


            float x; //-1..+1

            boolean curious;
            if (CC[0]!=null && CC[1]!=null /* both ready */) {

                float cMax = Math.max(c[0], c[1]);
                float cMin = Math.min(c[0], c[1]);
                float coherence = cMin / cMax;

                Random rng = n.random();
                float cur = curiosity().floatValue();
                if (cur > 0 && rng.nextFloat() <= cur) {
                    x = (rng.nextFloat() - 0.5f) * 2f;
//                    float curiEvi =
//                            //c2w(n.confDefault(BELIEF));
//                            //eviMin*2;
//                            Math.max(c2wSafe(w2cSafe(eviMin)*2), Util.mean(c[0], c[1])); //match desire conf, min=2*minConf

                    c[0] = c[1] = feedbackConf;
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


//                        //expectation
//                        float g0 = g[0]-0.5f;
//                        float g1 = g[1]-0.5f;
//                        df = 2f * ((g0) - (g1));
//                            // /Math.max(Math.abs(g0), Math.abs(g1));

                        //frequency -======================

                        //A. subtraction
                        x = ((g[0] - g[1])); //subtract

                        //B. difference, like the truth func
                        //df =  g[0] >= g[1] ?  (g[0] * (1f-g[1])) : -(g[1] * (1f-g[0]));


                        //experimental: lessen by a factor of how equally confident each goal is
                        if (fair) {
                            //fully fair
                                x *= coherence;
                            //x *= Math.sqrt(coherence); //less sharp than linear
                            //semi-fair
                                //df *= 0.5f + 0.5f * (eMin / eMax); //reduction by at most half
                        }
                        //df *= 1f - Math.abs(e[0] - e[1]) / eMax;
                        //df *= Util.sqr(eMin / eMax); //more cautious
                        //df *= Math.min(w2cSafe(e[0]), w2cSafe(e[1])) / w2cSafe(eMax);
                    }


                }

                x = Util.clamp(x, -1f, +1f);

                lastX[0] = x;

                float y = update.valueOf(x); //-1..+1
                //System.out.println(x + " " + y);


                //w2c(Math.abs(y) * c2w(restConf));
                PreciseTruth Nb,Ng, Pb,Pg;

                if (y == y) {
                //y: (-1..+1)
                    float yp, yn;
                    if (Math.abs(y) >= n.freqResolution.floatValue()) {
                        yp = 0.5f + y/2f;
                        yn = 1f - yp;
                    } else {
                        yp = yn = 0.5f;
                    }

//                    float yp = 0.5f + y/2f;
//                    float yn = 1f - yp;
                    float pbf = yp;
                    float nbf = yn;
                    Pb = $.t(pbf, feedbackConf);
                    Nb = $.t(nbf, feedbackConf);
//                    float goalEvi =
//                            eviMin;
//                    //max(eviMin, max(e[0], e[1]));
//                    Pg = curious || e[0] == 0 ? new PreciseTruth(yp, goalEvi, false) : null;
//                    Ng = curious || e[1] == 0 ? new PreciseTruth(yn, goalEvi, false) : null;



//                    float confBase = confMin*4; //~ alpha, learning rate
//                    float fThresh = Float.MIN_NORMAL;
//                    float yp = y > +fThresh ? Util.lerp(+y, confBase, feedbackConf) : confBase;
//                    float yn = y < -fThresh ? Util.lerp(-y, confBase, feedbackConf) : confBase;
//                    Pb = $.t(y > +fThresh ? 1 : 0, y > +fThresh ? yp : feedbackConf - yp);
//                    Nb = $.t(y < -fThresh ? 1 : 0, y < -fThresh ? yn : feedbackConf - yn);
//                    //Pg = curious || e[0] == 0 ? new PreciseTruth(1, Util.lerp(+y, confMin2, feedbackConf)) : null;
//                    Pg = null;
//                    //Ng = curious || e[1] == 0 ? new PreciseTruth(1, Util.lerp(-y, confMin2, feedbackConf)) : null;
//                    Ng = null;





//                    float fThresh = nar().freqResolution.floatValue();
//                    int sign = (y > fThresh ? +1 : (y < -fThresh ? -1 : 0));
//
//                    float feedConf =
//                            w2cSafe(c2wSafe(goalConf)/2f); //half/half
//                            //goalConf;
//                            //Math.max(confMin, goalConf * coherence);
//                    switch (sign) {
//                        case +1:
//                            //Pb = $.t(1f, Util.lerp(+y, confBase, feedbackConf));
//                            Pb = $.t(y/2f + 0.5f, feedConf);
//                            Nb =
//                                    //null;
//                                    $.t(0, feedConf);
//                            break;
//                        case -1:
//                            Pb =
//                                    //null;
//                                    $.t(0, feedConf);
//
//                            Nb = $.t(-y/2f + 0.5f, feedConf);
//                            break;
//                        case 0:
//                            //Pb = Nb = null; //no signal
//                            Pb = Nb = $.t(0, feedConf);
//                                    //Math.max(confMin, feedConf);
//                                    //w2cSafe(c2wSafe(feedConf)/2f))); //zero
//                            break;
//                        default:
//                            throw new UnsupportedOperationException();
//                    }
                    Pg = null;
                    Ng = null;


//                    if (curious) {
//                        e[0] = e[1] = 0; //reset to get full evidence override
//                    }
//                    float g0 = eviMax - e[0];
//                    Pg = g0 >= eviMin ? new PreciseTruth(yp, g0, false) : null;
//                    float g1 = eviMax - e[1];
//                    Ng = g1 >= eviMin ? new PreciseTruth(yn, g1, false) : null;
                } else {
                    Pb = Nb = Pg = Ng = null;
                }


                //System.out.println(Pb + "," + Nb + " <- " + g[0] + ";" + c[0] + ", " + g[1] + ';' + c[1]);

                CC[0].feedback(Pb, Pg, n);

                CC[1].feedback(Nb, Ng, n);


            }
        };

        CauseChannel<ITask> cause = nar().newCauseChannel(s);
        GoalActionAsyncConcept p = new GoalActionAsyncConcept(pt, this, cause, u);
        GoalActionAsyncConcept n = new GoalActionAsyncConcept(nt, this, cause, u);

        addAction(p);
        addAction(n);

        CC[0] = p; CC[1] = n;
        return CC;
    }

    default GoalActionConcept actionUnipolar(@NotNull Term s, @NotNull FloatToFloatFunction update) {
        return actionUnipolar(s, true, (x)->Float.NaN, update);
    }


    /**
     * update function receives a value in 0..1.0 corresponding directly to the present goal frequency
     */
    default GoalActionConcept actionUnipolar(@NotNull Term s, boolean freqOrExp, FloatToFloatFunction ifGoalMissing, @NotNull FloatToFloatFunction update) {



        final float[] lastF = {0.5f};
        return action(s, (b, g) -> {
            float gg = (g != null) ?
                    (freqOrExp ? g.freq() : g.expectation()) : ifGoalMissing.valueOf(lastF[0]);

            lastF[0] = gg;

            float bFreq = (gg == gg) ? update.valueOf(gg) : Float.NaN;
            if (bFreq == bFreq) {
                float confFeedback =
                        nar().confDefault(BELIEF);
                        //d!=null ? d.conf() : ..
                        //nar().confMin.floatValue() * 2;

                return $.t(bFreq, confFeedback);
            } else
                return null;

        });
    }

    /**
     * supplies values in range -1..+1, where 0 ==> expectation=0.5
     */
    @NotNull
    default GoalActionConcept actionExpUnipolar(@NotNull Term s, @NotNull FloatToFloatFunction update) {
        final float[] x = {0f}, xPrev = {0f};
        //final FloatNormalized y = new FloatNormalized(()->x[0]);
        return action(s, (b, d) -> {
            float o = (d != null) ?
                    //d.freq()
                    d.expectation() - 0.5f
                    : xPrev[0]; //0.5f /*Float.NaN*/;
            float ff;
            if (o >= 0f) {
                //y.relax(0.9f);
                //x[0] = o;
                float fb = update.valueOf(o /*y.asFloat()*/);
                if (fb != fb) {
                    //f = returxPrev[0];
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


