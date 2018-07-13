package nars.concept.util;

import jcog.TODO;
import jcog.pri.bag.Bag;
import nars.Op;
import nars.Task;
import nars.concept.Concept;
import nars.concept.NodeConcept;
import nars.concept.Operator;
import nars.concept.TaskConcept;
import nars.concept.dynamic.DynamicTruthBeliefTable;
import nars.concept.dynamic.DynamicTruthModel;
import nars.link.TermLinker;
import nars.subterm.Subterms;
import nars.table.BeliefTable;
import nars.table.EternalTable;
import nars.table.QuestionTable;
import nars.table.TemporalBeliefTable;
import nars.term.*;
import nars.term.compound.util.Image;
import org.eclipse.collections.impl.map.mutable.ConcurrentHashMap;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Predicate;

import static nars.Op.*;

/**
 * Created by me on 3/23/16.
 */
public abstract class ConceptBuilder implements BiFunction<Term, Termed, Termed> {

    protected final Map<Term, Conceptor> conceptors = new ConcurrentHashMap<>();

    public abstract QuestionTable questionTable(Term term, boolean questionOrQuest);

    public abstract BeliefTable newTable(Term t, boolean beliefOrGoal);

    public abstract EternalTable newEternalTable(Term c);

    public abstract TemporalBeliefTable newTemporalTable(Term c);

    public abstract Bag[] newLinkBags(Term term);

    public Concept taskConcept(final Term t) {
        DynamicTruthModel dmt = ConceptBuilder.dynamicModel(t);
        if (dmt != null) {

            return new TaskConcept(t,

                    //belief table
                    new DynamicTruthBeliefTable(t, newEternalTable(t), newTemporalTable(t), dmt, true),

                    //goal table
                    goalable(t) ?
                            new DynamicTruthBeliefTable(t, newEternalTable(t), newTemporalTable(t), dmt, false) :
                            BeliefTable.Empty,

                    this);

        } else {
            Term conceptor = Functor.func(t);
            if (conceptor != Op.Null) {
                @Nullable Conceptor cc = conceptors.get(conceptor);
                if (cc instanceof Conceptor) {

                    Concept x = cc.apply(conceptor, Operator.args(t));
                    if (x != null)
                        return (TaskConcept) x;
                }
            }

            return new TaskConcept(t, this);
        }
    }

    public abstract NodeConcept nodeConcept(final Term t);

    public void on(Conceptor c) {
        conceptors.put(c.term, c);
    }


    public static final Predicate<Term> validDynamicSubterm = x -> Task.validTaskTerm(x.unneg());


    public static boolean validDynamicSubterms(Subterms subterms) {
        return subterms.AND(validDynamicSubterm);
    }

    /**
     * returns the builder for the term, or null if the term is not dynamically truthable
     */
    @Nullable
    public static DynamicTruthModel dynamicModel(Term t) {

        if (t.hasAny(Op.VAR_QUERY.bit))
            return null; //TODO maybe this can answer query questions by index query

        switch (t.op()) {

            case INH:
                return dynamicInh(t);

            case SIM:
                //TODO NAL2 set identities?
                break;

                //TODO not done yet
            case IMPL:
                Term subj = t.sub(0);
                /* TODO:
                    ((&&,x,y,z,...) ==> z) from (x ==> z) and (y ==> z) //intersect pre
                    (--(--x && --y) ==> z) from (x ==> z) and (y ==> z) //union pre
                    (z ==> (x && y))  //intersect conc
                    (z ==> --(--x && --y))  //union conc
                 */
                Term su = subj;
                if (su.op() == CONJ && validDynamicSubterms(su.subterms())) {
                    return DynamicTruthModel.IsectSubj;
                }
                //TODO if subj is negated
                break;

            case CONJ:
                if (validDynamicSubterms(t.subterms()))
                    return DynamicTruthModel.ConjIntersection;
                break;

            case DIFFe:
                if (validDynamicSubterms(t.subterms()))
                    return DynamicTruthModel.DiffRoot;
                break;

            case NEG:
                throw new RuntimeException("negation terms can not be conceptualized as something separate from that which they negate");
        }
        return null;
    }

