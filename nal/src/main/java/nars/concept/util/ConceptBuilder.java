package nars.concept.util;

import jcog.pri.bag.Bag;
import nars.Op;
import nars.Task;
import nars.concept.Concept;
import nars.concept.NodeConcept;
import nars.concept.Operator;
import nars.concept.TaskConcept;
import nars.link.TermLinker;
import nars.subterm.Subterms;
import nars.table.BeliefTables;
import nars.table.dynamic.DynamicTruthBeliefTable;
import nars.table.eternal.EternalTable;
import nars.table.question.QuestionTable;
import nars.table.temporal.TemporalBeliefTable;
import nars.term.*;
import nars.term.util.Image;
import nars.truth.dynamic.DynamicTruthModel;
import org.eclipse.collections.impl.map.mutable.ConcurrentHashMap;
import org.eclipse.collections.impl.map.mutable.UnifiedMap;
import org.jetbrains.annotations.NotNull;
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

    public abstract BeliefTables newTables(Term t, boolean beliefOrGoal);

    public abstract EternalTable newEternalTable(Term c);

    public abstract TemporalBeliefTable newTemporalTable(Term c);

    public abstract Bag[] newLinkBags(Term term);

    public Concept taskConcept(final Term t) {
        DynamicTruthModel dmt = ConceptBuilder.dynamicModel(t);
        if (dmt != null) {

            return new TaskConcept(t,

                    //belief table
                    newDynamicBeliefTable(t, dmt, true),

                    //goal table
                    goalable(t) ?
                            newDynamicBeliefTable(t, dmt, false) :
                            BeliefTables.Empty,

                    this);

        } else {
            Term conceptor = Functor.func(t);
            if (conceptor != Op.Null) {
                @Nullable Conceptor cc = conceptors.get(conceptor);
                if (cc instanceof Conceptor) {

                    Concept x = cc.apply(conceptor, Operator.args(t));
                    if (x != null)
                        return x;
                }
            }

            return new TaskConcept(t, this);
        }
    }

    @NotNull
    public BeliefTables newDynamicBeliefTable(Term t, DynamicTruthModel dmt, boolean beliefOrGoal) {
        return new BeliefTables(
                new DynamicTruthBeliefTable(t, dmt, beliefOrGoal),
                newEternalTable(t),
                newTemporalTable(t)
        );
    }

    public abstract NodeConcept nodeConcept(final Term t);

    public void on(Conceptor c) {
        conceptors.put(c.term, c);
    }


    public static final Predicate<Term> validDynamicSubterm = x -> Task.taskConceptTerm(x.unneg());


    public static boolean validDynamicSubterms(Subterms subterms) {
        return subterms.AND(validDynamicSubterm);
    }
    public static boolean validDynamicSubtermsAndNoSharedVars(Term conj) {
        Subterms conjSubterms = conj.subterms();
        if (validDynamicSubterms(conjSubterms)) {
            if (conjSubterms.hasAny(VAR_DEP)) {
                Map<Term,Term> varLocations = new UnifiedMap(conjSubterms.subs());

                return conj.eventsWhile((when,event) -> {
                   if (event.hasAny(VAR_DEP)) {
                       boolean valid = event.recurseTerms((Compound x)->x.hasAny(VAR_DEP), (Term possiblyVar, Compound parent) -> {
                           if (possiblyVar.op()==VAR_DEP)
                               if (varLocations.putIfAbsent(possiblyVar, event)!=null)
                                   return false;
                           return true;
                       }, null);
                       if (!valid)
                           return false;
                   }
                   return true;
                }, 0, true, true, true, 0);
            }
            return true;
        }
        return false;
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
            case IMPL: {
                Term su = t.sub(0);
                if (su.hasAny(Op.VAR_INDEP))
                    return null;
                Term pu = t.sub(1);
                if (pu.hasAny(Op.VAR_INDEP))
                    return null;

                Op suo = su.op();
                //subject has special negation union case
                boolean subjDyn = (
                    suo == CONJ && validDynamicSubtermsAndNoSharedVars(su)
                        ||
                    suo == NEG && (su.unneg().op()==CONJ && validDynamicSubtermsAndNoSharedVars(su.unneg()))
                );
                boolean predDyn = (pu.op() == CONJ && validDynamicSubtermsAndNoSharedVars(pu));


                if (subjDyn && predDyn) {
                    //choose the simpler to dynamically calculate for
                    if (su.volume() <= pu.volume()) {
                        predDyn = false; //dyn subj
                    } else {
                        subjDyn = false; //dyn pred
                    }
                }

                if (subjDyn) {
                    if (suo==NEG) {
                        return DynamicTruthModel.DynamicSectTruth.UnionSubj;
                    } else {
                        return DynamicTruthModel.DynamicSectTruth.IsectSubj;
                    }
                } else if (predDyn) {
                    return DynamicTruthModel.DynamicSectTruth.IsectPred;
                }

                break;
            }

            case CONJ:
                if (validDynamicSubtermsAndNoSharedVars(t))
                    return DynamicTruthModel.DynamicConjTruth.ConjIntersection;
                break;

            case DIFFe:
                if (validDynamicSubterms(t.subterms()))
                    return DynamicTruthModel.DynamicDiffTruth.DiffRoot;
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


            if ((so == Op.SECTi) || (so == Op.SECTe) || (so == Op.DIFFe)) {

                //TODO move this to impl-specific test function
                Subterms subjsubs = subj.subterms();
                int s = subjsubs.subs();
                for (int i = 0; i < s; i++) {
                    if (!validDynamicSubterm.test(INH.the(subjsubs.sub(i), pred)))
                        return null;
                }

                switch (so) {
                    case SECTi:
                        return DynamicTruthModel.DynamicSectTruth.IsectSubj;
                    case SECTe:
                        return DynamicTruthModel.DynamicSectTruth.UnionSubj;
                    case DIFFe:
                        return DynamicTruthModel.DynamicDiffTruth.DiffSubj;
                }


            }


            if (((po == Op.SECTi) || (po == Op.SECTe) || (po == DIFFi))) {


                Compound cpred = (Compound) pred;
                int s = cpred.subs();
                for (int i = 0; i < s; i++) {
                    if (!validDynamicSubterm.test(INH.the(subj, cpred.sub(i))))
                        return null;
                }

                switch (po) {
                    case SECTi:
                        return DynamicTruthModel.DynamicSectTruth.UnionPred;
                    case SECTe:
                        return DynamicTruthModel.DynamicSectTruth.IsectPred;
                    case DIFFi:
                        return DynamicTruthModel.DynamicDiffTruth.DiffPred;
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

        Concept c = Task.taskConceptTerm(x) ? taskConcept(x) : nodeConcept(x);
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
            return null;
        }

        @Override
        public BeliefTables newTables(Term t, boolean beliefOrGoal) {
            return BeliefTables.Empty;
        }

        @Override
        public EternalTable newEternalTable(Term c) {
            return null;
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
