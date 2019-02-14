package nars.agent;

import jcog.Util;
import jcog.event.Off;
import jcog.math.FloatCached;
import jcog.math.FloatFirstOrderDifference;
import jcog.math.FloatNormalized;
import jcog.math.FloatSupplier;
import jcog.util.FloatConsumer;
import nars.$;
import nars.NAR;
import nars.Narsese;
import nars.concept.action.BiPolarAction;
import nars.concept.sensor.DigitizedScalar;
import nars.concept.sensor.SelectorSensor;
import nars.concept.sensor.Sensor;
import nars.concept.sensor.Signal;
import nars.term.Term;
import nars.term.atom.Atomic;
import org.apache.commons.math3.util.MathUtils;
import org.eclipse.collections.api.block.function.primitive.BooleanToObjectFunction;
import org.eclipse.collections.api.block.function.primitive.FloatToFloatFunction;

import java.util.List;
import java.util.function.*;

import static nars.$.*;
import static nars.agent.NAct.NEG;
import static nars.agent.NAct.PLUS;

/**
 * agent sensor builder
 */
public interface NSense {

    Atomic LOW = Atomic.the("low");
    Atomic MID = Atomic.the("mid");
    Atomic HIH = Atomic.the("hih");


    static Term switchTerm(String a, String b) throws Narsese.NarseseException {
        return switchTerm($(a), $(b));
    }


    static Term switchTerm(Term a, Term b) {

        return p(a, b);
        //return $.prop(a,b);
    }

    NAR nar();


    default Signal sense(Term term, BooleanSupplier value) {
        return sense(term, () -> value.getAsBoolean() ? 1f : 0f);
    }





    default Signal sense(Term term, FloatSupplier value) {
        Signal s = new Signal(term, value, nar());
        addSensor(s);
        return s;
    }




    <S extends Sensor> S addSensor(S s);

    /**
     * interpret an int as a selector between enumerated values
     TODO move to a SelectorSensor constuctor */
    default <E extends Enum> void senseSwitch(String term, Supplier<E> value) throws Narsese.NarseseException {
        E[] values = ((Class<? extends E>) value.get().getClass()).getEnumConstants();
        for (E e : values) {
            sense(switchTerm(term, e.toString()), () -> value.get() == e);
        }
    }

    /** TODO move to a SelectorSensor constuctor */
    default SelectorSensor senseSwitch(Term term, IntSupplier value, int min, int max) {
        return senseSwitch((e) -> switchTerm(term, the(e)), value, min, max);
    }

    /**
     * min inclusive, max exclusive
     * TODO move to a SelectorSensor constuctor */
    default SelectorSensor senseSwitch(IntFunction<Term> termer, IntSupplier value, int min, int max) {
        return senseSwitch(value, Util.intArray(min, max), termer);
    }
    default SelectorSensor senseSwitch(IntFunction<Term> termer, IntSupplier value, int N) {
        return senseSwitch(termer, value, 0, N);
    }

//    static class EnumSignal extends AbstractSensor {
//
//        @Override
//        public void update(long last, long now, long next, NAR nar) {
//
//        }
//
//        @Override
//        public Iterable<Termed> components() {
//            return null;
//        }
//    }

    /**
     * interpret an int as a selector between (enumerated) integer values
     */
    default SelectorSensor senseSwitch(IntSupplier value, int[] values, IntFunction<Term> termizer) {
        SelectorSensor ss = new SelectorSensor(value, values, termizer, nar());
        addSensor(ss);
        return ss;
    }


    /**
     * interpret an int as a selector between (enumerated) object values
     */
    default <O> void senseSwitch(String term, Supplier<O> value, O... values) throws Narsese.NarseseException {
        for (O e : values)
            sense(switchTerm(term, '"' + e.toString() + '"'), () -> value.get().equals(e));
    }

    /*
    default void senseFields(String id, Object o) {
        Field[] ff = o.getClass().getDeclaredFields();
        for (Field f : ff) {
            if (Modifier.isPublic(f.getModifiers())) {
                sense(id, o, f.getName());
            }
        }
    }



    default void sense(String id, Object o, String exp) {

        try {
            //Object x = Ognl.parseExpression(exp);
            Object initialValue = Ognl.getValue(exp, o);


            String classString = initialValue.getClass().toString().substring(6);
            switch (classString) {
                case "java.lang.Double":
                case "java.lang.Float":
                case "java.lang.Long":
                case "java.lang.Integer":
                case "java.lang.Short":
                case "java.lang.Byte":
                case "java.lang.Boolean":
                    senseNumber(id, o, exp);
                    break;

                //TODO String

                default:
                    throw new RuntimeException("not handled: " + classString);
            }

        } catch (Exception e1) {
            e1.printStackTrace();
        }

    }*/


    default List<Signal> senseNumber(int from, int to, IntFunction<String> id, IntFunction<FloatSupplier> v) {
        List<Signal> l = newArrayList(to - from);
        for (int i = from; i < to; i++) {
            l.add(senseNumber(id.apply(i), v.apply(i)));
        }
        return l;
    }


    default Signal senseNumberDifference(Term id, FloatSupplier v) {
        return senseNumber(id,difference(v));
    }

    default DigitizedScalar senseNumberDifferenceBi(Term id, FloatSupplier v) {
        return senseNumberBi(id, difference(v));
    }

