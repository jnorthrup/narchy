package nars.term.control;

import nars.$;
import nars.Op;
import nars.derive.premise.PreDerivation;
import nars.term.Term;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.function.Function;

import static nars.derive.Derivation.Belief;
import static nars.derive.Derivation.Task;

/**
 * TODO generify key/value
 */
public final class OpSwitch<D extends PreDerivation> extends AbstractPred<D> {

    public final EnumMap<Op, PrediTerm<D>> cases;
    /*@Stable*/
    public final PrediTerm[] swtch;
    public final boolean taskOrBelief;

    @Override
    public boolean test(PreDerivation m) {

        PrediTerm p = branch(m);
        if (p != null)
            return p.test(m);

        return true;
    }

    @Override
    public float cost() {
        return 0.2f;
    }

    public OpSwitch(boolean taskOrBelief, EnumMap<Op, PrediTerm<D>> cases) {
        super(/*$.impl*/ $.func("op", taskOrBelief ? Task  : Belief,
                $.p(cases.entrySet().stream().map(e -> $.p($.quote(e.getKey().toString()), e.getValue())).toArray(Term[]::new))));

        swtch = new PrediTerm[24]; 
        cases.forEach((k, v) -> swtch[k.id] = v);
        this.taskOrBelief = taskOrBelief;
        this.cases = cases;
    }

    @Override
    public PrediTerm<D> transform(Function<PrediTerm<D>, PrediTerm<D>> f) {
        EnumMap<Op, PrediTerm<D>> e2 = cases.clone();
        final boolean[] changed = {false};
        e2.replaceAll(((k, x) -> {
            PrediTerm<D> y = x.transform(f);
            if (y != x)
                changed[0] = true;
            return y;
        }));
        if (!changed[0])
            return this;
        else
            return new OpSwitch(taskOrBelief, e2);
    }


    @Nullable
    public PrediTerm<D> branch(PreDerivation m) {
        return swtch[(taskOrBelief ? m.taskTerm : m.beliefTerm).op().id];
    }






}
