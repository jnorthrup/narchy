package nars.concept;

import nars.NAR;
import nars.Op;
import nars.Task;
import nars.bag.Bag;
import nars.budget.control.Activation;
import nars.budget.policy.ConceptState;
import nars.table.BeliefTable;
import nars.table.QuestionTable;
import nars.term.Term;
import nars.term.Terms;
import nars.term.atom.Atomic;
import nars.term.atom.AtomicStringConstant;
import nars.term.container.TermContainer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;


public class AtomConcept extends AtomicStringConstant implements Concept {

    private final Bag<Term> termLinks;
    private final Bag<Task> taskLinks;
    @Nullable
    private ConceptState state;

    @NotNull
    private final Op op;

    @Nullable
    private Map meta;

    public AtomConcept(@NotNull Atomic atom, Bag<Term> termLinks, Bag<Task> taskLinks) {
        this(atom.toString(), atom.op(), termLinks, taskLinks);
    }

    protected AtomConcept(@NotNull String term, @NotNull Op op, Bag<Term> termLinks, Bag<Task> taskLinks) {
        super(term);

        this.op = op;

        this.termLinks = termLinks;
        this.taskLinks = taskLinks;

        this.state = ConceptState.Deleted;
    }

    @NotNull
    @Override
    public Op op() {
        return op;
    }


    @Override
    public ConceptState state() {
        return state;
    }

    @Override
    public void state(@NotNull ConceptState p, NAR nar) {
        ConceptState current = this.state;
        if (current!=p) {
            this.state = p;
            linkCapacity(p.linkCap(this, true),p.linkCap(this, false));
        }
    }


    @Override
    public @NotNull Bag<Task> tasklinks() {
        return taskLinks;
    }

    @NotNull
    @Override
    public Bag<Term> termlinks() {
        return termLinks;
    }

    @Override
    public @Nullable Map<Object, Object> meta() {
        return meta;
    }

    @Override
    public void setMeta(@NotNull Map newMeta) {
        this.meta = newMeta;
    }

    @Override
    public Activation process(@NotNull Task input, NAR nar) {
        throw new UnsupportedOperationException("Atom " + this + " can not process Tasks; " + input);
    }

    @NotNull
    @Override
    public BeliefTable beliefs() {
        return BeliefTable.EMPTY;
    }

    @NotNull
    @Override
    public BeliefTable goals() {
        return BeliefTable.EMPTY;
    }

    @Override
    @NotNull
    public QuestionTable questions() {
        return QuestionTable.EMPTY;
    }

    @NotNull
    @Override
    public QuestionTable quests() {
        return QuestionTable.EMPTY;
    }


    @Override
    public void delete(@NotNull NAR nar) {
        Concept.delete(this, nar);
        meta = null;
    }
}
