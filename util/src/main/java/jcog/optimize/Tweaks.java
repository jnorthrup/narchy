package jcog.optimize;

import jcog.list.FasterList;
import jcog.util.ObjectFloatToFloatFunction;
import org.eclipse.collections.api.block.function.primitive.FloatFunction;
import org.eclipse.collections.api.block.procedure.primitive.ObjectFloatProcedure;
import org.eclipse.collections.api.block.procedure.primitive.ObjectIntProcedure;
import org.eclipse.collections.api.tuple.Pair;

import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.function.Supplier;

import static org.eclipse.collections.impl.tuple.Tuples.pair;

/** schema for optimization experimentation */
abstract public class Tweaks<X>  {

    /** set of all partially or fully ready Tweaks */
    protected final List<Tweak<X>> all = new FasterList();

    public Tweaks() {
    }

    public Tweaks tweak(String key, ObjectIntProcedure<X> apply) {
        return tweak(key, Float.NaN, Float.NaN, Float.NaN, (X x, float v) -> {
            int i = Math.round(v);
            apply.accept(x, i);
            return i;
        });
    }

    public Tweaks tweak(String key, int min, int max, int inc, ObjectIntProcedure<X> apply) {
        return tweak(key, min, max, inc < 0 ? Float.NaN : inc, (X x, float v) -> {
            int i = Math.round(v);
            apply.accept(x, i);
            return i;
        });
    }

    public Tweaks<X> tweak(String id, float min, float max, float inc, ObjectFloatProcedure<X> apply) {
        all.add(new TweakFloat<>(id, min, max, inc, (X x, float v) -> {
            apply.value(x, v);
            return v;
        }));
        return this;
    }

    public Tweaks<X> tweak(String id, float min, float max, float inc, ObjectFloatToFloatFunction<X> apply) {
        all.add(new TweakFloat<>(id, min, max, inc, apply));
        return this;
    }


    abstract public Result<X> optimize(int maxIterations, int repeats, FloatFunction<Supplier<X>> eval);

    /** whether all parameters are specified and ready for experimentation
     * @param hints*/
    public Pair<List<Tweak<X>>, SortedSet<String>> get(Map<String, Float> hints) {
        final List<Tweak<X>> ready = new FasterList();
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

        return pair(ready, unknowns);
    }

    public Optimize<X> optimize(Supplier<X> subjects) {
        return new Optimize<>(subjects, this);
    }

    public Result<X> optimize(Supplier<X> subjects, int maxIterations, int repeats, FloatFunction<Supplier<X>> eval) {

        float sampleScore = eval.floatValueOf(subjects);
        System.out.println("control score=" + sampleScore); //TODO move to supereclass

        return optimize(subjects).run(maxIterations, repeats, eval);
    }

//    public int size() {
//        return ready.size();
//    }
//    public Tweak<X> get(int i) {
//        return ready.get(i);
//    }
//
//    public Stream<Tweak<X>> stream() {
//        return ready.stream();
//    }
//
//    @Override
//    public Iterator<Tweak<X>> iterator() {
//        return ready.iterator();
//    }


}