    public static DynamicTruthModel dynamicInh(Term t) {

        //quick pre-test
        Subterms tt = t.subterms();
        if (!tt.hasAny(Op.SectBits | Op.DiffBits | Op.PROD.bit))
            return null;

        if ((tt.OR(s -> s.isAny(Op.SectBits | Op.DiffBits)))) {


            DynamicTruthModel dmt = null;
            Term subj = tt.sub(0);
            Term pred = tt.sub(1);

            Op so = subj.op();
            Op po = pred.op();


            if ((so == Op.SECTi) || (so == Op.SECTe) || (so == Op.DIFFe)

            ) {

                //TODO move this to impl-specific test function
                Subterms subjsubs = subj.subterms();
                int s = subjsubs.subs();
//                Term[] x = new Term[s];
                for (int i = 0; i < s; i++) {
                    Term y;
                    if (!validDynamicSubterm.test(y = INH.the(subjsubs.sub(i), pred)))
                        return null;
//                    x[i] = y;
                }

                switch (so) {
                    case SECTi:
                        return DynamicTruthModel.IsectSubj;
                    case SECTe:
                        return DynamicTruthModel.UnionSubj;
                    case DIFFe:
                        return DynamicTruthModel.DiffSubj;
                }


            }


            if (((po == Op.SECTi) || (po == Op.SECTe) || (po == DIFFi))) {


                Compound cpred = (Compound) pred;
                int s = cpred.subs();
//                Term[] x = new Term[s];
                for (int i = 0; i < s; i++) {
                    Term y;
                    if (!validDynamicSubterm.test(y = INH.the(subj, cpred.sub(i))))
                        return null;
//                    x[i] = y;
                }

                switch (po) {
                    case SECTi:
                        return DynamicTruthModel.UnionPred;
                    case SECTe:
                        return DynamicTruthModel.IsectPred;
                    case DIFFi:
                        return DynamicTruthModel.DiffPred;
                }
            }
        }
        Term iNorm = Image.imageNormalize(t);
        if (!iNorm.equals(t)) {
            return DynamicTruthModel.ImageIdentity;
        }
        return null;
    }

    @Override
    public final Termed apply(Term x, Termed prev) {
        if (prev != null) {
            Concept c = ((Concept) prev);
            if (!c.isDeleted())
                return c;
        }

        return apply(x);
    }

    public final Termed apply(Term x) {

        x = x.the();
        if (x == null)
            throw new TODO(x + " is not a The");

        Concept c = Task.validTaskTerm(x) ? taskConcept(x) : nodeConcept(x);
        if (c == null)
            throw new NullPointerException("null Concept for term: " + x);

        start(c);

        return c;
    }

    /** called after constructing a new concept, or after a permanent concept has been installed */
    public void start(Concept c) {

    }

    abstract public TermLinker termlinker(Term term);

    /**
     * passes through terms without creating any concept anything
     */
    public static final ConceptBuilder NullConceptBuilder = new ConceptBuilder() {

        @Override
        public void on(Conceptor c) {
            throw new UnsupportedOperationException();
        }

        @Override
        public NodeConcept nodeConcept(Term t) {
            throw new UnsupportedOperationException();
        }

        @Override
        public TaskConcept taskConcept(Term t) {
            throw new UnsupportedOperationException();
        }

        @Override
        public TermLinker termlinker(Term term) {
            return TermLinker.NullLinker;
        }

        @Override
        public TemporalBeliefTable newTemporalTable(Term c) {
            return TemporalBeliefTable.Empty;
        }

        @Override
        public BeliefTable newTable(Term t, boolean beliefOrGoal) {
            return BeliefTable.Empty;
        }

        @Override
        public EternalTable newEternalTable(Term c) {
            return EternalTable.EMPTY;
        }

        @Override
        public QuestionTable questionTable(Term term, boolean questionOrQuest) {
            return QuestionTable.Empty;
        }

        @Override
        public Bag[] newLinkBags(Term term) {
            return new Bag[]{Bag.EMPTY, Bag.EMPTY};
        }
    };

}
