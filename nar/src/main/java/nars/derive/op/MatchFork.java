package nars.derive.op;

import nars.NAL;
import nars.derive.Derivation;
import nars.term.Term;
import nars.term.atom.Bool;
import nars.term.compound.LazyCompoundBuilder;
import nars.term.util.transform.AbstractTermTransform;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;

public final class MatchFork extends LazyCompoundBuilder implements Predicate<Derivation> {

    private int forkLimit = -1;
    final Set<Term> tried = new HashSet();
    private Taskify taskify;

    public MatchFork() {
    }

    public void reset(Taskify taskify, int forkLimit) {
        this.taskify = taskify;
        this.forkLimit = forkLimit;
        tried.clear();
    }

    @Override
    public boolean test(Derivation d) {

        Term x = taskify.pattern(d);

        Term y = AbstractTermTransform.transform(x, d.transform, this,
                (int)Math.ceil(NAL.derive.TERMIFY_TERM_VOLMAX_SCRATCH_FACTOR * d.termVolMax)
        );

        if (!(y instanceof Bool) && y.unneg().op().taskable) {

            if (forkLimit == 1 || tried.add(y)) {

                taskify.apply(y, d);

                if (forkLimit == 1 || tried.size() >= forkLimit)
                    return false; //CUT
            }
        }

        return true; //CONTINUE
    }



}
