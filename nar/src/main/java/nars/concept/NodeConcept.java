package nars.concept;

import jcog.data.map.CompactArrayMap;
import nars.NAR;
import nars.Task;
import nars.table.BeliefTable;
import nars.table.question.QuestionTable;
import nars.term.Term;
import nars.term.Termed;

import java.util.function.Function;
import java.util.stream.Stream;



/** a 'blank' concept which does not store any tasks */
public class NodeConcept implements Concept {

    public final Term term;

    /** cached here, == target.hashCode() */
    private final int hash;

    public final CompactArrayMap<String, Object> meta = new CompactArrayMap<>();


    public NodeConcept(Term term) {
        assert (term.op().conceptualizable): term + " not conceptualizable";
        this.term = term;
        this.hash = term.hashCode();

        if (!(this instanceof PermanentConcept))
            meta.put(DELETED, DELETED); //HACK start deleted to avoid re-deleting if flyweight dynamic
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
    public Stream<Task> tasks(boolean includeBeliefs, boolean includeQuestions, boolean includeGoals, boolean includeQuests) {
        return Stream.empty();
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


    @Override
    public boolean delete( NAR nar) {

        nar.emotion.conceptDelete.increment();

        Object[] c = meta.clearPut(DELETED, DELETED);
        //            if (linker instanceof TemplateTermLinker) ((FasterList)linker).clear(); //HACK TODO maybe add Linker.clear()
        return c == null || (c.length != 2 || c[0] != DELETED);
    }

    @Override
    public <X> X meta(String key, Function<String,X> valueIfAbsent) {
        return (X) meta.computeIfAbsent(key, valueIfAbsent);
    }

    @Override
    public <X> X meta(String key, Object value) {
        return (X) meta.put(key, value);
    }

    @Override
    public <X> X meta(String key) {
        return (X) meta.get(key);
    }




}
