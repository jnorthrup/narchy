package nars.game;

import jcog.Skill;
import jcog.TODO;
import jcog.Util;
import jcog.data.atomic.AtomicFloat;
import jcog.math.FloatRange;
import jcog.math.FloatSupplier;
import jcog.util.FloatConsumer;
import nars.$;
import nars.NAL;
import nars.NAR;
import nars.attention.What;
import nars.game.action.ActionSignal;
import nars.game.action.GoalActionConcept;
import nars.game.util.UnipolarMotor;
import nars.term.Term;
import nars.term.atom.IdempotInt;
import nars.truth.Truth;
import org.eclipse.collections.api.block.function.primitive.FloatFloatToObjectFunction;
import org.eclipse.collections.api.block.function.primitive.FloatToFloatFunction;
import org.eclipse.collections.api.block.predicate.primitive.BooleanPredicate;
import org.eclipse.collections.api.block.procedure.primitive.BooleanProcedure;
import org.eclipse.collections.api.block.procedure.primitive.IntProcedure;
import org.jetbrains.annotations.Nullable;

import java.util.function.BooleanSupplier;
import java.util.function.IntConsumer;
import java.util.function.IntPredicate;

import static nars.Op.BELIEF;

/**
 * Created by me on 9/30/16.
 */
@Skill({"Actuator", "Muscle", "Switch"}) public interface NAct {

    Term POS = IdempotInt.the(+1); ////$.the("\"+\"");
    Term NEG = IdempotInt.the(-1); //$.the("\"-\"");


    default NAR nar() {
        return what().nar;
    }

    What what();


    /**
     * TODO make BooleanPredicate version for feedback
     */
    default void actionToggle(Term t, float thresh, float defaultValue /* 0 or NaN */, float momentumOn, Runnable on, Runnable off) {


        float[] last = {(float) 0};
        actionUnipolar(t, new FloatToFloatFunction() {
            @Override
            public float valueOf(float f) {

                float f1 = f;
                boolean unknown = (f1 != f1) || (f1 < thresh && (f1 > (1f - thresh)));
                if (unknown) {
                    f1 = defaultValue == defaultValue ? defaultValue : last[0];
                }

                if (last[0] > 0.5f)
                    f1 = Util.lerp(momentumOn, f1, last[0]);

                boolean positive = f1 > 0.5f;


                if (positive) {
                    on.run();
                    return last[0] = 1f;
                } else {
                    off.run();
                    return last[0] = 0f;
                }
            }
        });
    }


    default @Nullable Truth toggle(@Nullable Truth d, Runnable on, Runnable off, boolean next) {
        float freq;
        if (next) {
            freq = (float) +1;
            on.run();
        } else {
            freq = 0f;
            off.run();
        }

        return $.INSTANCE.t(freq,

                nar().confDefault(BELIEF) /*d.conf()*/);
    }


    <A extends ActionSignal> A addAction(A c);

