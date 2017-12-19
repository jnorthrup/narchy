package nars.derive;

import nars.$;
import nars.Op;
import nars.control.Derivation;
import nars.term.Term;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.function.Function;

/**
 * TODO generify key/value
 */
public final class OpSwitch extends AbstractPred<Derivation> {

    public final EnumMap<Op, PrediTerm<Derivation>> cases;
    public final PrediTerm[] swtch;
    public final boolean taskOrBelief;

        @Override
    public boolean test(Derivation m) {

        PrediTerm p = branch(m);
        if (p != null)
            return p.test(m);

        return true;
    }

    @Override public float cost() {
        return 0.25f;
    }

    OpSwitch(boolean taskOrBelief, @NotNull EnumMap<Op, PrediTerm<Derivation>> cases) {
        super(/*$.impl*/ $.func("op", $.the(taskOrBelief ? "task" : "belief"), $.p(cases.entrySet().stream().map(e -> $.p($.quote(e.getKey().toString()), e.getValue())).toArray(Term[]::new))));

        swtch = new PrediTerm[24]; //check this range
        cases.forEach((k, v) -> swtch[k.id] = v);
        this.taskOrBelief = taskOrBelief;
        this.cases = cases;
    }

    @Override
    public PrediTerm<Derivation> transform(Function<PrediTerm<Derivation>, PrediTerm<Derivation>> f) {
        EnumMap<Op, PrediTerm<Derivation>> e2 = cases.clone();
        final boolean[] changed = {false};
        e2.replaceAll(((k, x) -> {
            PrediTerm<Derivation> y = x.transform(f);
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
    public PrediTerm<Derivation> branch(Derivation m) {
        return swtch[taskOrBelief ? m.taskOp : m.beliefOp];
    }

//    @Override
//    public PrediTerm<Derivation> exec(Derivation d, CPU cpu) {
//        return branch(d);
//    }

}
