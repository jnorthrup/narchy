package nars.agent;

import jcog.Util;
import jcog.math.FloatSupplier;
import jcog.util.FloatConsumer;
import nars.$;
import nars.NAR;
import nars.agent.util.UnipolarMotor;
import nars.concept.action.ActionConcept;
import nars.concept.action.GoalActionConcept;
import nars.term.Term;
import nars.truth.Truth;
import org.eclipse.collections.api.block.function.primitive.FloatToFloatFunction;
import org.eclipse.collections.api.block.predicate.primitive.BooleanPredicate;
import org.eclipse.collections.api.block.procedure.primitive.BooleanProcedure;
import org.jetbrains.annotations.Nullable;

import java.util.function.IntConsumer;
import java.util.function.IntPredicate;

import static nars.Op.BELIEF;

/**
 * Created by me on 9/30/16.
 */
public interface NAct {

    Term PLUS = $.the("\"+\"");
    Term NEG = $.the("\"-\"");


    NAR nar();


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
        actionPushButton(s, midThresh(), r);
    }

    default void actionPushButton(Term s, FloatSupplier thresh, Runnable r) {
        actionPushButton(s, b -> {
            if (b)
                r.run();
        }, thresh);
    }

    default GoalActionConcept actionToggle(Term s, BooleanProcedure onChange) {


        return actionPushButton(s, onChange);

    }

    default GoalActionConcept actionPushReleaseButton(Term t, BooleanProcedure on) {

        float thresh = 0.1f;
        return action(t, (b, g) -> {
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

    default GoalActionConcept actionPushButton(Term t, BooleanProcedure on) {
        return actionPushButton(t, on, midThresh());
    }

    default GoalActionConcept actionPushButton(Term t, BooleanProcedure on, FloatSupplier thresh) {
        return actionPushButton(t, x -> {
            on.value(x);
            return x;
        }, thresh);
    }

    /** normally, feedback indicating whether the action caused any effect is HELPFUL so this method is not going to be as good as the BooleanPredicate one */
    @Deprecated default GoalActionConcept[] actionPushButtonMutex(Term l, Term r, Runnable L, Runnable R) {
        return actionPushButtonMutex(l, r, L, R, midThresh());
    }

    /** normally, feedback indicating whether the action caused any effect is HELPFUL so this method is not going to be as good as the BooleanPredicate one */
    @Deprecated default GoalActionConcept[] actionPushButtonMutex(Term l, Term r, Runnable L, Runnable R, FloatSupplier thresh) {
        return actionPushButtonMutex(l, r, x -> {
            if (x) L.run();
        }, x -> {
            if (x) R.run();
        }, thresh);
    }

    /** normally, feedback indicating whether the action caused any effect is HELPFUL so this method is not going to be as good as the BooleanPredicate one */
    @Deprecated default GoalActionConcept[] actionPushButtonMutex(Term l, Term r, BooleanProcedure L, BooleanProcedure R) {
        return actionPushButtonMutex(l, r, L, R, midThresh());
    }

    /** normally, feedback indicating whether the action caused any effect is HELPFUL so this method is not going to be as good as the BooleanPredicate one */
    @Deprecated default GoalActionConcept[] actionPushButtonMutex(Term l, Term r, BooleanProcedure L, BooleanProcedure R, FloatSupplier thresh) {
        return actionPushButtonMutex(l, r,
                    x->{ L.value(x); return x; },
                    x->{ R.value(x); return x; },
                thresh);
    }

    default GoalActionConcept[] actionPushButtonMutex(Term l, Term r, BooleanPredicate L, BooleanPredicate R) {
        return actionPushButtonMutex(l, r, L, R, midThresh());
    }

    /**
     * TODO shared AttNode
     */
    default GoalActionConcept[] actionPushButtonMutex(Term l, Term r, BooleanPredicate L, BooleanPredicate R, FloatSupplier thresh) {

        boolean freqOrExp = false;

//        float compareThresh =
//                0;
                //sqr(Param.TRUTH_EPSILON);
        //0.5f;
        //0.5f + sqr(Param.TRUTH_EPSILON);
        //0.66f;

        float[] lr = new float[]{0f, 0f};

//        float decay =
//                //0.5f;
//                //0.9f;
//                1f; //instant

        NAR n = nar();
        GoalActionConcept LA = action(l, (b, g) -> {
            float ll = g != null ? (freqOrExp ? g.freq() : g.expectation()) : 0f;


            float t = thresh.asFloat();
            boolean x = (ll > t) && lr[1] < t; //(ll - lr[1] > compareThresh);
            boolean y = L.accept(x);
//            if (x && !y) ll = t;
            lr[0] = ll;

            return $.t(y ? 1 : 0, n.confDefault(BELIEF));

        });
        GoalActionConcept RA = action(r, (b, g) -> {
            float rr = g != null ? (freqOrExp ? g.freq() : g.expectation()) : 0f;


            float t = thresh.asFloat();
            boolean x = (rr > t) && lr[0] < t;// (rr - lr[0] > compareThresh);
            boolean y = R.accept(x);
//            if (x && !y) rr = t;
            lr[1] = rr;

            return $.t(y ? 1 : 0, n.confDefault(BELIEF));
        });

        for (GoalActionConcept x : new GoalActionConcept[]{LA, RA}) {
//            float freq = 0.5f;
//            float conf = 0.05f; //less than curiosity
//            x.goals().tables.add(new EternalTable(1));
//            x.goals().tableFirst(EternalTable.class).add(
//                    Remember.the(new NALTask(x.term(), GOAL,
//                            $.t(freq, conf), n.time(), Tense.ETERNAL, Tense.ETERNAL, n.evidence()), n), n);
//            x.beliefs().tables.add(new EternalTable(1));
//            x.beliefs().tableFirst(EternalTable.class).add(
//                    Remember.the(new NALTask(x.term(), BELIEF,
//                            $.t(0, conf), n.time(), Tense.ETERNAL, Tense.ETERNAL, n.evidence()), n), n);

            //x.resolution(0.5f);
        }

        return new GoalActionConcept[]{LA, RA};
    }

    default GoalActionConcept actionPushButton(Term t, BooleanPredicate on) {
        return actionPushButton(t, on, midThresh());
    }

    default FloatSupplier midThresh() {
        ///return () -> 0.5f + ScalarValue.EPSILON;
        //return () -> 0.5f + nar().freqResolution.get()/2f; ///<-ok for freq
        return () -> 0.5f; //<- for exp
    }


    default GoalActionConcept actionPushButton(Term t, BooleanPredicate on, FloatSupplier thresh) {


        FloatToFloatFunction ifGoalMissing =
                x -> 0; //Float.NaN;

        GoalActionConcept x = actionUnipolar(t, true, ifGoalMissing, (f) -> {
            boolean posOrNeg = f >= thresh.asFloat();
            return on.accept(posOrNeg) ?
                    1f :
                    0;  //deliberate off
            //Float.NaN; //default off
        });
        //x.resolution(0.5f);
        //{
        //resting state
        //NAR n = nar();
        //float conf =
        //        n.confMin.floatValue();
        //n.confDefault(BELIEF)/4;
        //BeliefTables xb = (BeliefTables) x.beliefs();
        //BeliefTables xg = (BeliefTables) x.goals();
        //xg.tables.add(new EternalTable(1));
//            xg.tableFirst(EternalTable.class).add(
//                    Remember.the(new NALTask(x.term(), GOAL,
//                            $.t(0, conf), n.time(), Tense.ETERNAL, Tense.ETERNAL, n.evidence()).pri(n), n), n);

//            xb.tables.add(new EternalTable(1));
//            xb.tableFirst(EternalTable.class).add(
//                    Remember.the(new NALTask(x.term(), BELIEF,
//                            $.t(0, conf), n.time(), Tense.ETERNAL, Tense.ETERNAL, n.evidence()).pri(n), n), n);
        //}
        return x;
    }


    default GoalActionConcept action(Term s, GoalActionConcept.MotorFunction update) {
        return addAction(new GoalActionConcept(s, nar(), update));
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

        ActionConcept.MotorFunction motor = new UnipolarMotor(freqOrExp, ifGoalMissing, update, f ->
                $.t(f, nar().confDefault(BELIEF))
        );

        return addAction(new GoalActionConcept(s, nar(), motor));
    }


    default BooleanPredicate debounce(Runnable f, float durations) {
        return debounce((x)-> { if (x) f.run();  }, durations);
    }

    default BooleanPredicate debounce(BooleanProcedure f, float durations) {
        return debounce((x)-> { f.value(x); return x; }, durations);
    }
    
    default BooleanPredicate debounce(BooleanPredicate f, float durations) {
        NAR n = nar();
        final long[] last = {Math.round(n.time() - durations * n.dur())};

        return (x)->{
            if (x) {
                long now = n.time();
                if (now - last[0] >= durations * n.dur()) {
                    if (f.accept(true)) {
                        last[0] = now;
                        return true;
                    }
                }
            }
            return f.accept(false);
        };
    }

//    /**
//     * the supplied value will be in the range -1..+1. if the predicate returns false, then
//     * it will not allow feedback through. this can be used for situations where the action
//     * hits a limit or boundary that it did not pass through.
//     * <p>
//     * TODO make a FloatToFloatFunction variation in which a returned value in 0..+1.0 proportionally decreasese the confidence of any feedback
//     */
//
//    default GoalActionConcept action(String s, GoalActionConcept.MotorFunction update) throws Narsese.NarseseException {
//        return action($.$(s), update);
//    }


//    default BeliefActionConcept react( Term s,  Consumer<Truth> update) {
//        return addAction(new BeliefActionConcept(s, nar(), update));
//    }
//    /**
//     * supplies values in range -1..+1, where 0 ==> expectation=0.5
//     */
//    default GoalActionConcept actionExpUnipolar(Term s, FloatToFloatFunction update) {
//        final float[] x = {0f}, xPrev = {0f};
//
//        return action(s, (b, d) -> {
//            float o = (d != null) ?
//
//                    d.expectation() - 0.5f
//                    : xPrev[0];
//            float ff;
//            if (o >= 0f) {
//
//
//                float fb = update.valueOf(o /*y.asFloat()*/);
//                if (fb != fb) {
//
//                    return null;
//                } else {
//                    xPrev[0] = fb;
//                }
//                ff = (fb / 2f) + 0.5f;
//            } else {
//                ff = 0f;
//            }
//            return $.t(unitize(ff), nar().confDefault(BELIEF));
//        });
//    }

}