    default @Nullable GoalActionConcept actionTriStateContinuous(Term s, IntPredicate i) {

        GoalActionConcept m = new GoalActionConcept(s, new ActionSignal.MotorFunction() {
            @Override
            public @Nullable Truth apply(@Nullable Truth b, @Nullable Truth d) {


                int ii;
                if (d == null) {
                    ii = 0;
                } else {
                    float f = d.freq();
                    float deadZoneFreqRadius =
                            //1f / 6;
                            1f / 12.0F;

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

                return $.INSTANCE.t(f, NAct.this.nar().confDefault(BELIEF));
            }
        }, nar());


        return addAction(m);
    }

    default @Nullable ActionSignal actionTriStatePWM(Term s, IntConsumer i) {
        ActionSignal m = new GoalActionConcept(s, new ActionSignal.MotorFunction() {
            @Override
            public @Nullable Truth apply(@Nullable Truth b, @Nullable Truth d) {


                int ii;
                if (d == null) {
                    ii = 0;
                } else {
                    float f = d.freq();
                    if (f == 1f) {
                        ii = +1;
                    } else if (f == (float) 0) {
                        ii = -1;
                    } else if (f > 0.5f) {
                        ii = NAct.this.nar().random().nextFloat() <= ((f - 0.5f) * 2f) ? +1 : 0;
                    } else if (f < 0.5f) {
                        ii = NAct.this.nar().random().nextFloat() <= ((0.5f - f) * 2f) ? -1 : 0;
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

                        $.INSTANCE.t(f,

                                NAct.this.nar().confDefault(BELIEF)
                        )

                        ;
            }
        }, nar());
        return addAction(m);
    }


    default void actionPushButton(Term s, Runnable r) {
        actionPushButton(s, midThresh(), r);
    }

    default void actionPushButton(Term s, FloatSupplier thresh, Runnable r) {
        actionPushButton(s, new BooleanProcedure() {
            @Override
            public void value(boolean b) {
                if (b)
                    r.run();
            }
        }, thresh);
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
        return actionPushButton(t, new BooleanPredicate() {
            @Override
            public boolean accept(boolean x) {
                on.value(x);
                return x;
            }
        }, thresh);
    }

    /** normally, feedback indicating whether the action caused any effect is HELPFUL so this method is not going to be as good as the BooleanPredicate one */
    @Deprecated default GoalActionConcept[] actionPushButtonMutex(Term l, Term r, Runnable L, Runnable R) {
        return actionPushButtonMutex(l, r, L, R, midThresh());
    }

    /** normally, feedback indicating whether the action caused any effect is HELPFUL so this method is not going to be as good as the BooleanPredicate one */
    @Deprecated default GoalActionConcept[] actionPushButtonMutex(Term l, Term r, Runnable L, Runnable R, FloatSupplier thresh) {
        return actionPushButtonMutex(l, r, new BooleanProcedure() {
            @Override
            public void value(boolean x) {
                if (x) L.run();
            }
        }, new BooleanProcedure() {
            @Override
            public void value(boolean x) {
                if (x) R.run();
            }
        }, thresh);
    }
    default GoalActionConcept[] actionPushButtonMutex(Term l, Term r, BooleanSupplier L, BooleanSupplier R, FloatSupplier thresh) {
        return actionPushButtonMutex(l, r,
                new BooleanPredicate() {
                    @Override
                    public boolean accept(boolean x) {
                        return x && L.getAsBoolean();
                    }
                },
                new BooleanPredicate() {
                    @Override
                    public boolean accept(boolean x) {
                        return x && R.getAsBoolean();
                    }
                },
            thresh, q());
    }
    /** normally, feedback indicating whether the action caused any effect is HELPFUL so this method is not going to be as good as the BooleanPredicate one */
    @Deprecated default GoalActionConcept[] actionPushButtonMutex(Term l, Term r, BooleanProcedure L, BooleanProcedure R) {
        return actionPushButtonMutex(l, r, L, R, midThresh());
    }

    /** normally, feedback indicating whether the action caused any effect is HELPFUL so this method is not going to be as good as the BooleanPredicate one */
    @Deprecated default GoalActionConcept[] actionPushButtonMutex(Term l, Term r, BooleanProcedure L, BooleanProcedure R, FloatSupplier thresh) {
        return actionPushButtonMutex(l, r,
                new BooleanPredicate() {
                    @Override
                    public boolean accept(boolean x) {
                        L.value(x);
                        return x;
                    }
                },
                new BooleanPredicate() {
                    @Override
                    public boolean accept(boolean x) {
                        R.value(x);
                        return x;
                    }
                },
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
        float delta = 1f/ (float) steps * (max - min);
        return actionStep(down,up, new IntPredicate() {
            @Override
            public boolean test(int c) {
                float before = x.asFloat();
                float next = Util.clamp(before + (float) c * delta, min, max);
                y.accept(next);
                float actualNext = x.asFloat();
                /** a significant change */
                return !Util.equals(before, actualNext, delta / 4.0F);
            }
        });

    }


    /**
     *
     *  determines a scalar value in range 0..1.0 representing the 'q' motivation for
     *  the given belief and goal truth
     */
    @FunctionalInterface @Skill("Q_Learning") interface QFunction {
        float q(@Nullable Truth belief, @Nullable Truth goal);

        QFunction GoalFreq = new QFunction() {
            @Override
            public float q(@Nullable Truth b, @Nullable Truth g) {
                return g != null ? g.freq() : 0f;
            }
        };
        QFunction GoalExp = new QFunction() {
            @Override
            public float q(@Nullable Truth b, @Nullable Truth g) {
                return g != null ? g.expectation() : 0f;
            }
        };

        QFunction GoalExpMinBeliefExp = new QFunction() {
            @Override
            public float q(@Nullable Truth b, @Nullable Truth g) {
                if (b == null) {
                    return GoalExp.q(b, g);
                } else {
                    if (g == null) {
                        return (float) 0; //TODO this could also be a way to introduce curiosity
                    } else {
                        return Util.unitize((g.expectation() - b.expectation()) / 2f + 0.5f);
                    }
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
    default GoalActionConcept[] actionPushButtonMutex(Term tl, Term tr, BooleanPredicate L, BooleanPredicate R, FloatSupplier thresh, QFunction Q) {

        assert(!tl.equals(tr));

        AtomicFloat l = new AtomicFloat((float) 0);
        AtomicFloat r = new AtomicFloat((float) 0);

//        float decay =
//                //0.5f;
//                //0.9f;
//                1f; //instant

        NAR n = nar();
        GoalActionConcept LA = action(tl, new GoalActionConcept.MotorFunction() {
            @Override
            public @Nullable Truth apply(@Nullable Truth b, @Nullable Truth g) {

                float q = Q.q(b, g);
                float qC =
                        //(g!=null ? g.expectation() : 0);
                        q;
                boolean xq = q >= thresh.asFloat();
                boolean y = L.accept(xq && qC >= r.floatValue());
                l.set(xq ? qC : (float) 0);


                float feedback =
                        (float) (y ? 1 : 0);
                float c =
                        n.confDefault(BELIEF);
                return $.INSTANCE.t(feedback, c);

            }
        });
        GoalActionConcept RA = action(tr, new GoalActionConcept.MotorFunction() {
            @Override
            public @Nullable Truth apply(@Nullable Truth b, @Nullable Truth g) {
                float q = Q.q(b, g);
                float qC =
                        //(g!=null ? g.expectation() : 0);
                        q;
                boolean xq = q >= thresh.asFloat();
                boolean y = R.accept(xq && qC >= l.floatValue());
                r.set(xq ? qC : (float) 0);

                float feedback =
                        (float) (y ? 1 : 0);
                float c =
                        n.confDefault(BELIEF);
                return $.INSTANCE.t(feedback, c);
            }
        });

//        for (GoalActionConcept x : new GoalActionConcept[]{LA, RA}) {
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
//        }

        return new GoalActionConcept[]{LA, RA};
    }

    default GoalActionConcept actionPushButton(Term t, BooleanPredicate on) {
        return actionPushButton(t, on, midThresh());
    }

    default FloatSupplier midThresh() {

        return new FloatSupplier() {
            @Override
            public float asFloat() {
                return 0.5f;
            }
        };
            //0.5f + nar().freqResolution.get()/2;


        //return () -> 0.66f;
        ///return () -> 0.5f + ScalarValue.EPSILON;
        //return () -> 0.5f + nar().freqResolution.get()/2f; ///<-ok for freq

        //return () -> 0.5f; //<- for exp
    }


    default GoalActionConcept actionPushButton(Term t, BooleanPredicate on, FloatSupplier thresh) {


        FloatToFloatFunction ifGoalMissing =
                new FloatToFloatFunction() {
                    @Override
                    public float valueOf(float x) {
                        return (float) 0;
                    }
                };

        GoalActionConcept x = actionUnipolar(t, true, ifGoalMissing, new FloatToFloatFunction() {
            @Override
            public float valueOf(float f) {
                boolean posOrNeg = f >= thresh.asFloat();
                return on.accept(posOrNeg) ?
                        1f :
                        (float) 0;  //deliberate off
                //Float.NaN; //default off
            }
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
        return actionUnipolar(s, new FloatToFloatFunction() {
            @Override
            public float valueOf(float x) {
                update.accept(x);
                return x;
            }
        });
    }

    default GoalActionConcept actionHemipolar(Term s, FloatConsumer update) {
        return actionHemipolar(s, new FloatToFloatFunction() {
            @Override
            public float valueOf(float x) {
                update.accept(x);
                return x;
            }
        });
    }

    /** maps the action range 0..1.0 to the 0.5..1.0 positive half of the frequency range.
     *  goal values <= 0.5 are squashed to zero.
     *  TODO make a negative polarity option
     */
    default GoalActionConcept actionHemipolar(Term s, FloatToFloatFunction update) {
        float epsilon = NAL.truth.TRUTH_EPSILON/ 2.0F;
        return actionUnipolar(s, new FloatToFloatFunction() {
            @Override
            public float valueOf(float raw) {
                if (raw == raw) {

                    if (raw > 0.5f + epsilon) {
                        float feedback = update.valueOf((raw - 0.5f) * 2.0F);
                        return feedback > (0.5f + epsilon) ? 0.5f + feedback / 2.0F : (float) 0;
                    } else {
                        float feedback = update.valueOf((float) 0);
                        return (float) 0; //override
                    }

                }
                return Float.NaN;
            }
        });
    }

    default GoalActionConcept actionUnipolar(Term s, FloatToFloatFunction update) {
        return actionUnipolar(s, true, new FloatToFloatFunction() {
            @Override
            public float valueOf(float x) {
                return x;
            }
        }, update);
    }

    default GoalActionConcept[] actionStep(Term down, Term up, IntProcedure each) {
        return actionStep(down, up, new IntPredicate() {
            @Override
            public boolean test(int e) {
                each.accept(e);
                return true;
            }
        });
    }

    default GoalActionConcept[] actionStep(Term down, Term up, IntPredicate each) {
        float thresh = 4.0F /6f;
        return actionPushButtonMutex(
            down,up,
                new BooleanPredicate() {
                    @Override
                    public boolean accept(boolean ifNeg) {
                        return ifNeg && each.test(-1);
                    }
                },
                new BooleanPredicate() {
                    @Override
                    public boolean accept(boolean ifPos) {
                        return ifPos && each.test(+1);
                    }
                },
                new FloatSupplier() {
                    @Override
                    public float asFloat() {
                        return thresh;
                    }
                },
            q()
        );
//        return actionTriStateContinuous(down, each);
    }


    /**
     * update function receives a value in 0..1.0 corresponding directly to the present goal frequency
     */
    default GoalActionConcept actionUnipolar(Term s, boolean freqOrExp, FloatToFloatFunction ifGoalMissing, FloatToFloatFunction update) {

        ActionSignal.MotorFunction motor = new UnipolarMotor(freqOrExp, ifGoalMissing, update,
                new FloatFloatToObjectFunction<Truth>() {
                    @Override
                    public Truth value(float feedbackFreq, float goalConf) {
                        return $.INSTANCE.t(feedbackFreq,
                                NAct.this.nar().confDefault(BELIEF)
                                //Math.max(nar().confMin.floatValue(), goalConf)
                        );
                    }
                }
        );

        return addAction(new GoalActionConcept(s, motor, nar()));
    }


    default BooleanPredicate debounce(Runnable f, float durations) {
        return debounce(new BooleanProcedure() {
            @Override
            public void value(boolean x) {
                if (x) f.run();
            }
        }, durations);
    }

    default BooleanPredicate debounce(BooleanProcedure f, float durations) {
        return debounce(new BooleanPredicate() {
            @Override
            public boolean accept(boolean x) {
                f.value(x);
                return x;
            }
        }, durations);
    }
    
    default BooleanPredicate debounce(BooleanPredicate f, float durations) {

        long[] last = { Long.MIN_VALUE };

        return new BooleanPredicate() {
            @Override
            public boolean accept(boolean x) {
                boolean y = false;
                if (x) {
                    What w = NAct.this.what();
                    long now = w.time();
                    long prev = last[0];
                    float period = durations * w.durPhysical();
                    if (prev == Long.MIN_VALUE) prev = (long) ((double) now - Math.ceil((double) period));
                    if ((float) (now - prev) >= period) {
                        last[0] = now;
                        y = true;
                    }
                }
                return f.accept(y);
            }
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


