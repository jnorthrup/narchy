package nars.agent;

import jcog.Skill;
import jcog.TODO;
import jcog.Util;
import jcog.math.FloatRange;
import jcog.math.FloatSupplier;
import jcog.util.FloatConsumer;
import nars.$;
import nars.NAR;
import nars.NAL;
import nars.agent.util.UnipolarMotor;
import nars.attention.What;
import nars.concept.action.AgentAction;
import nars.concept.action.GoalActionConcept;
import nars.term.Term;
import nars.term.atom.Int;
import nars.truth.Truth;
import org.eclipse.collections.api.block.function.primitive.FloatToFloatFunction;
import org.eclipse.collections.api.block.predicate.primitive.BooleanPredicate;
import org.eclipse.collections.api.block.procedure.primitive.BooleanProcedure;
import org.eclipse.collections.api.block.procedure.primitive.IntProcedure;
import org.jetbrains.annotations.Nullable;

import java.util.function.IntConsumer;
import java.util.function.IntPredicate;

import static nars.Op.BELIEF;

/**
 * Created by me on 9/30/16.
 */
public interface NAct {

    Term PLUS = Int.the(+1); ////$.the("\"+\"");
    Term NEG = Int.the(-1); //$.the("\"-\"");


    default NAR nar() {
        return what().nar;
    }

    What what();


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


    <A extends AgentAction> A addAction(A c);

    @Nullable
    default GoalActionConcept actionTriStateContinuous(Term s, IntPredicate i) {

        GoalActionConcept m = new GoalActionConcept(s, (b, d) -> {


            int ii;
            if (d == null) {
                ii = 0;
            } else {
                float f = d.freq();
                float deadZoneFreqRadius =
                        //1f / 6;
                        1f/12;

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
        }, nar());


        return addAction(m);
    }

    @Nullable
    default AgentAction actionTriStatePWM(Term s, IntConsumer i) {
        AgentAction m = new GoalActionConcept(s, (b, d) -> {


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
        }, nar());
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

//    default GoalActionConcept actionPushReleaseButton(Term t, BooleanProcedure on) {
//
//        float thresh = 0.1f;
//        return action(t, (b, g) -> {
//            float G = g != null ? g.expectation() : 0.0f;
//            boolean positive;
//            if (G > 0.5f) {
//                float f = G - (b != null ? b.expectation() : 0.5f);
//                positive = f >= thresh;
//            } else {
//                positive = false;
//            }
//            on.value(positive);
//            return $.t(positive ? 1 : 0, nar().confDefault(BELIEF));
//        });
//    }

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
                thresh, q());
    }

    default GoalActionConcept[] actionPushButtonMutex(Term l, Term r, BooleanPredicate L, BooleanPredicate R) {
        return actionPushButtonMutex(l, r, L, R, midThresh(), q());
    }

    default QFunction q() {
        return
                QFunction.GoalFreq;
                //QFunction.GoalExp;
                //QFunction.GoalFreqMinBeliefFreq;
                //QFunction.GoalExpMinBeliefExp;
    }

    /** adjusts increment/decrement rate according to provided curve;
     *    ex: short press moves in small steps, but pressing longer scans faster
     */
    default GoalActionConcept[] actionDial(Term down, Term up, FloatRange x, FloatToFloatFunction dursPressedToIncrement) {
        throw new TODO();
    }

    default GoalActionConcept[] actionDial(Term down, Term up, FloatRange x, int steps) {
        return actionDial(down, up, x::get, x::set, x.min, x.max, steps);
    }

    /** discrete rotary dial:
     *    a pair of up/down buttons for discretely incrementing and decrementing a value within a given range
     */
    default GoalActionConcept[] actionDial(Term down, Term up, FloatSupplier x, FloatConsumer y, float min, float max, int steps) {
        float delta = 1f/steps * (max - min);
        return actionStep(down,up, (c)->{
            float before = x.asFloat();
            float next = Util.clamp(before + c * delta, min, max);
            y.accept(next);
            float actualNext = x.asFloat();
            /** a significant change */
            return !Util.equals(before, actualNext, delta / 4);
        });

    }


    /**
     *
     *  determines a scalar value in range 0..1.0 representing the 'q' motivation for
     *  the given belief and goal truth
     */
    @FunctionalInterface @Skill("Q_Learning") interface QFunction {
        float q(@Nullable Truth belief, @Nullable Truth goal);

        QFunction GoalFreq = (b,g)-> g != null ? g.freq() : 0f;
        QFunction GoalExp = (b,g)-> g != null ? g.expectation() : 0f;

        QFunction GoalExpMinBeliefExp = (b,g)-> {
            if (b==null) {
                return GoalExp.q(b, g);
            } else {
                if (g == null) {
                    return 0; //TODO this could also be a way to introduce curiosity
                } else {
                    return Util.unitize((g.expectation() - b.expectation())/2f + 0.5f);
                }
            }
        };

    }

