package nars.derive.op;

import nars.derive.model.Derivation;
import nars.term.Term;
import nars.term.buffer.EvalTermBuffer;

import java.util.function.Predicate;

public final class UnifyMatchFork extends EvalTermBuffer implements Predicate<Derivation> {

    private Taskify taskify;
    private int workVolMax;

    public UnifyMatchFork() {
    }

    public void reset(Taskify taskify, int workVolMax) {
        this.taskify = taskify;
        this.workVolMax = workVolMax;
    }

    @Override
    public boolean test(Derivation d) {

        d.nar.emotion.deriveUnified.increment();

        Term x = taskify.termify.pattern(d);

        Term y = x.transform(d.transformDerived, this, workVolMax);

        taskify.apply(y, d);

        return true; //tried.size() < forkLimit;
    }



}
