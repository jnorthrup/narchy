package jcog.optimize;

import jcog.list.FasterList;
import org.eclipse.collections.api.block.procedure.primitive.ObjectFloatProcedure;
import org.eclipse.collections.api.block.procedure.primitive.ObjectIntProcedure;

import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Stream;

/** schema for optimization experimentation */
public class Tweaks<X> implements Iterable<Tweak<X>> {

    /** set of all partially or fully ready Tweaks */
    protected final List<Tweak<X>> all = new FasterList();

    /** set of all ready Tweaks */
    protected final List<Tweak<X>> ready = new FasterList();


    public Tweaks() {
    }

    public Tweaks tweak(String key, ObjectIntProcedure<X> apply) {
        return tweak(key, Float.NaN, Float.NaN, Float.NaN, (ObjectFloatProcedure<X>) (X x, float v) -> {
            apply.accept(x, Math.round(v));
        });
    }

    public Tweaks tweak(String key, int min, int max, int inc, ObjectIntProcedure<X> apply) {
        return tweak(key, min, max, inc, (ObjectFloatProcedure<X>) (X x, float v) -> {
            apply.accept(x, Math.round(v));
        });
    }

    public Tweaks tweak(String key, int min, int max, ObjectFloatProcedure<X> apply) {
        return tweak(key, min, max, 1f, apply);
    }

    public Tweaks tweak(float min, float max, float inc, ObjectFloatProcedure<X> apply) {
        return tweak(apply.toString(), min, max, inc, apply);
    }

    public Tweaks tweak(String parameter, ObjectFloatProcedure<X> apply) {
        return tweak(parameter, Float.NaN, Float.NaN,Float.NaN, apply);
    }

    public Tweaks tweak(String id, float min, float max, float inc, ObjectFloatProcedure<X> apply) {
        all.add(new TweakFloat<>(id, min, max, inc, apply));
        return this;
    }


    /** whether all parameters are specified and ready for experimentation
     * @param hints*/
    public SortedSet<String> unknown(Map<String, Float> hints) {
        ready.clear();

        TreeSet<String> unknowns = new TreeSet<>();
        for (Tweak<X> t : all) {
            List<String> u = t.unknown(hints);
            if (u.isEmpty()) {
                ready.add(t);
            } else {
                unknowns.addAll(u);
            }
        }

        return unknowns;
    }

    public int size() {
        return ready.size();
    }
    public Tweak<X> get(int i) {
        return ready.get(i);
    }

    public Stream<Tweak<X>> stream() {
        return ready.stream();
    }

    @Override
    public Iterator<Tweak<X>> iterator() {
        return ready.iterator();
    }

    public Optimize<X> optimize(Supplier<X> subjects) {
        return new Optimize(subjects, this);
    }
}