    /**
     * TODO shared AttNode
     * see: tristate buffer, tristable multivibrator
     * http://www.freepatentsonline.com/4990796.html
     * https://en.wikipedia.org/wiki/Three-state_logic
     */
    default GoalActionConcept[] actionPushButtonMutex(Term l, Term r, BooleanPredicate L, BooleanPredicate R, FloatSupplier thresh, QFunction Q) {

        assert(!l.equals(r));

        float[] lr = new float[]{0f, 0f};

//        float decay =
//                //0.5f;
//                //0.9f;
//                1f; //instant

        NAR n = nar();
        GoalActionConcept LA = action(l, (b, g) -> {
            float q = Q.q(b,g);


            float t = thresh.asFloat();
            boolean x = (q > t) && lr[1] <= t; //(ll - lr[1] > compareThresh);
            boolean y = L.accept(x);
            lr[0] =
                    //ll;
                    //x ? q : 0;
                    (x && y) ? q : 0;
                    //(x && y) ? ll : 0;
                    //y ? ll : 0;


            float feedback =
                    y ? 1 : 0;
                    //y ? 1 : Math.min(thresh.asFloat(),(g!=null ? g.freq() : 0));
            float c =
                    n.confDefault(BELIEF);
                    //Math.max(n.confMin.floatValue(), g!=null ? g.conf() : 0)
            return $.t(feedback, c);

        });
        GoalActionConcept RA = action(r, (b, g) -> {
            float q = Q.q(b,g);

            float t = thresh.asFloat();
            boolean x = (q > t) && lr[0] <= t;// (rr - lr[0] > compareThresh);
            boolean y = R.accept(x);
            lr[1] = //rr;
                    //x ? q : 0;
                    (x && y) ? q : 0;
                    //(x && y) ? rr : 0;
                    //y ? rr : 0;

            float feedback =
                    y ? 1 : 0;
                    //y ? 1 : Math.min(thresh.asFloat(),(g!=null ? g.freq() : 0));
            float c =
                    n.confDefault(BELIEF);
                    //Math.max(n.confMin.floatValue(), g!=null ? g.conf() : 0)
            return $.t(feedback, c);
        });

        for (GoalActionConcept x : new GoalActionConcept[]{LA, RA}) {
//            float freq = 0.5f;
//            float conf = 0.05f; //less than curiosity
//            x.goals().tables.addAt(new EternalTable(1));
//            x.goals().tableFirst(EternalTable.class).addAt(
//                    Remember.the(new NALTask(x.target(), GOAL,
//                            $.t(freq, conf), n.time(), Tense.ETERNAL, Tense.ETERNAL, n.evidence()), n), n);
//            x.beliefs().tables.addAt(new EternalTable(1));
//            x.beliefs().tableFirst(EternalTable.class).addAt(
//                    Remember.the(new NALTask(x.target(), BELIEF,
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

        GoalActionConcept x = actionUnipolar(t, false, ifGoalMissing, (f) -> {
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
        //xg.tables.addAt(new EternalTable(1));
//            xg.tableFirst(EternalTable.class).addAt(
//                    Remember.the(new NALTask(x.target(), GOAL,
//                            $.t(0, conf), n.time(), Tense.ETERNAL, Tense.ETERNAL, n.evidence()).pri(n), n), n);

//            xb.tables.addAt(new EternalTable(1));
//            xb.tableFirst(EternalTable.class).addAt(
//                    Remember.the(new NALTask(x.target(), BELIEF,
//                            $.t(0, conf), n.time(), Tense.ETERNAL, Tense.ETERNAL, n.evidence()).pri(n), n), n);
        //}
        return x;
    }


    default GoalActionConcept action(Term s, GoalActionConcept.MotorFunction update) {
        return addAction(new GoalActionConcept(s, update, nar()));
    }


    default GoalActionConcept actionUnipolar(Term s, FloatConsumer update) {
        return actionUnipolar(s, (x) -> {
            update.accept(x);
            return x;
        });
    }

    default GoalActionConcept actionHemipolar(Term s, FloatConsumer update) {
        return actionHemipolar(s, (x)->{ update.accept(x); return x; } );
    }

    /** maps the action range 0..1.0 to the 0.5..1.0 positive half of the frequency range.
     *  goal values <= 0.5 are squashed to zero.
     *  TODO make a negative polarity option
     */
    default GoalActionConcept actionHemipolar(Term s, FloatToFloatFunction update) {
        float epsilon = NAL.truth.TRUTH_EPSILON/2;
        return actionUnipolar(s, (raw)->{
            if (raw==raw) {

                if (raw > 0.5f + epsilon) {
                    float feedback = update.valueOf((raw - 0.5f) * 2);
                    return feedback > (0.5f + epsilon) ? 0.5f + feedback / 2 : 0;
                } else {
                    float feedback = update.valueOf( 0);
                    return 0; //override
                }

            }
            return Float.NaN;
        });
    }

    default GoalActionConcept actionUnipolar(Term s, FloatToFloatFunction update) {
        return actionUnipolar(s, true, (x) -> Float.NaN, update);
    }

    default GoalActionConcept[] actionStep(Term down, Term up, IntProcedure each) {
        return actionStep(down, up, (e)->{
            each.accept(e);
            return true;
        });
    }

    default GoalActionConcept[] actionStep(Term down, Term up, IntPredicate each) {
        float thresh = 4/6f;
        return actionPushButtonMutex(
            down,up,
            ifNeg -> ifNeg && each.test(-1),
            ifPos -> ifPos && each.test(+1),
            ()->thresh,
            q()
        );
//        return actionTriStateContinuous(down, each);
    }

    /**
     * update function receives a value in 0..1.0 corresponding directly to the present goal frequency
     */
    default GoalActionConcept actionUnipolar(Term s, boolean freqOrExp, FloatToFloatFunction ifGoalMissing, FloatToFloatFunction update) {

        AgentAction.MotorFunction motor = new UnipolarMotor(freqOrExp, ifGoalMissing, update,
            (feedbackFreq,goalConf) ->
                $.t(feedbackFreq,
                        //Math.max(nar().confMin.floatValue(), goalConf)
                        nar().confDefault(BELIEF)
                )
        );

        return addAction(new GoalActionConcept(s, motor, nar()));
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


