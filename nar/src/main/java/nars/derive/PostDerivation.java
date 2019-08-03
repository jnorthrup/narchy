package nars.derive;

import jcog.pri.UnitPri;
import nars.Task;
import nars.term.Term;
import nars.truth.Truth;

import java.util.function.Consumer;
import java.util.function.Function;

public class PostDerivation extends UnitPri implements Consumer<Derivation> {

    final byte punc;
    final Truth truth;
    final Function<Term,Term> unanon;
    private final Consumer<Derivation> taskify;
    private final Task task, belief;
    private final Term beliefTerm;
    private final boolean temporal;

    public PostDerivation(Derivation d, Consumer<Derivation> taskify) {
        super(d.parentPri());
        this.belief = d._belief;
        this.beliefTerm = d.beliefTerm;
        this.task = d._task;
        this.punc = d.concPunc;
        this.truth = d.concTruth;
        this.unanon = d.anon::get; //TODO cache this in a trimmed shared version
        this.temporal = d.temporal;
        this.taskify = taskify;

    }

    @Override
    public void accept(Derivation d) {
        d._task = task; d._belief = belief; d.beliefTerm = beliefTerm;
        d.concTruth = truth;
        d.concPunc = punc;
//        d.unanon = unanon;
        d.temporal = temporal;
        taskify.accept(d);
    }
}
