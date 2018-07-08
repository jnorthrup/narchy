package nars.concept;

import jcog.bag.Bag;
import jcog.data.map.CompactArrayMap;
import jcog.pri.PriReference;
import nars.NAR;
import nars.Op;
import nars.Task;
import nars.concept.util.ConceptBuilder;
import nars.concept.util.ConceptState;
import nars.link.TaskLink;
import nars.link.TermLinker;
import nars.table.BeliefTable;
import nars.table.QuestionTable;
import nars.term.Term;
import nars.term.Termed;

import java.util.function.Function;
import java.util.stream.Stream;

import static nars.concept.util.ConceptState.New;

/** a 'blank' concept which does not store any tasks */
public class NodeConcept implements Concept {

    public final Term term;
    public final Bag<?, TaskLink> taskLinks;
    public final Bag<Term, PriReference<Term>> termLinks;
    public transient ConceptState state = New;
    private final TermLinker linker;

    private final int hash;

    protected final CompactArrayMap<String, Object> meta = new CompactArrayMap<>();

    public NodeConcept(Term term, NAR nar) {
        this(term, nar.conceptBuilder);
    }
    public NodeConcept(Term term, TermLinker linker, NAR nar) {
        this(term, linker, nar.conceptBuilder);
    }

    public NodeConcept(Term term, ConceptBuilder b) {
        this(term, b.termlinker(term), b.newLinkBags(term));
    }

    public NodeConcept(Term term, TermLinker linker, ConceptBuilder b) {
        this(term, linker, b.newLinkBags(term));
    }

    public NodeConcept(Term term, TermLinker linker, Bag[] bags) {
        assert (term.op().conceptualizable): term + " not conceptualizable";
        this.term = term;
        this.termLinks = bags[0];
        this.taskLinks = bags[1];
        this.hash = term.hashCode();

        this.linker = linker;
//        if (Param.DEBUG_EXTRA) {
//            for (Term target : this.linker) {
//                if (!target.term().equals(target.term().concept())) {
//                    throw new RuntimeException("attempted non-root linkage: " + target);
//                }
//            }
//        }

    }


    @Override public BeliefTable beliefs() { return BeliefTable.Empty; }

    @Override public BeliefTable goals() { return BeliefTable.Empty; }

    @Override public QuestionTable questions() { return QuestionTable.Empty; }

    @Override public QuestionTable quests() { return QuestionTable.Empty; }

    @Override
    public Term term() {
        return term;
    }


    @Override
    public final /*@NotNull*/ Op op() {
        return term.op();
    }


    @Override
    public Bag<?, TaskLink> tasklinks() {
        return taskLinks;
    }

    @Override
    public Bag<Term, PriReference<Term>> termlinks() {
        return termLinks;
    }


    @Override
    public final TermLinker linker() {
        return linker;
    }

    @Override
    public Stream<Task> tasks(boolean includeBeliefs, boolean includeQuestions, boolean includeGoals, boolean includeQuests) {
        return Stream.empty();
    }

    @Override
    public final ConceptState state() {
        return state;
    }

    @Override
    public ConceptState state(ConceptState state) {
        ConceptState current = this.state;
        if (current != state) {
            this.state = state;
            stateChanged();
        }
        return state;
    }

    protected void stateChanged() {
        termLinks.setCapacity(state.linkCap(this, true));
        taskLinks.setCapacity(state.linkCap(this, false));
    }


    @Override
    public final boolean equals(Object obj) {
        
        return this == obj || (obj instanceof Termed && term.equals(((Termed) obj).term()));
        
    }

    @Override
    public final int hashCode() {
        return hash;
    }

    @Override
    public final String toString() {
        return term.toString();
    }

//    @Override
//    public int subs() {
//        return term.subs();
//    }
//
//    @Override
//    public int varIndep() {
//        return term.varIndep();
//    }
//
//    @Override
//    public int varDep() {
//        return term.varDep();
//    }
//
//    @Override
//    public int varQuery() {
//        return term.varQuery();
//    }
//
//    @Deprecated
//    @Override
//    public int varPattern() {
//        return term.varPattern();
//    }
//
//    @Deprecated
//    @Override
//    public int complexity() {
//        return term.complexity();
//    }
//
//    @Deprecated
//    @Override
//    public int structure() {
//        return term.structure();
//    }
//
//    @Override
//    public int volume() {
//        return term.volume();
//    }
//
//
//    @Override
//    public boolean isNormalized() {
//        return term.isNormalized();
//    }

    @Override
    public void delete( NAR nar) {
        termLinks.delete();
        taskLinks.delete();
        meta.clear();
        state(ConceptState.Deleted);
    }


    @Override
    public <X> X meta(String key, Function<String,Object> valueIfAbsent) {
        return (X) meta.computeIfAbsent(key, valueIfAbsent);
    }

    @Override
    public void meta(String key, Object value) {
        meta.put(key, value);
    }

    @Override
    public <X> X meta(String key) {
        return (X) meta.get(key);
    }


}
