package nars.agent;

import jcog.Util;
import jcog.event.On;
import jcog.math.*;
import jcog.util.FloatConsumer;
import nars.$;
import nars.NAR;
import nars.Narsese;
import nars.concept.action.BiPolarAction;
import nars.concept.sensor.DigitizedScalar;
import nars.concept.sensor.Sensor;
import nars.concept.sensor.Signal;
import nars.term.Term;
import nars.term.atom.Atomic;
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
     */
    default <E extends Enum> void senseSwitch(String term, Supplier<E> value) throws Narsese.NarseseException {
        E[] values = ((Class<? extends E>) value.get().getClass()).getEnumConstants();
        for (E e : values) {
            Term t = switchTerm(term, e.toString());
            sense(t, () -> value.get() == e);
        }
    }

    default void senseSwitch(Term term, IntSupplier value, int min, int max) {
        senseSwitch(value, Util.intSequence(min, max), (e) -> switchTerm(term, the(e)));
    }

    /**
     * interpret an int as a selector between (enumerated) integer values
     */
    default void senseSwitch(IntSupplier value, int[] values, IntFunction<Term> termizer) {
        for (int e : values) {
            Term t = termizer.apply(e);
            sense(t, () -> value.getAsInt() == e);
        }
    }

    /**
     * interpret an int as a selector between (enumerated) object values
     */
    default <O> void senseSwitch(String term, Supplier<O> value, O... values) throws Narsese.NarseseException {
        for (O e : values) {
            Term t = switchTerm(term, '"' + e.toString() + '"');
            sense(t, () -> value.get().equals(e));
        }
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

    
    default List<Signal> senseNumber(int from, int to, IntFunction<String> id, IntFunction<FloatSupplier> v) throws Narsese.NarseseException {
        List<Signal> l = newArrayList(to - from);
        for (int i = from; i < to; i++) {
            l.add(senseNumber(id.apply(i), v.apply(i)));
        }
        return l;
    }


    default Signal senseNumberDifference(Term id, FloatSupplier v) {
        return senseNumber(id, new FloatPolarNormalized(
                new FloatFirstOrderDifference(nar()::time, v)));
    }

    default DigitizedScalar senseNumberDifferenceBi(Term id, FloatSupplier v) {
        FloatNormalized x = new FloatPolarNormalized(
                new FloatFirstOrderDifference(nar()::time, v));

        return senseNumber(x, DigitizedScalar.FuzzyNeedle, inh(id, LOW), inh(id, HIH));
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

    On onFrame(Consumer r);

    
    default DigitizedScalar senseNumber(IntFunction<Term> levelTermizer, FloatSupplier v, int precision, DigitizedScalar.ScalarEncoder model) {


        return senseNumber(v, model,
                Util.map(0, precision,
                        levelTermizer, Term[]::new));
    }

    
    default DigitizedScalar senseNumberBi(Term id, FloatSupplier v) {
        return senseNumber(v, DigitizedScalar.FuzzyNeedle, p(id, LOW), p(id, HIH));
    }

    
    default DigitizedScalar senseNumberTri(Term id, FloatSupplier v) {
        return senseNumber(v, DigitizedScalar.Needle, p(id, LOW), p(id, MID), p(id, HIH));
    }

    default Signal senseNumber(String id, FloatSupplier v) {
        return senseNumber($$(id), v);
    }

    default BiPolarAction actionBipolar(Term s, FloatToFloatFunction update) {
        return actionBipolar(s, false, update);
    }

    default BiPolarAction actionBipolar(Term s, boolean fair, FloatToFloatFunction update) {
        return actionBipolarFrequencyDifferential(s, fair, update);
    }

    default BiPolarAction actionBipolarFrequencyDifferential(Term id, boolean fair, FloatToFloatFunction motor) {
        return actionBipolarFrequencyDifferential(posOrNeg -> $.p(id, posOrNeg ? PLUS : NEG), fair, motor);
    }
    default BiPolarAction actionBipolarFrequencyDifferential(BooleanToObjectFunction<Term> s, boolean fair, FloatToFloatFunction motor) {
        BiPolarAction a = addSensor(new BiPolarAction(s,
                new BiPolarAction.DefaultPolarization(fair, this),
                motor, nar()));

        nar().on(a.pos);
        nar().on(a.neg);
        ((NAgent)this).addAction(a.pos);
        ((NAgent)this).addAction(a.neg);
        return a;
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
                        return 0f;
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

            return !Util.equals(a, b, Float.MIN_NORMAL);
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
