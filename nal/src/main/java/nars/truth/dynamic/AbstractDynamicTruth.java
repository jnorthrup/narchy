package nars.truth.dynamic;

import jcog.pri.bag.Bag;
import nars.NAR;
import nars.Op;
import nars.Param;
import nars.Task;
import nars.concept.Concept;
import nars.concept.TaskConcept;
import nars.concept.util.ConceptBuilder;
import nars.table.BeliefTable;
import nars.table.BeliefTables;
import nars.table.dynamic.DynamicTruthTable;
import nars.task.util.Answer;
import nars.term.Term;
import nars.term.atom.Bool;
import nars.truth.Truth;

import java.util.List;
import java.util.function.Predicate;

import static nars.Op.*;

/**
 * Created by me on 12/4/16.
 */
abstract public class AbstractDynamicTruth {

    abstract public Truth apply(DynEvi var1, NAR nar);

    public final DynStampEvi eval(final Term superTerm, boolean beliefOrGoal, long start, long end, Predicate<Task> superFilter, boolean forceProjection, NAR n) {

        assert (superTerm.op() != NEG);

        DynStampEvi d = new DynStampEvi(0); //TODO pool?


        Predicate<Task> filter =
                Param.DYNAMIC_TRUTH_STAMP_OVERLAP_FILTER ?
                        Answer.filter(superFilter, d::doesntOverlap) :
                        superFilter;


        //TODO expand the callback interface allowing models more specific control over matching/answering/sampling subtasks

        return components(superTerm, start, end, (Term subTerm, long subStart, long subEnd) -> {

            if (subTerm instanceof Bool)
                return false;

            boolean negated = subTerm.op() == Op.NEG;
            if (negated)
                subTerm = subTerm.unneg();

            Concept subConcept = n.conceptualizeDynamic(subTerm);
            if (!(subConcept instanceof TaskConcept))
                return false;

            BeliefTable table = (BeliefTable) subConcept.table(beliefOrGoal ? BELIEF : GOAL);
            Task bt = forceProjection ? table.answer(subStart, subEnd, subTerm, filter, n) :
                    table.match(subStart, subEnd, subTerm, filter, n);
            if (bt == null)
                return false;

            if (!acceptComponent(superTerm, subTerm, bt))
                return false;

            /* project to a specific time, and apply negation if necessary */
            bt = Task.project(forceProjection, true, bt, subStart, subEnd, n, negated);
            if (bt == null)
                return false;

            return d.add(bt);

        }) ? d : null;
    }

    public abstract boolean components(Term superterm, long start, long end, ObjectLongLongPredicate<Term> each);

    /**
     * used to reconstruct a dynamic term from some or all components
     */
    abstract public Term reconstruct(Term superterm, List<Task> c, NAR nar, long start, long end);

    /**
     * allow filtering of resolved Tasks
     */
    public boolean acceptComponent(Term superTerm, Term componentTerm, Task componentTask) {
        return true;
    }

    public BeliefTable newTable(Term t, boolean beliefOrGoal, ConceptBuilder cb) {
        return new BeliefTables(
                new DynamicTruthTable(t, this, beliefOrGoal),
                cb.newTemporalTable(t, beliefOrGoal),
                cb.newEternalTable(t)
        );
    }

    public Bag newTaskLinkBag(Term t, ConceptBuilder b) {
        return b.newLinkBag(t);
    }

    @FunctionalInterface
    public interface ObjectLongLongPredicate<T> {
        boolean accept(T object, long start, long end);
    }





}