    default DigitizedScalar senseNumberDifferenceBi(Term id, float clampRange, FloatSupplier v) {
        return senseNumberBi(id, difference(v, clampRange));
    }

    default FloatNormalized difference(FloatSupplier v) {
        return new FloatNormalized(
                new FloatFirstOrderDifference(nar()::time, v).nanIfZero(), -1, +1);
    }

    default FloatNormalized difference(FloatSupplier v, float clampRange) {
        FloatFirstOrderDifference delta = new FloatFirstOrderDifference(nar()::time, v) {
            @Override
            public float asFloat() {
                float x = super.asFloat();
                if (x == x)
                    return Util.clamp(x, -clampRange, clampRange);
                return x;
            }
        };///.nanIfZero();

        return new FloatNormalized(delta, -1, +1);
    }

    default Signal senseNumber(Term id, FloatSupplier v) {
        Signal c = new Signal(id, v, nar());
        addSensor(c);
        return c;
    }



    default DigitizedScalar senseNumber(FloatSupplier v, DigitizedScalar.ScalarEncoder model, Term... states) {

        assert (states.length > 1);

        DigitizedScalar fs = new DigitizedScalar(
                new FloatCached(v, nar()::time),
                model, nar(),
                states
        );
        addSensor(fs);
        return fs;
    }

    Off onFrame(Consumer r);

    
    default DigitizedScalar senseNumber(IntFunction<Term> levelTermizer, FloatSupplier v, int precision, DigitizedScalar.ScalarEncoder model) {


        return senseNumber(v, model,
                Util.map(0, precision,
                        Term[]::new, levelTermizer));
    }

    default DigitizedScalar senseAngle(FloatSupplier angleInRadians, int divisions, Term root) {
        return senseAngle(angleInRadians, divisions, root,
                angle -> $.inh(the(angle), root)
                //angle -> $.inh($.pRadix(angle, 2, divisions-1), root)
        );
    }

    default DigitizedScalar senseAngle(FloatSupplier angleInRadians, int divisions, Term root, IntFunction<Term> termizer) {
        DigitizedScalar ang = senseNumber(termizer,
                //$.inst($.the(angle), ANGLE),
                //$.func("ang", id, $.the(angle)) /*SETe.the($.the(angle)))*/,
                //$.funcImageLast("ang", id, $.the(angle)) /*SETe.the($.the(angle)))*/,
                //$.inh( /*id,*/ $.the(angle),"ang") /*SETe.the($.the(angle)))*/,
                ()->(float) (0.5 + 0.5 * MathUtils.normalizeAngle(angleInRadians.asFloat(), 0) / (Math.PI)),
                divisions,
                //DigitizedScalar.Needle
                DigitizedScalar.FuzzyNeedle
        );
        return ang;
    }

    default Signal senseNumber(String id, FloatSupplier v) {
        return senseNumber($$(id), v);
    }

    default DigitizedScalar senseNumberBi(Term id, FloatSupplier v) {
        return senseNumber(v, DigitizedScalar.FuzzyNeedle, p(id, LOW), p(id, HIH));
    }

    default DigitizedScalar senseNumberTri(Term id, FloatSupplier v) {
        return senseNumber(v, DigitizedScalar.FuzzyNeedle, p(id, LOW), p(id, MID), p(id, HIH));
    }



    default BiPolarAction actionBipolar(Term s, FloatToFloatFunction update) {
        return actionBipolar(s, false, update);
    }

    default BiPolarAction actionBipolar(Term s, boolean fair, FloatToFloatFunction update) {
        return actionBipolarFrequencyDifferential(s, fair, update);
    }

    default BiPolarAction actionBipolarFrequencyDifferential(Term id, boolean fair, FloatToFloatFunction motor) {
        return actionBipolarFrequencyDifferential(posOrNeg -> $.inh(posOrNeg ? PLUS : NEG, id), fair, motor);
    }
    default BiPolarAction actionBipolarFrequencyDifferential(BooleanToObjectFunction<Term> s, boolean fair, FloatToFloatFunction motor) {
        BiPolarAction pn = new BiPolarAction(s,
                new BiPolarAction.DefaultPolarization(fair),
                motor, nar());

        NAgent a = (NAgent) this;
        a.addAction(pn.pos);
        a.addAction(pn.neg);

        pn.attn.reparent(a.attnAction);

        pn.pos.attn.reparent(pn.attn);
        pn.neg.attn.reparent(pn.attn);

        onFrame(x -> pn.sense(a.prev, a.now, a.nar()));
        return pn;
    }

    /**
     * tri-state implemented as delta version memory of last state.
     * initial state is neutral.
     */
    default BiPolarAction actionTriState(Term cc, IntPredicate i) {

        float deadZoneFreqRadius =
                1 / 6f;

        return actionBipolar(cc, false, (float f) -> {

            int s;
            if (f > deadZoneFreqRadius)
                s = +1;
            else if (f < -deadZoneFreqRadius)
                s = -1;
            else
                s = 0;

            if (i.test(s)) {


                switch (s) {
                    case -1:
                        return -1f;
                    case 0:
                        //return 0f;
                        return Float.NaN;
                    case +1:
                        return +1f;
                    default:
                        throw new RuntimeException();
                }

            }

            return 0f;

        });
//        float res = 0.5f;
//        g[0].resolution(res);
//        g[1].resolution(res);
//        return g;
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

            return !Util.equals(a, b);
        });


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

}
