package nars.derive;

import nars.$;
import nars.Op;
import nars.term.Term;
import nars.term.pred.AbstractPred;
import nars.term.pred.PrediTerm;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.function.Function;

/**
 * TODO generify key/value
 */
public final class OpSwitch<D extends ProtoDerivation> extends AbstractPred<D> {

    public final EnumMap<Op, PrediTerm<D>> cases;
    public final PrediTerm[] swtch;
    public final boolean taskOrBelief;

    @Override
    public boolean test(ProtoDerivation m) {

        PrediTerm p = branch(m);
        if (p != null)
            return p.test(m);

        return true;
    }

    @Override
    public float cost() {
        return 0.25f;
    }

    OpSwitch(boolean taskOrBelief, @NotNull EnumMap<Op, PrediTerm<D>> cases) {
        super(/*$.impl*/ $.func("op", $.the(taskOrBelief ? "task" : "belief"), $.pFast(cases.entrySet().stream().map(e -> $.p($.quote(e.getKey().toString()), e.getValue())).toArray(Term[]::new))));

        swtch = new PrediTerm[24]; //check this range
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
    public PrediTerm<D> branch(ProtoDerivation m) {
        return swtch[taskOrBelief ? m._taskOp : m._beliefOp];
    }

//    @Override
//    public PrediTerm<D> exec(Derivation d, CPU cpu) {
//        return branch(d);
//    }

}
