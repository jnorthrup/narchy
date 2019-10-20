package nars.term.control;

import nars.$;
import nars.Op;
import nars.derive.PreDerivation;
import nars.term.Term;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.Map;
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
        super(/*$.impl*/ $.func("op", taskOrBelief ? Task  : Belief,
                $.p(cases.entrySet().stream().map(e -> $.p($.quote(e.getKey().toString()), e.getValue())).toArray(Term[]::new))));

        swtch = new PREDICATE[24];
        for (var entry : cases.entrySet()) {
            var k = entry.getKey();
            var v = entry.getValue();
            swtch[k.id] = v;
        }
        this.taskOrBelief = taskOrBelief;
        this.cases = cases;
    }

    @Override
    public PREDICATE<D> transform(Function<PREDICATE<D>, PREDICATE<D>> f) {
        var e2 = cases.clone();
        boolean[] changed = {false};
        e2.replaceAll(((k, x) -> {
            var y = x.transform(f);
            if (y != x)
                changed[0] = true;
            return y;
        }));
		return !changed[0] ? this : new SWITCH(taskOrBelief, e2);
    }


    public @Nullable PREDICATE<D> branch(PreDerivation m) {
        return swtch[(taskOrBelief ? m.taskTerm : m.beliefTerm).opID()];
    }






}
