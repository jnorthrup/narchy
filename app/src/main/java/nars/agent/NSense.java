package nars.agent;

import jcog.Util;
import jcog.event.On;
import jcog.math.*;
import nars.$;
import nars.NAR;
import nars.Narsese;
import nars.Op;
import nars.concept.signal.DigitizedScalar;
import nars.concept.signal.Signal;
import nars.control.channel.CauseChannel;
import nars.task.ITask;
import nars.term.Term;
import nars.term.atom.Atomic;
import nars.truth.Truth;
import org.eclipse.collections.api.block.function.primitive.FloatToObjectFunction;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.Map;
import java.util.function.*;

import static nars.$.*;

/**
 * agent sensor builder
 */
public interface NSense {

    @NotNull Atomic LOW = Atomic.the("low");
    @NotNull Atomic MID = Atomic.the("mid");
    @NotNull Atomic HIH = Atomic.the("hih");

    @NotNull
    static Term switchTerm(String a, String b) throws Narsese.NarseseException {
        return switchTerm($(a), $(b));
    }

    @NotNull
    static Term switchTerm(Term a, Term b) {

        return p(a, b);
    }

    @NotNull Map<Signal, CauseChannel<ITask>> sensors();

    NAR nar();

    @NotNull
    default Signal sense(@NotNull Term term, @NotNull BooleanSupplier value) {
        return sense(term, () -> value.getAsBoolean() ? 1f : 0f);
    }

    @NotNull
    default Signal sense(@NotNull Term term, FloatSupplier value) {
        return sense(term, value, (x) -> $.t(x, nar().confDefault(Op.BELIEF)));
    }

    @NotNull
    default Signal sense(@NotNull Term term, FloatSupplier value, FloatToObjectFunction<Truth> truthFunc) {
        Signal s = new Signal(term, value, nar());
        addSensor(s);
        return s;
    }

    @NotNull
    default Signal sense(CauseChannel c, Term term, FloatSupplier value, FloatToObjectFunction<Truth> truthFunc) {
        Signal s = new Signal(term, value, nar());
        addSensor(s, c);
        return s;
    }

    default void addSensor(Signal s, CauseChannel cause) {
        CauseChannel<ITask> existing = sensors().put(s, cause);
        assert (existing == null);
    }

    default void addSensor(Signal c) {
        CauseChannel<ITask> cause = nar().newChannel(c);
        addSensor(c, cause);
    }

    /**
     * learning rate
     */
    default float alpha() {
        return nar().confDefault(Op.BELIEF);
    }

    /**
     * interpret an int as a selector between enumerated values
     */
    default <E extends Enum> void senseSwitch(String term, @NotNull Supplier<E> value) throws Narsese.NarseseException {
        E[] values = ((Class<? extends E>) value.get().getClass()).getEnumConstants();
        for (E e : values) {
            Term t = switchTerm(term, e.toString());
            sense(t, () -> value.get() == e);
        }
    }

    default void senseSwitch(Term term, @NotNull IntSupplier value, int min, int max) {
        senseSwitch(term, value, Util.intSequence(min, max));
    }

    /**
     * interpret an int as a selector between (enumerated) integer values
     */
    default void senseSwitch(Term term, @NotNull IntSupplier value, @NotNull int[] values) {
        for (int e : values) {
            Term t = switchTerm(term, the(e));
            sense(t, () -> value.getAsInt() == e);
        }
    }

    /**
     * interpret an int as a selector between (enumerated) object values
     */
    default <O> void senseSwitch(String term, @NotNull Supplier<O> value, @NotNull O... values) throws Narsese.NarseseException {
        for (O e : values) {
            Term t = switchTerm(term, '"' + e.toString() + '"');
            sense(t, () -> value.get().equals(e));
        }
    }


    default void senseFields(String id, @NotNull Object o) {
        Field[] ff = o.getClass().getDeclaredFields();
        for (Field f : ff) {
            if (Modifier.isPublic(f.getModifiers())) {
                sense(id, o, f.getName());
            }
        }
    }


    default void sense(String id, Object o, @NotNull String exp) {


    }

    @NotNull
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
        Signal c = new Signal(id, v, nar()
        );
        addSensor(c);
        return c;
    }

    @NotNull
    default DigitizedScalar senseNumber(FloatSupplier v, DigitizedScalar.ScalarEncoder model, Term... states) {

        assert (states.length > 1);

        DigitizedScalar fs = new DigitizedScalar(
                new FloatCached(v, nar()::time),
                model, nar(),
                states
        );


        onFrame(fs);
        return fs;
    }

    On onFrame(Consumer r);

    @NotNull
    default DigitizedScalar senseNumber(IntFunction<Term> levelTermizer, FloatSupplier v, int precision, DigitizedScalar.ScalarEncoder model) {


        return senseNumber(v, model,
                Util.map(0, precision,
                        levelTermizer, Term[]::new));
    }

    @NotNull
    default DigitizedScalar senseNumberBi(Term id, FloatSupplier v) {
        return senseNumber(v, DigitizedScalar.FuzzyNeedle, p(id, LOW), p(id, HIH));
    }

    @NotNull
    default DigitizedScalar senseNumberTri(Term id, FloatSupplier v) {
        return senseNumber(v, DigitizedScalar.Needle, p(id, LOW), p(id, MID), p(id, HIH));
    }

    default Signal senseNumber(String id, FloatSupplier v) throws Narsese.NarseseException {
        return senseNumber($(id), v);
    }


}
