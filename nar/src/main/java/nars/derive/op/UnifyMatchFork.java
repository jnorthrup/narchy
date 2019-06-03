package nars.derive.op;

import nars.derive.model.Derivation;
import nars.term.Term;
import nars.term.buffer.TermBuffer;
import org.eclipse.collections.impl.set.mutable.UnifiedSet;

import java.util.Set;
import java.util.function.Predicate;

public final class UnifyMatchFork extends TermBuffer implements Predicate<Derivation> {

    private int forkLimit = -1;
    final Set<Term> tried = new UnifiedSet(4, 0.99f);
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

        if (y.unneg().op().taskable && tried.add(y)) {

            taskify.apply(y, d);

            return tried.size() < forkLimit; //CUT
        }

        return true; //CONTINUE
    }



}
