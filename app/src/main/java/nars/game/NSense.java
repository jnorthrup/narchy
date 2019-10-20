package nars.game;

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
import nars.Op;
import nars.game.action.BiPolarAction;
import nars.game.sensor.*;
import nars.term.Term;
import nars.term.atom.Atomic;
import nars.term.atom.theInt;
import org.apache.commons.math3.util.MathUtils;
import org.eclipse.collections.api.block.function.primitive.BooleanToObjectFunction;
import org.eclipse.collections.api.block.function.primitive.FloatToFloatFunction;

import java.util.List;
import java.util.function.*;

import static nars.$.*;
import static nars.game.NAct.NEG;
import static nars.game.NAct.POS;

/**
 * agent sensor builder
 */
public interface NSense {

    Atomic LOW = theInt.the(-1); //Atomic.the("low");
    Atomic MID = theInt.the(0); //Atomic.the("mid");
    Atomic HIH = theInt.the(+1); //Atomic.the("hih");


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










    <S extends GameLoop> S addSensor(S s);

    /**
     * interpret an int as a selector between enumerated values
     TODO move to a SelectorSensor constuctor */
    default <E extends Enum> void senseSwitch(String term, Supplier<E> value) throws Narsese.NarseseException {
        var values = ((Class<? extends E>) value.get().getClass()).getEnumConstants();
        for (var e : values) {
            sense(switchTerm(term, e.toString()), () -> value.get() == e);
        }
    }

    /** TODO move to a SelectorSensor constuctor */
    default SelectorSensor senseSwitch(Term term, IntSupplier value, int min, int max) {
        return senseSwitch(min, max, value, (e) -> switchTerm(term, the(e)));
    }

    /**
     * min inclusive, max exclusive
     * TODO move to a SelectorSensor constuctor */
    default SelectorSensor senseSwitch(int min, int max, IntSupplier value, IntFunction<Term> termer) {
        return senseSwitch(value, Util.intArray(min, max), termer);
    }
    default SelectorSensor senseSwitch(int N, IntSupplier value, IntFunction<Term> termer) {
        return senseSwitch(0, N, value, termer);
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
        var ss = new SelectorSensor(nar(), values, value, termizer);
        addSensor(ss);
        return ss;
    }


    /**
     * interpret an int as a selector between (enumerated) object values
     */
    default <O> void senseSwitch(String term, Supplier<O> value, O... values) throws Narsese.NarseseException {
        for (var e : values)
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
        for (var i = from; i < to; i++) {
            l.add(senseNumber(id.apply(i), v.apply(i)));
        }
        return l;
    }


    /** normalized */
    default Signal senseNumberDifference(Term id, FloatSupplier v) {
        return sense(id,difference(v));
    }

    default DigitizedScalar senseNumberDifferenceBi(Term id, FloatSupplier v) {
        return senseNumberBi(id, difference(v));
    }

    default DigitizedScalar senseNumberDifferenceBi(Term id, float clampRange, FloatSupplier v) {
        return senseNumberBi(id, difference(v, clampRange));
    }
    default DigitizedScalar senseNumberDifferenceTri(Term id, float clampRange, FloatSupplier v) {
        return senseNumberTri(id, difference(v, clampRange));
    }

    default FloatNormalized difference(FloatSupplier v) {
        return new FloatNormalized(
                new FloatFirstOrderDifference(nar()::time, v)
                    //.nanIfZero()
            , -Float.MIN_NORMAL, Float.MIN_NORMAL, true);
    }

    default FloatNormalized difference(FloatSupplier v, float clampRange) {
        var delta = new FloatFirstOrderDifference(nar()::time, v) {
            @Override
            public float asFloat() {
                var x = super.asFloat();
                if (x == x)
                    return Util.clamp(x, -clampRange, clampRange);
                return x;
            }
        };///.nanIfZero();

        return new FloatNormalized(delta, -1, +1);
    }

    default Signal sense(Term id, FloatSupplier v) {
        Signal c = new ScalarSignal(id, v, nar());
        addSensor(c);
        return c;
    }

    default DigitizedScalar senseNumber(FloatSupplier v, DigitizedScalar.ScalarEncoder model, Term... states) {

        assert (states.length > 1);

        var fs = new DigitizedScalar(
                new FloatCached(v, nar()::time),
                model, nar(),
                states
        );
        addSensor(fs);
        return fs;
    }

