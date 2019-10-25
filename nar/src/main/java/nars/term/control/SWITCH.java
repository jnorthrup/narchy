package nars.term.control;

import nars.$;
import nars.Op;
import nars.derive.PreDerivation;
import nars.term.Term;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;

import static nars.derive.util.DerivationFunctors.Belief;
import static nars.derive.util.DerivationFunctors.Task;

/**
 * TODO generify key/value
 * default impl:
 *    require keys map to integer states. then switch on the integer (not some equality / map-like comparison)
 */
public final class SWITCH<D extends PreDerivation> extends AbstractPred<D> {

    public final EnumMap<Op, PREDICATE<D>> cases;
    /*@Stable*/
    public final PREDICATE[] swtch;
    public final boolean taskOrBelief;

    @Override
    public boolean test(PreDerivation m) {

        PREDICATE p = branch(m);
        if (p != null)
            return p.test(m);

        return true;
    }

    @Override
    public float cost() {
        return 0.2f;
    }

    public SWITCH(boolean taskOrBelief, EnumMap<Op, PREDICATE<D>> cases) {
        super(/*$.impl*/ $.INSTANCE.func("op", taskOrBelief ? Task  : Belief,
                $.INSTANCE.p(cases.entrySet().stream().map(new Function<Map.Entry<Op, PREDICATE<D>>, Term>() {
                    @Override
                    public Term apply(Map.Entry<Op, PREDICATE<D>> e) {
                        return $.INSTANCE.p($.INSTANCE.quote(e.getKey().toString()), e.getValue());
                    }
                }).toArray(Term[]::new))));

        swtch = new PREDICATE[24];
        for (Map.Entry<Op, PREDICATE<D>> entry : cases.entrySet()) {
            Op k = entry.getKey();
            PREDICATE<D> v = entry.getValue();
            swtch[(int) k.id] = v;
        }
        this.taskOrBelief = taskOrBelief;
        this.cases = cases;
    }

    @Override
    public PREDICATE<D> transform(Function<PREDICATE<D>, PREDICATE<D>> f) {
        EnumMap<Op, PREDICATE<D>> e2 = cases.clone();
        boolean[] changed = {false};
        e2.replaceAll((new BiFunction<Op, PREDICATE<D>, PREDICATE<D>>() {
            @Override
            public PREDICATE<D> apply(Op k, PREDICATE<D> x) {
                PREDICATE<D> y = x.transform(f);
                if (y != x)
                    changed[0] = true;
                return y;
            }
        }));
		return !changed[0] ? this : new SWITCH(taskOrBelief, e2);
    }


    public @Nullable PREDICATE<D> branch(PreDerivation m) {
        return swtch[(taskOrBelief ? m.taskTerm : m.beliefTerm).opID()];
    }






}
