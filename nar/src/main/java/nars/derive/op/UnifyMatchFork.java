package nars.derive.op;

import nars.derive.model.Derivation;
import nars.term.Term;
import nars.term.atom.Bool;
import nars.term.buffer.TermBuffer;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;

public final class UnifyMatchFork extends TermBuffer implements Predicate<Derivation> {

    private int forkLimit = -1;
    final Set<Term> tried = new HashSet(16, 0.99f);
    private Taskify taskify;
    private int workVolMax;

    public UnifyMatchFork() {
    }

    public void reset(Taskify taskify, int forkLimit, int workVolMax) {
        this.taskify = taskify;
        this.forkLimit = forkLimit;
        this.workVolMax = workVolMax;
        tried.clear();
    }

    @Override
    public boolean test(Derivation d) {

        d.nar.emotion.deriveUnified.increment();

        Term x = taskify.pattern(d);

        Term y = x.transform(d.transform, this, workVolMax);

        d.nar.emotion.deriveMatchTransformed.increment();

        if (!(y instanceof Bool) && y.unneg().op().taskable) {

            if (forkLimit == 1 || tried.add(y)) {

                taskify.apply(y, d);

                return forkLimit != 1 && tried.size() < forkLimit; //CUT
            }
        }

        return true; //CONTINUE
    }



}