    Off onFrame(Consumer r);

    
    default DigitizedScalar senseNumber(int precision, DigitizedScalar.ScalarEncoder model, FloatSupplier v, IntFunction<Term> levelTermizer) {


        return senseNumber(v, model,
                Util.map(0, precision,
                        Term[]::new, levelTermizer));
    }

    default DigitizedScalar senseAngle(FloatSupplier angleInRadians, int divisions, Term root) {
        return senseAngle(divisions, root, angleInRadians,
                angle -> $.inh(root,the(angle))
                //angle -> $.inh($.pRadix(angle, 2, divisions-1), root)
        );
    }

    default DigitizedScalar senseAngle(int divisions, Term root, FloatSupplier angleInRadians, IntFunction<Term> termizer) {
        var ang = senseNumber(divisions, DigitizedScalar.FuzzyNeedle, ()->(float) (0.5 + 0.5 * MathUtils.normalizeAngle(angleInRadians.asFloat(), 0) / (2 * Math.PI)), termizer
                //$.inst($.the(angle), ANGLE),
                //$.func("ang", id, $.the(angle)) /*SETe.the($.the(angle)))*/,
                //$.funcImageLast("ang", id, $.the(angle)) /*SETe.the($.the(angle)))*/,
                //$.inh( /*id,*/ $.the(angle),"ang") /*SETe.the($.the(angle)))*/,
                //DigitizedScalar.Needle
        );
        return ang;
    }

    default Signal senseNumber(String id, FloatSupplier v) {
        return sense($$(id), v);
    }

    default DigitizedScalar senseNumberBi(Term id, FloatSupplier v) {
        return senseNumber(v, DigitizedScalar.FuzzyNeedle, inh(id, LOW), inh(id, HIH));
    }

    default DigitizedScalar senseNumberTri(Term id, FloatSupplier v) {
        return senseNumber(v, DigitizedScalar.FuzzyNeedle, inh(id, LOW), inh(id, MID), inh(id, HIH));
    }



    default BiPolarAction actionBipolar(Term s, FloatToFloatFunction update) {
        return actionBipolar(s, false, update);
    }

    default BiPolarAction actionBipolar(Term s, boolean fair, FloatToFloatFunction update) {
        return actionBipolarFrequencyDifferential(s, fair, update);
    }

    default BiPolarAction actionBipolarFrequencyDifferential(Term template, boolean fair, FloatToFloatFunction motor) {
        if (!template.hasAny(Op.VAR_QUERY)) {
            //throw new TermException("has no query variables", template);
            template = $.inh(template, $.varQuery(1));
        }

        var t = template;
        return actionBipolarFrequencyDifferential(
                fair, posOrNeg -> t.replace($.varQuery(1), posOrNeg ? POS : NEG),
                motor);
    }
    default BiPolarAction actionBipolarFrequencyDifferential(boolean fair, BooleanToObjectFunction<Term> s, FloatToFloatFunction motor) {
        var pn = new BiPolarAction(s,
                new BiPolarAction.DefaultPolarization(fair),
                motor, nar());

        var g = (Game) this;
//        a.addAction(pn.pos);
//        a.addAction(pn.neg);

        var nar = g.nar();

        //HACK until BipolarAction imjplemetns Action
        nar.add(pn);
        g.addAction(pn.pos);
        g.addAction(pn.neg);
        g.onFrame(aa->pn.accept(g));

        nar.control.input(pn.pri, g.actionPri);

        nar.control.input(pn.pos.pri, pn.pri);

        nar.control.input(pn.neg.pri, pn.pri);

        return pn;
    }

    /**
     * tri-state implemented as delta version memory of last state.
     * initial state is neutral.
     */
    default BiPolarAction actionTriState(Term cc, IntPredicate i) {

        var deadZoneFreqRadius =
                1 / 6f;

        return actionBipolar(cc, false, f -> {

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
        var amp = new float[1];
        var dt = 0.1f;
        var max = 1f;
        var decay = 0.9f;
        actionTriState(s, (i) -> {
            var a = amp[0];
            var b = Util.clamp((a * decay) + dt * i, -max, max);
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
